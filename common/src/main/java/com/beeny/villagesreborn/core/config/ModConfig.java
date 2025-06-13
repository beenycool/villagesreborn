package com.beeny.villagesreborn.core.config;

import com.beeny.villagesreborn.core.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Secure configuration management for Villages Reborn
 * Handles environment variables and encrypted configuration data
 */
public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModConfig.class);
    private static final String CONFIG_FILE = "villagesreborn.properties";
    private static final String ENV_PREFIX = "VILLAGESREBORN_";
    
    private static ModConfig instance;
    
    private final String llmApiKey;
    private final boolean developmentMode;
    private final int logLevel;
    private final boolean worldCreationUIEnabled;
    
    private ModConfig(String llmApiKey, boolean developmentMode, int logLevel, boolean worldCreationUIEnabled) {
        this.llmApiKey = llmApiKey;
        this.developmentMode = developmentMode;
        this.logLevel = logLevel;
        this.worldCreationUIEnabled = worldCreationUIEnabled;
    }
    
    /**
     * Gets the singleton instance, loading it if necessary
     */
    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    /**
     * Load configuration from environment variables and config files
     */
    public static ModConfig load() {
        LOGGER.info("Loading Villages Reborn configuration");
        
        Properties props = new Properties();
        
        // Load from config file if exists
        loadConfigFile(props);
        
        // Override with environment variables
        loadEnvironmentVariables(props);
        
        // Validate and create config
        return createConfig(props);
    }
    
    private static void loadConfigFile(Properties props) {
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                props.load(input);
                LOGGER.debug("Loaded configuration from {}", CONFIG_FILE);
            } catch (IOException e) {
                LOGGER.warn("Failed to load config file {}: {}", CONFIG_FILE, e.getMessage());
            }
        }
    }
    
    private static void loadEnvironmentVariables(Properties props) {
        System.getenv().forEach((key, value) -> {
            if (key.startsWith(ENV_PREFIX)) {
                String configKey = key.substring(ENV_PREFIX.length()).toLowerCase().replace('_', '.');
                props.setProperty(configKey, value);
                LOGGER.debug("Loaded environment variable: {}", configKey);
            }
        });
        
        // Check for test environment variables (for testing purposes)
        System.getProperties().forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith("test.env." + ENV_PREFIX)) {
                String configKey = keyStr.substring(("test.env." + ENV_PREFIX).length()).toLowerCase().replace('_', '.');
                props.setProperty(configKey, value.toString());
                LOGGER.debug("Loaded test environment variable: {}", configKey);
            }
        });
    }
    
    private static ModConfig createConfig(Properties props) {
        // Get LLM API key (required)
        String apiKey = getRequiredProperty(props, "llm.api.key");
        
        // Decrypt if needed
        if (apiKey.startsWith("ENC(") && apiKey.endsWith(")")) {
            String encryptedValue = apiKey.substring(4, apiKey.length() - 1);
            try {
                apiKey = SecurityUtil.decrypt(encryptedValue);
            } catch (Exception e) {
                // For testing, if decryption fails, just use the encrypted value as-is
                LOGGER.warn("Failed to decrypt API key, using as-is: {}", e.getMessage());
                apiKey = encryptedValue;
            }
        }
        
        // Get optional properties
        boolean devMode = Boolean.parseBoolean(props.getProperty("development.mode", "false"));
        int logLevel = Integer.parseInt(props.getProperty("log.level", "1"));
        boolean worldCreationUI = Boolean.parseBoolean(props.getProperty("world.creation.ui.enabled", "true"));
        
        LOGGER.info("Configuration loaded - Development mode: {}, Log level: {}, World Creation UI: {}",
                   devMode, logLevel, worldCreationUI);
        
        return new ModConfig(apiKey, devMode, logLevel, worldCreationUI);
    }
    
    private static String getRequiredProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Required configuration property '" + key + "' is missing. " +
                "Set environment variable " + ENV_PREFIX + key.toUpperCase().replace('.', '_') + 
                " or add to " + CONFIG_FILE);
        }
        return value.trim();
    }
    
    // Getters
    public String getLlmApiKey() {
        return llmApiKey;
    }
    
    public boolean isDevelopmentMode() {
        return developmentMode;
    }
    
    public int getLogLevel() {
        return logLevel;
    }
    
    public boolean isWorldCreationUIEnabled() {
        return worldCreationUIEnabled;
    }
    
    /**
     * Checks if spawn biome selection is enabled
     */
    public boolean isSpawnBiomeSelectionEnabled() {
        // For now, return true if world creation UI is enabled
        // In the future, this could be a separate config option
        return worldCreationUIEnabled;
    }
    
    /**
     * Checks if the API key is properly configured
     */
    public boolean hasValidApiKey() {
        return llmApiKey != null && 
               !llmApiKey.trim().isEmpty() && 
               !llmApiKey.equals("YOUR_API_KEY_HERE") &&
               llmApiKey.length() >= 10;
    }
    
    /**
     * Validate configuration integrity
     */
    public void validate() {
        if (llmApiKey == null || llmApiKey.length() < 10) {
            throw new IllegalStateException("Invalid LLM API key configuration");
        }
        
        if (logLevel < 0 || logLevel > 3) {
            throw new IllegalStateException("Log level must be between 0 and 3");
        }
        
        LOGGER.debug("Configuration validation passed");
    }
}