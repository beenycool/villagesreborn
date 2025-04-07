package com.beeny.village;

import net.minecraft.entity.player.PlayerEntity;
import java.util.*;

/**
 * Represents the memory system for villagers, storing past interactions with players
 * and other significant events that affect villager behavior and dialogue.
 */
public class VillagerMemory {
    // Constants for memory decay and importance
    private static final int MAX_MEMORIES = 20;
    private static final float MEMORY_DECAY_RATE = 0.05f;
    private static final float INTERACTION_IMPORTANCE_THRESHOLD = 0.3f;
    
    // Core memory storage
    private final List<Memory> memories;
    private final Map<UUID, Float> playerRelationships;
    private final Set<String> knownCraftingRecipes;
    private final Set<String> significantLocations;
    private final Map<Culture.ProfessionType, Integer> skillLevels;
    
    // Evolving personality
    private Map<Culture.CulturalTrait, Float> personalityTraits;
    private int traumaLevel;
    private int joyLevel;
    
    /**
     * Represents a single memory of an event or interaction
     */
    public static class Memory implements Comparable<Memory> {
        private final String description;
        private final long timestamp;
        private final MemoryType type;
        private float importance;
        private UUID associatedPlayerId;
        private final Map<String, Object> additionalData;
        
        public enum MemoryType {
            PLAYER_INTERACTION, // Trading, gifting, attacking
            VILLAGE_EVENT,      // Festivals, raids, elections
            PERSONAL_MILESTONE, // Profession change, skill increase
            ENVIRONMENTAL,      // Weather events, structure destruction/creation
            SOCIAL_INTERACTION  // Interactions with other villagers
        }
        
        public Memory(String description, MemoryType type, float importance) {
            this.description = description;
            timestamp = System.currentTimeMillis();
            this.type = type;
            this.importance = importance;
            associatedPlayerId = null;
            additionalData = new HashMap<>();
        }
        
        public Memory withPlayer(UUID playerId) { this.associatedPlayerId = playerId; return this; }
        
        public Memory withData(String key, Object value) { additionalData.put(key, value); return this; }
        
        public String getDescription() { return description; }
        public long getTimestamp() { return timestamp; }
        public MemoryType getType() { return type; }
        public float getImportance() { return importance; }
        public UUID getAssociatedPlayerId() { return associatedPlayerId; }
        public Object getData(String key) { return additionalData.get(key); }
        
        public void decayImportance(float rate) { importance = Math.max(0.01f, importance - rate); }
        
        @Override
        public int compareTo(Memory other) {
            return Float.compare(other.importance, this.importance);
        }
    }
    
    /**
     * Creates a new VillagerMemory instance with default values
     */
    public VillagerMemory() {
        memories = new ArrayList<>();
        playerRelationships = new HashMap<>();
        knownCraftingRecipes = new HashSet<>();
        significantLocations = new HashSet<>();
        skillLevels = new HashMap<>();
        personalityTraits = new HashMap<>();
        traumaLevel = 0;
        joyLevel = 0;
        
        // Initialize basic personality traits with neutral values
        initializePersonalityTraits();
    }
    
    /**
     * Initialize personality traits with neutral values
     */
    private void initializePersonalityTraits() {
        for (Culture.CulturalTrait trait : Culture.CulturalTrait.values()) {
            personalityTraits.put(trait, 0.0f);
        }
    }
    
    /**
     * Adds a new memory to the villager's memory
     * @param memory The memory to add
     */
    public void addMemory(Memory memory) {
        memories.add(memory);
        
        // Update player relationship if this memory involves a player
        if (memory.getAssociatedPlayerId() != null) {
            updatePlayerRelationship(memory.getAssociatedPlayerId(), memory.getImportance());
        }
        
        // Evolve personality traits based on the memory
        evolvePersonalityTraits(memory);
        
        // Maintain memory limit by removing least important memories if needed
        if (memories.size() > MAX_MEMORIES) {
            cullMemories();
        }
    }
    
    /**
     * Creates and adds a player interaction memory
     * @param player The player who interacted with the villager
     * @param description Description of the interaction
     * @param importance How important this interaction is (0.0-1.0)
     * @param isPositive Whether the interaction was positive
     */
    public void recordPlayerInteraction(PlayerEntity player, String description, float importance, boolean isPositive) {
        Memory memory = new Memory(
            description, 
            Memory.MemoryType.PLAYER_INTERACTION,
            importance
        ).withPlayer(player.getUuid());
        
        if (isPositive) {
            memory.withData("sentiment", "positive");
            joyLevel += Math.max(1, Math.round(importance * 3));
        } else {
            memory.withData("sentiment", "negative");
            traumaLevel += Math.max(1, Math.round(importance * 3));
        }
        
        addMemory(memory);
    }
    
