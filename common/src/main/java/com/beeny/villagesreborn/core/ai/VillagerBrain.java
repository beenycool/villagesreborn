package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.conversation.ConversationContext;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.NBTCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced VillagerBrain implementation with PromptBuilder integration and rich memory support.
 * This class manages a villager's personality, mood, memories, and relationships, with optimizations
 * to reduce the performance impact of complex AI calculations.
 */
public class VillagerBrain {
    public static final int MAX_SHORT_TERM_MEMORY = 50;
    public static final int DEFAULT_MEMORY_LIMIT = 150;
    private static final long MOOD_DECAY_INTERVAL_MS = 60000; // 1 minute
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerBrain.class);
    
    private final UUID villagerUUID;
    private int memoryLimit = DEFAULT_MEMORY_LIMIT;
    private PersonalityProfile personalityTraits;
    private MoodState currentMood;
    private ConversationHistory shortTermMemory;
    private MemoryBank longTermMemory; // Enhanced MemoryBank with rich narratives
    private Map<UUID, RelationshipData> relationshipMap;
    private LLMApiClient llmApiClient;
    private PromptBuilder promptBuilder;
    private String villagerName = "Unknown";
    private String profession = "Unemployed";
    private String villageName = "Unknown Village";
    private long lastInteractionTime = 0;
    private long conversationCooldown = 0;
    private long lastDecayTime = System.currentTimeMillis();

    public VillagerBrain(UUID villagerUUID) {
        this.villagerUUID = villagerUUID;
        this.personalityTraits = new PersonalityProfile();
        this.currentMood = new MoodState();
        this.shortTermMemory = new ConversationHistory();
        this.longTermMemory = new MemoryBank();
        this.relationshipMap = new HashMap<>();
        this.promptBuilder = new PromptBuilder();
    }

    public UUID getVillagerUUID() { return villagerUUID; }
    public PersonalityProfile getPersonalityTraits() { return personalityTraits; }
    public MoodState getCurrentMood() { return currentMood; }
    public ConversationHistory getShortTermMemory() { return shortTermMemory; }
    public MemoryBank getLongTermMemory() { return longTermMemory; }
    /**
     * @deprecated Use {@link #getLongTermMemory()} instead.
     */
    @Deprecated
    public MemoryBank getMemoryBank() { return longTermMemory; }
    public Map<UUID, RelationshipData> getRelationshipMap() { return relationshipMap; }
    public String getVillagerName() { return villagerName; }
    public String getProfession() { return profession; }
    public String getVillageName() { return villageName; }

    public void setVillagerName(String name) { this.villagerName = name; }
    public void setProfession(String profession) { this.profession = profession; }
    public void setVillageName(String villageName) { this.villageName = villageName; }
    
    public int getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(int memoryLimit) { 
        this.memoryLimit = Math.max(50, Math.min(500, memoryLimit)); // Clamp to valid range
        // Apply memory limit to memory bank
        if (longTermMemory != null) {
            longTermMemory.setMemoryLimit(memoryLimit);
        }
    }

    public void setLLMApiClient(LLMApiClient apiClient) {
        this.llmApiClient = apiClient;
    }

    public void setConversationCooldown(long cooldown) {
        this.conversationCooldown = cooldown;
    }

    /**
     * Updates the last interaction time to the current system time.
     * This is used for managing conversation cooldowns.
     */
    public void updateLastInteractionTime() {
        this.lastInteractionTime = System.currentTimeMillis();
    }

    /**
     * Processes a message from a player, generates a response, and updates the villager's internal state.
     * This method is optimized to cache sentiment analysis and handle conversation cooldowns.
     *
     * @param player   The player interacting with the villager.
     * @param message  The message from the player.
     * @param context  The current conversation context.
     * @return A {@link CompletableFuture} containing the villager's response, or null if on cooldown or an error occurs.
     */
    public CompletableFuture<String> processMessage(Player player, String message, ConversationContext context) {
        if (System.currentTimeMillis() - lastInteractionTime < conversationCooldown) {
            return CompletableFuture.completedFuture(null); // Return empty future for cooldown
        }

        if (llmApiClient == null) {
            return CompletableFuture.completedFuture(null); // No LLM client available
        }
        
        // Cache sentiment analysis to avoid re-computing it for the same message
        float sentimentChange = calculateSentimentChange(message);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build comprehensive prompt using enhanced PromptBuilder
                String fullPrompt = promptBuilder.buildPrompt(this, context);
                
                // Add the current player message to the prompt
                String finalPrompt = fullPrompt.replace("[Waiting for input]", message);
                
                // Build request with enhanced prompt
                ConversationRequest request = ConversationRequest.builder()
                    .prompt(finalPrompt)
                    .build();

                // Get response
                ConversationResponse response = llmApiClient.generateConversationResponse(request).get();

                if (!response.isSuccess()) {
                    return null;
                }

                // Store interaction in both short-term memory and enhanced long-term memory
                ConversationInteraction interaction = new ConversationInteraction(
                    System.currentTimeMillis(),
                    player.getUUID(),
                    message,
                    response.getResponse(),
                    currentMood.copy(),
                    context.getLocation()
                );
                shortTermMemory.addInteraction(interaction);
                
                // Record detailed interaction in enhanced memory bank
                recordDetailedPlayerInteraction(player, message, response.getResponse(), context, sentimentChange);

                // Update relationship and mood
                updateRelationship(player, sentimentChange);
                updateMood(sentimentChange);

                updateLastInteractionTime();
                return response.getResponse();
            } catch (Exception e) {
                LOGGER.error("Error processing message for villager {}: {}", villagerUUID, e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Records a detailed player interaction in the enhanced memory bank.
     *
     * @param player          The player who interacted.
     * @param message         The player's message.
     * @param response        The villager's response.
     * @param context         The conversation context.
     * @param sentimentChange The calculated sentiment change from the interaction.
     */
    private void recordDetailedPlayerInteraction(Player player, String message, String response, ConversationContext context, float sentimentChange) {
        // Record the detailed interaction
        longTermMemory.recordPlayerInteraction(
            player.getUUID(),
            player.getName(),
            determineInteractionType(message),
            message,
            response,
            sentimentChange
        );
    }
    
    /**
     * Calculates sentiment change from player interaction
     */
    private float calculateSentimentChange(String message) {
        // Simple sentiment analysis - could be enhanced with more sophisticated methods
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("thank") || lowerMessage.contains("please") || lowerMessage.contains("help")) {
            return 0.2f; // Positive
        } else if (lowerMessage.contains("stupid") || lowerMessage.contains("hate") || lowerMessage.contains("bad")) {
            return -0.3f; // Negative
        } else if (lowerMessage.contains("hello") || lowerMessage.contains("hi") || lowerMessage.contains("good")) {
            return 0.1f; // Slightly positive
        }
        
        return 0.0f; // Neutral
    }
    
    /**
     * Determines the type of interaction based on message content
     */
    private String determineInteractionType(String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("trade") || lowerMessage.contains("buy") || lowerMessage.contains("sell")) {
            return "trade";
        } else if (lowerMessage.contains("help") || lowerMessage.contains("quest")) {
            return "request_help";
        } else if (lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "greeting";
        } else if (lowerMessage.contains("?")) {
            return "question";
        } else {
            return "conversation";
        }
    }

    public RelationshipData getRelationship(Player player) {
        return relationshipMap.computeIfAbsent(player.getUUID(), uuid -> new RelationshipData(uuid));
    }

    /**
     * Updates the relationship with a player based on the sentiment of an interaction.
     *
     * @param player          The player to update the relationship with.
     * @param sentimentChange The sentiment change from the interaction.
     */
    public void updateRelationship(Player player, float sentimentChange) {
        RelationshipData relationship = getRelationship(player);
        relationship.incrementInteractionCount();
        relationship.adjustTrust(sentimentChange * 0.5f);
        relationship.adjustFriendship(sentimentChange);
    }

    /**
     * Updates the villager's mood based on the sentiment of an interaction.
     *
     * @param sentimentChange The sentiment change from the interaction.
     */
    public void updateMood(float sentimentChange) {
        // Simple mood adjustment - could be more sophisticated
        currentMood.setLastUpdated(System.currentTimeMillis());
    }

    /**
     * Decays the villager's mood over time. This method is designed to be called periodically.
     */
    public void decayMood() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDecayTime >= MOOD_DECAY_INTERVAL_MS) {
            long timeElapsed = currentTime - lastDecayTime;
            currentMood.decay(timeElapsed);
            lastDecayTime = currentTime;
        }
    }

    public void cleanupOldMemories(long threshold) {
        shortTermMemory.cleanup(threshold, MAX_SHORT_TERM_MEMORY);
    }

    public boolean isValid() {
        return villagerUUID != null && personalityTraits != null && currentMood != null;
    }

    public NBTCompound toNBT() {
        NBTCompound nbt = new NBTCompound();
        nbt.putUUID("villagerUUID", villagerUUID);
        nbt.putInt("memoryLimit", memoryLimit);
        nbt.putString("villagerName", villagerName);
        nbt.putString("profession", profession);
        nbt.putString("villageName", villageName);
        nbt.putLong("lastInteractionTime", lastInteractionTime);
        nbt.putLong("conversationCooldown", conversationCooldown);
        nbt.putLong("lastDecayTime", lastDecayTime);

        if (personalityTraits != null) {
            nbt.put("personality", personalityTraits.toNBT());
        }
        if (currentMood != null) {
            nbt.put("mood", currentMood.toNBT());
        }
        if (shortTermMemory != null) {
            nbt.put("shortTermMemory", shortTermMemory.toNBT());
        }
        if (longTermMemory != null) {
            nbt.put("longTermMemory", longTermMemory.toNBT());
        }
        
        NBTCompound relationshipsNBT = new NBTCompound();
        for (Map.Entry<UUID, RelationshipData> entry : relationshipMap.entrySet()) {
            relationshipsNBT.put(entry.getKey().toString(), entry.getValue().toNBT());
        }
        nbt.put("relationshipMap", relationshipsNBT);

        return nbt;
    }

    public static VillagerBrain fromNBT(NBTCompound nbt) {
        UUID villagerUUID = nbt.getUUID("villagerUUID");
        VillagerBrain brain = new VillagerBrain(villagerUUID);

        brain.setMemoryLimit(nbt.contains("memoryLimit") ? nbt.getInt("memoryLimit") : DEFAULT_MEMORY_LIMIT);
        brain.setVillagerName(nbt.contains("villagerName") ? nbt.getString("villagerName") : "Unknown");
        brain.setProfession(nbt.contains("profession") ? nbt.getString("profession") : "Unemployed");
        brain.setVillageName(nbt.contains("villageName") ? nbt.getString("villageName") : "Unknown Village");
        brain.lastInteractionTime = nbt.contains("lastInteractionTime") ? nbt.getLong("lastInteractionTime") : 0;
        brain.setConversationCooldown(nbt.contains("conversationCooldown") ? nbt.getLong("conversationCooldown") : 0);
        brain.lastDecayTime = nbt.contains("lastDecayTime") ? nbt.getLong("lastDecayTime") : System.currentTimeMillis();

        if (nbt.contains("personality")) {
            brain.personalityTraits = PersonalityProfile.fromNBT(nbt.getCompound("personality"));
        }
        if (nbt.contains("mood")) {
            brain.currentMood = MoodState.fromNBT(nbt.getCompound("mood"));
        }
        if (nbt.contains("shortTermMemory")) {
            brain.shortTermMemory = ConversationHistory.fromNBT(nbt.getCompound("shortTermMemory"));
        }
        if (nbt.contains("longTermMemory")) {
            NBTCompound ltmNBT = nbt.getCompound("longTermMemory");
            if (ltmNBT != null) {
                brain.longTermMemory = MemoryBank.fromNBT(ltmNBT);
            }
        }

        if (nbt.contains("relationshipMap")) {
            NBTCompound relationshipsNBT = nbt.getCompound("relationshipMap");
            if (relationshipsNBT != null) {
                for (String key : relationshipsNBT.getData().keySet()) {
                    UUID playerUUID = UUID.fromString(key);
                    NBTCompound relationshipNBT = relationshipsNBT.getCompound(key);
                    if (relationshipNBT != null) {
                        RelationshipData data = RelationshipData.fromNBT(relationshipNBT);
                        brain.relationshipMap.put(playerUUID, data);
                    }
                }
            }
        }

        return brain;
    }

    // Enhanced memory methods
    
    /**
     * Record a personal story in the villager's memory
     */
    public void recordPersonalStory(String title, String narrative, List<String> keyCharacters, String emotionalTone, boolean isOngoing) {
        com.beeny.villagesreborn.core.ai.memory.EmotionalTone tone;
        try {
            tone = com.beeny.villagesreborn.core.ai.memory.EmotionalTone.valueOf(emotionalTone.toUpperCase());
        } catch (IllegalArgumentException e) {
            tone = com.beeny.villagesreborn.core.ai.memory.EmotionalTone.NEUTRAL;
        }
        
        longTermMemory.addPersonalStory(title, narrative, keyCharacters, tone, isOngoing);
    }
    
    /**
     * Record a village event that this villager witnessed or participated in
     */
    public void recordVillageEvent(String eventType, String description, List<UUID> participants, String impactLevel) {
        com.beeny.villagesreborn.core.ai.memory.EventImpact impact;
        try {
            impact = com.beeny.villagesreborn.core.ai.memory.EventImpact.valueOf(impactLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            impact = com.beeny.villagesreborn.core.ai.memory.EventImpact.NEUTRAL;
        }
        
        longTermMemory.recordVillageEvent(eventType, description, participants, impact);
    }
    
    /**
     * Record a traumatic event that may affect future behavior
     */
    public void recordTrauma(String description, String trigger, float severity, List<UUID> involvedEntities) {
        longTermMemory.recordTrauma(description, trigger, severity, involvedEntities);
    }
    
    /**
     * Record a joyful memory that can improve mood
     */
    public void recordJoy(String description, List<UUID> sharedWith, float happinessLevel) {
        longTermMemory.recordJoyfulMemory(description, sharedWith, happinessLevel);
    }
    
    /**
     * Generate a life story summary for this villager
     */
    public String generateLifeStory() {
        return longTermMemory.generateLifeStorySummary();
    }
    
    /**
     * Check if this villager holds a grudge against a specific entity
     */
    public boolean holdsGrudgeAgainst(UUID entityUUID) {
        return longTermMemory.holdsGrudge(entityUUID);
    }

    // Legacy methods (maintained for backward compatibility)
    
    /**
     * Handle message from PlayerEntity (alias for processMessage)
     * @param player PlayerEntity (using Player as the base type)
     * @param message The message from the player
     * @return Response string
     */
    public CompletableFuture<String> handleMessage(Player player, String message) {
        ConversationContext defaultContext = getCurrentConversationContext();
        return processMessage(player, message, defaultContext);
    }
    
    /**
     * Record a memory entry in short-term memory
     * @param entry Memory entry to record
     */
    public void recordMemory(String entry) {
        // Create a simple interaction for the memory entry
        ConversationInteraction interaction = new ConversationInteraction(
            System.currentTimeMillis(),
            null, // No specific player for general memory
            "",   // No player message
            entry, // Store as villager response/thought
            currentMood.copy(),
            null  // No specific location
        );
        shortTermMemory.addInteraction(interaction);
        
        // Also add to long-term memory as a significant memory
        longTermMemory.addSignificantMemory(entry);
    }
    
    /**
     * Get conversation history as list of strings
     * @return List of conversation strings
     */
    public List<String> getConversationHistory() {
        List<String> history = new ArrayList<>();
        for (ConversationInteraction interaction : shortTermMemory.getInteractions()) {
            String playerMsg = interaction.getPlayerMessage();
            String villagerMsg = interaction.getVillagerResponse();
            
            if (playerMsg != null && !playerMsg.isEmpty()) {
                history.add("Player: " + playerMsg);
            }
            if (villagerMsg != null && !villagerMsg.isEmpty()) {
                history.add("Villager: " + villagerMsg);
            }
        }
        return history;
    }
    
    /**
     * Update mood state
     * @param mood New mood state
     */
    public void updateMood(MoodState mood) {
        this.currentMood = mood;
    }
    
    /**
     * Get current mood state
     * @return Current mood
     */
    public MoodState getMood() {
        return currentMood;
    }
    
    /**
     * Update relationship data for a player
     * @param playerId Player UUID
     * @param data Relationship data
     */
    public void updateRelationship(UUID playerId, RelationshipData data) {
        relationshipMap.put(playerId, data);
    }
    
    /**
     * Get relationship data for a player by UUID
     * @param playerId Player UUID
     * @return Relationship data or null if not found
     */
    public RelationshipData getRelationship(UUID playerId) {
        return relationshipMap.get(playerId);
    }
    
    /**
     * Add an interaction to the conversation history
     * @param player the player in the interaction
     * @param playerMessage the player's message
     * @param villagerResponse the villager's response
     */
    public void addInteraction(Player player, String playerMessage, String villagerResponse) {
        ConversationInteraction interaction = new ConversationInteraction(
            System.currentTimeMillis(),
            player.getUUID(),
            playerMessage,
            villagerResponse,
            currentMood.copy(),
            "Village" // Default location
        );
        shortTermMemory.addInteraction(interaction);
        
        // Also record in enhanced memory bank
        recordDetailedPlayerInteraction(player, playerMessage, villagerResponse, getCurrentConversationContext(), calculateSentimentChange(playerMessage));
    }
    
    /**
     * Retrieve the current conversation context.
     * @return the current ConversationContext
     */
    private ConversationContext getCurrentConversationContext() {
        ConversationContext context = new ConversationContext();
        context.setLocation("Village");
        return context;
    }
}