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
 * Enhanced VillagerBrain implementation with PromptBuilder integration
 */
public class VillagerBrain {
    public static final int MAX_SHORT_TERM_MEMORY = 50;
    
    private final UUID villagerUUID;
    private PersonalityProfile personalityTraits;
    private MoodState currentMood;
    private ConversationHistory shortTermMemory;
    private MemoryBank longTermMemory;
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
            // Build comprehensive prompt using PromptBuilder
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

            // Store interaction
            ConversationInteraction interaction = new ConversationInteraction(
                System.currentTimeMillis(),
                player.getUUID(),
                message,
                response.getResponse(),
                currentMood.copy(),
                context.getLocation()
            );
            shortTermMemory.addInteraction(interaction);

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

    public RelationshipData getRelationship(Player player) {
        return relationshipMap.computeIfAbsent(player.getUUID(), uuid -> new RelationshipData(uuid));
    }

    public void updateRelationship(Player player, String message) {
        RelationshipData relationship = getRelationship(player);
        relationship.incrementInteractionCount();
        // Simplified relationship update
        relationship.adjustTrust(0.1f);
        relationship.adjustFriendship(0.1f);
    }

    public void updateMood(Player player, String message) {
        // Simplified mood update
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

    // New methods required by task specification
    
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
    }
}