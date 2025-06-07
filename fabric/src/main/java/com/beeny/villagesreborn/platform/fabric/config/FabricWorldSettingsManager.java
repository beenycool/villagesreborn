package com.beeny.villagesreborn.platform.fabric.config;

import com.beeny.villagesreborn.core.config.WorldSettingsManager;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldDataPersistent;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric-specific implementation of WorldSettingsManager
 * Provides access to world settings stored in persistent world data
 */
public class FabricWorldSettingsManager extends WorldSettingsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricWorldSettingsManager.class);
    private static FabricWorldSettingsManager instance;
    
    private FabricWorldSettingsManager() {
        super();
    }
    
    public static FabricWorldSettingsManager getInstance() {
        if (instance == null) {
            synchronized (FabricWorldSettingsManager.class) {
                if (instance == null) {
                    instance = new FabricWorldSettingsManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize the Fabric settings manager as the global instance
     */
    public static void initialize() {
        // Replace the global instance with the Fabric-specific one
        WorldSettingsManager.instance = getInstance();
        LOGGER.info("Initialized Fabric world settings manager");
    }
    
    @Override
    protected VillagesRebornWorldSettings loadWorldSettings(Object world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            LOGGER.warn("Attempted to load settings for non-ServerWorld: {}", world.getClass().getSimpleName());
            return null;
        }
        
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(serverWorld);
            VillagesRebornWorldSettings settings = worldData.getSettings();
            
            if (settings == null) {
                LOGGER.debug("No settings found for world, creating defaults");
                settings = new VillagesRebornWorldSettings();
                worldData.setSettings(settings);
            }
            
            return settings;
        } catch (Exception e) {
            LOGGER.error("Failed to load world settings for world: {}", serverWorld.getRegistryKey().getValue(), e);
            return null;
        }
    }
    
    /**
     * Saves settings to persistent world data
     */
    public void saveWorldSettings(ServerWorld world, VillagesRebornWorldSettings settings) {
        try {
            VillagesRebornWorldDataPersistent worldData = VillagesRebornWorldDataPersistent.get(world);
            worldData.setSettings(settings);
            
            // Update cache
            updateWorldSettings(world, settings);
            
            LOGGER.debug("Saved world settings for world: {}", world.getRegistryKey().getValue());
        } catch (Exception e) {
            LOGGER.error("Failed to save world settings for world: {}", world.getRegistryKey().getValue(), e);
        }
    }
    
    /**
     * Gets settings specifically for ServerWorld objects
     */
    public VillagesRebornWorldSettings getServerWorldSettings(ServerWorld world) {
        return getWorldSettings(world);
    }
} 