    /**
     * Records a village event in the villager's memory
     * @param eventName Name of the event
     * @param description Description of what happened
     * @param importance How important this event is (0.0-1.0)
     */
    public void recordVillageEvent(String eventName, String description, float importance) {
        Memory memory = new Memory(
            description,
            Memory.MemoryType.VILLAGE_EVENT,
            importance
        ).withData("eventName", eventName);
        
        addMemory(memory);
    }
    
    /**
     * Records learning a new crafting recipe
     * @param recipeName Name of the recipe learned
     */
    public void learnCraftingRecipe(String recipeName) {
        knownCraftingRecipes.add(recipeName);
        
        Memory memory = new Memory(
            "Learned how to craft " + recipeName,
            Memory.MemoryType.PERSONAL_MILESTONE,
            0.6f
        ).withData("recipe", recipeName);
        
        addMemory(memory);
    }
    
    /**
     * Updates the relationship with a player based on an interaction
     * @param playerId The UUID of the player
     * @param interactionValue The value of the interaction (-1.0 to 1.0)
     */
    public void updatePlayerRelationship(UUID playerId, float interactionValue) {
        float currentRelationship = playerRelationships.getOrDefault(playerId, 0.0f);
        float newRelationship = Math.max(-1.0f, Math.min(1.0f, currentRelationship + interactionValue));
        playerRelationships.put(playerId, newRelationship);
    }
    
    /**
     * Get the current relationship value with a player
     * @param playerId The UUID of the player
     * @return The relationship value (-1.0 to 1.0)
     */
    public float getPlayerRelationship(UUID playerId) {
        return playerRelationships.getOrDefault(playerId, 0.0f);
    }
    
    /**
     * Increase skill level for a profession
     * @param profession The profession to increase skill in
     * @param amount The amount to increase
     */
    public void increaseSkill(Culture.ProfessionType profession, int amount) {
        int currentLevel = skillLevels.getOrDefault(profession, 0);
        skillLevels.put(profession, currentLevel + amount);
        
        if ((currentLevel + amount) > currentLevel) {
            Memory memory = new Memory(
                "Improved " + profession.name().toLowerCase() + " skills",
                Memory.MemoryType.PERSONAL_MILESTONE,
                0.4f
            ).withData("profession", profession)
             .withData("level", currentLevel + amount);
            
            addMemory(memory);
        }
    }
    
    /**
     * Get the skill level for a specific profession
     * @param profession The profession to check
     * @return The skill level
     */
    public int getSkillLevel(Culture.ProfessionType profession) {
        return skillLevels.getOrDefault(profession, 0);
    }
    
    /**
     * Get relevant memories that match certain criteria
     * @param types The types of memories to retrieve
     * @param minImportance The minimum importance threshold
     * @return List of matching memories
     */
    public List<Memory> getMemories(Set<Memory.MemoryType> types, float minImportance) {
        return memories.stream()
            .filter(mem -> types.contains(mem.getType()))
            .filter(mem -> mem.getImportance() >= minImportance)
            .sorted()
            .toList();
    }
    
    /**
     * Get memories associated with a specific player
     * @param playerId The UUID of the player
     * @return List of memories involving this player
     */
    public List<Memory> getMemoriesInvolvingPlayer(UUID playerId) {
        return memories.stream()
            .filter(mem -> playerId.equals(mem.getAssociatedPlayerId()))
            .sorted()
            .toList();
    }
    
    /**
     * Evolve personality traits based on a new memory
     * @param memory The memory that might affect personality
     */
    private void evolvePersonalityTraits(Memory memory) {
        float impactStrength = memory.getImportance() * 0.1f;
        
        if (memory.getType() == Memory.MemoryType.PLAYER_INTERACTION) {
            String sentiment = (String) memory.getData("sentiment");
            if ("positive".equals(sentiment)) {
                // Positive interactions make villager more open to players
                adjustTrait(Culture.CulturalTrait.DIPLOMATIC, impactStrength);
                adjustTrait(Culture.CulturalTrait.ADAPTABLE, impactStrength * 0.5f);
            } else {
                // Negative interactions make villager more cautious
                adjustTrait(Culture.CulturalTrait.ISOLATIONIST, impactStrength);
                adjustTrait(Culture.CulturalTrait.CONSERVATIVE, impactStrength * 0.5f);
            }
        } else if (memory.getType() == Memory.MemoryType.VILLAGE_EVENT) {
            // Village events tend to reinforce community bonds
            adjustTrait(Culture.CulturalTrait.COMMUNAL, impactStrength);
        } else if (memory.getType() == Memory.MemoryType.PERSONAL_MILESTONE) {
            // Personal achievements boost confidence and craft focus
            adjustTrait(Culture.CulturalTrait.CRAFTING_FOCUSED, impactStrength);
            adjustTrait(Culture.CulturalTrait.INDUSTRIOUS, impactStrength * 0.7f);
        }
        
        // Balance personality to ensure no trait becomes too extreme
        balancePersonality();
    }
    
