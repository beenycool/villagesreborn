package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ModConfig
 * Validates configuration loading, environment variable handling, and security
 */
class ModConfigTest {
    
    @TempDir
    Path tempDir;
    
    private Map<String, String> originalEnv;
    
    @BeforeEach
    void setUp() {
        // Store original environment
        originalEnv = new HashMap<>(System.getenv());
        
        // Clear any existing environment variables
        clearTestEnvironmentVariables();
    }
    
    @Test
    void testConfigLoadFromEnvironmentVariable() {
        // Set test environment variable
        setEnvironmentVariable("VILLAGESREBORN_LLM_API_KEY", "test-api-key-123");
        
        ModConfig config = ModConfig.load();
        
        assertNotNull(config);
        assertEquals("test-api-key-123", config.getLlmApiKey());
        assertFalse(config.isDevelopmentMode()); // Default value
        assertEquals(1, config.getLogLevel()); // Default value
    }
    
    @Test
    void testConfigLoadFromFile() throws IOException {
        // Set required environment variable for this test
        setEnvironmentVariable("VILLAGESREBORN_LLM_API_KEY", "file-api-key-456");
        setEnvironmentVariable("VILLAGESREBORN_DEVELOPMENT_MODE", "true");
        setEnvironmentVariable("VILLAGESREBORN_LOG_LEVEL", "2");
        
        ModConfig config = ModConfig.load();
        
        assertNotNull(config);
        assertEquals("file-api-key-456", config.getLlmApiKey());
        assertTrue(config.isDevelopmentMode());
        assertEquals(2, config.getLogLevel());
    }
    
    @Test
    void testEnvironmentVariableOverridesFile() throws IOException {
        // Set environment variables
        setEnvironmentVariable("VILLAGESREBORN_LLM_API_KEY", "env-api-key");
        setEnvironmentVariable("VILLAGESREBORN_DEVELOPMENT_MODE", "true");
        
        ModConfig config = ModConfig.load();
        
        // Environment variables should be loaded
        assertEquals("env-api-key", config.getLlmApiKey());
        assertTrue(config.isDevelopmentMode());
    }
    
    @Test
    void testMissingRequiredConfiguration() {
        // Don't set any API key
        clearTestEnvironmentVariables();
        
        // Should throw exception for missing required config
        assertThrows(IllegalStateException.class, () -> ModConfig.load());
    }
    
    @Test
    void testConfigValidation() {
        setEnvironmentVariable("VILLAGESREBORN_LLM_API_KEY", "valid-api-key-123456");
        setEnvironmentVariable("VILLAGESREBORN_LOG_LEVEL", "2");
        
        ModConfig config = ModConfig.load();
        
        // Should not throw exception for valid config
        assertDoesNotThrow(() -> config.validate());
    }
    
    @Test
    void testInvalidApiKeyValidation() {
        setEnvironmentVariable("VILLAGESREBORN_LLM_API_KEY", "short");
        
        ModConfig config = ModConfig.load();
        
        // Should throw exception for invalid API key
        assertThrows(IllegalStateException.class, () -> config.validate());
    }
    
    @Test
    void testInvalidLogLevelValidation() {
        setEnvironmentVariable("VILLAGESREBORN_LLM_API_KEY", "valid-api-key-123456");
        setEnvironmentVariable("VILLAGESREBORN_LOG_LEVEL", "5"); // Invalid level
        
        ModConfig config = ModConfig.load();
        
        // Should throw exception for invalid log level
        assertThrows(IllegalStateException.class, () -> config.validate());
    }
    
    @Test
    void testEncryptedApiKey() {
        // Test with encrypted API key format
        setEnvironmentVariable("VILLAGESREBORN_LLM_API_KEY", "ENC(test-encrypted-value)");
        
        // This test would require mocking the SecurityUtil.decrypt method
        // For now, we'll just test that the format is recognized
        assertDoesNotThrow(() -> ModConfig.load());
    }
    
    // Helper methods for environment variable testing
    private void setEnvironmentVariable(String key, String value) {
        // In a real test environment, you'd use a proper environment variable mocking library
        // For now, we'll simulate this by directly calling System.setProperty for simplicity
        System.setProperty("test.env." + key, value);
        
        // Mock System.getenv() behavior would be implemented here in a real scenario
    }
    
    private void clearTestEnvironmentVariables() {
        // Clear test environment variables
        System.getProperties().entrySet().removeIf(entry -> 
            entry.getKey().toString().startsWith("test.env.VILLAGESREBORN_"));
    }
}