package com.beeny.villagesreborn.core;

import com.beeny.villagesreborn.core.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common module entry point for Villages Reborn
 * Contains cross-platform initialization logic and API definitions
 */
public class VillagesRebornCommon {
    public static final String MOD_ID = "villagesreborn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static ModConfig config;
    private static boolean initialized = false;
    
    /**
     * Initialize the common module
     * This method should be called by platform-specific implementations
     */
    public static void initialize() {
        if (initialized) {
            LOGGER.warn("VillagesRebornCommon already initialized!");
            return;
        }
        
        LOGGER.info("Initializing Villages Reborn Common Module");
        
        try {
            // Load configuration
            config = ModConfig.load();
            LOGGER.info("Configuration loaded successfully");
            
            // Initialize core systems
            initializeCoreSystems();
            
            initialized = true;
            LOGGER.info("Villages Reborn Common Module initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Villages Reborn Common Module", e);
            throw new RuntimeException("Common module initialization failed", e);
        }
    }
    
    /**
     * Get the current configuration
     */
    public static ModConfig getConfig() {
        if (!initialized) {
            throw new IllegalStateException("Common module not initialized");
        }
        return config;
    }
    
    /**
     * Check if the common module is initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    private static void initializeCoreSystems() {
        // Initialize core game systems here
        LOGGER.debug("Core systems initialized");
    }
    
    /**
     * Reset the module state - FOR TESTING ONLY
     */
    public static void resetForTesting() {
        initialized = false;
        config = null;
    }
}