    /**
     * Adjust a specific personality trait
     * @param trait The trait to adjust
     * @param amount The amount to adjust by (positive or negative)
     */
    private void adjustTrait(Culture.CulturalTrait trait, float amount) {
        float current = personalityTraits.getOrDefault(trait, 0.0f);
        float newValue = Math.max(-1.0f, Math.min(1.0f, current + amount));
        personalityTraits.put(trait, newValue);
    }
    
    /**
     * Balance personality traits to prevent extremes
     */
    private void balancePersonality() {
        // Opposites tend to balance each other
        balanceOpposites(Culture.CulturalTrait.TRADITIONAL, Culture.CulturalTrait.INNOVATIVE);
        balanceOpposites(Culture.CulturalTrait.DIPLOMATIC, Culture.CulturalTrait.ISOLATIONIST);
        balanceOpposites(Culture.CulturalTrait.COMMUNAL, Culture.CulturalTrait.INDIVIDUALISTIC);
        balanceOpposites(Culture.CulturalTrait.CONSERVATIVE, Culture.CulturalTrait.ADAPTABLE);
    }
    
    /**
     * Balance opposites by slightly pulling extreme values toward center
     */
    private void balanceOpposites(Culture.CulturalTrait trait1, Culture.CulturalTrait trait2) {
        float value1 = personalityTraits.getOrDefault(trait1, 0.0f);
        float value2 = personalityTraits.getOrDefault(trait2, 0.0f);
        
        // If both traits are strong in same direction, pull back slightly
        if ((value1 > 0.7f && value2 > 0.7f) || (value1 < -0.7f && value2 < -0.7f)) {
            personalityTraits.put(trait1, value1 * 0.95f);
            personalityTraits.put(trait2, value2 * 0.95f);
        }
    }
    
    /**
     * Remove the least important memories when over the limit
     */
    private void cullMemories() {
        // Sort by importance
        memories.sort(null);
        
        // Remove the least important memories until we're under the limit
        while (memories.size() > MAX_MEMORIES) {
            memories.remove(memories.size() - 1);
        }
    }
    
    /**
     * Updates memory state, decaying old memories and determining which should be forgotten
     * Should be called periodically, like once per in-game day
     */
    public void updateMemories() {
        // Decay importance of all memories
        for (Memory memory : memories) {
            memory.decayImportance(MEMORY_DECAY_RATE);
        }
        
        // Remove memories that have decayed below the threshold
        memories.removeIf(memory -> memory.getImportance() < INTERACTION_IMPORTANCE_THRESHOLD);
    }
    
    /**
     * Get a value representing a personality trait
     * @param trait The trait to check
     * @return The trait value (-1.0 to 1.0)
     */
    public float getPersonalityTrait(Culture.CulturalTrait trait) {
        return personalityTraits.getOrDefault(trait, 0.0f);
    }
    
    /**
     * Get the dominant personality traits
     * @return Set of the most pronounced personality traits
     */
    public Set<Culture.CulturalTrait> getDominantTraits() {
        Set<Culture.CulturalTrait> dominant = new HashSet<>();
        float threshold = 0.6f;
        
        for (Map.Entry<Culture.CulturalTrait, Float> entry : personalityTraits.entrySet()) {
            if (Math.abs(entry.getValue()) >= threshold) {
                dominant.add(entry.getKey());
            }
        }
        
        return dominant;
    }
    
    /**
     * Get trauma level of the villager
     * @return The trauma level
     */
    public int getTraumaLevel() {
        return traumaLevel;
    }
    
    /**
     * Get joy level of the villager
     * @return The joy level
     */
    public int getJoyLevel() {
        return joyLevel;
    }
    
    /**
     * Get emotional state description based on trauma and joy levels
     * @return Text description of emotional state
     */
    public String getEmotionalState() {
        int emotionalBalance = joyLevel - traumaLevel;
        
        if (emotionalBalance > 20) return "Elated";
        if (emotionalBalance > 10) return "Happy";
        if (emotionalBalance > 5) return "Content";
        if (emotionalBalance > -5) return "Neutral";
        if (emotionalBalance > -10) return "Discontent";
        if (emotionalBalance > -20) return "Unhappy";
        return "Distressed";
    }
    
    /**
     * Get known crafting recipes
     * @return Set of recipe names the villager knows
     */
    public Set<String> getKnownRecipes() {
        return Collections.unmodifiableSet(knownCraftingRecipes);
    }
    
    /**
     * Check if villager knows a specific recipe
     * @param recipeName Name of the recipe
     * @return True if the villager knows this recipe
     */
    public boolean knowsRecipe(String recipeName) {
        return knownCraftingRecipes.contains(recipeName);
    }
}