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
    
    // This test is problematic as SecureBackupPath.createBackupPath relies on Path.normalize() and Path.resolveSibling(),
    // which don't guarantee stripping valid foreign separators from filename segments. 
    // The structural correctness is better tested in SecureBackupPathUtilityTest.
    /*
    @Test
    void crossPlatform_NormalizationPreventsMixedSeparators() {
        // Test mixed separators get normalized
        Path configPath = Paths.get("config/subdir\\app.properties").normalize();
        Path backupPath = configPath.resolveSibling(configPath.getFileName() + ".backup");
        
        // Should not contain mixed separators after normalization
        String backupStr = backupPath.toString();
        String osSeparator = java.io.File.separator;

        if (osSeparator.equals("/")) {
            assertFalse(backupStr.contains("\\"), 
                       "Path should not contain backslashes on Unix-like system: " + backupStr);
        } else { // Assuming osSeparator is "\" for Windows
            assertFalse(backupStr.contains("/"), 
                       "Path should not contain forward slashes on Windows: " + backupStr);
        }
    }
    */
    
    @Test
    @EnabledOnOs(OS.WINDOWS) // Ensure this test only runs on Windows
    void securityTest_PreventDirectoryTraversalOnWindows() {
        Path configPath = Paths.get("C:\\config\\..\\evil.properties");

        // SecureBackupPath.createBackupPath will handle normalization internally.
        Path backupPath = SecureBackupPath.createBackupPath(configPath);
        
        // Expected path after normalization and backup creation: C:\evil.properties.backup
        Path expectedParentDir = Paths.get("C:\\"); // Parent of C:\evil.properties
        Path actualParentDir = backupPath.getParent();

        assertNotNull(actualParentDir, "Parent of backup path should not be null");
        
        // Compare normalized absolute paths for parent directories
        assertEquals(expectedParentDir.toAbsolutePath().normalize().toString().toLowerCase(), 
                     actualParentDir.toAbsolutePath().normalize().toString().toLowerCase(),
                     "Backup parent directory should be 'C:\\' after normalization. Backup path: " + backupPath);
        
        assertEquals("evil.properties.backup", backupPath.getFileName().toString(),
                     "Backup filename is incorrect.");
        
        // Check the full path representation for good measure on Windows
        // Normalize backupPath before toString() to handle any OS-specific representations if needed,
        // though direct comparison might be fine if parent and filename are verified.
        assertEquals("C:\\evil.properties.backup", 
                     backupPath.normalize().toString(), // Normalize for consistent string representation
                     "Full backup path is incorrect.");

        assertFalse(backupPath.toString().contains(".."), 
                   "Backup path string should not contain '..': " + backupPath.toString());
    }
}