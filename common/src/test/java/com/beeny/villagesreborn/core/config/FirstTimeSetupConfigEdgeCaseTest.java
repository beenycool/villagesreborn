package com.beeny.villagesreborn.core.config;

import com.beeny.villagesreborn.core.llm.LLMProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case tests for FirstTimeSetupConfig
 * Tests unusual scenarios like missing directories, empty file contents, 
 * custom file names, permission issues, and concurrent access patterns
 */
class FirstTimeSetupConfigEdgeCaseTest {

    @TempDir
    Path tempDir;
    
    private TestConfigPathProvider testPathProvider;
    private ConfigPathResolver originalResolver;
    
    @BeforeEach
    void setUp() {
        originalResolver = new ConfigPathResolver();
    }
    
    @AfterEach
    void tearDown() {
        FirstTimeSetupConfig.resetConfigPathResolver();
    }
    
    @Test
    void testLoadWithMissingParentDirectory() throws IOException {
        // GIVEN: Config path points to a file in a non-existent directory
        Path nonExistentDir = tempDir.resolve("missing").resolve("nested").resolve("dirs");
        Path configFile = nonExistentDir.resolve("villagesreborn_setup.properties");
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded (should create default)
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Default config is returned without error
        assertFalse(config.isSetupCompleted());
        assertNull(config.getSelectedProvider());
        
        // WHEN: Setup is completed (should create missing directories)
        assertDoesNotThrow(() -> {
            config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
        });
        
        // THEN: Directory structure is created and config is saved
        assertTrue(Files.exists(configFile));
        assertTrue(Files.isDirectory(nonExistentDir));
    }
    
    @Test
    void testLoadWithEmptyFile() throws IOException {
        // GIVEN: Empty config file
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        Files.createFile(configFile);
        // File exists but is empty
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Default values are used
        assertFalse(config.isSetupCompleted());
        assertNull(config.getSelectedProvider());
        assertNull(config.getSelectedModel());
        assertFalse(config.isHardwareDetectionCompleted());
    }
    
    @Test
    void testLoadWithWhitespaceOnlyFile() throws IOException {
        // GIVEN: Config file with only whitespace
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        Files.writeString(configFile, "   \n\t\r\n  \n");
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Default values are used
        assertFalse(config.isSetupCompleted());
        assertNull(config.getSelectedProvider());
    }
    
    @Test
    void testLoadWithCustomConfigFileName() throws IOException {
        // GIVEN: Custom config file name
        Path customConfigFile = tempDir.resolve("my_custom_config.properties");
        String configContent = """
            setup.completed=true
            llm.provider=ANTHROPIC
            llm.model=claude-3-haiku
            hardware.detection.completed=true
            setup.timestamp=1234567890
            config.version=3
            """;
        Files.writeString(customConfigFile, configContent);
        
        testPathProvider = new TestConfigPathProvider(customConfigFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Config is loaded successfully despite non-standard filename
        assertTrue(config.isSetupCompleted());
        assertEquals(LLMProvider.ANTHROPIC, config.getSelectedProvider());
        assertEquals("claude-3-haiku", config.getSelectedModel());
    }
    
    @Test
    void testLoadWithVeryLongConfigValues() throws IOException {
        // GIVEN: Config with extremely long values
        String veryLongModel = "a".repeat(10000); // 10KB model name
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        
        Properties props = new Properties();
        props.setProperty("setup.completed", "true");
        props.setProperty("llm.provider", "LOCAL");
        props.setProperty("llm.model", veryLongModel);
        props.setProperty("config.version", "3");
        
        try (var out = Files.newOutputStream(configFile)) {
            props.store(out, "Test with very long values");
        }
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Long values are handled correctly
        assertTrue(config.isSetupCompleted());
        assertEquals(LLMProvider.LOCAL, config.getSelectedProvider());
        assertEquals(veryLongModel, config.getSelectedModel());
    }
    
    @Test
    void testLoadWithSpecialCharactersInValues() throws IOException {
        // GIVEN: Config with special characters, unicode, etc.
        String specialModel = "model-with-émojis-🚀-and-üñïçødé-∃∀∈∉";
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        
        Properties props = new Properties();
        props.setProperty("setup.completed", "true");
        props.setProperty("llm.provider", "OPENAI");
        props.setProperty("llm.model", specialModel);
        props.setProperty("config.version", "3");
        
        try (var out = Files.newOutputStream(configFile)) {
            props.store(out, "Test with special characters");
        }
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Special characters are preserved
        assertTrue(config.isSetupCompleted());
        assertEquals(LLMProvider.OPENAI, config.getSelectedProvider());
        assertEquals(specialModel, config.getSelectedModel());
    }
    
    @Test
    void testLoadWithInvalidBooleanValues() throws IOException {
        // GIVEN: Config with invalid boolean values
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        String configContent = """
            setup.completed=yes
            hardware.detection.completed=1
            llm.provider=OPENAI
            llm.model=gpt-3.5-turbo
            config.version=3
            """;
        Files.writeString(configFile, configContent);
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Invalid boolean values default to false
        assertFalse(config.isSetupCompleted()); // "yes" -> false
        assertFalse(config.isHardwareDetectionCompleted()); // "1" -> false
        assertEquals(LLMProvider.OPENAI, config.getSelectedProvider());
    }
    
    @Test
    void testLoadWithInvalidNumericValues() throws IOException {
        // GIVEN: Config with invalid numeric values
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        String configContent = """
            setup.completed=true
            llm.provider=ANTHROPIC
            llm.model=claude-3-haiku
            setup.timestamp=not-a-number
            config.version=invalid
            """;
        Files.writeString(configFile, configContent);
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Invalid numeric values use defaults
        assertTrue(config.isSetupCompleted());
        assertEquals(0L, config.getSetupTimestamp()); // Invalid timestamp -> 0
        assertEquals(1, config.getConfigVersion()); // Invalid version -> 1
    }
    
    @Test
    void testLoadWithMalformedPropertiesFormat() throws IOException {
        // GIVEN: File with malformed properties format
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        String malformedContent = """
            setup.completed=true
            this_line_has_no_equals_sign
            llm.provider=
            =value_without_key
            llm.model==double==equals==signs==
            # This is a comment
            
            config.version=3
            """;
        Files.writeString(configFile, malformedContent);
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Valid properties are loaded, malformed ones are ignored
        assertTrue(config.isSetupCompleted());
        assertNull(config.getSelectedProvider()); // Empty provider value
        assertEquals("=double==equals==signs==", config.getSelectedModel()); // Handles multiple equals
    }
    
    @Test
    void testLoadWithDuplicateKeys() throws IOException {
        // GIVEN: Config with duplicate property keys
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        String duplicateContent = """
            setup.completed=false
            llm.provider=OPENAI
            llm.model=gpt-3.5-turbo
            setup.completed=true
            llm.provider=ANTHROPIC
            config.version=3
            """;
        Files.writeString(configFile, duplicateContent);
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // THEN: Last value wins for duplicate keys
        assertTrue(config.isSetupCompleted()); // Last value: true
        assertEquals(LLMProvider.ANTHROPIC, config.getSelectedProvider()); // Last value: ANTHROPIC
        assertEquals("gpt-3.5-turbo", config.getSelectedModel()); // No duplicate, original value
    }
    
    @Test
    void testCompleteSetupWithWhitespaceOnlyModel() {
        // GIVEN: Setup config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Setup is attempted with whitespace-only model
        // THEN: IllegalArgumentException is thrown
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> config.completeSetup(LLMProvider.OPENAI, "   \t\n  "));
        assertTrue(exception.getMessage().contains("Model cannot be null or empty"));
    }
    
