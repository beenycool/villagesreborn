package com.beeny.villagesreborn.core.world;

import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * World-specific configuration settings for Villages Reborn
 * These settings are stored per-world and can be configured during world creation
 * Platform-agnostic implementation with serialization handled by platform modules
 */
public class VillagesRebornWorldSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornWorldSettings.class);
    
    // Villager AI Configuration
    private int villagerMemoryLimit = 150;        // Range: 50-500
    private float aiAggressionLevel = 0.3f;       // Range: 0.0-1.0
    private boolean enableAdvancedAI = true;
    
    // Village Expansion
    private boolean autoExpansionEnabled = true;
    private int maxVillageSize = 50;             // Range: 10-200
    private float expansionRate = 1.0f;          // Range: 0.1-2.0
    
    // Phase 2: Enhanced Village Expansion
    private boolean biomeSpecificExpansion = true;
    private int maxCaravanDistance = 1000;      // Range: 500-5000
    private boolean interdimensionalVillages = false;
    private int villageGenerationDensity = 2;   // Range: 1-5
    
    // Feature Toggles
    private boolean electionsEnabled = true;
    private boolean assistantVillagersEnabled = true;
    private boolean dynamicTradingEnabled = true;
    private boolean villagerRelationships = true;
    
    // Performance Settings
    private boolean adaptivePerformance = true;
    private int tickOptimizationLevel = 1;       // Range: 0-3
    
    // Version tracking
    private String version = "2.0";
    private long createdTimestamp = System.currentTimeMillis();
    
    // Custom data storage for extensions
    private Map<String, Object> customData = new HashMap<>();
    
    // Default constructor
    public VillagesRebornWorldSettings() {}
    
    // Full constructor
    public VillagesRebornWorldSettings(int villagerMemoryLimit, float aiAggressionLevel, boolean enableAdvancedAI,
                                     boolean autoExpansionEnabled, int maxVillageSize, float expansionRate,
                                     boolean biomeSpecificExpansion, int maxCaravanDistance,
                                     boolean interdimensionalVillages, int villageGenerationDensity,
                                     boolean electionsEnabled, boolean assistantVillagersEnabled,
                                     boolean dynamicTradingEnabled, boolean villagerRelationships,
                                     boolean adaptivePerformance, int tickOptimizationLevel,
                                     String version, long createdTimestamp) {
        this.villagerMemoryLimit = villagerMemoryLimit;
        this.aiAggressionLevel = aiAggressionLevel;
        this.enableAdvancedAI = enableAdvancedAI;
        this.autoExpansionEnabled = autoExpansionEnabled;
        this.maxVillageSize = maxVillageSize;
        this.expansionRate = expansionRate;
        this.biomeSpecificExpansion = biomeSpecificExpansion;
        this.maxCaravanDistance = maxCaravanDistance;
        this.interdimensionalVillages = interdimensionalVillages;
        this.villageGenerationDensity = villageGenerationDensity;
        this.electionsEnabled = electionsEnabled;
        this.assistantVillagersEnabled = assistantVillagersEnabled;
        this.dynamicTradingEnabled = dynamicTradingEnabled;
        this.villagerRelationships = villagerRelationships;
        this.adaptivePerformance = adaptivePerformance;
        this.tickOptimizationLevel = tickOptimizationLevel;
        this.version = version;
        this.createdTimestamp = createdTimestamp;
    }
    
    /**
     * Creates default settings based on hardware capabilities
     */
    public static VillagesRebornWorldSettings createDefaults(HardwareInfo hardware) {
        VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
        
        // Hardware-based defaults
        HardwareTier tier = hardware.getHardwareTier();
        switch (tier) {
            case LOW -> {
                settings.villagerMemoryLimit = 50;
                settings.enableAdvancedAI = false;
                settings.tickOptimizationLevel = 3;
                settings.maxVillageSize = 25;
                settings.expansionRate = 0.5f;
            }
            case MEDIUM -> {
                settings.villagerMemoryLimit = 150;
                settings.enableAdvancedAI = true;
                settings.tickOptimizationLevel = 2;
                settings.maxVillageSize = 50;
                settings.expansionRate = 1.0f;
            }
            case HIGH -> {
                settings.villagerMemoryLimit = 300;
                settings.enableAdvancedAI = true;
                settings.tickOptimizationLevel = 1;
                settings.maxVillageSize = 100;
                settings.expansionRate = 1.5f;
            }
            case UNKNOWN -> {
                settings.villagerMemoryLimit = 100;
                settings.enableAdvancedAI = false;
                settings.tickOptimizationLevel = 2;
                settings.maxVillageSize = 40;
                settings.expansionRate = 0.8f;
            }
        }
        
        LOGGER.info("Created default world settings for hardware tier: {}", tier);
        return settings;
    }
    
    /**
     * Serializes settings to a Map for platform-agnostic storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("villager_memory_limit", villagerMemoryLimit);
        map.put("ai_aggression_level", aiAggressionLevel);
        map.put("enable_advanced_ai", enableAdvancedAI);
        map.put("auto_expansion_enabled", autoExpansionEnabled);
        map.put("max_village_size", maxVillageSize);
        map.put("expansion_rate", expansionRate);
        // Phase 2 fields
        map.put("biome_specific_expansion", biomeSpecificExpansion);
        map.put("max_caravan_distance", maxCaravanDistance);
        map.put("interdimensional_villages", interdimensionalVillages);
        map.put("village_generation_density", villageGenerationDensity);
        map.put("elections_enabled", electionsEnabled);
        map.put("assistant_villagers_enabled", assistantVillagersEnabled);
        map.put("dynamic_trading_enabled", dynamicTradingEnabled);
        map.put("villager_relationships", villagerRelationships);
        map.put("adaptive_performance", adaptivePerformance);
        map.put("tick_optimization_level", tickOptimizationLevel);
        map.put("version", version);
        map.put("created_timestamp", createdTimestamp);
        map.put("custom_data", new HashMap<>(customData));
        return map;
    }
    
    /**
     * Deserializes settings from a Map
     */
    public static VillagesRebornWorldSettings fromMap(Map<String, Object> map) {
        try {
            VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
            
            if (map.containsKey("villager_memory_limit")) {
                settings.villagerMemoryLimit = ((Number) map.get("villager_memory_limit")).intValue();
            }
            if (map.containsKey("ai_aggression_level")) {
                settings.aiAggressionLevel = ((Number) map.get("ai_aggression_level")).floatValue();
            }
            if (map.containsKey("enable_advanced_ai")) {
                settings.enableAdvancedAI = (Boolean) map.get("enable_advanced_ai");
            }
            if (map.containsKey("auto_expansion_enabled")) {
                settings.autoExpansionEnabled = (Boolean) map.get("auto_expansion_enabled");
            }
            if (map.containsKey("max_village_size")) {
                settings.maxVillageSize = ((Number) map.get("max_village_size")).intValue();
            }
            if (map.containsKey("expansion_rate")) {
                settings.expansionRate = ((Number) map.get("expansion_rate")).floatValue();
            }
            // Phase 2 fields
            if (map.containsKey("biome_specific_expansion")) {
                settings.biomeSpecificExpansion = (Boolean) map.get("biome_specific_expansion");
            }
            if (map.containsKey("max_caravan_distance")) {
                settings.maxCaravanDistance = ((Number) map.get("max_caravan_distance")).intValue();
            }
            if (map.containsKey("interdimensional_villages")) {
                settings.interdimensionalVillages = (Boolean) map.get("interdimensional_villages");
            }
            if (map.containsKey("village_generation_density")) {
                settings.villageGenerationDensity = ((Number) map.get("village_generation_density")).intValue();
            }
            if (map.containsKey("elections_enabled")) {
                settings.electionsEnabled = (Boolean) map.get("elections_enabled");
            }
            if (map.containsKey("assistant_villagers_enabled")) {
                settings.assistantVillagersEnabled = (Boolean) map.get("assistant_villagers_enabled");
            }
            if (map.containsKey("dynamic_trading_enabled")) {
                settings.dynamicTradingEnabled = (Boolean) map.get("dynamic_trading_enabled");
            }
            if (map.containsKey("villager_relationships")) {
                settings.villagerRelationships = (Boolean) map.get("villager_relationships");
            }
            if (map.containsKey("adaptive_performance")) {
                settings.adaptivePerformance = (Boolean) map.get("adaptive_performance");
            }
            if (map.containsKey("tick_optimization_level")) {
                settings.tickOptimizationLevel = ((Number) map.get("tick_optimization_level")).intValue();
            }
            if (map.containsKey("version")) {
                settings.version = (String) map.get("version");
            }
            if (map.containsKey("created_timestamp")) {
                settings.createdTimestamp = ((Number) map.get("created_timestamp")).longValue();
            }
            if (map.containsKey("custom_data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customDataMap = (Map<String, Object>) map.get("custom_data");
                if (customDataMap != null) {
                    settings.customData = new HashMap<>(customDataMap);
                }
            }
            
            return settings;
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize world settings from map, using defaults", e);
            return new VillagesRebornWorldSettings();
        }
    }
    
    /**
     * Creates a deep copy of this settings object
     */
    public VillagesRebornWorldSettings copy() {
        return new VillagesRebornWorldSettings(
            villagerMemoryLimit, aiAggressionLevel, enableAdvancedAI,
            autoExpansionEnabled, maxVillageSize, expansionRate,
            biomeSpecificExpansion, maxCaravanDistance, interdimensionalVillages, villageGenerationDensity,
            electionsEnabled, assistantVillagersEnabled, dynamicTradingEnabled,
            villagerRelationships, adaptivePerformance, tickOptimizationLevel,
            version, createdTimestamp
        );
    }
    
    /**
     * Validates settings and clamps values to acceptable ranges
     */
    public void validate() {
        villagerMemoryLimit = Math.max(50, Math.min(500, villagerMemoryLimit));
        aiAggressionLevel = Math.max(0.0f, Math.min(1.0f, aiAggressionLevel));
        maxVillageSize = Math.max(10, Math.min(200, maxVillageSize));
        expansionRate = Math.max(0.1f, Math.min(2.0f, expansionRate));
        // Phase 2 validation
        maxCaravanDistance = Math.max(500, Math.min(5000, maxCaravanDistance));
        villageGenerationDensity = Math.max(1, Math.min(5, villageGenerationDensity));
        tickOptimizationLevel = Math.max(0, Math.min(3, tickOptimizationLevel));
    }
    
    // Getters and Setters
    public int getVillagerMemoryLimit() { return villagerMemoryLimit; }
    public void setVillagerMemoryLimit(int villagerMemoryLimit) { this.villagerMemoryLimit = villagerMemoryLimit; }
    
    public float getAiAggressionLevel() { return aiAggressionLevel; }
    public void setAiAggressionLevel(float aiAggressionLevel) { this.aiAggressionLevel = aiAggressionLevel; }
    
    public boolean isEnableAdvancedAI() { return enableAdvancedAI; }
    public void setEnableAdvancedAI(boolean enableAdvancedAI) { this.enableAdvancedAI = enableAdvancedAI; }
    
    public boolean isAutoExpansionEnabled() { return autoExpansionEnabled; }
    public void setAutoExpansionEnabled(boolean autoExpansionEnabled) { this.autoExpansionEnabled = autoExpansionEnabled; }
    
    public int getMaxVillageSize() { return maxVillageSize; }
    public void setMaxVillageSize(int maxVillageSize) { this.maxVillageSize = maxVillageSize; }
    
    public float getExpansionRate() { return expansionRate; }
    public void setExpansionRate(float expansionRate) { this.expansionRate = expansionRate; }
    
    // Phase 2 getters and setters
    public boolean isBiomeSpecificExpansion() { return biomeSpecificExpansion; }
    public void setBiomeSpecificExpansion(boolean biomeSpecificExpansion) { this.biomeSpecificExpansion = biomeSpecificExpansion; }
    
    public int getMaxCaravanDistance() { return maxCaravanDistance; }
    public void setMaxCaravanDistance(int maxCaravanDistance) { this.maxCaravanDistance = maxCaravanDistance; }
    
    public boolean isInterdimensionalVillages() { return interdimensionalVillages; }
    public void setInterdimensionalVillages(boolean interdimensionalVillages) { this.interdimensionalVillages = interdimensionalVillages; }
    
    public int getVillageGenerationDensity() { return villageGenerationDensity; }
    public void setVillageGenerationDensity(int villageGenerationDensity) { this.villageGenerationDensity = villageGenerationDensity; }
    
    public boolean isElectionsEnabled() { return electionsEnabled; }
    public void setElectionsEnabled(boolean electionsEnabled) { this.electionsEnabled = electionsEnabled; }
    
    public boolean isAssistantVillagersEnabled() { return assistantVillagersEnabled; }
    public void setAssistantVillagersEnabled(boolean assistantVillagersEnabled) { this.assistantVillagersEnabled = assistantVillagersEnabled; }
    
    public boolean isDynamicTradingEnabled() { return dynamicTradingEnabled; }
    public void setDynamicTradingEnabled(boolean dynamicTradingEnabled) { this.dynamicTradingEnabled = dynamicTradingEnabled; }
    
    public boolean isVillagerRelationships() { return villagerRelationships; }
    public void setVillagerRelationships(boolean villagerRelationships) { this.villagerRelationships = villagerRelationships; }
    
    public boolean isAdaptivePerformance() { return adaptivePerformance; }
    public void setAdaptivePerformance(boolean adaptivePerformance) { this.adaptivePerformance = adaptivePerformance; }
    
    public int getTickOptimizationLevel() { return tickOptimizationLevel; }
    public void setTickOptimizationLevel(int tickOptimizationLevel) { this.tickOptimizationLevel = tickOptimizationLevel; }
    
    public String getVersion() { return version; }
    public long getCreatedTimestamp() { return createdTimestamp; }
    
    public Map<String, Object> getCustomData() { return customData; }
    public void setCustomData(Map<String, Object> customData) { this.customData = customData != null ? customData : new HashMap<>(); }
    
    @Override
    public String toString() {
        return "VillagesRebornWorldSettings{" +
                "villagerMemoryLimit=" + villagerMemoryLimit +
                ", aiAggressionLevel=" + aiAggressionLevel +
                ", enableAdvancedAI=" + enableAdvancedAI +
                ", autoExpansionEnabled=" + autoExpansionEnabled +
                ", maxVillageSize=" + maxVillageSize +
                ", expansionRate=" + expansionRate +
                ", electionsEnabled=" + electionsEnabled +
                ", assistantVillagersEnabled=" + assistantVillagersEnabled +
                ", dynamicTradingEnabled=" + dynamicTradingEnabled +
                ", villagerRelationships=" + villagerRelationships +
                ", adaptivePerformance=" + adaptivePerformance +
                ", tickOptimizationLevel=" + tickOptimizationLevel +
                ", version='" + version + '\'' +
                '}';
    }
}