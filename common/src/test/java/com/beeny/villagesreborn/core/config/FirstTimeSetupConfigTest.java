package com.beeny.villagesreborn.core.config;

import com.beeny.villagesreborn.core.llm.LLMProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FirstTimeSetupConfig functionality
 */
class FirstTimeSetupConfigTest {
    
    @TempDir
    Path tempDir;
    
    private String originalUserDir;
    
    @BeforeEach
    void setUp() {
        // Save original user.dir and set to temp directory for isolated tests
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        
        // Reset to default resolver to ensure clean state
        FirstTimeSetupConfig.resetConfigPathResolver();
    }
    
    @AfterEach
    void tearDown() {
        // Restore original user.dir
        if (originalUserDir != null) {
            System.setProperty("user.dir", originalUserDir);
        }
        
        // Reset to default resolver to avoid affecting other tests
        FirstTimeSetupConfig.resetConfigPathResolver();
    }
    
    @Test
    void testInitialState() {
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        assertFalse(config.isSetupCompleted());
        assertFalse(config.isHardwareDetectionCompleted());
        assertNull(config.getSelectedProvider());
        assertNull(config.getSelectedModel());
    }
    
    @Test
    void testCompleteSetup() {
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
        
        assertTrue(config.isSetupCompleted());
        assertTrue(config.isHardwareDetectionCompleted());
        assertEquals(LLMProvider.OPENAI, config.getSelectedProvider());
        assertEquals("gpt-3.5-turbo", config.getSelectedModel());
    }
    
    @Test
    void testSaveAndLoad() {
        // Create and complete setup
        FirstTimeSetupConfig config1 = FirstTimeSetupConfig.load();
        config1.completeSetup(LLMProvider.ANTHROPIC, "claude-3-haiku");
        
        // Load fresh config - should persist the settings
        FirstTimeSetupConfig config2 = FirstTimeSetupConfig.load();
        
        assertTrue(config2.isSetupCompleted());
        assertTrue(config2.isHardwareDetectionCompleted());
        assertEquals(LLMProvider.ANTHROPIC, config2.getSelectedProvider());
        assertEquals("claude-3-haiku", config2.getSelectedModel());
    }
    
    @Test
    void testReset() {
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        // Complete setup first
        config.completeSetup(LLMProvider.GROQ, "llama2-70b-4096");
        assertTrue(config.isSetupCompleted());
        
        // Reset
        config.reset();
        
        assertFalse(config.isSetupCompleted());
        assertFalse(config.isHardwareDetectionCompleted());
        assertNull(config.getSelectedProvider());
        assertNull(config.getSelectedModel());
        
        // Verify config file is deleted
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        assertFalse(Files.exists(configFile));
    }
    
    @Test
    void testLoadWithExistingFile() throws IOException {
        // Create a config file manually
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        String configContent = """
            setup.completed=true
            hardware.detection.completed=true
            llm.provider=LOCAL
            llm.model=llama2:7b
            """;
        Files.writeString(configFile, configContent);
        
        // Load config
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        assertTrue(config.isSetupCompleted());
        assertTrue(config.isHardwareDetectionCompleted());
        assertEquals(LLMProvider.LOCAL, config.getSelectedProvider());
        assertEquals("llama2:7b", config.getSelectedModel());
    }
    
    @Test
    void testLoadWithInvalidProvider() throws IOException {
        // Create a config file with invalid provider
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        String configContent = """
            setup.completed=true
            hardware.detection.completed=true
            llm.provider=INVALID_PROVIDER
            llm.model=some-model
            """;
        Files.writeString(configFile, configContent);
        
        // Load config - should handle invalid provider gracefully
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        
        assertTrue(config.isSetupCompleted());
        assertTrue(config.isHardwareDetectionCompleted());
        assertNull(config.getSelectedProvider()); // Invalid provider should be null
        assertEquals("some-model", config.getSelectedModel());
    }
    
    @Test
    void testLoadWithCorruptedFile() throws IOException {
        // Create a corrupted config file
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        Files.writeString(configFile, "this is not valid properties content!");
        
        // Should load with defaults without throwing exception
        assertDoesNotThrow(() -> {
            FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
            assertFalse(config.isSetupCompleted());
        });
    }
    
    @Test
    void testMultipleProviders() {
        // Test all providers can be saved and loaded
        for (LLMProvider provider : LLMProvider.values()) {
            FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
            
            // Reset first to ensure clean state
            config.reset();
            
            // Complete setup with this provider
            config.completeSetup(provider, "test-model");
            
            // Load fresh config and verify
            FirstTimeSetupConfig reloadedConfig = FirstTimeSetupConfig.load();
            assertEquals(provider, reloadedConfig.getSelectedProvider());
            assertEquals("test-model", reloadedConfig.getSelectedModel());
        }
    }
    
    @Test
    void testConfigFilePersistence() {
        FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
        config.completeSetup(LLMProvider.OPENROUTER, "openai/gpt-4");
        
        // Check that config file exists
        Path configFile = tempDir.resolve("villagesreborn_setup.properties");
        assertTrue(Files.exists(configFile));
        
        // Verify file contains expected content
        assertDoesNotThrow(() -> {
            String content = Files.readString(configFile);
            assertTrue(content.contains("setup.completed=true"));
            assertTrue(content.contains("llm.provider=OPENROUTER"));
            assertTrue(content.contains("llm.model=openai/gpt-4"));
        });
    }
}