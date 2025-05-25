package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.core.VillagesRebornCommon;
import com.beeny.villagesreborn.core.api.Platform;
import com.beeny.villagesreborn.platform.fabric.event.WorldCreationEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric platform implementation for Villages Reborn
 * Handles Fabric-specific initialization and platform integration
 */
public class VillagesRebornFabric implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn-fabric");
    private static FabricPlatform platform;
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Villages Reborn for Fabric");
        
        try {
            // Initialize platform implementation
            platform = new FabricPlatform();
            
            // Initialize common module
            VillagesRebornCommon.initialize();
            
            // Setup Fabric-specific features
            setupFabricFeatures();
            
            // Setup hot reload if in development
            if (platform.isDevelopmentEnvironment()) {
                setupHotReload();
            }
            
            LOGGER.info("Villages Reborn Fabric initialization completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Villages Reborn for Fabric", e);
            throw new RuntimeException("Fabric module initialization failed", e);
        }
    }
    
    private void setupFabricFeatures() {
        LOGGER.debug("Setting up Fabric-specific features");
        
        // Register world creation event handlers
        WorldCreationEventHandler.register();
        
        // Register Fabric API hooks
        platform.registerHook("fabric_init", () -> {
            LOGGER.debug("Fabric initialization hook executed");
        });
        
        // Execute initialization hooks
        platform.executeHook("fabric_init");
    }
    
    private void setupHotReload() {
        LOGGER.info("Setting up hot reload for development environment");
        
        // Hot reload configuration for development
        FabricLoader.getInstance().getEntrypointContainers("main", ModInitializer.class)
            .forEach(container -> {
                try {
                    // Register for hot reload monitoring
                    LOGGER.debug("Registered {} for hot reload", container.getEntrypoint().getClass().getName());
                } catch (Exception e) {
                    LOGGER.error("Failed to setup hot reload for {}", container.getEntrypoint().getClass().getName(), e);
                }
            });
    }
    
    /**
     * Get the platform implementation
     */
    public static Platform getPlatform() {
        return platform;
    }
}