package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.conversation.ConversationContext;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.NBTCompound;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced VillagerBrain implementation with PromptBuilder integration and rich memory support
 */
public class VillagerBrain {
    public static final int MAX_SHORT_TERM_MEMORY = 50;
    
    private final UUID villagerUUID;
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
    public MemoryBank getMemoryBank() { return longTermMemory; } // Enhanced access method
    public Map<UUID, RelationshipData> getRelationshipMap() { return relationshipMap; }
    public String getVillagerName() { return villagerName; }
    public String getProfession() { return profession; }
    public String getVillageName() { return villageName; }

    public void setVillagerName(String name) { this.villagerName = name; }
    public void setProfession(String profession) { this.profession = profession; }
    public void setVillageName(String villageName) { this.villageName = villageName; }

    public void setLLMApiClient(LLMApiClient apiClient) {
        this.llmApiClient = apiClient;
    }

    public void setConversationCooldown(long cooldown) {
        this.conversationCooldown = cooldown;
    }

    public void updateLastInteractionTime() {
        this.lastInteractionTime = System.currentTimeMillis();
    }

    public String processMessage(Player player, String message, ConversationContext context) {
        // Check cooldown
        if (System.currentTimeMillis() - lastInteractionTime < conversationCooldown) {
            return null;
        }

        if (llmApiClient == null) {
            return null;
        }

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
            CompletableFuture<ConversationResponse> future = llmApiClient.generateConversationResponse(request);
            ConversationResponse response = future.get();

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
            recordDetailedPlayerInteraction(player, message, response.getResponse(), context);

            // Update relationship
            updateRelationship(player, message);

            // Update mood
            updateMood(player, message);

            updateLastInteractionTime();
            return response.getResponse();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Records a detailed player interaction in the enhanced memory bank
     */
    private void recordDetailedPlayerInteraction(Player player, String message, String response, ConversationContext context) {
        // Determine sentiment change based on message content and current mood
        float sentimentChange = calculateSentimentChange(message);
        
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

    public void updateRelationship(Player player, String message) {
        RelationshipData relationship = getRelationship(player);
        relationship.incrementInteractionCount();
        
        // Enhanced relationship update based on sentiment
        float sentimentChange = calculateSentimentChange(message);
        relationship.adjustTrust(sentimentChange * 0.5f);
        relationship.adjustFriendship(sentimentChange);
    }

    public void updateMood(Player player, String message) {
        // Enhanced mood update based on interaction
        float sentimentChange = calculateSentimentChange(message);
        // Simple mood adjustment - could be more sophisticated
        currentMood.setLastUpdated(System.currentTimeMillis());
    }

    public void decayMood(long timeElapsed) {
        // Simplified mood decay
        currentMood.decay(timeElapsed);
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
        nbt.put("personality", personalityTraits.toNBT());
        nbt.put("mood", currentMood.toNBT());
        nbt.put("shortTermMemory", shortTermMemory.toNBT());
        return nbt;
    }

    public static VillagerBrain fromNBT(NBTCompound nbt) {
        UUID uuid = nbt.getUUID("villagerUUID");
        VillagerBrain brain = new VillagerBrain(uuid);
        
        if (nbt.contains("personality")) {
            brain.personalityTraits = PersonalityProfile.fromNBT(nbt.getCompound("personality"));
        }
        if (nbt.contains("mood")) {
            brain.currentMood = MoodState.fromNBT(nbt.getCompound("mood"));
        }
        if (nbt.contains("shortTermMemory")) {
            brain.shortTermMemory = ConversationHistory.fromNBT(nbt.getCompound("shortTermMemory"));
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
    public String handleMessage(Player player, String message) {
        // Use a default conversation context for this simplified interface
        ConversationContext defaultContext = new ConversationContext();
        defaultContext.setLocation("Village");
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
        recordDetailedPlayerInteraction(player, playerMessage, villagerResponse, new ConversationContext());
    }
}