package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.common.NBTCompound;
import com.beeny.villagesreborn.core.config.WorldSettingsManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced VillagerBrainManager implementation with persistence and load/save operations
 * Manages villager AI brains with NBT-based persistence support
 */
public class VillagerBrainManagerImpl implements VillagerBrainManager {
    
    private final Map<UUID, VillagerBrain> brainCache = new ConcurrentHashMap<>();
    private final VillagerBrainPersistence persistence;
    
    public VillagerBrainManagerImpl(VillagerBrainPersistence persistence) {
        this.persistence = persistence;
    }
    
    @Override
    public void processOverheardMessage(VillagerEntity villager, Player player, String message) {
        VillagerBrain brain = getBrain(villager);
        
        // Update brain based on overheard conversation
        brain.recordMemory("Overheard " + player.getName() + " say: " + message);
        
        // Adjust mood slightly based on message content
        if (isPositiveMessage(message)) {
            float newHappiness = brain.getCurrentMood().getHappiness() + 0.05f;
            brain.getCurrentMood().setHappiness(newHappiness);
        } else if (isNegativeMessage(message)) {
            float newHappiness = brain.getCurrentMood().getHappiness() - 0.05f;
            brain.getCurrentMood().setHappiness(newHappiness);
        }
        
        // Save updated brain
        saveBrain(villager, brain);
    }
    
    @Override
    public VillagerBrain getBrain(VillagerEntity villager) {
        UUID villagerUUID = villager.getUUID();
        
        // Check cache first
        VillagerBrain cachedBrain = brainCache.get(villagerUUID);
        if (cachedBrain != null) {
            return cachedBrain;
        }
        
        // Try to load from persistence
        VillagerBrain loadedBrain = persistence.loadBrain(villagerUUID);
        if (loadedBrain != null) {
            brainCache.put(villagerUUID, loadedBrain);
            return loadedBrain;
        }
        
        // Create new brain
        VillagerBrain newBrain = createNewBrain(villager);
        brainCache.put(villagerUUID, newBrain);
        persistence.saveBrain(villagerUUID, newBrain);
        
        return newBrain;
    }
    
    @Override
    public void saveBrain(VillagerEntity villager, VillagerBrain brain) {
        UUID villagerUUID = villager.getUUID();
        brainCache.put(villagerUUID, brain);
        persistence.saveBrain(villagerUUID, brain);
    }
    
    /**
     * Creates a new brain with randomized personality and default values
     */
    private VillagerBrain createNewBrain(VillagerEntity villager) {
        VillagerBrain brain = new VillagerBrain(villager.getUUID());
        
        // Apply world-specific memory limit
        Object world = villager.getWorld();
        if (world != null) {
            int memoryLimit = WorldSettingsManager.getInstance().getVillagerMemoryLimit(world);
            brain.setMemoryLimit(memoryLimit);
        }
        
        // Generate random personality based on profession
        PersonalityProfile personality = generatePersonality(villager.getProfession());
        copyPersonalityTraits(brain.getPersonalityTraits(), personality);
        
        // Set initial mood (already neutral by default)
        MoodState initialMood = new MoodState();
        brain.updateMood(initialMood);
        
        return brain;
    }
    
    /**
     * Generates personality traits based on villager profession
     */
    private PersonalityProfile generatePersonality(String profession) {
        PersonalityProfile profile = new PersonalityProfile();
        
        // Set profession-specific traits using existing TraitType values
        switch (profession.toLowerCase()) {
            case "farmer":
                profile.adjustTrait(TraitType.HELPFULNESS, 0.8f);
                profile.adjustTrait(TraitType.FRIENDLINESS, 0.7f);
                break;
            case "librarian":
                profile.adjustTrait(TraitType.CURIOSITY, 0.9f);
                profile.adjustTrait(TraitType.HELPFULNESS, 0.8f);
                profile.adjustTrait(TraitType.FORMALITY, 0.7f);
                break;
            case "blacksmith":
                profile.adjustTrait(TraitType.HELPFULNESS, 0.8f);
                profile.adjustTrait(TraitType.FORMALITY, 0.6f);
                break;
            case "cleric":
                profile.adjustTrait(TraitType.HELPFULNESS, 0.9f);
                profile.adjustTrait(TraitType.FORMALITY, 0.8f);
                profile.adjustTrait(TraitType.FRIENDLINESS, 0.7f);
                break;
            default:
                // Random personality for generic villagers
                randomizePersonalityTraits(profile);
                break;
        }
        
        return profile;
    }
    
    /**
     * Copies traits from one personality profile to another
     */
    private void copyPersonalityTraits(PersonalityProfile target, PersonalityProfile source) {
        for (TraitType trait : TraitType.values()) {
            target.adjustTrait(trait, source.getTraitInfluence(trait));
        }
    }
    
    /**
     * Randomizes personality traits
     */
    private void randomizePersonalityTraits(PersonalityProfile profile) {
        for (TraitType trait : TraitType.values()) {
            profile.adjustTrait(trait, (float) Math.random());
        }
    }
    
    /**
     * Simple sentiment analysis for overheard messages
     */
    private boolean isPositiveMessage(String message) {
        String lower = message.toLowerCase();
        return lower.contains("good") || lower.contains("great") || lower.contains("awesome") ||
               lower.contains("thanks") || lower.contains("love") || lower.contains("happy");
    }
    
    private boolean isNegativeMessage(String message) {
        String lower = message.toLowerCase();
        return lower.contains("bad") || lower.contains("terrible") || lower.contains("hate") ||
               lower.contains("angry") || lower.contains("sad") || lower.contains("awful");
    }
    
    /**
     * Cleans up old brain data and unused entries
     */
    public void performMaintenance() {
        long currentTime = System.currentTimeMillis();
        long threshold = currentTime - (24 * 60 * 60 * 1000); // 24 hours
        
        brainCache.values().forEach(brain -> {
            brain.cleanupOldMemories(threshold);
            brain.decayMood();
        });
    }
    
    /**
     * Gets statistics about the brain manager
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("cachedBrains", brainCache.size());
        stats.put("averageMemorySize", brainCache.values().stream()
            .mapToInt(brain -> brain.getShortTermMemory().getInteractions().size())
            .average().orElse(0));
        return stats;
    }
    
    /**
     * Interface for brain persistence operations
     */
    public interface VillagerBrainPersistence {
        VillagerBrain loadBrain(UUID villagerUUID);
        void saveBrain(UUID villagerUUID, VillagerBrain brain);
        void deleteBrain(UUID villagerUUID);
    }
    
    /**
     * Default implementation using in-memory storage (for testing)
     */
    public static class InMemoryPersistence implements VillagerBrainPersistence {
        private final Map<UUID, NBTCompound> storage = new ConcurrentHashMap<>();
        
        @Override
        public VillagerBrain loadBrain(UUID villagerUUID) {
            NBTCompound nbt = storage.get(villagerUUID);
            return nbt != null ? VillagerBrain.fromNBT(nbt) : null;
        }
        
        @Override
        public void saveBrain(UUID villagerUUID, VillagerBrain brain) {
            storage.put(villagerUUID, brain.toNBT());
        }
        
        @Override
        public void deleteBrain(UUID villagerUUID) {
            storage.remove(villagerUUID);
        }
    }
}