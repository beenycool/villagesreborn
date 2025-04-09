package com.beeny.village;

import com.beeny.config.ModConfig;        // Import new config
import com.beeny.config.ModConfigManager; // Import new config manager
// Keep old config import if needed elsewhere, otherwise remove. For now, assume not needed here.
// import com.beeny.config.VillagesConfig;
import net.minecraft.entity.player.PlayerEntity;
import java.util.*;
public class VillagerMemory {
    private static final int MAX_MEMORIES = 20;
    private static final float MEMORY_DECAY_RATE = 0.05f;
    private static final float INTERACTION_IMPORTANCE_THRESHOLD = 0.3f;

    private final List<Memory> memories;
    private final Map<UUID, Float> playerRelationships;
    private final Set<String> knownCraftingRecipes;
    private final Set<String> significantLocations;
    private final Map<Culture.ProfessionType, Integer> skillLevels;

    private Map<Culture.CulturalTrait, Float> personalityTraits;
    private int traumaLevel;
    private int joyLevel;

    public static class Memory implements Comparable<Memory> {
        private final String description;
        private final long timestampTicks; // Store game ticks instead of milliseconds
        private final MemoryType type;
        private float importance;
        private UUID associatedPlayerId;
        private final Map<String, Object> additionalData;

        public enum MemoryType {
            PLAYER_INTERACTION,
            VILLAGE_EVENT,
            PERSONAL_MILESTONE,
            ENVIRONMENTAL,
            SOCIAL_INTERACTION
        }

        // Constructor now takes game ticks
        public Memory(String description, MemoryType type, float importance, long gameTickTimestamp) {
            this.description = description;
            this.timestampTicks = gameTickTimestamp; // Store game ticks
            this.type = type;
            this.importance = importance;
            this.associatedPlayerId = null;
            this.additionalData = new HashMap<>();
        }

        public Memory withPlayer(UUID playerId) { this.associatedPlayerId = playerId; return this; }

        public Memory withData(String key, Object value) { additionalData.put(key, value); return this; }

        public String getDescription() { return description; }
        public long getTimestampTicks() { return timestampTicks; } // Return game ticks
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

    public VillagerMemory() {
        memories = new ArrayList<>();
        playerRelationships = new HashMap<>();
        knownCraftingRecipes = new HashSet<>();
        significantLocations = new HashSet<>();
        skillLevels = new HashMap<>();
        personalityTraits = new HashMap<>();
        traumaLevel = 0;
        joyLevel = 0;
        initializePersonalityTraits();
    }

    private void initializePersonalityTraits() {
        for (Culture.CulturalTrait trait : Culture.CulturalTrait.values()) {
            personalityTraits.put(trait, 0.0f);
        }
    }

    public void addMemory(Memory memory) {
        memories.add(memory);
        if (memory.getAssociatedPlayerId() != null) {
            updatePlayerRelationship(memory.getAssociatedPlayerId(), memory.getImportance());
        }
        evolvePersonalityTraits(memory);
        if (memories.size() > MAX_MEMORIES) {
            cullMemories();
        }
    }

