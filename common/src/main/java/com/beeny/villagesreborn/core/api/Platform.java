package com.beeny.villagesreborn.core.api;

/**
 * Platform abstraction interface for cross-platform functionality
 * Implementations provide platform-specific behavior for Fabric, Forge, etc.
 */
public interface Platform {
    
    /**
     * Get the current platform name
     */
    String getPlatformName();
    
    /**
     * Check if the game is running in a development environment
     */
    boolean isDevelopmentEnvironment();
    
    /**
     * Check if the platform supports the given feature
     */
    boolean supportsFeature(String feature);
    
    /**
     * Get the mod loader version
     */
    String getModLoaderVersion();
    
    /**
     * Get the Minecraft version
     */
    String getMinecraftVersion();
    
    /**
     * Get the platform's configuration directory
     */
    String getConfigDirectory();
    
    /**
     * Register a platform-specific hook
     */
    void registerHook(String hookName, Runnable hook);
    
    /**
     * Execute a platform-specific hook
     */
    void executeHook(String hookName);
}