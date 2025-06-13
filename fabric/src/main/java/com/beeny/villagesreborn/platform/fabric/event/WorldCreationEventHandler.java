package com.beeny.villagesreborn.platform.fabric.event;

import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldDataPersistent;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import com.beeny.villagesreborn.core.world.WorldCreationSettingsCapture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles world creation and lifecycle events for Villages Reborn
 * Manages initialization and persistence of world-specific settings
 */
public class WorldCreationEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCreationEventHandler.class);
    
    /**
     * Registers event handlers for world creation and lifecycle
     */
    public static void register() {
        ServerWorldEvents.LOAD.register(WorldCreationEventHandler::onServerWorldLoad);
        ServerLifecycleEvents.SERVER_STOPPING.register(WorldCreationEventHandler::onServerStopping);
    }
    
    /**
     * Called when a server world is loaded
     * Initializes world data if it doesn't exist
     */
    private static void onServerWorldLoad(MinecraftServer server, ServerWorld world) {
        try {
            LOGGER.info("Server world loaded: {}", world.getRegistryKey().getValue());
            
            // Get or create world data
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            
            // Initialize with default settings if this is a new world
            if (!worldData.hasSettings()) {
                LOGGER.info("Initializing new world - checking for captured settings");
                
                // First try to get captured settings from world creation UI
                VillagesRebornWorldSettings capturedSettings = WorldCreationSettingsCapture.retrieve();
                if (capturedSettings != null) {
                    LOGGER.info("Applying captured settings from world creation UI: {}", capturedSettings);
                    worldData.setSettings(capturedSettings);
                    
                    // Also check for spawn biome choice and store it
                    try {
                        var spawnBiomeChoice = WorldCreationSettingsCapture.getSpawnBiomeChoice();
                        if (spawnBiomeChoice != null) {
                            // Check if the object is actually an instance of BiomeDisplayInfo before casting
                            if (spawnBiomeChoice instanceof com.beeny.villagesreborn.platform.fabric.biome.BiomeDisplayInfo biomeDisplayInfo) {
                                LOGGER.info("Storing spawn biome choice: {}", biomeDisplayInfo.getRegistryKey().getValue());
                                
                                // Store the spawn biome choice in world data
                                var spawnBiomeStorageManager = com.beeny.villagesreborn.platform.fabric.spawn.managers.SpawnBiomeStorageManager.getInstance();
                                var spawnChoiceData = new com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData(
                                    biomeDisplayInfo.getRegistryKey(), 
                                    System.currentTimeMillis()
                                );
                                spawnBiomeStorageManager.setWorldSpawnBiome(world, spawnChoiceData);
                                LOGGER.info("Successfully stored spawn biome choice for world");
                            } else {
                                LOGGER.warn("Spawn biome choice is not a BiomeDisplayInfo instance: {}", spawnBiomeChoice.getClass().getName());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to store spawn biome choice", e);
                    }
                } else {
                    // Fall back to hardware-based defaults
                    LOGGER.info("No captured settings found, using hardware-based defaults");
                    try {
                        var hardwareInfo = HardwareInfoManager.getInstance().getHardwareInfo();
                        var defaultSettings = VillagesRebornWorldSettings.createDefaults(hardwareInfo);
                        
                        worldData.setSettings(defaultSettings);
                        LOGGER.info("Applied default settings based on hardware tier: {}",
                                   hardwareInfo.getHardwareTier());
                    } catch (Exception e) {
                        LOGGER.error("Failed to get hardware info, using fallback defaults", e);
                        worldData.setSettings(new VillagesRebornWorldSettings());
                    }
                }
            } else {
                LOGGER.debug("Loaded existing world settings: {}", worldData.getSettings());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle world load event for world: {}", 
                        world.getRegistryKey().getValue(), e);
        }
    }
    
    /**
     * Called when the server is stopping
     * Ensures all world data is properly saved
     */
    private static void onServerStopping(MinecraftServer server) {
        try {
            LOGGER.info("Server stopping, ensuring world data is saved");
            
            for (ServerWorld world : server.getWorlds()) {
                try {
                    VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
                    if (worldData.hasSettings()) {
                        worldData.markDirty(); // Ensure data is marked for saving
                        LOGGER.debug("Marked world data as dirty for saving: {}", 
                                   world.getRegistryKey().getValue());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to save world data for world: {}", 
                               world.getRegistryKey().getValue(), e);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle server stopping event", e);
        }
    }
    
    /**
     * Initialize world data for a new world with specific settings
     * This method can be called during world creation
     */
    public static void initializeForNewWorld(ServerWorld world, VillagesRebornWorldSettings settings) {
        try {
            LOGGER.info("Initializing new world with custom settings: {}", 
                       world.getRegistryKey().getValue());
            
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            worldData.setSettings(settings);
            
            LOGGER.info("Successfully initialized world with custom settings");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize world with custom settings", e);
        }
    }
    
    /**
     * Update a specific setting for a world at runtime
     */
    public static void updateWorldSetting(ServerWorld world, String settingName, Object value) {
        try {
            LOGGER.debug("Updating world setting: {} = {}", settingName, value);
            
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            worldData.updateSetting(settingName, value);
            
            LOGGER.debug("Successfully updated world setting: {}", settingName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to update world setting: {} = {}", settingName, value, e);
        }
    }
    
    /**
     * Get the current settings for a world
     */
    public static VillagesRebornWorldSettings getWorldSettings(ServerWorld world) {
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            return worldData.getSettings();
        } catch (Exception e) {
            LOGGER.error("Failed to get world settings, returning defaults", e);
            return new VillagesRebornWorldSettings();
        }
    }
    
    /**
     * Check if a world has custom settings configured
     */
    public static boolean hasWorldSettings(ServerWorld world) {
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            return worldData.hasSettings();
        } catch (Exception e) {
            LOGGER.error("Failed to check if world has settings", e);
            return false;
        }
    }
}