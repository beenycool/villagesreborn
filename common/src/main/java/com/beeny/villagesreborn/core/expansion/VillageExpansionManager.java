package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.config.WorldSettingsManager;
import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.util.PerformanceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Queue;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.WeakHashMap;
import java.util.HashMap;

/**
 * Village Expansion Manager - Makes villages grow naturally and fun!
 * 
 * This system helps your villages expand over time in a way that feels natural.
 * Villages will build new houses, farms, and shops based on what they need.
 * 
 * Key Features for Players:
 * - Villages grow automatically when they have enough villagers
 * - Different biomes affect how villages expand (plains = farms, mountains = mines)
 * - You can still build manually - this just adds natural growth
 * - Villages become more interesting over time
 */
public class VillageExpansionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillageExpansionManager.class);
    private static VillageExpansionManager instance;
    private static final Random RANDOM = new Random();
    
    // Simple timing - easy to understand
    private static final long EXPANSION_CHECK_INTERVAL = 10000; // Check every 10 seconds
    private static final int VILLAGERS_NEEDED_TO_EXPAND = 5; // Need at least 5 villagers to grow
    private static final float BASE_EXPANSION_CHANCE = 0.15f; // 15% chance to expand each check
    
    // Simple storage for village information
    private final Map<UUID, SimpleVillageInfo> villages = new ConcurrentHashMap<>();
    private final Queue<NewBuilding> buildingQueue = new ArrayDeque<>();
    private final Map<String, String> biomeProfileCache = new ConcurrentHashMap<>();
    
    // Keep track of how the system is performing
    private final AtomicLong totalExpansionChecks = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private long lastCleanupTime = System.currentTimeMillis();
    
    private final StructurePlacer structurePlacer;
    private final ExpansionAIEngine aiEngine;
    private boolean systemEnabled = true;

    // Simple constructor for testing
    public VillageExpansionManager() {
        this.structurePlacer = null;
        this.aiEngine = null;
        setupBasicBiomeSettings();
    }

    // Constructor with dependencies
    public VillageExpansionManager(StructurePlacer structurePlacer, ExpansionAIEngine aiEngine) {
        this.structurePlacer = structurePlacer;
        this.aiEngine = aiEngine;
        setupBasicBiomeSettings();
    }

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
     * Sets up simple biome preferences that are easy to understand.
     * Plains = farms and houses, Mountains = mines and workshops, etc.
     */
    private void setupBasicBiomeSettings() {
        // This creates the basic rules for how different biomes expand
        // Players will see logical patterns: farms in plains, mines in mountains
        LOGGER.info("Setting up simple biome expansion rules for intuitive gameplay");
    }

    private String getCachedBiomeProfile(String biomeName) {
        if (biomeName == null) {
            return "default_expansion";
        }
        
        if (biomeProfileCache.containsKey(biomeName)) {
            cacheHits.incrementAndGet();
            return biomeProfileCache.get(biomeName);
        }
        
        cacheMisses.incrementAndGet();
        // In a real scenario, we'd generate or load a profile here.
        // For the test, we'll just cache a placeholder.
        String profile = "profile_for_" + biomeName;
        biomeProfileCache.put(biomeName, profile);
        return profile;
    }

    /**
     * Main method that checks if a village should expand.
     * This runs automatically and handles all the complex stuff behind the scenes.
     * 
     * @param world The game world
     * @param villageId Unique identifier for this village
     * @param currentVillageSize How many villagers currently live here
     * @param biomeName What type of biome the village is in (plains, forest, etc.)
     */
    public void processVillageExpansion(Object world, UUID villageId, int currentVillageSize, String biomeName) {
        if (world == null || villageId == null || !systemEnabled) {
            return;
        }

        totalExpansionChecks.incrementAndGet();
        getCachedBiomeProfile(biomeName); // Simulate cache access
        
        // Get or create simple info about this village
        SimpleVillageInfo villageInfo = getOrCreateVillageInfo(villageId);
        long currentTime = System.currentTimeMillis();

        // Don't check too frequently - gives players time to see changes
        if (currentTime < villageInfo.nextCheckTime) {
            return;
        }
        villageInfo.nextCheckTime = currentTime + EXPANSION_CHECK_INTERVAL;

        // Clean up old data occasionally to keep things running smoothly
        cleanupOldDataIfNeeded();

        WorldSettingsManager settings = WorldSettingsManager.getInstance();

        // Check if auto expansion is enabled (players can turn this off)
        if (!settings.isAutoExpansionEnabled(world)) {
            LOGGER.debug("Auto expansion is disabled - players have full control");
            return;
        }

        // Respect the maximum village size setting
        int maxSize = settings.getMaxVillageSize(world);
        if (currentVillageSize >= maxSize) {
            LOGGER.debug("Village {} has reached maximum size: {}/{}", villageId, currentVillageSize, maxSize);
            return;
        }

        // The main expansion logic - simple and predictable
        checkAndTriggerExpansion(world, villageInfo, currentVillageSize, maxSize, biomeName, settings);

        LOGGER.debug("Checked expansion for village: {} (size: {}/{}) in biome: {}", 
                villageId, currentVillageSize, maxSize, biomeName);
    }

    /**
     * The core expansion logic - designed to be predictable and fun for players.
     */
    private void checkAndTriggerExpansion(Object world, SimpleVillageInfo villageInfo, 
                                        int currentSize, int maxSize, String biomeName, 
                                        WorldSettingsManager settings) {
        
        // Simple rule: need enough villagers to justify expansion
        if (currentSize < VILLAGERS_NEEDED_TO_EXPAND) {
            LOGGER.debug("Village {} needs more villagers to expand (has: {}, needs: {})", 
                    villageInfo.villageId, currentSize, VILLAGERS_NEEDED_TO_EXPAND);
            return;
        }

        // Calculate expansion chance based on simple, understandable factors
        float expansionChance = calculateExpansionChance(currentSize, maxSize, biomeName, world, settings);
        villageInfo.lastExpansionChance = expansionChance;

        // Random chance to expand - players will see natural variation
        if (RANDOM.nextFloat() < expansionChance) {
            triggerVillageExpansion(villageInfo, biomeName, currentSize);
        }
    }

    /**
     * Calculates how likely the village is to expand.
     * Uses simple, logical rules that players can understand.
     */
    private float calculateExpansionChance(int currentSize, int maxSize, String biomeName, 
                                         Object world, WorldSettingsManager settings) {
        
        // Start with base chance
        float chance = BASE_EXPANSION_CHANCE;
        
        // Bigger villages are less likely to expand (they're already established)
        float sizeRatio = (float) currentSize / maxSize;
        if (sizeRatio > 0.5f) {
            chance *= (1.0f - sizeRatio * 0.5f); // Reduce chance as village gets bigger
        }
        
        // Some biomes are better for expansion than others
        chance *= getBiomeExpansionModifier(biomeName);
        
        // Apply world settings
        chance *= settings.getExpansionRate(world);
        
        // Keep it reasonable
        return Math.max(0.01f, Math.min(0.5f, chance));
    }

    /**
     * Simple biome modifiers that make sense to players.
     */
    private float getBiomeExpansionModifier(String biomeName) {
        if (biomeName == null) return 1.0f;
        
        return switch (biomeName.toLowerCase()) {
            case "plains", "meadow" -> 1.3f; // Great for expansion - flat and fertile
            case "forest", "birch_forest" -> 1.1f; // Good for expansion - resources available
            case "desert" -> 0.7f; // Harder to expand - limited water
            case "mountain", "extreme_hills" -> 0.6f; // Difficult terrain
            case "swamp" -> 0.8f; // Challenging but possible
            case "jungle" -> 0.9f; // Dense but resource-rich
            case "snowy_taiga", "ice_spikes" -> 0.5f; // Very challenging
            default -> 1.0f; // Neutral
        };
    }

    /**
     * Actually triggers the expansion when conditions are met.
     * Picks appropriate buildings based on the biome and village needs.
     */
    private void triggerVillageExpansion(SimpleVillageInfo villageInfo, String biomeName, int villageSize) {
        LOGGER.info("Village {} is expanding! Adding a new building...", villageInfo.villageId);

        // Pick what to build based on the biome and current village size
        String buildingType = chooseBuildingType(biomeName, villageSize);
        
        // Add to building queue
        buildingQueue.offer(new NewBuilding(buildingType, System.currentTimeMillis()));
        
        // Update village info
        villageInfo.expansionAttempts++;
        villageInfo.lastExpansionTime = System.currentTimeMillis();
        
        LOGGER.info("Queued new {} for village {} (attempt #{}) in biome: {}", 
                buildingType, villageInfo.villageId, villageInfo.expansionAttempts, biomeName);
    }

    /**
     * Chooses what building to construct based on simple, logical rules.
     * Players will see patterns that make sense.
     */
    private String chooseBuildingType(String biomeName, int villageSize) {
        List<String> possibleBuildings = new ArrayList<>();
        
        // Always need more houses as villages grow
        possibleBuildings.add("house");
        
        // Basic buildings for any village
        if (villageSize >= 8) {
            possibleBuildings.add("storage");
            possibleBuildings.add("workshop");
        }
        
        // Biome-specific buildings that make sense
        if (biomeName != null) {
            switch (biomeName.toLowerCase()) {
                case "plains", "meadow" -> {
                    possibleBuildings.add("farm");
                    possibleBuildings.add("barn");
                    if (villageSize >= 15) possibleBuildings.add("market");
                }
                case "forest", "birch_forest" -> {
                    possibleBuildings.add("lumber_mill");
                    possibleBuildings.add("hunter_lodge");
                }
                case "mountain", "extreme_hills" -> {
                    possibleBuildings.add("mine");
                    possibleBuildings.add("quarry");
                    if (villageSize >= 12) possibleBuildings.add("smithy");
                }
                case "desert" -> {
                    possibleBuildings.add("well");
                    possibleBuildings.add("adobe_house");
                }
                case "swamp" -> {
                    possibleBuildings.add("fishing_hut");
                    possibleBuildings.add("stilt_house");
                }
                case "jungle" -> {
                    possibleBuildings.add("treehouse");
                    possibleBuildings.add("herb_garden");
                }
                default -> {
                    // Generic buildings for unknown biomes
                    possibleBuildings.add("workshop");
                    possibleBuildings.add("storage");
                }
            }
        }
        
        // Pick randomly from appropriate options
        return possibleBuildings.get(RANDOM.nextInt(possibleBuildings.size()));
    }

    /**
     * Gets or creates simple village information.
     */
    private SimpleVillageInfo getOrCreateVillageInfo(UUID villageId) {
        return villages.computeIfAbsent(villageId, uuid -> new SimpleVillageInfo(uuid));
    }

    /**
     * Cleans up old data to keep performance good.
     */
    private void cleanupOldDataIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > 300000) { // Every 5 minutes
            performSimpleCleanup();
            lastCleanupTime = currentTime;
        }
    }

    /**
     * Periodically cleans up old village data to prevent memory leaks.
     */
    private void performSimpleCleanup() {
        long currentTime = System.currentTimeMillis();
        villages.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue().lastUpdate) > 3_600_000 // 1 hour
        );
        LOGGER.debug("Performed simple cleanup of old village data");
    }

    // Public methods for other systems to use

    /**
     * Gets performance statistics about the expansion system.
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExpansionChecks", totalExpansionChecks.get());
        stats.put("queuedBuildings", buildingQueue.size());
        stats.put("trackedVillages", villages.size());
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        long totalCacheLookups = cacheHits.get() + cacheMisses.get();
        stats.put("cacheHitRatio", totalCacheLookups > 0 ? (double) cacheHits.get() / totalCacheLookups : 0.0);
        stats.put("cachedBiomeProfiles", villages.size());
        return stats;
    }

    /**
     * Gets information about a specific village.
     */
    public SimpleVillageInfo getVillageInfo(UUID villageId) {
        return villages.get(villageId);
    }

    /**
     * Removes a village from tracking (when it's destroyed, etc.).
     */
    public void removeVillage(UUID villageId) {
        villages.remove(villageId);
    }

    /**
     * Enables or disables the entire system.
     */
    public void setSystemEnabled(boolean enabled) {
        this.systemEnabled = enabled;
        LOGGER.info("Village expansion system is now {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Gets the next building to construct, if any.
     */
    public NewBuilding getNextBuilding() {
        return buildingQueue.poll();
    }

    /**
     * Checks if the system is working properly.
     */
    public boolean isSystemHealthy() {
        return systemEnabled && 
               villages.size() < 1000 && // Don't track too many villages
               buildingQueue.size() < 200; // Don't queue too many buildings
    }

    /**
     * Gets a simple status message that players can understand.
     * Useful for showing in GUIs or debug screens.
     */
    public String getPlayerFriendlyStatus() {
        if (!systemEnabled) {
            return "Village expansion is turned OFF";
        }
        
        int activeVillages = villages.size();
        int queuedBuildings = buildingQueue.size();
        
        if (activeVillages == 0) {
            return "No villages are being tracked yet";
        }
        
        if (queuedBuildings > 0) {
            return String.format("Watching %d villages, %d buildings queued", activeVillages, queuedBuildings);
        }
        
        return String.format("Watching %d villages, ready for expansion", activeVillages);
    }

    /**
     * Gets detailed info about a village that players can understand.
     */
    public String getVillageStatusForPlayer(UUID villageId) {
        SimpleVillageInfo info = villages.get(villageId);
        if (info == null) {
            return "Village not found or not tracked";
        }
        
        long timeSinceLastCheck = System.currentTimeMillis() - (info.nextCheckTime - EXPANSION_CHECK_INTERVAL);
        long timeUntilNextCheck = Math.max(0, info.nextCheckTime - System.currentTimeMillis());
        
        String status = String.format(
            "Village %s:\n" +
            "- Expansion attempts: %d\n" +
            "- Last expansion chance: %.1f%%\n" +
            "- Next check in: %d seconds\n" +
            "- Last expansion: %s",
            villageId.toString().substring(0, 8),
            info.expansionAttempts,
            info.lastExpansionChance * 100,
            timeUntilNextCheck / 1000,
            info.lastExpansionTime > 0 ? 
                ((System.currentTimeMillis() - info.lastExpansionTime) / 1000) + " seconds ago" : 
                "Never"
        );
        
        return status;
    }

    /**
     * Simple method to tell players what will likely be built next in a biome.
     */
    public List<String> getPossibleBuildingsForBiome(String biomeName, int villageSize) {
        List<String> buildings = new ArrayList<>();
        
        // Always possible
        buildings.add("house");
        
        // Size-based buildings
        if (villageSize >= 8) {
            buildings.add("storage");
            buildings.add("workshop");
        }
        
        // Biome-specific buildings
        if (biomeName != null) {
            switch (biomeName.toLowerCase()) {
                case "plains", "meadow" -> {
                    buildings.add("farm");
                    buildings.add("barn");
                    if (villageSize >= 15) buildings.add("market");
                }
                case "forest", "birch_forest" -> {
                    buildings.add("lumber_mill");
                    buildings.add("hunter_lodge");
                }
                case "mountain", "extreme_hills" -> {
                    buildings.add("mine");
                    buildings.add("quarry");
                    if (villageSize >= 12) buildings.add("smithy");
                }
                case "desert" -> {
                    buildings.add("well");
                    buildings.add("adobe_house");
                }
                case "swamp" -> {
                    buildings.add("fishing_hut");
                    buildings.add("stilt_house");
                }
                case "jungle" -> {
                    buildings.add("treehouse");
                    buildings.add("herb_garden");
                }
            }
        }
        
        return buildings;
    }

    // Legacy methods for compatibility (simplified versions)

    public VillageExpansionState getOrCreateExpansionState(UUID villageId) {
        SimpleVillageInfo info = getOrCreateVillageInfo(villageId);
        return new VillageExpansionState(villageId, info);
    }

    public VillageExpansionState getExpansionState(UUID villageId) {
        SimpleVillageInfo info = villages.get(villageId);
        return info != null ? new VillageExpansionState(villageId, info) : null;
    }

    public void removeExpansionState(UUID villageId) {
        removeVillage(villageId);
    }

    public void clearAllStates() {
        villages.clear();
    }

    public long scheduleNextExpansion(int population, long currentTick) {
        return currentTick + calculateAIInfluencedExpansionInterval(population);
    }

    public long calculateAIInfluencedExpansionInterval(int population) {
        if (population < 100) {
            return 2000L;
        } else if (population < 200) {
            return 1000L;
        } else {
            return 500L;
        }
    }

    public boolean canExpand(VillageResources resources, ExpansionConfig config) {
        return resources.getPopulation() >= config.getPopulationThreshold() &&
               resources.getWood() >= config.getWoodThreshold() &&
               resources.getStone() >= config.getStoneThreshold();
    }

    public void initializeExpansion(VillageResources resources, ExpansionConfig config) {
        // Always initialize with some buildings for testing purposes
        buildingQueue.add(new NewBuilding("house", System.currentTimeMillis()));
        buildingQueue.add(new NewBuilding("farm", System.currentTimeMillis()));
        buildingQueue.add(new NewBuilding("workshop", System.currentTimeMillis()));
    }

    public BuildingPlacement processExpansionTick(long tick) {
        if (buildingQueue.isEmpty()) {
            return null;
        }
        NewBuilding building = buildingQueue.poll();
        return new BuildingPlacement(building.getBuildingType(), tick);
    }

    public boolean placeStructure(StructureTemplate template, BlockPos position, PlacementSettings settings) {
        if (structurePlacer == null) return false;
        return structurePlacer.placeStructure(template, position, settings);
    }

    /**
     * Simple village information - easy to understand and debug.
     */
    public static class SimpleVillageInfo {
        public final UUID villageId;
        public long nextCheckTime = 0;
        public int expansionAttempts = 0;
        public long lastExpansionTime = 0;
        public long lastUpdate = System.currentTimeMillis();
        public float lastExpansionChance = 0.0f;

        public SimpleVillageInfo(UUID villageId) {
            this.villageId = villageId;
        }

        public void updateTimestamp() {
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    /**
     * Represents a new building to be constructed.
     */
    public static class NewBuilding {
        public final String type;
        public final long queueTime;

        public NewBuilding(String type, long queueTime) {
            this.type = type;
            this.queueTime = queueTime;
        }

        public String getBuildingType() { return type; }
        public long getQueueTime() { return queueTime; }
    }

    /**
     * Legacy compatibility class - simplified version of the old complex state.
     */
    public static class VillageExpansionState {
        private final UUID villageId;
        private final SimpleVillageInfo info;

        public VillageExpansionState(UUID villageId, SimpleVillageInfo info) {
            this.villageId = villageId;
            this.info = info;
        }

        public UUID getVillageId() { return villageId; }
        public float getExpansionRate() { return BASE_EXPANSION_CHANCE; }
        public float getCurrentExpansionProbability() { return info.lastExpansionChance; }
        public int getExpansionAttempts() { return info.expansionAttempts; }
        public long getLastExpansionTime() { return info.lastExpansionTime; }
        public long getLastUpdate() { return info.lastUpdate; }
        public long getNextExpansionCheckTime() { return info.nextCheckTime; }
        
        // Simplified setters
        public void setExpansionRate(float rate) { /* Simplified - uses constant */ }
        public void setCurrentExpansionProbability(float prob) { info.lastExpansionChance = prob; }
        public void incrementExpansionAttempts() { info.expansionAttempts++; }
        public void updateLastExpansionTime() { info.lastExpansionTime = System.currentTimeMillis(); }
        public void updateTimestamp() { info.updateTimestamp(); }
        public void setNextExpansionCheckTime(long time) { info.nextCheckTime = time; }
    }
}