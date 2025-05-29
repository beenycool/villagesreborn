package com.beeny.villagesreborn.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AIConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIConfig.class);
    private static final String CONFIG_FILE = "villagesreborn-ai.properties";
    private static final String ENV_PREFIX = "VILLAGESREBORN_AI_";
    
    private static AIConfig instance;
    
    private final int quirkCheckInterval;
    private final boolean quirksEnabled;
    private final float defaultQuirkWeight;
    
    private AIConfig(int quirkCheckInterval, boolean quirksEnabled, float defaultQuirkWeight) {
        this.quirkCheckInterval = quirkCheckInterval;
        this.quirksEnabled = quirksEnabled;
        this.defaultQuirkWeight = defaultQuirkWeight;
    }
    
    public static AIConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    public static AIConfig load() {
        LOGGER.info("Loading Villages Reborn AI configuration");
        
        Properties props = new Properties();
        loadConfigFile(props);
        loadEnvironmentVariables(props);
        
        return createConfig(props);
    }
    
    private static void loadConfigFile(Properties props) {
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                props.load(input);
                LOGGER.debug("Loaded AI configuration from {}", CONFIG_FILE);
            } catch (IOException e) {
                LOGGER.warn("Failed to load AI config file {}: {}", CONFIG_FILE, e.getMessage());
            }
        }
    }
    
    private static void loadEnvironmentVariables(Properties props) {
        System.getenv().forEach((key, value) -> {
            if (key.startsWith(ENV_PREFIX)) {
                String configKey = key.substring(ENV_PREFIX.length()).toLowerCase().replace('_', '.');
                props.setProperty(configKey, value);
                LOGGER.debug("Loaded AI environment variable: {}", configKey);
            }
        });
    }
    
    private static AIConfig createConfig(Properties props) {
        int interval = Integer.parseInt(props.getProperty("quirk.check.interval", "600")); // Default: 30 seconds (600 ticks)
        boolean enabled = Boolean.parseBoolean(props.getProperty("quirks.enabled", "true"));
        float weight = Float.parseFloat(props.getProperty("default.quirk.weight", "0.5"));
        
        LOGGER.info("AI Configuration loaded - Quirks Enabled: {}, Check Interval: {} ticks, Default Weight: {}",
                   enabled, interval, weight);
        
        return new AIConfig(interval, enabled, weight);
    }
    
    // Getters
    public int getQuirkCheckInterval() {
        return quirkCheckInterval;
    }
    
    public boolean quirksEnabled() {
        return quirksEnabled;
    }
    
    public float getDefaultQuirkWeight() {
        return defaultQuirkWeight;
    }
}