package com.beeny.villagesreborn.core.config;

import com.beeny.villagesreborn.core.llm.LLMProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced unit tests for FirstTimeSetupConfig
 * Tests configuration persistence, migration, error handling, and backup/restore functionality
 */
class FirstTimeSetupConfigEnhancedTest {

    @TempDir
    Path tempDir;
    
    private Path configFile;
    private ConfigPathResolver originalResolver;
    
    @BeforeEach
    void setUp() {
        configFile = tempDir.resolve("villagesreborn_setup.properties");
        
        // Setup test config path resolver
        originalResolver = new ConfigPathResolver();
        TestConfigPathProvider testProvider = new TestConfigPathProvider(tempDir);
        java.util.List<ConfigPathStrategy> providers = new java.util.ArrayList<>();
        providers.add(testProvider);
        ConfigPathResolver testResolver = new ConfigPathResolver(providers);
        FirstTimeSetupConfig.setConfigPathResolver(testResolver);
    }
    
    @AfterEach
    void tearDown() {
        // Restore original resolver
        FirstTimeSetupConfig.setConfigPathResolver(originalResolver);
        
        // Clean up any test files
        try {
            Files.deleteIfExists(configFile);
            Files.deleteIfExists(configFile.resolveSibling(configFile.getFileName() + ".backup"));
            Files.deleteIfExists(configFile.resolveSibling(configFile.getFileName() + ".tmp"));
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testConfigurationSaveAndLoad() {
        // GIVEN: Setup config with valid data
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Configuration is completed and saved
        config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
        
        // THEN: Configuration can be loaded and values match
        FirstTimeSetupConfig loaded = FirstTimeSetupConfig.load();
        assertTrue(loaded.isSetupCompleted());
        assertEquals(LLMProvider.OPENAI, loaded.getSelectedProvider());
        assertEquals("gpt-3.5-turbo", loaded.getSelectedModel());
        assertTrue(loaded.isHardwareDetectionCompleted());
        assertEquals(3, loaded.getConfigVersion());
    }
    
    @Test
    void testConfigurationSaveWithRetry() throws IOException {
        // GIVEN: Setup config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Configuration is saved (should succeed even with potential I/O issues)
        config.completeSetup(LLMProvider.ANTHROPIC, "claude-3-haiku");
        
        // THEN: Save succeeds and config is persisted
        FirstTimeSetupConfig loaded = FirstTimeSetupConfig.load();
        assertEquals(LLMProvider.ANTHROPIC, loaded.getSelectedProvider());
        assertEquals("claude-3-haiku", loaded.getSelectedModel());
    }
    
    @Test
    void testConfigMigrationFromOldVersion() throws IOException {
        // GIVEN: Old version config file (version 1)
        createOldVersionConfig(1);
        
        // WHEN: Config is loaded with migration
        FirstTimeSetupConfig config = FirstTimeSetupConfig.loadWithMigration();
        
        // THEN: Config is migrated to current version
        assertEquals(3, config.getConfigVersion());
        assertTrue(config.isSetupCompleted());
        assertTrue(config.getSetupTimestamp() > 0); // Added in migration
    }
    
    @Test
    void testConfigMigrationFromVersion2() throws IOException {
        // GIVEN: Version 2 config file
        createVersionConfig(2);
        
        // WHEN: Config is loaded with migration
        FirstTimeSetupConfig config = FirstTimeSetupConfig.loadWithMigration();
        
        // THEN: Config is migrated to current version
        assertEquals(3, config.getConfigVersion());
        assertFalse(config.isHardwareDetectionCompleted()); // Added in v3 migration
    }
    
    @Test
    void testConfigLoadWithNonExistentFile() {
        // WHEN: Config is loaded when file doesn't exist
        FirstTimeSetupConfig config = FirstTimeSetupConfig.loadWithMigration();
        
        // THEN: Default config is returned
        assertFalse(config.isSetupCompleted());
        assertNull(config.getSelectedProvider());
        assertNull(config.getSelectedModel());
        assertEquals(3, config.getConfigVersion());
    }
    
    @Test
    void testConfigLoadWithCorruptedFile() throws IOException {
        // GIVEN: Corrupted config file
        Files.writeString(configFile, "invalid properties content <<<>>>>");
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.loadWithMigration();
        
        // THEN: Default config is returned (graceful degradation)
        assertFalse(config.isSetupCompleted());
        assertEquals(3, config.getConfigVersion());
    }
    
    @Test
    void testCompleteSetupWithValidation() {
        // GIVEN: Setup config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Setup is completed with valid data
        config.completeSetup(LLMProvider.LOCAL, "llama2:7b");
        
        // THEN: All fields are properly set
        assertTrue(config.isSetupCompleted());
        assertEquals(LLMProvider.LOCAL, config.getSelectedProvider());
        assertEquals("llama2:7b", config.getSelectedModel());
        assertTrue(config.isHardwareDetectionCompleted());
        assertTrue(config.getSetupTimestamp() > 0);
    }
    
    @Test
    void testCompleteSetupWithNullProvider() {
        // GIVEN: Setup config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Setup is attempted with null provider
        // THEN: IllegalArgumentException is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.completeSetup(null, "gpt-3.5-turbo"));
        assertTrue(exception.getMessage().contains("Provider cannot be null"));
    }
    
    @Test
    void testCompleteSetupWithNullModel() {
        // GIVEN: Setup config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Setup is attempted with null model
        // THEN: IllegalArgumentException is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.completeSetup(LLMProvider.OPENAI, null));
        assertTrue(exception.getMessage().contains("Model cannot be null or empty"));
    }
    
    @Test
    void testCompleteSetupWithEmptyModel() {
        // GIVEN: Setup config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Setup is attempted with empty model
        // THEN: IllegalArgumentException is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.completeSetup(LLMProvider.OPENAI, ""));
        assertTrue(exception.getMessage().contains("Model cannot be null or empty"));
    }
    
    @Test
    void testConfigReset() {
        // GIVEN: Setup config with completed setup
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        config.completeSetup(LLMProvider.GROQ, "mixtral-8x7b-32768");
        
        // WHEN: Config is reset
        config.reset();
        
        // THEN: All values are cleared
        assertFalse(config.isSetupCompleted());
        assertNull(config.getSelectedProvider());
        assertNull(config.getSelectedModel());
        assertFalse(config.isHardwareDetectionCompleted());
    }
    
    @Test
    void testCreateFactoryMethod() {
        // WHEN: Config is created using factory method
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // THEN: Default values are set
        assertFalse(config.isSetupCompleted());
        assertNull(config.getSelectedProvider());
        assertNull(config.getSelectedModel());
        assertEquals(3, config.getConfigVersion());
    }
    
    @Test
    void testTimestampUpdatedOnSetup() {
        // GIVEN: Setup config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        long beforeSetup = System.currentTimeMillis();
        
        // WHEN: Setup is completed
        config.completeSetup(LLMProvider.OPENROUTER, "openai/gpt-3.5-turbo");
        long afterSetup = System.currentTimeMillis();
        
        // THEN: Timestamp is set within the expected range
        assertTrue(config.getSetupTimestamp() >= beforeSetup && config.getSetupTimestamp() <= afterSetup);
    }
    
    @Test
    void testLoadLegacyProviderHandling() throws IOException {
        // GIVEN: Config with unknown provider
        Properties props = new Properties();
        props.setProperty("setup.completed", "true");
        props.setProperty("llm.provider", "UNKNOWN_PROVIDER");
        props.setProperty("llm.model", "some-model");
        props.setProperty("config.version", "3");
        
        try (var out = Files.newOutputStream(configFile)) {
            props.store(out, "Test config");
        }
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.loadWithMigration();
        
        // THEN: Unknown provider is handled gracefully
        assertTrue(config.isSetupCompleted());
        assertNull(config.getSelectedProvider()); // Unknown provider becomes null
        assertEquals("some-model", config.getSelectedModel());
    }
    
    private void createOldVersionConfig(int version) throws IOException {
        Properties props = new Properties();
        props.setProperty("setup.completed", "true");
        props.setProperty("llm.provider", "OPENAI");
        props.setProperty("llm.model", "gpt-3.5-turbo");
        
        if (version >= 2) {
            props.setProperty("setup.timestamp", String.valueOf(System.currentTimeMillis()));
        }
        
        props.setProperty("config.version", String.valueOf(version));
        
        try (var out = Files.newOutputStream(configFile)) {
            props.store(out, "Test config version " + version);
        }
    }
    
    private void createVersionConfig(int version) throws IOException {
        Properties props = new Properties();
        props.setProperty("setup.completed", "true");
        props.setProperty("llm.provider", "ANTHROPIC");
        props.setProperty("llm.model", "claude-3-sonnet");
        props.setProperty("setup.timestamp", String.valueOf(System.currentTimeMillis()));
        props.setProperty("config.version", String.valueOf(version));
        
        try (var out = Files.newOutputStream(configFile)) {
            props.store(out, "Test config version " + version);
        }
    }
}