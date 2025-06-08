package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.config.WorldSettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages AI behavior for villagers based on world settings.
 * This manager reduces the performance impact of AI calculations by processing them on a fixed-time interval
 * rather than on every game tick. It controls aggression levels, advanced AI features, and behavior patterns.
 */
public class VillagerAIBehaviorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerAIBehaviorManager.class);
    private static final long AI_PROCESSING_INTERVAL_MS = 2000; // Process AI every 2 seconds
    private static VillagerAIBehaviorManager instance;
    
    private final Map<UUID, VillagerBehaviorState> villagerStates = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    private VillagerAIBehaviorManager() {}
    
    public static VillagerAIBehaviorManager getInstance() {
        if (instance == null) {
            synchronized (VillagerAIBehaviorManager.class) {
                if (instance == null) {
                    instance = new VillagerAIBehaviorManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Processes AI behavior for a villager based on world settings.
     * This method is designed to be called on each game tick, but it internally limits the processing
     * of advanced AI to a fixed interval to optimize performance.
     *
     * @param villager The villager entity whose AI is to be processed.
     */
    public void processVillagerAI(VillagerEntity villager) {
        if (villager == null || villager.getWorld() == null) {
            return;
        }
        
        // Get or create behavior state to manage time-based checks
        VillagerBehaviorState state = getOrCreateBehaviorState(villager);
        long currentTime = System.currentTimeMillis();

        // Limit AI processing to a fixed interval to improve performance
        if (currentTime < state.getNextAiUpdateTime()) {
            return;
        }
        state.setNextAiUpdateTime(currentTime + AI_PROCESSING_INTERVAL_MS);
        
        Object world = villager.getWorld();
        WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
        
        // Check if advanced AI is enabled
        if (!settingsManager.isAdvancedAIEnabled(world)) {
            // Use basic AI behavior only
            processBasicAI(villager);
            return;
        }
        
        // Get or create behavior state
        
        // Apply aggression level
        float aggressionLevel = settingsManager.getAiAggressionLevel(world);
        state.setAggressionLevel(aggressionLevel);
        
        // Process advanced AI behaviors
        processAdvancedAI(villager, state, world);
    }
    
    /**
     * Processes basic AI behavior (when advanced AI is disabled)
     */
    private void processBasicAI(VillagerEntity villager) {
        // Basic AI behavior - minimal processing
        LOGGER.debug("Processing basic AI for villager: {}", villager.getUUID());
    }
    
    /**
     * Processes advanced AI behavior with full feature set
     */
    private void processAdvancedAI(VillagerEntity villager, VillagerBehaviorState state, Object world) {
        WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
        
        // Apply memory-based decision making
        int memoryLimit = settingsManager.getVillagerMemoryLimit(world);
        state.updateTimestamp(); // Record that an update has occurred
        processMemoryBasedBehavior(villager, state, memoryLimit);
        
        // Apply aggression-based behavior
        float aggressionLevel = state.getAggressionLevel();
        processAggressionBehavior(villager, state, aggressionLevel);
        
        // Process relationship-based behavior if enabled
        if (settingsManager.isVillagerRelationshipsEnabled(world)) {
            processRelationshipBehavior(villager, state);
        }
        
        // Process dynamic trading if enabled
        if (settingsManager.isDynamicTradingEnabled(world)) {
            processDynamicTrading(villager, state);
        }
        
        LOGGER.debug("Processed advanced AI for villager: {} with aggression: {}", 
                    villager.getUUID(), aggressionLevel);
    }
    
    /**
     * Processes memory-based behavior decisions
     */
    private void processMemoryBasedBehavior(VillagerEntity villager, VillagerBehaviorState state, int memoryLimit) {
        if (memoryLimit > 300) {
            state.setDecisionComplexity(DecisionComplexity.HIGH);
        } else if (memoryLimit > 150) {
            state.setDecisionComplexity(DecisionComplexity.MEDIUM);
        } else {
            state.setDecisionComplexity(DecisionComplexity.LOW);
        }
    }
    
    /**
     * Processes aggression-based behavior
     */
    private void processAggressionBehavior(VillagerEntity villager, VillagerBehaviorState state, float aggressionLevel) {
        if (aggressionLevel > 0.8f) {
            state.setTerritorialBehavior(true);
            state.setFriendliness(0.2f);
        } else if (aggressionLevel > 0.6f) {
            state.setTerritorialBehavior(true);
            state.setFriendliness(0.5f);
        } else if (aggressionLevel > 0.4f) {
            state.setTerritorialBehavior(false);
            state.setFriendliness(0.7f);
        } else if (aggressionLevel > 0.2f) {
            state.setTerritorialBehavior(false);
            state.setFriendliness(0.8f);
        } else {
            state.setTerritorialBehavior(false);
            state.setFriendliness(0.9f);
        }
    }
    
    /**
     * Processes relationship-based behavior
     */
    private void processRelationshipBehavior(VillagerEntity villager, VillagerBehaviorState state) {
        state.setRelationshipAware(true);
    }
    
    /**
     * Processes dynamic trading behavior
     */
    private void processDynamicTrading(VillagerEntity villager, VillagerBehaviorState state) {
        state.setDynamicTradingEnabled(true);
    }
    
    /**
     * Gets or creates a behavior state for a villager.
     *
     * @param villager The villager for whom the state is being retrieved.
     * @return The {@link VillagerBehaviorState} for the given villager.
     */
    private VillagerBehaviorState getOrCreateBehaviorState(VillagerEntity villager) {
        return villagerStates.computeIfAbsent(villager.getUUID(), uuid -> new VillagerBehaviorState(uuid));
    }
    
    /**
     * Gets the current behavior state for a villager
     */
    public VillagerBehaviorState getBehaviorState(VillagerEntity villager) {
        return villagerStates.get(villager.getUUID());
    }
    
    /**
     * Removes behavior state for a villager (cleanup).
     *
     * @param villagerUUID The UUID of the villager whose state is to be removed.
     */
    public void removeBehaviorState(UUID villagerUUID) {
        villagerStates.remove(villagerUUID);
    }
    
    /**
     * Clears all behavior states (for world unload, etc.)
     */
    public void clearAllStates() {
        villagerStates.clear();
    }
    
    /**
     * Represents the current behavior state of a villager
     */
    public static class VillagerBehaviorState {
        private final UUID villagerUUID;
        private float aggressionLevel = 0.3f;
        private DecisionComplexity decisionComplexity = DecisionComplexity.MEDIUM;
        private boolean territorialBehavior = false;
        private float friendliness = 0.7f;
        private boolean relationshipAware = false;
        private boolean dynamicTradingEnabled = false;
        private long lastUpdate = System.currentTimeMillis();
        private long nextAiUpdateTime = 0;
        
        public VillagerBehaviorState(UUID villagerUUID) {
            this.villagerUUID = villagerUUID;
        }
        
        // Getters and setters
        public UUID getVillagerUUID() { return villagerUUID; }
        public float getAggressionLevel() { return aggressionLevel; }
        public void setAggressionLevel(float aggressionLevel) { this.aggressionLevel = aggressionLevel; }
        public DecisionComplexity getDecisionComplexity() { return decisionComplexity; }
        public void setDecisionComplexity(DecisionComplexity decisionComplexity) { this.decisionComplexity = decisionComplexity; }
        public boolean isTerritorialBehavior() { return territorialBehavior; }
        public void setTerritorialBehavior(boolean territorialBehavior) { this.territorialBehavior = territorialBehavior; }
        public float getFriendliness() { return friendliness; }
        public void setFriendliness(float friendliness) { this.friendliness = friendliness; }
        public boolean isRelationshipAware() { return relationshipAware; }
        public void setRelationshipAware(boolean relationshipAware) { this.relationshipAware = relationshipAware; }
        public boolean isDynamicTradingEnabled() { return dynamicTradingEnabled; }
        public void setDynamicTradingEnabled(boolean dynamicTradingEnabled) { this.dynamicTradingEnabled = dynamicTradingEnabled; }
        public long getLastUpdate() { return lastUpdate; }
        public void updateTimestamp() { this.lastUpdate = System.currentTimeMillis(); }
        public long getNextAiUpdateTime() { return nextAiUpdateTime; }
        public void setNextAiUpdateTime(long nextAiUpdateTime) { this.nextAiUpdateTime = nextAiUpdateTime; }
    }
    
    /**
     * Enum for decision complexity levels
     */
    public enum DecisionComplexity {
        LOW,    // Simple decisions, limited memory
        MEDIUM, // Moderate complexity, balanced memory usage
        HIGH    // Complex decisions, extensive memory usage
    }
} 