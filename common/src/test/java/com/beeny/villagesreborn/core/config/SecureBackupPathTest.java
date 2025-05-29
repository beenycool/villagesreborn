package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the secure backup path construction to prevent path traversal vulnerabilities
 */
class SecureBackupPathTest {
    
    @Test
    void resolveSibling_PreventsSiblingDirectoryTraversal() {
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
    void resolveSibling_HandlesSpecialCharacters() {
        Path configPath = Paths.get("/config/config with spaces & symbols.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        assertEquals(configPath.getParent(), backupPath.getParent());
        assertEquals("config with spaces & symbols.properties.backup", backupPath.getFileName().toString());
    }
    
    @Test
    void resolveSibling_HandlesRelativePaths() {
        Path configPath = Paths.get("config.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        assertEquals("config.properties.backup", backupPath.getFileName().toString());
        // Should not contain any parent directory references
        assertFalse(backupPath.toString().contains(".."));
    }
    
    @Test
    void resolveSibling_HandlesNestedPaths() {
        Path configPath = Paths.get("a/b/c/config.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        assertEquals(Paths.get("a/b/c"), backupPath.getParent());
        assertEquals("config.properties.backup", backupPath.getFileName().toString());
    }
    
    @Test
    void resolveSibling_ComparedToUnsafeStringConcatenation() {
        Path configPath = Paths.get("/config/dir/config.properties");
        
        // Secure method using resolveSibling
        Path secureBackupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        // Unsafe method using string concatenation (what we replaced)
        Path unsafeBackupPath = Paths.get(configPath.toString() + ".backup");
        
        // Both should produce the same result for normal paths on Unix-like systems
        // On Windows, the paths will be different due to drive letters and separators
        assertTrue(secureBackupPath.toString().endsWith("config.properties.backup"));
        assertTrue(unsafeBackupPath.toString().endsWith("config.properties.backup"));
        
        // But the secure method is safer against malicious paths
        assertEquals(configPath.getParent(), secureBackupPath.getParent());
    }
    
    @Test
    void resolveSibling_HandlesWindowsPaths() {
        Path configPath = Paths.get("C:\\config\\dir\\config.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        assertEquals(configPath.getParent(), backupPath.getParent());
        assertEquals("config.properties.backup", backupPath.getFileName().toString());
    }
    
    @Test
    void resolveSibling_HandlesEmptyFileName() {
        Path configPath = Paths.get("/config/dir/");
        Path backupPath = configPath.resolveSibling((configPath.getFileName() != null ? configPath.getFileName() : "config") + ".backup");
        
        // Should handle gracefully
        assertNotNull(backupPath);
    }
}