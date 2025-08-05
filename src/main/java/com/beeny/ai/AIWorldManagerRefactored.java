package com.beeny.ai;

import com.beeny.ai.core.AISubsystemManager;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.learning.VillagerLearningSystem;
import com.beeny.ai.social.VillagerGossipManager;
import com.beeny.ai.planning.VillagerGOAPRefactored;
import com.beeny.ai.quests.VillagerQuestSystem;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Refactored AI World Manager that uses the subsystem pattern
 * instead of the god object anti-pattern.
 * 
 * This replaces the monolithic AIWorldManager with a clean,
 * focused coordinator that delegates to specialized subsystems.
 */
public class AIWorldManagerRefactored {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIWorldManagerRefactored.class);
    
    private static AIWorldManagerRefactored instance;
    
    private final MinecraftServer server;
    private final AISubsystemManager subsystemManager;
    
    // Individual subsystem references for direct access when needed
    private final VillagerEmotionSystem emotionSystem;
    private final VillagerLearningSystem learningSystem;
    private final VillagerGossipManager gossipManager;
    private final VillagerGOAPRefactored planningSystem;
    private final VillagerQuestSystem questSystem;
    private final VillagerAIManager aiManager;
    
    private AIWorldManagerRefactored(@NotNull MinecraftServer server) {
        this.server = server;
        this.subsystemManager = new AISubsystemManager(server);
        
        // Create subsystems
        this.emotionSystem = new VillagerEmotionSystem();
        this.learningSystem = new VillagerLearningSystem();
        this.gossipManager = new VillagerGossipManager(this);
        this.planningSystem = new VillagerGOAPRefactored();
        this.questSystem = new VillagerQuestSystem();
        this.aiManager = new VillagerAIManager();
        
        // Register subsystems with the manager (order matters for dependencies)
        subsystemManager.registerSubsystem(emotionSystem);    // High priority - affects others
        subsystemManager.registerSubsystem(aiManager);        // Medium priority - general state management
        subsystemManager.registerSubsystem(learningSystem);   // Medium priority
        subsystemManager.registerSubsystem(planningSystem);   // Medium priority
        // questSystem currently does not implement AISubsystem; do not register to avoid type error
        // Note: gossipManager doesn't implement AISubsystem yet, would need refactoring
        
        LOGGER.info("AIWorldManagerRefactored initialized with {} subsystems", 
                   subsystemManager.getSubsystems().size());
    }
    
    /**
     * Initialize singleton with server instance
     */
    public static void initialize(@NotNull MinecraftServer server) {
        if (instance == null) {
            instance = new AIWorldManagerRefactored(server);
            instance.start();
        }
    }
    
    /**
     * Get singleton instance
     */
    @NotNull
    public static AIWorldManagerRefactored getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AIWorldManagerRefactored not initialized. Call initialize() first.");
        }
        return instance;
    }
    
    /**
     * Shutdown and clear singleton
     */
    public static void cleanup() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
    
    /**
     * Start the AI system
     */
    public void start() {
        subsystemManager.start();
        LOGGER.info("AIWorldManagerRefactored started successfully");
    }
    
    /**
     * Initialize AI for a villager
     */
    public void initializeVillagerAI(@NotNull VillagerEntity villager) {
        subsystemManager.initializeVillager(villager);
        
        // Handle gossip manager separately since it doesn't implement AISubsystem yet
        try {
            com.beeny.data.VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                gossipManager.initializeVillagerGossip(villager, data);
            }
        } catch (Exception e) {
            LOGGER.error("Error initializing gossip for villager {}", villager.getUuidAsString(), e);
        }
    }
    
    /**
     * Update AI for a villager
     */
    public void updateVillagerAI(@NotNull VillagerEntity villager) {
        if (villager.getWorld().isClient) return;
        
        subsystemManager.updateVillager(villager);
        
        // Handle gossip manager updates separately
        try {
            gossipManager.updateVillagerGossip(villager);
        } catch (Exception e) {
            LOGGER.error("Error updating gossip for villager {}", villager.getUuidAsString(), e);
        }
    }
    
    /**
     * Clean up AI for a villager
     */
    public void cleanupVillagerAI(@NotNull String villagerUuid) {
        subsystemManager.cleanupVillager(villagerUuid);
        
        // Handle gossip manager cleanup separately
        try {
            gossipManager.cleanupVillager(villagerUuid);
        } catch (Exception e) {
            LOGGER.error("Error cleaning up gossip for villager {}", villagerUuid, e);
        }
    }
    
    /**
     * Process emotional event for a villager (compatibility method)
     */
    public void processVillagerEmotionalEvent(@NotNull VillagerEntity villager, 
                                            @NotNull VillagerEmotionSystem.EmotionalEvent event) {
        emotionSystem.processEmotionalEvent(villager, event);
    }
    
    /**
     * Get emotional state for a villager (compatibility method)
     */
    @Nullable
    public VillagerEmotionSystem.EmotionalState getVillagerEmotionalState(@NotNull VillagerEntity villager) {
        return emotionSystem.getEmotionalState(villager);
    }
    
    /**
     * Shutdown the AI system
     */
    public void shutdown() {
        LOGGER.info("Shutting down AIWorldManagerRefactored...");
        
        // Shutdown gossip manager first
        try {
            gossipManager.shutdown();
        } catch (Exception e) {
            LOGGER.error("Error shutting down gossip manager", e);
        }
        
        // Shutdown subsystem manager (handles individual subsystems)
        subsystemManager.shutdown();
        
        LOGGER.info("AIWorldManagerRefactored shutdown complete");
    }
    
    /**
     * Get comprehensive analytics
     */
    @NotNull
    public Map<String, Object> getAIAnalytics() {
        Map<String, Object> analytics = subsystemManager.getAnalytics();
        
        // Add gossip analytics separately
        try {
            analytics.put("gossip", gossipManager.getAnalytics());
        } catch (Exception e) {
            analytics.put("gossip_error", e.getMessage());
        }
        
        return analytics;
    }
    
    // Direct subsystem access for compatibility and special cases
    @NotNull public VillagerEmotionSystem getEmotionSystem() { return emotionSystem; }
    @NotNull public VillagerLearningSystem getLearningSystem() { return learningSystem; }
    @NotNull public VillagerGossipManager getGossipManager() { return gossipManager; }
    @NotNull public VillagerGOAPRefactored getPlanningSystem() { return planningSystem; }
    @NotNull public VillagerQuestSystem getQuestSystem() { return questSystem; }
    @NotNull public VillagerAIManager getAIManager() { return aiManager; }
    @NotNull public VillagerAIManager getVillagerAIManager() { return aiManager; } // Compatibility alias
    @NotNull public AISubsystemManager getSubsystemManager() { return subsystemManager; }
    @NotNull public MinecraftServer getServer() { return server; }
    
    // Legacy compatibility methods for VillagerAIManager
    public void onVillagerTrade(@NotNull net.minecraft.entity.passive.VillagerEntity villager, 
                               @NotNull net.minecraft.entity.player.PlayerEntity player, 
                               boolean successful, int value) {
        aiManager.onVillagerTrade(villager, player, successful, value);
    }
    
    @NotNull
    public java.util.List<net.minecraft.text.Text> getVillagerAIStatus(@NotNull net.minecraft.entity.passive.VillagerEntity villager) {
        return aiManager.getVillagerAIStatus(villager);
    }
    
    @Nullable
    public VillagerAIManager.VillagerAIState getVillagerAIState(@NotNull net.minecraft.entity.passive.VillagerEntity villager) {
        return aiManager.getVillagerAIState(villager);
    }
    
    @NotNull
    public Map<String, Object> getGlobalAIAnalytics() {
        return aiManager.getGlobalAIAnalytics();
    }
}