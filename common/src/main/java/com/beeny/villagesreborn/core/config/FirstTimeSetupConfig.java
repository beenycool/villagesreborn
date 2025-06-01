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
    private static final int CURRENT_CONFIG_VERSION = 3;
    
    private static ConfigPathResolver configPathResolver = new ConfigPathResolver();
    private static final Object SAVE_LOCK = new Object();
    
    private boolean setupCompleted;
    private LLMProvider selectedProvider;
    private String selectedModel;
    private boolean hardwareDetectionCompleted;
    private long setupTimestamp;
    private int configVersion;
    
    private FirstTimeSetupConfig(boolean setupCompleted, LLMProvider selectedProvider,
                               String selectedModel, boolean hardwareDetectionCompleted,
                               long setupTimestamp, int configVersion) {
        this.setupCompleted = setupCompleted;
        this.selectedProvider = selectedProvider;
        this.selectedModel = selectedModel;
        this.hardwareDetectionCompleted = hardwareDetectionCompleted;
        this.setupTimestamp = setupTimestamp;
        this.configVersion = configVersion;
    }
    
    /**
     * Get the config file path using the appropriate strategy.
     * Uses FabricLoader's config directory when available, otherwise falls back to working directory.
     */
    private static Path getConfigPath() {
        return configPathResolver.resolveConfigPath();
    }
    
    // Package-private method for testing with custom resolver
    static void setConfigPathResolver(ConfigPathResolver resolver) {
        configPathResolver = resolver;
    }
    
    // Package-private method for testing to reset to default resolver
    static void resetConfigPathResolver() {
        configPathResolver = new ConfigPathResolver();
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
     * Load configuration with migration support
     */
    public static FirstTimeSetupConfig loadWithMigration() {
        Path configPath = getConfigPath();
        
        if (!Files.exists(configPath)) {
            LOGGER.debug("Config file does not exist, creating default");
            return createDefaultConfig();
        }
        
        try {
            Properties rawConfig = loadRawConfig(configPath);
            int configVersion = detectConfigVersion(rawConfig);
            
            if (configVersion < CURRENT_CONFIG_VERSION) {
                LOGGER.info("Migrating config from version {} to {}", configVersion, CURRENT_CONFIG_VERSION);
                Properties migratedConfig = migrateConfig(rawConfig, configVersion);
                return createFromProperties(migratedConfig);
            } else {
                return createFromProperties(rawConfig);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration, attempting backup restoration", e);
            return attemptBackupRestoration(configPath);
        }
    }
    
    /**
     * Attempts to restore configuration from backup when main config is corrupt
     */
    private static FirstTimeSetupConfig attemptBackupRestoration(Path configPath) {
        try {
            Path backupPath = createStaticSecureBackupPath(configPath);
            if (Files.exists(backupPath)) {
                LOGGER.info("Attempting to restore from backup: {}", backupPath);
                Properties backupConfig = loadRawConfig(backupPath);
                
                // Restore the backup to main config
                Files.copy(backupPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Successfully restored config from backup");
                
                return createFromProperties(backupConfig);
            }
        } catch (Exception backupException) {
            LOGGER.warn("Failed to restore from backup", backupException);
        }
        
        LOGGER.info("No valid backup found, creating default configuration");
        return createDefaultConfig();
    }
    
    /**
     * Static version of createSecureBackupPath for use in static methods
     */
    private static Path createStaticSecureBackupPath(Path configPath) {
        return SecureBackupPath.createBackupPath(configPath);
    }

    /**
     * Save the current configuration to file with retry logic
     */
    public void save() {
        synchronized (SAVE_LOCK) {
            saveWithRetry();
        }
    }

    private void saveWithRetry() {
        int maxRetries = 3;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Properties props = createPropertiesFromConfig();
                Path configPath = getConfigPath();
                
                // Ensure parent directory exists (only if configPath has a parent)
                Path parentDir = configPath.getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }
                
                // Write to temporary file first
                Path tempPath = Paths.get(configPath.toString() + ".tmp");
                props.store(Files.newOutputStream(tempPath), "Villages Reborn First-Time Setup Configuration");
                
                // Atomic move to final location
                Files.move(tempPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                LOGGER.info("Saved setup configuration to {} (attempt {})", configPath, attempt);
                return; // Success
                
            } catch (IOException e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to save after " + maxRetries + " attempts", e);
                }
                
                LOGGER.warn("Save attempt {} failed, retrying: {}", attempt, e.getMessage());
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Save interrupted", ie);
                }
            }
        }
    }

    private Properties createPropertiesFromConfig() {
        Properties props = new Properties();
        props.setProperty("setup.completed", String.valueOf(setupCompleted));
        props.setProperty("hardware.detection.completed", String.valueOf(hardwareDetectionCompleted));
        props.setProperty("setup.timestamp", String.valueOf(setupTimestamp));
        props.setProperty("config.version", String.valueOf(CURRENT_CONFIG_VERSION));
        
        if (selectedProvider != null) {
            props.setProperty("llm.provider", selectedProvider.name());
        }
        if (selectedModel != null) {
            props.setProperty("llm.model", selectedModel);
        }
        
        return props;
    }
    
    /**
     * Mark the setup as completed with the selected configuration and validation
     */
    public void completeSetup(LLMProvider provider, String model) {
        validateProvider(provider);
        validateModel(model);
        
        // Create backup of existing config
        Path configPath = getConfigPath();
        if (Files.exists(configPath)) {
            createConfigBackup(configPath);
        }
        
        try {
            this.setupCompleted = true;
            this.selectedProvider = provider;
            this.selectedModel = model;
            this.hardwareDetectionCompleted = true;
            this.setupTimestamp = System.currentTimeMillis();
            this.configVersion = CURRENT_CONFIG_VERSION;
            
            saveWithRetry();
            
            // Verify save was successful
            verifyConfigSaved();
            
            LOGGER.info("First-time setup completed with provider: {}, model: {}", provider, model);
            
        } catch (Exception e) {
            // Restore from backup if save failed
            restoreConfigBackup(configPath);
            throw new RuntimeException("Failed to save setup configuration", e);
        }
    }

    private void validateProvider(LLMProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
    }

    private void validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be null or empty");
        }
    }

    private void createConfigBackup(Path configPath) {
        try {
            Path backupPath = createSecureBackupPath(configPath);
            Files.copy(configPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Created config backup at {}", backupPath);
        } catch (IOException e) {
            LOGGER.warn("Failed to create config backup", e);
        }
    }

    private void restoreConfigBackup(Path configPath) {
        try {
            Path backupPath = createSecureBackupPath(configPath);
            if (Files.exists(backupPath)) {
                Files.copy(backupPath, configPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Restored config from backup");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to restore config backup", e);
        }
    }

    /**
     * Creates a secure backup path that prevents directory traversal attacks
     * and handles special characters safely.
     */
    private Path createSecureBackupPath(Path configPath) {
        return SecureBackupPath.createBackupPath(configPath);
    }

    private void verifyConfigSaved() {
        FirstTimeSetupConfig loaded = load();
        if (!loaded.isSetupCompleted()) {
            throw new RuntimeException("Config verification failed");
        }
    }
    
    /**
     * Reset the setup configuration (for testing or re-setup)
     */
    public void reset() {
        synchronized (SAVE_LOCK) {
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
    }
    
    private static Properties loadRawConfig(Path configPath) throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(configPath));
        return props;
    }

    private static int detectConfigVersion(Properties props) {
        String versionStr = props.getProperty("config.version", "1");
        try {
            return Integer.parseInt(versionStr);
        } catch (NumberFormatException e) {
            return 1; // Default to version 1
        }
    }

    private static Properties migrateConfig(Properties rawConfig, int fromVersion) {
        Properties migrated = new Properties(rawConfig);
        
        // Migration from version 1 to 2
        if (fromVersion < 2) {
            migrated.setProperty("setup.timestamp", String.valueOf(System.currentTimeMillis()));
        }
        
        // Migration from version 2 to 3
        if (fromVersion < 3) {
            // Add any new fields introduced in version 3
            if (!migrated.containsKey("hardware.detection.completed")) {
                migrated.setProperty("hardware.detection.completed", "false");
            }
        }
        
        migrated.setProperty("config.version", String.valueOf(CURRENT_CONFIG_VERSION));
        return migrated;
    }

    private static FirstTimeSetupConfig createDefaultConfig() {
        return new FirstTimeSetupConfig(false, null, null, false, 0, CURRENT_CONFIG_VERSION);
    }

    private static FirstTimeSetupConfig createFromProperties(Properties props) {
        boolean setupCompleted = Boolean.parseBoolean(props.getProperty("setup.completed", "false"));
        boolean hardwareDetectionCompleted = Boolean.parseBoolean(props.getProperty("hardware.detection.completed", "false"));
        
        // Parse numeric values with safe fallbacks for invalid inputs
        long setupTimestamp = parseTimestampSafely(props.getProperty("setup.timestamp", "0"));
        int configVersion = parseVersionSafely(props.getProperty("config.version", "1"));
        
        LLMProvider selectedProvider = null;
        String providerName = props.getProperty("llm.provider");
        if (providerName != null && !providerName.trim().isEmpty()) {
            try {
                selectedProvider = LLMProvider.valueOf(providerName);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown LLM provider in config: {}", providerName);
            }
        }
        
        String selectedModel = props.getProperty("llm.model");
        
        return new FirstTimeSetupConfig(setupCompleted, selectedProvider, selectedModel,
            hardwareDetectionCompleted, setupTimestamp, configVersion);
    }
    
    /**
     * Safely parse timestamp value, returning 0 for invalid inputs
     */
    private static long parseTimestampSafely(String timestampStr) {
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid timestamp value in config: {}, using default", timestampStr);
            return 0L;
        }
    }
    
    /**
     * Safely parse config version, returning 1 for invalid inputs
     */
    private static int parseVersionSafely(String versionStr) {
        try {
            return Integer.parseInt(versionStr);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid config version in config: {}, using default", versionStr);
            return 1;
        }
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

    public long getSetupTimestamp() {
        return setupTimestamp;
    }

    public int getConfigVersion() {
        return configVersion;
    }

    /**
     * Create a new config instance for testing
     */
    public static FirstTimeSetupConfig create() {
        return createDefaultConfig();
    }
}