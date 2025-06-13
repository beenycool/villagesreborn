package com.beeny.villagesreborn.core.config;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages access to world-specific settings for various game systems
 * This class provides a centralized way to access configuration settings
 * that affect villager AI, village expansion, and other mod features
 */
public class WorldSettingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldSettingsManager.class);
    protected static WorldSettingsManager instance;
    
    // Cache settings per world to avoid frequent lookups
    private final Map<String, VillagesRebornWorldSettings> worldSettingsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastCacheUpdate = new ConcurrentHashMap<>();
    private static final long CACHE_TIMEOUT_MS = 30000; // 30 seconds
    
    protected WorldSettingsManager() {}
    
    public static WorldSettingsManager getInstance() {
        if (instance == null) {
            synchronized (WorldSettingsManager.class) {
                if (instance == null) {
                    instance = new WorldSettingsManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets the settings for a specific world
     * This method should be implemented by platform-specific code
     */
    public VillagesRebornWorldSettings getWorldSettings(Object world) {
        String worldKey = getWorldKey(world);
        
        // Check cache first
        Long lastUpdate = lastCacheUpdate.get(worldKey);
        if (lastUpdate != null && (System.currentTimeMillis() - lastUpdate) < CACHE_TIMEOUT_MS) {
            VillagesRebornWorldSettings cached = worldSettingsCache.get(worldKey);
            if (cached != null) {
                return cached;
            }
        }
        
        // Load settings from world data
        VillagesRebornWorldSettings settings = loadWorldSettings(world);
        if (settings == null) {
            settings = new VillagesRebornWorldSettings();
            LOGGER.debug("Using default settings for world: {}", worldKey);
        }
        
        // Cache the settings
        worldSettingsCache.put(worldKey, settings);
        lastCacheUpdate.put(worldKey, System.currentTimeMillis());
        
        return settings;
    }
    
    /**
     * Updates the cached settings for a world
     */
    public void updateWorldSettings(Object world, VillagesRebornWorldSettings settings) {
        String worldKey = getWorldKey(world);
        worldSettingsCache.put(worldKey, settings);
        lastCacheUpdate.put(worldKey, System.currentTimeMillis());
        LOGGER.debug("Updated cached settings for world: {}", worldKey);
    }
    
    /**
     * Clears the cache for a specific world
     */
    public void clearWorldCache(Object world) {
        String worldKey = getWorldKey(world);
        worldSettingsCache.remove(worldKey);
        lastCacheUpdate.remove(worldKey);
        LOGGER.debug("Cleared cache for world: {}", worldKey);
    }
    
    /**
     * Clears all cached settings
     */
    public void clearAllCache() {
        worldSettingsCache.clear();
        lastCacheUpdate.clear();
        LOGGER.debug("Cleared all world settings cache");
    }
    
    /**
     * Gets the villager memory limit for a specific world
     */
    public int getVillagerMemoryLimit(Object world) {
        return getWorldSettings(world).getVillagerMemoryLimit();
    }
    
    /**
     * Gets the AI aggression level for a specific world
     */
    public float getAiAggressionLevel(Object world) {
        return getWorldSettings(world).getAiAggressionLevel();
    }
    
    /**
     * Checks if advanced AI is enabled for a specific world
     */
    public boolean isAdvancedAIEnabled(Object world) {
        return getWorldSettings(world).isEnableAdvancedAI();
    }
    
    /**
     * Checks if auto expansion is enabled for a specific world
     */
    public boolean isAutoExpansionEnabled(Object world) {
        return getWorldSettings(world).isAutoExpansionEnabled();
    }
    
    /**
     * Gets the maximum village size for a specific world
     */
    public int getMaxVillageSize(Object world) {
        return getWorldSettings(world).getMaxVillageSize();
    }
    
    /**
     * Gets the expansion rate for a specific world
     */
    public float getExpansionRate(Object world) {
        return getWorldSettings(world).getExpansionRate();
    }
    
    /**
     * Checks if biome-specific expansion is enabled for a specific world
     */
    public boolean isBiomeSpecificExpansionEnabled(Object world) {
        return getWorldSettings(world).isBiomeSpecificExpansion();
    }
    
    /**
     * Gets the maximum caravan distance for a specific world
     */
    public int getMaxCaravanDistance(Object world) {
        return getWorldSettings(world).getMaxCaravanDistance();
    }
    
    /**
     * Checks if interdimensional villages are enabled for a specific world
     */
    public boolean isInterdimensionalVillagesEnabled(Object world) {
        return getWorldSettings(world).isInterdimensionalVillages();
    }
    
    /**
     * Gets the village generation density for a specific world
     */
    public int getVillageGenerationDensity(Object world) {
        return getWorldSettings(world).getVillageGenerationDensity();
    }
    
    /**
     * Checks if elections are enabled for a specific world
     */
    public boolean isElectionsEnabled(Object world) {
        return getWorldSettings(world).isElectionsEnabled();
    }
    
    /**
     * Checks if assistant villagers are enabled for a specific world
     */
    public boolean isAssistantVillagersEnabled(Object world) {
        return getWorldSettings(world).isAssistantVillagersEnabled();
    }
    
    /**
     * Checks if dynamic trading is enabled for a specific world
     */
    public boolean isDynamicTradingEnabled(Object world) {
        return getWorldSettings(world).isDynamicTradingEnabled();
    }
    
    /**
     * Checks if villager relationships are enabled for a specific world
     */
    public boolean isVillagerRelationshipsEnabled(Object world) {
        return getWorldSettings(world).isVillagerRelationships();
    }
    
    /**
     * Checks if adaptive performance is enabled for a specific world
     */
    public boolean isAdaptivePerformanceEnabled(Object world) {
        return getWorldSettings(world).isAdaptivePerformance();
    }
    
    /**
     * Gets the tick optimization level for a specific world
     */
    public int getTickOptimizationLevel(Object world) {
        return getWorldSettings(world).getTickOptimizationLevel();
    }
    
    /**
     * Platform-specific method to load world settings
     * This should be overridden by platform implementations
     */
    protected VillagesRebornWorldSettings loadWorldSettings(Object world) {
        // Default implementation returns null
        // Platform-specific implementations should override this
        return null;
    }
    
    /**
     * Generates a unique key for a world object
     */
    private final Map<Object, String> worldKeyMap = new ConcurrentHashMap<>();
    
    private String getWorldKey(Object world) {
        if (world == null) {
            return "default";
        }
        return worldKeyMap.computeIfAbsent(world, w -> w.getClass().getSimpleName() + "_" + UUID.randomUUID());
    }
} 