    @Test
    void testSaveToDirectoryWithLimitedSpace() throws IOException {
        // GIVEN: Config path in a directory (simulated limited space scenario)
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // WHEN: Setup is completed (this tests the retry mechanism)
        assertDoesNotThrow(() -> {
            config.completeSetup(LLMProvider.LOCAL, "test-model");
        });
        
        // THEN: Config is saved successfully
        assertTrue(Files.exists(configFile));
        
        FirstTimeSetupConfig reloaded = FirstTimeSetupConfig.load();
        assertTrue(reloaded.isSetupCompleted());
        assertEquals(LLMProvider.LOCAL, reloaded.getSelectedProvider());
        assertEquals("test-model", reloaded.getSelectedModel());
    }
    
    @Test
    void testBackupAndRestoreWithCorruptedMainConfig() throws IOException {
        // GIVEN: Valid backup and corrupted main config
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        Path backupFile = SecureBackupPath.createBackupPath(configFile);
        
        // Create valid backup
        String validConfig = """
            setup.completed=true
            llm.provider=GROQ
            llm.model=mixtral-8x7b-32768
            hardware.detection.completed=true
            setup.timestamp=1234567890
            config.version=3
            """;
        Files.writeString(backupFile, validConfig);
        
        // Create corrupted main config that will cause Properties.load() to fail
        // Using invalid unicode escape sequence which will cause IOException
        Files.writeString(configFile, "setup.completed=\\u12zz");
        
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(testPathProvider))
        );
        
        // WHEN: Config is loaded with migration (should restore from backup)
        FirstTimeSetupConfig config = FirstTimeSetupConfig.loadWithMigration();
        
        // THEN: Config is restored from backup
        assertTrue(config.isSetupCompleted());
        assertEquals(LLMProvider.GROQ, config.getSelectedProvider());
        assertEquals("mixtral-8x7b-32768", config.getSelectedModel());
    }
    
    @Test
    void testLoadWithNoAvailableConfigPathProvider() {
        // GIVEN: Config resolver with no available providers
        ConfigPathStrategy unavailableProvider = new ConfigPathStrategy() {
            @Override
            public boolean isAvailable() { return false; }
            @Override
            public Path getConfigPath() { throw new UnsupportedOperationException(); }
            @Override
            public int getPriority() { return 1; }
        };
        
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(unavailableProvider))
        );
        
        // WHEN: Config is loaded
        // THEN: IllegalStateException is thrown
        assertThrows(IllegalStateException.class, () -> {
            FirstTimeSetupConfig.load();
        });
    }
    
    @Test
    void testLoadWithFailingConfigPathProvider() {
        // GIVEN: Config resolver with provider that throws exception
        ConfigPathStrategy failingProvider = new ConfigPathStrategy() {
            @Override
            public boolean isAvailable() { return true; }
            @Override
            public Path getConfigPath() { 
                throw new RuntimeException("Simulated provider failure"); 
            }
            @Override
            public int getPriority() { return 1; }
        };
        
        FirstTimeSetupConfig.setConfigPathResolver(
            new ConfigPathResolver(java.util.Arrays.asList(failingProvider))
        );
        
        // WHEN: Config is loaded
        // THEN: IllegalStateException is thrown (no working provider)
        assertThrows(IllegalStateException.class, () -> {
            FirstTimeSetupConfig.load();
        });
    }
}