    // Method now requires current game tick
    public void recordPlayerInteraction(PlayerEntity player, String description, float importance, boolean isPositive, long currentTick) {
        Memory memory = new Memory(
            description,
            Memory.MemoryType.PLAYER_INTERACTION,
            importance,
            currentTick // Pass current game tick
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

    // Method now requires current game tick
    public void recordVillageEvent(String eventName, String description, float importance, long currentTick) {
        Memory memory = new Memory(
            description,
            Memory.MemoryType.VILLAGE_EVENT,
            importance,
            currentTick // Pass current game tick
        ).withData("eventName", eventName);

        addMemory(memory);
    }

    // Method now requires current game tick
    public void learnCraftingRecipe(String recipeName, long currentTick) {
        knownCraftingRecipes.add(recipeName);

        Memory memory = new Memory(
            "Learned how to craft " + recipeName,
            Memory.MemoryType.PERSONAL_MILESTONE,
            0.6f,
            currentTick // Pass current game tick
        ).withData("recipe", recipeName);

        addMemory(memory);
    }

    public void updatePlayerRelationship(UUID playerId, float interactionValue) {
        // This method updates relationship score based on interaction value,
        // but the interaction value itself should be calculated where the interaction happens,
        // considering config multipliers (intensity, bias) there.
        // This method just applies the final calculated value.
        // We remove the config reading from here.
        float adjustedInteractionValue = interactionValue; // Assume value is already adjusted

        float currentRelationship = playerRelationships.getOrDefault(playerId, 0.0f);
        // Calculate new relationship using the adjusted value
        float newRelationship = Math.max(-1.0f, Math.min(1.0f, currentRelationship + adjustedInteractionValue));
        playerRelationships.put(playerId, newRelationship);
    }

    public float getPlayerRelationship(UUID playerId) {
        return playerRelationships.getOrDefault(playerId, 0.0f);
    }

    public void increaseSkill(Culture.ProfessionType profession, int amount) {
        int currentLevel = skillLevels.getOrDefault(profession, 0);
        skillLevels.put(profession, currentLevel + amount);

    // Method now requires current game tick
    public void recordSkillIncrease(Culture.ProfessionType profession, int newLevel, long currentTick) {
         Memory memory = new Memory(
            "Improved " + profession.name().toLowerCase() + " skills",
            Memory.MemoryType.PERSONAL_MILESTONE,
            0.4f,
            currentTick // Pass current game tick
        ).withData("profession", profession)
         .withData("level", newLevel);

        addMemory(memory);
    }

    // Refactored increaseSkill to call recordSkillIncrease
    public void increaseSkill(Culture.ProfessionType profession, int amount, long currentTick) {
        int currentLevel = skillLevels.getOrDefault(profession, 0);
        int newLevel = currentLevel + amount;
        skillLevels.put(profession, newLevel);

        if (newLevel > currentLevel) { // Only record if level actually increased
            recordSkillIncrease(profession, newLevel, currentTick);
        }
    }

    public int getSkillLevel(Culture.ProfessionType profession) {
        return skillLevels.getOrDefault(profession, 0);
    }

    public List<Memory> getMemories(Set<Memory.MemoryType> types, float minImportance) {
        return memories.stream()
            .filter(mem -> types.contains(mem.getType()))
            .filter(mem -> mem.getImportance() >= minImportance)
            .sorted()
            .toList();
    }

    public List<Memory> getMemoriesInvolvingPlayer(UUID playerId) {
        return memories.stream()
            .filter(mem -> playerId.equals(mem.getAssociatedPlayerId()))
            .sorted()
            .toList();
    }

    private void evolvePersonalityTraits(Memory memory) {
        float impactStrength = memory.getImportance() * 0.1f;

        if (memory.getType() == Memory.MemoryType.PLAYER_INTERACTION) {
            String sentiment = (String) memory.getData("sentiment");
            if ("positive".equals(sentiment)) {
                adjustTrait(Culture.CulturalTrait.DIPLOMATIC, impactStrength);
                adjustTrait(Culture.CulturalTrait.ADAPTABLE, impactStrength * 0.5f);
            } else {
                adjustTrait(Culture.CulturalTrait.ISOLATIONIST, impactStrength);
                adjustTrait(Culture.CulturalTrait.CONSERVATIVE, impactStrength * 0.5f);
            }
        } else if (memory.getType() == Memory.MemoryType.VILLAGE_EVENT) {
            adjustTrait(Culture.CulturalTrait.COMMUNAL, impactStrength);
        } else if (memory.getType() == Memory.MemoryType.PERSONAL_MILESTONE) {
            adjustTrait(Culture.CulturalTrait.CRAFTING_FOCUSED, impactStrength);
            adjustTrait(Culture.CulturalTrait.INDUSTRIOUS, impactStrength * 0.7f);
        }

        balancePersonality();
    }

    private void adjustTrait(Culture.CulturalTrait trait, float amount) {
        float current = personalityTraits.getOrDefault(trait, 0.0f);
        float newValue = Math.max(-1.0f, Math.min(1.0f, current + amount));
        personalityTraits.put(trait, newValue);
    }

    private void balancePersonality() {
        balanceOpposites(Culture.CulturalTrait.TRADITIONAL, Culture.CulturalTrait.INNOVATIVE);
        balanceOpposites(Culture.CulturalTrait.DIPLOMATIC, Culture.CulturalTrait.ISOLATIONIST);
        balanceOpposites(Culture.CulturalTrait.COMMUNAL, Culture.CulturalTrait.INDIVIDUALISTIC);
        balanceOpposites(Culture.CulturalTrait.CONSERVATIVE, Culture.CulturalTrait.ADAPTABLE);
    }

    private void balanceOpposites(Culture.CulturalTrait trait1, Culture.CulturalTrait trait2) {
        float value1 = personalityTraits.getOrDefault(trait1, 0.0f);
        float value2 = personalityTraits.getOrDefault(trait2, 0.0f);

        if ((value1 > 0.7f && value2 > 0.7f) || (value1 < -0.7f && value2 < -0.7f)) {
            personalityTraits.put(trait1, value1 * 0.95f);
            personalityTraits.put(trait2, value2 * 0.95f);
        }
    }

    private void cullMemories() {
        memories.sort(null);
        while (memories.size() > MAX_MEMORIES) {
            memories.remove(memories.size() - 1);
        }
    }

    // Method now accepts current game tick and uses ModConfig
    public void updateMemories(long currentGameTick) {
        // Get memory duration in ticks from ModConfig
        ModConfig config = ModConfigManager.getConfig();
        long maxMemoryAgeTicks = config.gameplay.memoryDurationTicks;

        // Decay importance first
        // Use iterator to allow removal during iteration
        Iterator<Memory> iterator = memories.iterator();
        while (iterator.hasNext()) {
             Memory memory = iterator.next();
             memory.decayImportance(MEMORY_DECAY_RATE);
             // Remove memories older than the configured duration in ticks
             if ((currentGameTick - memory.getTimestampTicks()) > maxMemoryAgeTicks) {
                 iterator.remove();
             }
        }
        // Cull memories if list is still too large after removing old ones
        if (memories.size() > MAX_MEMORIES) {
             cullMemories(); // cullMemories already sorts and removes the least important
        }
    }

    public float getPersonalityTrait(Culture.CulturalTrait trait) {
        return personalityTraits.getOrDefault(trait, 0.0f);
    }

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

    public int getTraumaLevel() {
        return traumaLevel;
    }

    public int getJoyLevel() {
        return joyLevel;
    }

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

    public Set<String> getKnownRecipes() {
        return Collections.unmodifiableSet(knownCraftingRecipes);
    }

    public boolean knowsRecipe(String recipeName) {
        return knownCraftingRecipes.contains(recipeName);
    }
}