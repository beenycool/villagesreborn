package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.ai.VillagerBrainManager;
import com.beeny.villagesreborn.core.ai.VillagerProximityDetector;
import com.beeny.villagesreborn.core.ai.PromptBuilder;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.llm.LLMApiClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced ConversationRouter implementation that routes validated conversations 
 * to appropriate villagers and manages response flow asynchronously.
 * Optimized to reduce performance impact by limiting concurrent responses and caching.
 */
public class ConversationRouterImpl implements ConversationRouter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationRouterImpl.class);
    
    private static final double CONVERSATION_RANGE = 8.0;
    private static final int MAX_CONCURRENT_RESPONSES = 3; // Limit concurrent villager responses
    private static final long CONVERSATION_COOLDOWN_MS = 5000; // 5 second cooldown per villager
    private static final long RESPONSE_CACHE_DURATION_MS = 10000; // Cache responses for 10 seconds
    
    private final VillagerProximityDetector proximityDetector;
    private final VillagerBrainManager brainManager;
    private final ResponseDeliveryManager responseManager;
    private final LLMApiClient llmApiClient;
    private final ExecutorService conversationExecutor;
    
    // Performance optimization caches
    private final Map<UUID, Long> villagerLastResponse = new ConcurrentHashMap<>();
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    
    public ConversationRouterImpl(VillagerProximityDetector proximityDetector,
                                 VillagerBrainManager brainManager,
                                 ResponseDeliveryManager responseManager,
                                 LLMApiClient llmApiClient) {
        this.proximityDetector = proximityDetector;
        this.brainManager = brainManager;
        this.responseManager = responseManager;
        this.llmApiClient = llmApiClient;
        this.conversationExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_RESPONSES);
    }
    
    @Override
    public void routeMessage(Player player, VillagerEntity villager, String message) {
        // Legacy method - convert to new format
        ConversationContext context = new ConversationContext(
            player, message, System.currentTimeMillis(), "overworld"
        );
        
        // Check cooldown before processing
        if (isVillagerOnCooldown(villager.getUUID())) {
            LOGGER.debug("Villager {} is on conversation cooldown", villager.getUUID());
            return;
        }
        
        // Process single villager asynchronously
        conversationExecutor.submit(() -> {
            processVillagerResponse(villager, context);
        });
    }
    
    @Override
    public void routeConversation(ConversationContext context) {
        List<VillagerEntity> nearbyVillagers = proximityDetector
            .findNearbyVillagers(context.getSender().getBlockPos(), (int) CONVERSATION_RANGE);
        
        if (nearbyVillagers.isEmpty()) {
            return;
        }
        
        // Filter villagers that are not on cooldown and limit the number
        List<VillagerEntity> eligibleVillagers = nearbyVillagers.stream()
            .filter(villager -> !isVillagerOnCooldown(villager.getUUID()))
            .limit(MAX_CONCURRENT_RESPONSES) // Limit to reduce performance impact
            .collect(Collectors.toList());
        
        if (eligibleVillagers.isEmpty()) {
            LOGGER.debug("No eligible villagers for conversation (all on cooldown)");
            return;
        }
        
        LOGGER.debug("Processing conversation for {} eligible villagers", eligibleVillagers.size());
        
        // Process each eligible villager asynchronously
        for (VillagerEntity villager : eligibleVillagers) {
            conversationExecutor.submit(() -> {
                processVillagerResponse(villager, context);
            });
        }
    }
    
    /**
     * Checks if a villager is on conversation cooldown.
     */
    private boolean isVillagerOnCooldown(UUID villagerUUID) {
        Long lastResponse = villagerLastResponse.get(villagerUUID);
        if (lastResponse == null) {
            return false;
        }
        return System.currentTimeMillis() - lastResponse < CONVERSATION_COOLDOWN_MS;
    }
    
    /**
     * Records that a villager has responded to a conversation.
     */
    private void recordVillagerResponse(UUID villagerUUID) {
        villagerLastResponse.put(villagerUUID, System.currentTimeMillis());
        
        // Clean up old entries periodically
        if (villagerLastResponse.size() > 100) {
            cleanupCooldownCache();
        }
    }
    
    /**
     * Removes expired cooldown entries.
     */
    private void cleanupCooldownCache() {
        long currentTime = System.currentTimeMillis();
        villagerLastResponse.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > CONVERSATION_COOLDOWN_MS);
    }
    
    private void processVillagerResponse(VillagerEntity villager, ConversationContext context) {
        try {
            VillagerBrain brain = brainManager.getBrain(villager);
            
            // Check for cached response first
            String cacheKey = generateResponseCacheKey(villager, context);
            CachedResponse cached = responseCache.get(cacheKey);
            
            String response;
            if (cached != null && System.currentTimeMillis() - cached.timestamp < RESPONSE_CACHE_DURATION_MS) {
                LOGGER.debug("Using cached response for villager {}", villager.getUUID());
                response = cached.response;
            } else {
                response = generateResponse(brain, context);
                
                // Cache the response if it's valid
                if (response != null && !response.trim().isEmpty()) {
                    responseCache.put(cacheKey, new CachedResponse(response, System.currentTimeMillis()));
                    
                    // Clean up cache periodically
                    if (responseCache.size() > 50) {
                        cleanupResponseCache();
                    }
                }
            }
            
            if (response != null && !response.trim().isEmpty()) {
                recordVillagerResponse(villager.getUUID());
                responseManager.deliverResponse(villager, context.getSender(), response);
                brain.addInteraction(context.getSender(), context.getMessage(), response);
                brainManager.saveBrain(villager, brain);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process villager response for {}: {}", villager.getUUID(), e.getMessage());
        }
    }
    
    /**
     * Generates a cache key for responses based on villager and context.
     */
    private String generateResponseCacheKey(VillagerEntity villager, ConversationContext context) {
        return String.format("%s_%s_%d", 
            villager.getUUID().toString(),
            context.getMessage().toLowerCase().trim(),
            context.getMessage().length() / 10); // Group by message length ranges
    }
    
    /**
     * Removes expired response cache entries.
     */
    private void cleanupResponseCache() {
        long currentTime = System.currentTimeMillis();
        responseCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > RESPONSE_CACHE_DURATION_MS);
    }
    
    private String generateResponse(VillagerBrain brain, ConversationContext context) {
        try {
            // Build prompt using enhanced PromptBuilder
            ConversationRequest request = buildPromptRequest(brain, context);
            
            // Get LLM response with timeout
            CompletableFuture<ConversationResponse> future = llmApiClient.generateConversationResponse(request);
            ConversationResponse response = future.get(3, java.util.concurrent.TimeUnit.SECONDS); // 3 second timeout
            
            if (response.isSuccess()) {
                return response.getResponse();
            } else {
                LOGGER.warn("LLM request failed: {}", response.getErrorMessage());
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error generating LLM response: {}", e.getMessage());
            return null;
        }
    }
    
    private ConversationRequest buildPromptRequest(VillagerBrain brain, ConversationContext context) {
        String systemPrompt = buildSystemPrompt(brain, context);
        String userPrompt = buildUserPrompt(context);
        String combinedPrompt = systemPrompt + "\n\n" + userPrompt;
        
        return ConversationRequest.builder()
            .prompt(combinedPrompt)
            .maxTokens(150)
            .temperature(0.8f)
            .build();
    }
    
    private String buildSystemPrompt(VillagerBrain brain, ConversationContext context) {
        String personalityDesc = brain.getPersonalityTraits().generateDescription();
        String moodDesc = brain.getCurrentMood().getOverallMood().toString();
        String playerName = context.getSender().getName();
        String villagerName = "Villager"; // Will be set by actual villager entity
        String profession = "Villager"; // Will be set by actual villager entity
        
        return String.format("""
            You are %s, a %s villager in a Minecraft village.
            Personality: %s
            Current mood: %s
            
            Guidelines:
            - Respond as this specific villager would
            - Keep responses brief (1-2 sentences)
            - Reference village life when relevant
            - Show personality through speech patterns
            - Express emotion appropriate to mood
            - Use minecraft-appropriate language
            - No modern references
            """, villagerName, profession, personalityDesc, moodDesc);
    }
    
    private String buildUserPrompt(ConversationContext context) {
        return String.format("Player %s says: \"%s\"", 
            context.getSender().getName(), context.getMessage());
    }
    
    public void shutdown() {
        if (conversationExecutor != null && !conversationExecutor.isShutdown()) {
            conversationExecutor.shutdown();
        }
    }
    
    /**
     * Clears all cached data (e.g., on world unload).
     */
    public void clearAllCaches() {
        villagerLastResponse.clear();
        responseCache.clear();
    }
    
    /**
     * Cache entry for conversation responses.
     */
    private static class CachedResponse {
        final String response;
        final long timestamp;
        
        CachedResponse(String response, long timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
    }
}