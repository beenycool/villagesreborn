package com.beeny.villagesreborn.platform.fabric.ai;

import com.beeny.villagesreborn.core.VillagesRebornCommon;
import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.ai.PersonalityProfile;
import com.beeny.villagesreborn.core.ai.MoodState;
import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.config.WorldSettingsManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integrates AI systems with actual Minecraft villagers
 * Manages villager brains, personalities, and AI behaviors
 */
public class VillagerAIIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerAIIntegration.class);
    private static VillagerAIIntegration instance;
    
    private final Map<UUID, VillagerBrain> villagerBrains = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAIUpdate = new ConcurrentHashMap<>();
    private boolean enabled = false;
    
    private VillagerAIIntegration() {
        checkIfEnabled();
    }
    
    public static VillagerAIIntegration getInstance() {
        if (instance == null) {
            instance = new VillagerAIIntegration();
        }
        return instance;
    }
    
    /**
     * Checks if AI integration should be enabled based on configuration
     */
    private void checkIfEnabled() {
        try {
            var config = VillagesRebornCommon.getConfig();
            this.enabled = config.hasValidApiKey();
            LOGGER.info("AI Integration enabled: {}", enabled);
        } catch (Exception e) {
            this.enabled = false;
            LOGGER.warn("AI Integration disabled due to configuration error: {}", e.getMessage());
        }
    }
    
    /**
     * Initializes AI for a villager
     */
    public void initializeVillagerAI(VillagerEntity villager, ServerWorld world) {
        if (!enabled) {
            LOGGER.debug("AI disabled, skipping initialization for villager: {}", villager.getUuid());
            return;
        }
        
        try {
            UUID villagerUUID = villager.getUuid();
            
            if (villagerBrains.containsKey(villagerUUID)) {
                LOGGER.debug("Villager {} already has AI initialized", villagerUUID);
                return;
            }
            
            // Create new brain for villager
            VillagerBrain brain = new VillagerBrain(villagerUUID);
            
            // Set basic info
            String villagerName = villager.hasCustomName() ? 
                villager.getCustomName().getString() : 
                "Villager_" + villagerUUID.toString().substring(0, 8);
            brain.setVillagerName(villagerName);
            brain.setProfession(villager.getVillagerData().getProfession().toString());
            
            // Initialize personality based on profession and random factors
            generatePersonality(brain, villager);
            
            // Set world-specific settings
            WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
            int memoryLimit = settingsManager.getVillagerMemoryLimit(world);
            brain.setMemoryLimit(memoryLimit);
            
            // Store the brain
            villagerBrains.put(villagerUUID, brain);
            lastAIUpdate.put(villagerUUID, System.currentTimeMillis());
            
            LOGGER.info("Initialized AI for villager: {} ({})", villagerName, villager.getVillagerData().getProfession());
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize AI for villager: {}", villager.getUuid(), e);
        }
    }
    
    /**
     * Updates AI for a villager (should be called periodically)
     */
    public void updateVillagerAI(VillagerEntity villager, ServerWorld world) {
        if (!enabled) return;
        
        try {
            UUID villagerUUID = villager.getUuid();
            VillagerBrain brain = villagerBrains.get(villagerUUID);
            
            if (brain == null) {
                initializeVillagerAI(villager, world);
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            long lastUpdate = lastAIUpdate.getOrDefault(villagerUUID, 0L);
            
            // Only update every 5 seconds to avoid performance issues
            if (currentTime - lastUpdate < 5000) {
                return;
            }
            
            // Update mood and memory decay
            brain.decayMood();
            brain.cleanupOldMemories(currentTime - 86400000); // 24 hours
            
            // Apply world settings
            WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
            if (settingsManager.isAdvancedAIEnabled(world)) {
                processAdvancedAI(villager, brain, world);
            }
            
            lastAIUpdate.put(villagerUUID, currentTime);
            
        } catch (Exception e) {
            LOGGER.error("Failed to update AI for villager: {}", villager.getUuid(), e);
        }
    }
    
    /**
     * Processes advanced AI behaviors
     */
    private void processAdvancedAI(VillagerEntity villager, VillagerBrain brain, ServerWorld world) {
        try {
            // Simple AI behavior based on personality and mood
            PersonalityProfile personality = brain.getPersonalityTraits();
            MoodState mood = brain.getCurrentMood();
            
            // Example: Adjust villager behavior based on personality traits
            if (personality.getTraitInfluence(com.beeny.villagesreborn.core.ai.TraitType.CURIOSITY) > 0.7f && Math.random() < 0.1f) {
                // Curious villagers might explore more
                LOGGER.debug("Villager {} is feeling curious", brain.getVillagerName());
            }
            
            if (mood.getHappiness() < 0.3f && Math.random() < 0.05f) {
                // Sad villagers might need cheering up
                LOGGER.debug("Villager {} is feeling sad", brain.getVillagerName());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to process advanced AI for villager: {}", villager.getUuid(), e);
        }
    }
    
    /**
     * Generates a personality profile for a villager using the available trait types
     */
    private void generatePersonality(VillagerBrain brain, VillagerEntity villager) {
        PersonalityProfile personality = brain.getPersonalityTraits();
        
        // Base personality on profession using available trait types
        String profession = villager.getVillagerData().getProfession().toString();
        
        switch (profession.toLowerCase()) {
            case "librarian":
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.CURIOSITY, 0.8f + (float)(Math.random() * 0.2f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.HELPFULNESS, 0.7f + (float)(Math.random() * 0.3f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.FORMALITY, 0.6f + (float)(Math.random() * 0.4f));
                break;
            case "blacksmith", "weaponsmith", "armorer":
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.FRIENDLINESS, 0.4f + (float)(Math.random() * 0.3f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.HELPFULNESS, 0.8f + (float)(Math.random() * 0.2f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.FORMALITY, 0.7f + (float)(Math.random() * 0.3f));
                break;
            case "farmer":
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.FRIENDLINESS, 0.8f + (float)(Math.random() * 0.2f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.HELPFULNESS, 0.9f + (float)(Math.random() * 0.1f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.HUMOR, 0.6f + (float)(Math.random() * 0.4f));
                break;
            case "cleric":
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.FRIENDLINESS, 0.7f + (float)(Math.random() * 0.3f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.HELPFULNESS, 0.8f + (float)(Math.random() * 0.2f));
                personality.adjustTrait(com.beeny.villagesreborn.core.ai.TraitType.FORMALITY, 0.8f + (float)(Math.random() * 0.2f));
                break;
            default:
                // Random personality for other professions
                for (com.beeny.villagesreborn.core.ai.TraitType trait : com.beeny.villagesreborn.core.ai.TraitType.values()) {
                    personality.adjustTrait(trait, (float)Math.random());
                }
        }
        
        LOGGER.debug("Generated personality for {} {}: Curiosity={}, Friendliness={}, Helpfulness={}",
            profession, villager.getUuid().toString().substring(0, 8),
            personality.getTraitInfluence(com.beeny.villagesreborn.core.ai.TraitType.CURIOSITY),
            personality.getTraitInfluence(com.beeny.villagesreborn.core.ai.TraitType.FRIENDLINESS),
            personality.getTraitInfluence(com.beeny.villagesreborn.core.ai.TraitType.HELPFULNESS));
    }
    
    /**
     * Gets the brain for a villager (may be null if not initialized)
     */
    public VillagerBrain getVillagerBrain(UUID villagerUUID) {
        return villagerBrains.get(villagerUUID);
    }
    
    /**
     * Creates or updates a relationship between two villagers
     */
    public void updateVillagerRelationship(UUID villager1, UUID villager2, float sentimentChange, String reason) {
        if (!enabled) return;
        
        try {
            VillagerBrain brain1 = villagerBrains.get(villager1);
            VillagerBrain brain2 = villagerBrains.get(villager2);
            
            if (brain1 != null && brain2 != null) {
                // Update relationship data
                RelationshipData relationship1 = brain1.getRelationship(villager2);
                RelationshipData relationship2 = brain2.getRelationship(villager1);
                
                relationship1.adjustTrust(sentimentChange * 0.5f);
                relationship1.adjustFriendship(sentimentChange);
                relationship1.incrementInteractionCount();
                
                relationship2.adjustTrust(sentimentChange * 0.5f);
                relationship2.adjustFriendship(sentimentChange);
                relationship2.incrementInteractionCount();
                
                LOGGER.debug("Updated relationship between {} and {}: {} (reason: {})",
                    brain1.getVillagerName(), brain2.getVillagerName(), sentimentChange, reason);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to update villager relationship", e);
        }
    }
    
    /**
     * Gets statistics about the AI system
     */
    public String getAIStats() {
        return String.format("AI Integration: %s, Active Brains: %d, Enabled: %s",
            enabled ? "Online" : "Offline",
            villagerBrains.size(),
            enabled);
    }
    
    /**
     * Enables or disables AI integration
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("AI Integration {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Cleans up AI data for villagers that no longer exist
     */
    public void cleanup() {
        // This would be called periodically to remove data for despawned villagers
        // For now, just log the cleanup
        LOGGER.debug("AI cleanup - tracking {} villager brains", villagerBrains.size());
    }
} 