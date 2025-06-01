package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Windows-specific tests for secure backup path construction
 */
class SecureBackupPathWindowsTest {
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void windowsPaths_ProduceCorrectBackupPaths() {
        // Test absolute Windows path with drive letter
        Path configPath = Paths.get("C:\\Users\\test\\config\\app.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        assertEquals("C:\\Users\\test\\config", backupPath.getParent().toString());
        assertEquals("app.properties.backup", backupPath.getFileName().toString());
        assertEquals("C:\\Users\\test\\config\\app.properties.backup", backupPath.toString());
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void windowsPaths_HandleUNCPaths() {
        // Test UNC network path
        Path configPath = Paths.get("\\\\server\\share\\config\\app.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        assertEquals("\\\\server\\share\\config", backupPath.getParent().toString());
        assertEquals("app.properties.backup", backupPath.getFileName().toString());
    }
    
    @Test
    @EnabledOnOs(OS.WINDOWS) 
    void windowsPaths_HandleRelativePathsWithBackslashes() {
        // Test relative path with Windows separators
        Path configPath = Paths.get("config\\subdir\\app.properties");
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        assertEquals("config\\subdir", backupPath.getParent().toString());
        assertEquals("app.properties.backup", backupPath.getFileName().toString());
    }
    
    @Test
    void crossPlatform_NormalizationPreventsMixedSeparators() {
        // Test mixed separators get normalized
        Path configPath = Paths.get("config/subdir\\app.properties").normalize();
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        // Should not contain mixed separators after normalization
        String backupStr = backupPath.toString();
        assertFalse(backupStr.contains("/") && backupStr.contains("\\"), 
                   "Path should not contain mixed separators: " + backupStr);
    }
    
    @Test
    void securityTest_PreventDirectoryTraversalOnWindows() {
        // Test that malicious filename doesn't escape directory
        Path configPath = Paths.get("C:\\config\\..\\evil.properties");
        Path normalizedPath = configPath.normalize();
        Path backupPath = normalizedPath.resolveSibling(normalizedPath.getFileName() + ".backup");
        
        // After normalization, the path should be safe
        assertTrue(backupPath.toString().startsWith("C:\\"), 
                  "Path should start with C:\\ after normalization");
        assertFalse(backupPath.toString().contains(".."), 
                   "Backup path should not contain parent directory references");
    }
}