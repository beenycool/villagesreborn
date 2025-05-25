package com.beeny.villagesreborn.core.config;

import com.beeny.villagesreborn.core.llm.LLMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages first-time setup configuration persistence
 * Tracks whether the welcome screen has been completed and stores user preferences
 */
public class FirstTimeSetupConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirstTimeSetupConfig.class);
    private static final String SETUP_CONFIG_FILE = "villagesreborn_setup.properties";
    
    private boolean setupCompleted;
    private LLMProvider selectedProvider;
    private String selectedModel;
    private boolean hardwareDetectionCompleted;
    
    private FirstTimeSetupConfig(boolean setupCompleted, LLMProvider selectedProvider, 
                               String selectedModel, boolean hardwareDetectionCompleted) {
        this.setupCompleted = setupCompleted;
        this.selectedProvider = selectedProvider;
        this.selectedModel = selectedModel;
        this.hardwareDetectionCompleted = hardwareDetectionCompleted;
    }
    
    /**
     * Get the config file path, respecting the current working directory
     */
    private static Path getConfigPath() {
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, SETUP_CONFIG_FILE);
    }
    
    /**
     * Load first-time setup configuration from file
     */
    public static FirstTimeSetupConfig load() {
        LOGGER.debug("Loading first-time setup configuration");
        
        Properties props = new Properties();
        Path configPath = getConfigPath();
        
        if (Files.exists(configPath)) {
            try {
                props.load(Files.newInputStream(configPath));
                LOGGER.debug("Loaded setup configuration from {}", configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed to load setup config file {}: {}", configPath, e.getMessage());
            }
        } else {
            LOGGER.debug("Setup configuration file does not exist, using defaults");
        }
        
        return createFromProperties(props);
    }
    
    /**
     * Save the current configuration to file
     */
    public void save() {
        LOGGER.debug("Saving first-time setup configuration");
        
        Properties props = new Properties();
        props.setProperty("setup.completed", String.valueOf(setupCompleted));
        props.setProperty("hardware.detection.completed", String.valueOf(hardwareDetectionCompleted));
        
        if (selectedProvider != null) {
            props.setProperty("llm.provider", selectedProvider.name());
        }
        if (selectedModel != null) {
            props.setProperty("llm.model", selectedModel);
        }
        
        Path configPath = getConfigPath();
        try {
            // Ensure parent directory exists
            Files.createDirectories(configPath.getParent());
            props.store(Files.newOutputStream(configPath), "Villages Reborn First-Time Setup Configuration");
            LOGGER.info("Saved setup configuration to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save setup configuration to {}", configPath, e);
        }
    }
    
    /**
     * Mark the setup as completed with the selected configuration
     */
    public void completeSetup(LLMProvider provider, String model) {
        this.setupCompleted = true;
        this.selectedProvider = provider;
        this.selectedModel = model;
        this.hardwareDetectionCompleted = true;
        save();
        LOGGER.info("First-time setup completed with provider: {}, model: {}", provider, model);
    }
    
    /**
     * Reset the setup configuration (for testing or re-setup)
     */
    public void reset() {
        this.setupCompleted = false;
        this.selectedProvider = null;
        this.selectedModel = null;
        this.hardwareDetectionCompleted = false;
        
        Path configPath = getConfigPath();
        try {
            Files.deleteIfExists(configPath);
            LOGGER.info("Reset first-time setup configuration");
        } catch (IOException e) {
            LOGGER.warn("Failed to delete setup config file during reset", e);
        }
    }
    
    private static FirstTimeSetupConfig createFromProperties(Properties props) {
        boolean setupCompleted = Boolean.parseBoolean(props.getProperty("setup.completed", "false"));
        boolean hardwareDetectionCompleted = Boolean.parseBoolean(props.getProperty("hardware.detection.completed", "false"));
        
        LLMProvider selectedProvider = null;
        String providerName = props.getProperty("llm.provider");
        if (providerName != null) {
            try {
                selectedProvider = LLMProvider.valueOf(providerName);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown LLM provider in config: {}", providerName);
            }
        }
        
        String selectedModel = props.getProperty("llm.model");
        
        return new FirstTimeSetupConfig(setupCompleted, selectedProvider, selectedModel, hardwareDetectionCompleted);
    }
    
    // Getters
    public boolean isSetupCompleted() {
        return setupCompleted;
    }
    
    public LLMProvider getSelectedProvider() {
        return selectedProvider;
    }
    
    public String getSelectedModel() {
        return selectedModel;
    }
    
    public boolean isHardwareDetectionCompleted() {
        return hardwareDetectionCompleted;
    }
}