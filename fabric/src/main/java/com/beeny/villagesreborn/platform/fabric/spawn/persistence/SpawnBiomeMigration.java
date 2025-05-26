package com.beeny.villagesreborn.platform.fabric.spawn.persistence;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldDataPersistent;
import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.VillagesRebornWorldSettingsExtensions;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handles migration from legacy static spawn biome storage to the new persistent storage system
 */
public class SpawnBiomeMigration {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnBiomeMigration.class);
    private static final String MIGRATION_MARKER_KEY = "spawn_biome_migration_v2";
    
    /**
     * Performs migration from legacy static data to the new storage system
     * @param world The server world to migrate data for
     */
    public static void performMigration(ServerWorld world) {
        if (world == null) {
            LOGGER.warn("Cannot perform migration for null world");
            return;
        }
        
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            
            // Check if migration already performed
            if (worldData.hasSettings()) {
                Map<String, Object> customData = worldData.getSettings().getCustomData();
                if (customData.containsKey(MIGRATION_MARKER_KEY)) {
                    LOGGER.debug("Migration already performed for world: {}", world.getRegistryKey().getValue());
                    return;
                }
            }
            
            // Check for legacy static data
            SpawnBiomeChoiceData legacyChoice = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice();
            if (legacyChoice != null) {
                // Migrate to new storage using the helper methods we'll add to VillagesRebornWorldDataPersistent
                setSpawnBiomeChoice(worldData, legacyChoice);
                
                // Clear legacy static storage
                VillagesRebornWorldSettingsExtensions.resetForTest();
                
                LOGGER.info("Migrated legacy spawn biome choice: {} for world: {}", 
                    legacyChoice, world.getRegistryKey().getValue());
            } else {
                LOGGER.debug("No legacy spawn biome choice found for migration in world: {}", 
                    world.getRegistryKey().getValue());
            }
            
            // Mark migration as complete
            markMigrationComplete(worldData);
            
        } catch (Exception e) {
            LOGGER.error("Failed to perform spawn biome migration for world: {}", 
                world.getRegistryKey().getValue(), e);
        }
    }
    
    /**
     * Checks if migration is needed for the specified world
     * @param world The server world to check
     * @return true if migration is needed, false otherwise
     */
    public static boolean needsMigration(ServerWorld world) {
        if (world == null) {
            return false;
        }
        
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            
            if (!worldData.hasSettings()) {
                return hasLegacyData(); // Migration needed if legacy data exists
            }
            
            Map<String, Object> customData = worldData.getSettings().getCustomData();
            Object migrationMarker = customData.get(MIGRATION_MARKER_KEY);
            
            // Migration needed if marker is missing or false
            return migrationMarker == null || !(Boolean) migrationMarker;
            
        } catch (Exception e) {
            LOGGER.error("Failed to check migration status for world: {}", 
                world.getRegistryKey().getValue(), e);
            return false;
        }
    }
    
    /**
     * Checks if legacy data exists that needs migration
     * @return true if legacy data exists, false otherwise
     */
    public static boolean hasLegacyData() {
        try {
            return VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice() != null;
        } catch (Exception e) {
            LOGGER.error("Failed to check for legacy data", e);
            return false;
        }
    }
    
    /**
     * Forces migration for a world, even if already marked as complete
     * @param world The server world to force migration for
     */
    public static void forceMigration(ServerWorld world) {
        if (world == null) {
            LOGGER.warn("Cannot force migration for null world");
            return;
        }
        
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            
            // Remove migration marker to force re-migration
            if (worldData.hasSettings()) {
                Map<String, Object> customData = worldData.getSettings().getCustomData();
                customData.remove(MIGRATION_MARKER_KEY);
                worldData.markDirty();
            }
            
            // Perform migration
            performMigration(world);
            
            LOGGER.info("Forced migration for world: {}", world.getRegistryKey().getValue());
            
        } catch (Exception e) {
            LOGGER.error("Failed to force migration for world: {}", 
                world.getRegistryKey().getValue(), e);
        }
    }
    
    /**
     * Clears migration marker for testing purposes
     * @param world The server world to clear marker for
     */
    public static void clearMigrationMarkerForTest(ServerWorld world) {
        if (world == null) {
            return;
        }
        
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            
            if (worldData.hasSettings()) {
                Map<String, Object> customData = worldData.getSettings().getCustomData();
                customData.remove(MIGRATION_MARKER_KEY);
                worldData.markDirty();
                LOGGER.debug("Cleared migration marker for test in world: {}", 
                    world.getRegistryKey().getValue());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to clear migration marker for world: {}", 
                world.getRegistryKey().getValue(), e);
        }
    }
    
    /**
     * Helper method to set spawn biome choice in world data
     * @param worldData The world data persistent instance
     * @param choice The spawn biome choice data
     */
    private static void setSpawnBiomeChoice(VillagesRebornWorldDataPersistent worldData, 
                                          SpawnBiomeChoiceData choice) {
        if (!worldData.hasSettings()) {
            worldData.setSettings(new com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings());
        }
        
        var settings = worldData.getSettings();
        Map<String, Object> customData = settings.getCustomData();
        Map<String, Object> spawnBiomeMap = SpawnBiomeNBTHandler.serializeToMap(choice);
        
        customData.put("spawn_biome_choices", spawnBiomeMap);
        customData.put("spawn_biome_updated", System.currentTimeMillis());
        
        settings.setCustomData(customData);
        worldData.setSettings(settings);
        worldData.markDirty();
    }
    
    /**
     * Helper method to mark migration as complete
     * @param worldData The world data persistent instance
     */
    private static void markMigrationComplete(VillagesRebornWorldDataPersistent worldData) {
        if (!worldData.hasSettings()) {
            worldData.setSettings(new com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings());
        }
        
        var settings = worldData.getSettings();
        Map<String, Object> customData = settings.getCustomData();
        customData.put(MIGRATION_MARKER_KEY, true);
        customData.put("migration_timestamp", System.currentTimeMillis());
        
        settings.setCustomData(customData);
        worldData.setSettings(settings);
        worldData.markDirty();
        
        LOGGER.debug("Marked migration as complete");
    }
}