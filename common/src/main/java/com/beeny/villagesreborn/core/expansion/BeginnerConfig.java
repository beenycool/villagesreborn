package com.beeny.villagesreborn.core.expansion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple configuration loader for beginners.
 * Reads settings from a plain text file that's easy to understand and edit.
 */
public class BeginnerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeginnerConfig.class);
    private static BeginnerConfig instance;
    
    // Default values (used if config file is missing or corrupted)
    private static final long DEFAULT_CHECK_INTERVAL = 10; // seconds
    private static final int DEFAULT_MIN_VILLAGERS = 5;
    private static final float DEFAULT_EXPANSION_CHANCE = 0.15f; // 15%
    
    private final Properties config;
    private boolean configLoaded = false;

    private BeginnerConfig() {
        this.config = new Properties();
        loadConfiguration();
    }

    public static BeginnerConfig getInstance() {
        if (instance == null) {
            synchronized (BeginnerConfig.class) {
                if (instance == null) {
                    instance = new BeginnerConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Loads the configuration from the simple config file.
     * If the file is missing or has errors, uses safe defaults.
     */
    private void loadConfiguration() {
        try (InputStream stream = getClass().getResourceAsStream("/villagesreborn_simple_config.properties")) {
            if (stream != null) {
                config.load(stream);
                configLoaded = true;
                LOGGER.info("Loaded beginner-friendly configuration successfully");
            } else {
                LOGGER.warn("Configuration file not found, using default values");
                useDefaults();
            }
        } catch (IOException e) {
            LOGGER.error("Error reading configuration file, using defaults: {}", e.getMessage());
            useDefaults();
        }
    }

    /**
     * Sets up safe default values if config file can't be loaded.
     */
    private void useDefaults() {
        config.setProperty("expansion_check_interval_seconds", String.valueOf(DEFAULT_CHECK_INTERVAL));
        config.setProperty("minimum_villagers_for_expansion", String.valueOf(DEFAULT_MIN_VILLAGERS));
        config.setProperty("base_expansion_chance_percent", String.valueOf((int)(DEFAULT_EXPANSION_CHANCE * 100)));
        
        // Set up default biome modifiers
        config.setProperty("plains_expansion_modifier", "1.3");
        config.setProperty("forest_expansion_modifier", "1.1");
        config.setProperty("desert_expansion_modifier", "0.7");
        config.setProperty("mountain_expansion_modifier", "0.6");
        config.setProperty("swamp_expansion_modifier", "0.8");
        
        configLoaded = true;
    }

    /**
     * Gets how often to check for expansion (in milliseconds for the game).
     */
    public long getExpansionCheckInterval() {
        int seconds = getIntValue("expansion_check_interval_seconds", (int)DEFAULT_CHECK_INTERVAL);
        return seconds * 1000L; // Convert to milliseconds
    }

    /**
     * Gets minimum villagers needed before expansion starts.
     */
    public int getMinimumVillagersForExpansion() {
        return getIntValue("minimum_villagers_for_expansion", DEFAULT_MIN_VILLAGERS);
    }

    /**
     * Gets base expansion chance (as a decimal between 0 and 1).
     */
    public float getBaseExpansionChance() {
        int percent = getIntValue("base_expansion_chance_percent", (int)(DEFAULT_EXPANSION_CHANCE * 100));
        return Math.max(0.01f, Math.min(1.0f, percent / 100.0f)); // Keep it reasonable
    }

    /**
     * Gets expansion modifier for a specific biome.
     */
    public float getBiomeExpansionModifier(String biomeName) {
        if (biomeName == null) return 1.0f;
        
        String key = biomeName.toLowerCase().replace(" ", "_") + "_expansion_modifier";
        float modifier = getFloatValue(key, 1.0f);
        
        // Keep modifiers within reasonable bounds
        return Math.max(0.1f, Math.min(3.0f, modifier));
    }

    /**
     * Gets village size threshold for medium villages (when storage/workshops unlock).
     */
    public int getMediumVillageThreshold() {
        return getIntValue("medium_village_threshold", 8);
    }

    /**
     * Gets village size threshold for large villages (when markets unlock).
     */
    public int getLargeVillageThreshold() {
        return getIntValue("large_village_threshold", 15);
    }

    /**
     * Gets village size threshold for smithies in mountain villages.
     */
    public int getSmithyThreshold() {
        return getIntValue("smithy_threshold", 12);
    }

    /**
     * Gets maximum number of villages to track (for performance).
     */
    public int getMaxTrackedVillages() {
        return getIntValue("max_tracked_villages", 1000);
    }

    /**
     * Gets maximum building queue size (for performance).
     */
    public int getMaxBuildingQueue() {
        return getIntValue("max_building_queue", 200);
    }

    /**
     * Helper method to safely get integer values from config.
     */
    private int getIntValue(String key, int defaultValue) {
        try {
            String value = config.getProperty(key);
            if (value != null) {
                return Integer.parseInt(value.trim());
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid integer value for {}, using default: {}", key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Helper method to safely get float values from config.
     */
    private float getFloatValue(String key, float defaultValue) {
        try {
            String value = config.getProperty(key);
            if (value != null) {
                return Float.parseFloat(value.trim());
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid float value for {}, using default: {}", key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Returns true if the configuration was loaded successfully.
     */
    public boolean isConfigLoaded() {
        return configLoaded;
    }

    /**
     * Gets a simple status message about the configuration.
     */
    public String getConfigStatus() {
        if (!configLoaded) {
            return "Configuration not loaded - using safe defaults";
        }
        
        return String.format(
            "Configuration loaded successfully:\n" +
            "- Check interval: %d seconds\n" +
            "- Min villagers: %d\n" +
            "- Expansion chance: %.1f%%\n" +
            "- Medium village threshold: %d\n" +
            "- Large village threshold: %d",
            getExpansionCheckInterval() / 1000,
            getMinimumVillagersForExpansion(),
            getBaseExpansionChance() * 100,
            getMediumVillageThreshold(),
            getLargeVillageThreshold()
        );
    }

    /**
     * Reloads the configuration from file.
     * Useful if players edit the config file while the game is running.
     */
    public void reloadConfiguration() {
        config.clear();
        loadConfiguration();
        LOGGER.info("Configuration reloaded");
    }
} 