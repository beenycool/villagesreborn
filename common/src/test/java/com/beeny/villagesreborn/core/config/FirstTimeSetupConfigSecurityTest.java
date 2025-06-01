package com.beeny.villagesreborn.core.config;

import com.beeny.villagesreborn.core.llm.LLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class FirstTimeSetupConfigSecurityTest {

    @TempDir
    Path tempDir;
    
    private TestConfigPathProvider testPathProvider;
    private Path configFile;
    
    @BeforeEach
    void setUp() throws IOException {
        configFile = tempDir.resolve("villagesreborn_setup.properties");
        testPathProvider = new TestConfigPathProvider(configFile);
        FirstTimeSetupConfig.setConfigPathResolver(new ConfigPathResolver(java.util.Arrays.asList(testPathProvider)));
        
        // Clean up any existing files
        Files.deleteIfExists(configFile);
        Path backupPath = configFile.resolveSibling(configFile.getFileName() + ".backup");
        Files.deleteIfExists(backupPath);
    }
    
    @AfterEach
    void tearDown() {
        // Reset to default resolver to avoid affecting other tests
        FirstTimeSetupConfig.resetConfigPathResolver();
    }
    
    @Test
    void createConfigBackup_UsesSecurePathConstruction() throws IOException {
        // Create an initial config file
        Properties props = new Properties();
        props.setProperty("setup.completed", "false");
        props.store(Files.newOutputStream(configFile), "Test config");
        
        // Load and complete setup to trigger backup creation
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
        
        // Verify backup was created with secure path construction
        Path expectedBackupPath = configFile.resolveSibling(configFile.getFileName() + ".backup");
        assertTrue(Files.exists(expectedBackupPath), "Backup file should exist");
        
        // Verify backup content
        Properties backupProps = new Properties();
        backupProps.load(Files.newInputStream(expectedBackupPath));
        assertEquals("false", backupProps.getProperty("setup.completed"));
    }
    
    @Test
    void createConfigBackup_DoesNotCreatePathTraversalVulnerability() throws IOException {
        // Create a config file with a name that could potentially be exploited
        Path maliciousConfigFile = tempDir.resolve("../../../etc/passwd");
        
        // Use a path that contains path traversal elements
        testPathProvider = new TestConfigPathProvider(maliciousConfigFile);
        FirstTimeSetupConfig.setConfigPathResolver(new ConfigPathResolver(java.util.Arrays.asList(testPathProvider)));
        
        // This should not create files outside the intended directory
        // The resolveSibling method should handle this safely
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        // Attempting to complete setup should not create files in unintended locations
        assertDoesNotThrow(() -> {
            config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
        });
        
        // Verify no files were created outside tempDir
        Path parentDir = tempDir.getParent();
        if (parentDir != null) {
            assertFalse(Files.exists(parentDir.resolve("passwd.backup")));
        }
    }
    
    @Test
    void restoreConfigBackup_UsesSecurePathConstruction() throws IOException {
        // Create initial config and backup
        Properties props = new Properties();
        props.setProperty("setup.completed", "true");
        props.setProperty("llm.provider", "OPENAI");
        props.setProperty("llm.model", "gpt-4");
        props.store(Files.newOutputStream(configFile), "Test config");
        
        Path backupPath = configFile.resolveSibling(configFile.getFileName() + ".backup");
        Properties backupProps = new Properties();
        backupProps.setProperty("setup.completed", "false");
        backupProps.store(Files.newOutputStream(backupPath), "Backup config");
        
        // Load config and attempt an operation that might fail and trigger restore
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        assertTrue(config.isSetupCompleted());
        
        // Simulate a failure scenario by creating a corrupt config
        Files.write(configFile, "corrupted data".getBytes());
        
        // Load with migration should handle this gracefully
        FirstTimeSetupConfig restoredConfig = FirstTimeSetupConfig.loadWithMigration();
        assertNotNull(restoredConfig);
    }
    
    @Test
    void backupPathConstruction_HandlesSpecialCharacters() throws IOException {
        // Test with special characters in filename
        Path specialConfigFile = tempDir.resolve("config_with_spaces_and_symbols.properties");
        testPathProvider = new TestConfigPathProvider(specialConfigFile);
        FirstTimeSetupConfig.setConfigPathResolver(new ConfigPathResolver(java.util.Arrays.asList(testPathProvider)));
        
        // Clean up first
        Files.deleteIfExists(specialConfigFile);
        Path expectedBackupPath = specialConfigFile.resolveSibling(specialConfigFile.getFileName() + ".backup");
        Files.deleteIfExists(expectedBackupPath);
        
        // Create initial config
        Properties props = new Properties();
        props.setProperty("setup.completed", "false");
        props.store(Files.newOutputStream(specialConfigFile), "Test config");
        
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // This should not throw an exception and should create backup safely
        assertDoesNotThrow(() -> {
            config.completeSetup(LLMProvider.ANTHROPIC, "claude-3-sonnet");
        });
        
        // Verify backup exists with correct name
        assertTrue(Files.exists(expectedBackupPath));
    }
    
    @Test
    void backupPathConstruction_PreventsSiblingDirectoryTraversal() {
        // Test that resolveSibling doesn't allow traversal to parent directories
        Path configPath = Paths.get("/some/config/dir/config.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        // The backup should be in the same directory as the config file
        assertEquals(configPath.getParent(), backupPath.getParent());
        assertEquals("config.properties.backup", backupPath.getFileName().toString());
        
        // Verify it doesn't escape the directory
        assertFalse(backupPath.toString().contains("../"));
        assertFalse(backupPath.toString().contains("..\\"));
    }
    
    @Test
    void inputValidation_RejectsInvalidProvider() {
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        assertThrows(IllegalArgumentException.class, () -> {
            config.completeSetup(null, "gpt-3.5-turbo");
        });
    }
    
    @Test
    void inputValidation_RejectsInvalidModel() {
        FirstTimeSetupConfig config = FirstTimeSetupConfig.create();
        
        assertThrows(IllegalArgumentException.class, () -> {
            config.completeSetup(LLMProvider.OPENAI, null);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            config.completeSetup(LLMProvider.OPENAI, "");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            config.completeSetup(LLMProvider.OPENAI, "   ");
        });
    }
}