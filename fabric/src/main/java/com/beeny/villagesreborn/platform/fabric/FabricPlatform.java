package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.core.api.Platform;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric-specific platform implementation
 * Provides Fabric Loader integration and platform-specific functionality
 */
public class FabricPlatform implements Platform {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricPlatform.class);
    private final Map<String, Runnable> hooks = new HashMap<>();
    private final FabricLoader fabricLoader;
    
    public FabricPlatform() {
        this.fabricLoader = FabricLoader.getInstance();
        LOGGER.debug("Fabric platform implementation initialized");
    }
    
    @Override
    public String getPlatformName() {
        return "Fabric";
    }
    
    @Override
    public boolean isDevelopmentEnvironment() {
        return fabricLoader.isDevelopmentEnvironment();
    }
    
    @Override
    public boolean supportsFeature(String feature) {
        switch (feature.toLowerCase()) {
            case "mixins":
            case "fabric-api":
            case "hot-reload":
            case "development-tools":
                return true;
            case "forge-compatibility":
                return false;
            default:
                LOGGER.debug("Unknown feature queried: {}", feature);
                return false;
        }
    }
    
    @Override
    public String getModLoaderVersion() {
        return fabricLoader.getModContainer("fabricloader")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }
    
    @Override
    public String getMinecraftVersion() {
        return fabricLoader.getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }
    
    @Override
    public String getConfigDirectory() {
        return fabricLoader.getConfigDir().toString();
    }
    
    @Override
    public void registerHook(String hookName, Runnable hook) {
        if (hookName == null || hook == null) {
            throw new IllegalArgumentException("Hook name and implementation cannot be null");
        }
        
        hooks.put(hookName, hook);
        LOGGER.debug("Registered hook: {}", hookName);
    }
    
    @Override
    public void executeHook(String hookName) {
        Runnable hook = hooks.get(hookName);
        if (hook != null) {
            try {
                hook.run();
                LOGGER.debug("Executed hook: {}", hookName);
            } catch (Exception e) {
                LOGGER.error("Failed to execute hook: {}", hookName, e);
            }
        } else {
            LOGGER.warn("Hook not found: {}", hookName);
        }
    }
    
    /**
     * Get Fabric-specific loader instance
     */
    public FabricLoader getFabricLoader() {
        return fabricLoader;
    }
    
    /**
     * Check if a specific mod is loaded
     */
    public boolean isModLoaded(String modId) {
        return fabricLoader.isModLoaded(modId);
    }
    
    /**
     * Get the game directory
     */
    public String getGameDirectory() {
        return fabricLoader.getGameDir().toString();
    }
}