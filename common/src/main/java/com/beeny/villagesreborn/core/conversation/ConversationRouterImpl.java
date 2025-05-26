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

/**
 * Enhanced ConversationRouter implementation that routes validated conversations 
 * to appropriate villagers and manages response flow asynchronously
 */
public class ConversationRouterImpl implements ConversationRouter {
    
    private static final double CONVERSATION_RANGE = 8.0;
    
    private final VillagerProximityDetector proximityDetector;
    private final VillagerBrainManager brainManager;
    private final ResponseDeliveryManager responseManager;
    private final LLMApiClient llmApiClient;
    private final ExecutorService conversationExecutor;
    
    public ConversationRouterImpl(VillagerProximityDetector proximityDetector,
                                 VillagerBrainManager brainManager,
                                 ResponseDeliveryManager responseManager,
                                 LLMApiClient llmApiClient) {
        this.proximityDetector = proximityDetector;
        this.brainManager = brainManager;
        this.responseManager = responseManager;
        this.llmApiClient = llmApiClient;
        this.conversationExecutor = Executors.newFixedThreadPool(10);
    }
    
    @Override
    public void routeMessage(Player player, VillagerEntity villager, String message) {
        // Legacy method - convert to new format
        ConversationContext context = new ConversationContext(
            player, message, System.currentTimeMillis(), "overworld"
        );
        
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
        
        // Process each villager asynchronously to prevent blocking
        for (VillagerEntity villager : nearbyVillagers) {
            conversationExecutor.submit(() -> {
                processVillagerResponse(villager, context);
            });
        }
    }
    
    private void processVillagerResponse(VillagerEntity villager, ConversationContext context) {
        try {
            VillagerBrain brain = brainManager.getBrain(villager);
            String response = generateResponse(brain, context);
            
            if (response != null && !response.trim().isEmpty()) {
                responseManager.deliverResponse(villager, context.getSender(), response);
                brain.addInteraction(context.getSender(), context.getMessage(), response);
                brainManager.saveBrain(villager, brain);
            }
        } catch (Exception e) {
            System.err.println("Failed to process villager response: " + e.getMessage());
        }
    }
    
    private String generateResponse(VillagerBrain brain, ConversationContext context) {
        try {
            // Build prompt using enhanced PromptBuilder
            ConversationRequest request = buildPromptRequest(brain, context);
            
            // Get LLM response
            CompletableFuture<ConversationResponse> future = llmApiClient.generateConversationResponse(request);
            ConversationResponse response = future.get();
            
            if (response.isSuccess()) {
                return response.getResponse();
            } else {
                System.err.println("LLM request failed: " + response.getErrorMessage());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error generating LLM response: " + e.getMessage());
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
}