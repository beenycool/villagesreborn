package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.config.WorldSettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages village expansion based on world settings
 * Controls auto expansion, village size limits, expansion rates, and biome-specific expansion
 */
public class VillageExpansionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillageExpansionManager.class);
    private static VillageExpansionManager instance;
    
    private final Map<UUID, VillageExpansionState> villageStates = new ConcurrentHashMap<>();
    
    private VillageExpansionManager() {}
    
    public static VillageExpansionManager getInstance() {
        if (instance == null) {
            synchronized (VillageExpansionManager.class) {
                if (instance == null) {
                    instance = new VillageExpansionManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Processes village expansion for a village based on world settings
     */
    public void processVillageExpansion(Object world, UUID villageId, int currentVillageSize, String biomeName) {
        if (world == null || villageId == null) {
            return;
        }
        
        WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
        
        // Check if auto expansion is enabled
        if (!settingsManager.isAutoExpansionEnabled(world)) {
            LOGGER.debug("Auto expansion disabled for village: {}", villageId);
            return;
        }
        
        // Get or create expansion state
        VillageExpansionState state = getOrCreateExpansionState(villageId);
        
        // Check village size limits
        int maxVillageSize = settingsManager.getMaxVillageSize(world);
        if (currentVillageSize >= maxVillageSize) {
            LOGGER.debug("Village {} has reached maximum size: {}/{}", villageId, currentVillageSize, maxVillageSize);
            return;
        }
        
        // Apply expansion rate
        float expansionRate = settingsManager.getExpansionRate(world);
        state.setExpansionRate(expansionRate);
        
        // Check biome-specific expansion
        boolean biomeSpecificExpansion = settingsManager.isBiomeSpecificExpansionEnabled(world);
        if (biomeSpecificExpansion) {
            processBiomeSpecificExpansion(state, biomeName, world);
        }
        
        // Process expansion logic
        processExpansionLogic(state, currentVillageSize, maxVillageSize, world);
        
        LOGGER.debug("Processed expansion for village: {} (size: {}/{}, rate: {})", 
                    villageId, currentVillageSize, maxVillageSize, expansionRate);
    }
    
    /**
     * Processes biome-specific expansion rules
     */
    private void processBiomeSpecificExpansion(VillageExpansionState state, String biomeName, Object world) {
        WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
        
        // Different biomes have different expansion characteristics
        float biomeModifier = getBiomeExpansionModifier(biomeName);
        state.setBiomeExpansionModifier(biomeModifier);
        
        // Check for interdimensional villages
        if (settingsManager.isInterdimensionalVillagesEnabled(world)) {
            state.setInterdimensionalExpansionAllowed(true);
        }
        
        // Apply village generation density
        int generationDensity = settingsManager.getVillageGenerationDensity(world);
        state.setGenerationDensity(generationDensity);
    }
    
    /**
     * Gets the expansion modifier for a specific biome
     */
    private float getBiomeExpansionModifier(String biomeName) {
        if (biomeName == null) {
            return 1.0f;
        }
        
        return switch (biomeName.toLowerCase()) {
            case "plains", "meadow" -> 1.2f; // Easier expansion
            case "forest", "birch_forest" -> 1.0f; // Normal expansion
            case "desert", "badlands" -> 0.8f; // Slower expansion
            case "mountain", "extreme_hills" -> 0.6f; // Much slower expansion
            case "swamp", "mangrove_swamp" -> 0.7f; // Difficult expansion
            case "taiga", "snowy_taiga" -> 0.9f; // Slightly slower expansion
            default -> 1.0f; // Default modifier
        };
    }
    
    /**
     * Processes the main expansion logic
     */
    private void processExpansionLogic(VillageExpansionState state, int currentSize, int maxSize, Object world) {
        WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
        
        // Calculate expansion probability based on settings
        float baseExpansionRate = state.getExpansionRate();
        float biomeModifier = state.getBiomeExpansionModifier();
        float finalExpansionRate = baseExpansionRate * biomeModifier;
        
        // Apply size-based scaling (larger villages expand slower)
        float sizeRatio = (float) currentSize / maxSize;
        float sizeModifier = 1.0f - (sizeRatio * 0.5f); // Reduce expansion rate as village grows
        finalExpansionRate *= sizeModifier;
        
        state.setCurrentExpansionProbability(finalExpansionRate);
        
        // Check if expansion should occur this tick
        if (shouldExpandThisTick(finalExpansionRate)) {
            triggerExpansion(state, world);
        }
        
        // Update caravan distance limits
        int maxCaravanDistance = settingsManager.getMaxCaravanDistance(world);
        state.setMaxCaravanDistance(maxCaravanDistance);
    }
    
    /**
     * Determines if expansion should occur this tick
     */
    private boolean shouldExpandThisTick(float expansionProbability) {
        // Simple probability check - could be enhanced with more sophisticated timing
        return Math.random() < (expansionProbability / 1000.0f); // Scale down for per-tick probability
    }
    
    /**
     * Triggers village expansion
     */
    private void triggerExpansion(VillageExpansionState state, Object world) {
        LOGGER.info("Triggering village expansion for village: {}", state.getVillageId());
        
        // This would trigger actual expansion logic in the game
        // For now, just update the state
        state.incrementExpansionAttempts();
        state.updateLastExpansionTime();
        
        // Here you would implement the actual expansion logic:
        // - Find suitable expansion locations
        // - Generate new buildings
        // - Spawn new villagers
        // - Update village boundaries
    }
    
    /**
     * Gets or creates an expansion state for a village
     */
    private VillageExpansionState getOrCreateExpansionState(UUID villageId) {
        return villageStates.computeIfAbsent(villageId, uuid -> new VillageExpansionState(uuid));
    }
    
    /**
     * Gets the current expansion state for a village
     */
    public VillageExpansionState getExpansionState(UUID villageId) {
        return villageStates.get(villageId);
    }
    
    /**
     * Removes expansion state for a village (cleanup)
     */
    public void removeExpansionState(UUID villageId) {
        villageStates.remove(villageId);
    }
    
    /**
     * Clears all expansion states (for world unload, etc.)
     */
    public void clearAllStates() {
        villageStates.clear();
    }
    
    /**
     * Represents the current expansion state of a village
     */
    public static class VillageExpansionState {
        private final UUID villageId;
        private float expansionRate = 1.0f;
        private float biomeExpansionModifier = 1.0f;
        private float currentExpansionProbability = 0.0f;
        private boolean interdimensionalExpansionAllowed = false;
        private int generationDensity = 2;
        private int maxCaravanDistance = 1000;
        private int expansionAttempts = 0;
        private long lastExpansionTime = 0;
        private long lastUpdate = System.currentTimeMillis();
        
        public VillageExpansionState(UUID villageId) {
            this.villageId = villageId;
        }
        
        // Getters and setters
        public UUID getVillageId() { return villageId; }
        public float getExpansionRate() { return expansionRate; }
        public void setExpansionRate(float expansionRate) { this.expansionRate = expansionRate; }
        public float getBiomeExpansionModifier() { return biomeExpansionModifier; }
        public void setBiomeExpansionModifier(float biomeExpansionModifier) { this.biomeExpansionModifier = biomeExpansionModifier; }
        public float getCurrentExpansionProbability() { return currentExpansionProbability; }
        public void setCurrentExpansionProbability(float currentExpansionProbability) { this.currentExpansionProbability = currentExpansionProbability; }
        public boolean isInterdimensionalExpansionAllowed() { return interdimensionalExpansionAllowed; }
        public void setInterdimensionalExpansionAllowed(boolean interdimensionalExpansionAllowed) { this.interdimensionalExpansionAllowed = interdimensionalExpansionAllowed; }
        public int getGenerationDensity() { return generationDensity; }
        public void setGenerationDensity(int generationDensity) { this.generationDensity = generationDensity; }
        public int getMaxCaravanDistance() { return maxCaravanDistance; }
        public void setMaxCaravanDistance(int maxCaravanDistance) { this.maxCaravanDistance = maxCaravanDistance; }
        public int getExpansionAttempts() { return expansionAttempts; }
        public void incrementExpansionAttempts() { this.expansionAttempts++; }
        public long getLastExpansionTime() { return lastExpansionTime; }
        public void updateLastExpansionTime() { this.lastExpansionTime = System.currentTimeMillis(); }
        public long getLastUpdate() { return lastUpdate; }
        public void updateTimestamp() { this.lastUpdate = System.currentTimeMillis(); }
    }
}