package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SecureBackupPath utility class
 */
class SecureBackupPathUtilityTest {
    
    @Test
    void createBackupPath_NullConfigPath_ThrowsException() {
        assertThrows(NullPointerException.class, () -> 
            SecureBackupPath.createBackupPath(null));
    }
    
    @Test
    void createBackupPath_ValidPath_CreatesBackupInSameDirectory() {
        Path configPath = Paths.get("config/app.properties");
        Path backupPath = SecureBackupPath.createBackupPath(configPath);
        
        assertEquals("config", backupPath.getParent().toString());
        assertEquals("app.properties.backup", backupPath.getFileName().toString());
        assertTrue(backupPath.toString().endsWith("app.properties.backup"));
    }
    
    @Test
    void createBackupPath_WithCustomSuffix_UsesCustomSuffix() {
        Path configPath = Paths.get("config/app.properties");
        Path backupPath = SecureBackupPath.createBackupPath(configPath, "bak");
        
        assertEquals("config", backupPath.getParent().toString());
        assertEquals("app.properties.bak", backupPath.getFileName().toString());
    }
    
    @Test
    void createBackupPath_CustomSuffixWithDot_HandlesCorrectly() {
        Path configPath = Paths.get("config/app.properties");
        Path backupPath = SecureBackupPath.createBackupPath(configPath, ".bak");
        
        assertEquals("app.properties.bak", backupPath.getFileName().toString());
    }
    
    @Test
    void createBackupPath_EmptySuffix_ThrowsException() {
        Path configPath = Paths.get("config/app.properties");
        
        assertThrows(IllegalArgumentException.class, () -> 
            SecureBackupPath.createBackupPath(configPath, ""));
        assertThrows(IllegalArgumentException.class, () -> 
            SecureBackupPath.createBackupPath(configPath, "   "));
        assertThrows(NullPointerException.class, () -> 
            SecureBackupPath.createBackupPath(configPath, null));
    }
    
    @Test
    void createBackupPath_DirectoryPath_ThrowsException() {
        // Test with a path that resolves to a directory (null filename after normalization)
        Path directoryPath = Paths.get(".");
        
        assertThrows(IllegalArgumentException.class, () ->
            SecureBackupPath.createBackupPath(directoryPath));
    }
    
    @Test
    void createBackupPath_PathWithTraversal_NormalizesSecurely() {
        Path maliciousPath = Paths.get("config/../secrets/app.properties");
        Path backupPath = SecureBackupPath.createBackupPath(maliciousPath);
        
        // After normalization, should be in secrets directory, not config
        assertEquals("secrets", backupPath.getParent().toString());
        assertEquals("app.properties.backup", backupPath.getFileName().toString());
        assertFalse(backupPath.toString().contains(".."));
    }
    
    @Test
    void isSecurePath_ValidPath_ReturnsTrue() {
        assertTrue(SecureBackupPath.isSecurePath(Paths.get("config/app.properties")));
        assertTrue(SecureBackupPath.isSecurePath(Paths.get("a/b/c/file.txt")));
    }
    
    @Test
    void isSecurePath_NullPath_ReturnsFalse() {
        assertFalse(SecureBackupPath.isSecurePath(null));
    }
    
    @Test
    void isSecurePath_PathWithTraversal_ReturnsFalse() {
        // Note: This tests the raw path string after normalization
        Path pathWithTraversal = Paths.get("config/../../../etc/passwd").normalize();
        if (pathWithTraversal.toString().contains("..")) {
            assertFalse(SecureBackupPath.isSecurePath(pathWithTraversal));
        }
    }
    
    @Test
    void isSecurePath_DirectoryPath_ReturnsFalse() {
        // Test directory paths that normalize to null filename
        assertFalse(SecureBackupPath.isSecurePath(Paths.get(".")));
        // Test current directory reference
        Path dotPath = Paths.get(".");
        assertFalse(SecureBackupPath.isSecurePath(dotPath));
    }
    
    @Test
    void getBackupDirectory_ValidPath_ReturnsParentDirectory() {
        Path configPath = Paths.get("config/subdir/app.properties");
        Path backupDir = SecureBackupPath.getBackupDirectory(configPath);
        
        assertEquals(Paths.get("config/subdir"), backupDir);
    }
    
    @Test
    void getBackupDirectory_RootFile_ThrowsException() {
        Path rootPath = Paths.get("app.properties");
        
        assertThrows(IllegalArgumentException.class, () -> 
            SecureBackupPath.getBackupDirectory(rootPath));
    }
    
    @Test
    void getBackupDirectory_NullPath_ThrowsException() {
        assertThrows(NullPointerException.class, () -> 
            SecureBackupPath.getBackupDirectory(null));
    }
    
    @Test
    void windowsSpecific_AbsolutePath_HandlesCorrectly() {
        // This test works on all platforms but simulates Windows paths
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            Path configPath = Paths.get("C:\\config\\app.properties");
            Path backupPath = SecureBackupPath.createBackupPath(configPath);
            
            assertEquals("C:\\config", backupPath.getParent().toString());
            assertEquals("app.properties.backup", backupPath.getFileName().toString());
        }
    }
    
    @Test
    void crossPlatform_MixedSeparators_NormalizesCorrectly() {
        // Test that mixed separators get normalized appropriately by SecureBackupPath.createBackupPath
        Path rawMixedPath = Paths.get("config/subdir\\app.properties"); // Use raw mixed path
        Path backupPath = SecureBackupPath.createBackupPath(rawMixedPath); // Pass raw path to method under test

        Path expectedParent;
        String expectedFilename;

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            // On Windows, '\' is a separator
            expectedParent = Paths.get("config/subdir");
            expectedFilename = "app.properties.backup";
        } else {
            // On Linux/macOS, '\' is a valid filename character
            expectedParent = Paths.get("config");
            expectedFilename = "subdir\\app.properties.backup";
        }

        Path actualParent = backupPath.getParent();
        assertNotNull(actualParent, "Parent of backupPath should not be null. Path: " + backupPath);

        assertEquals(
            expectedParent.toAbsolutePath().normalize().toString().toLowerCase(),
            actualParent.toAbsolutePath().normalize().toString().toLowerCase(),
            "Parent paths should match after normalization and absolutization."
        );
        assertEquals(expectedFilename, backupPath.getFileName().toString());
    }
    
    @Test
    void specialCharacters_HandledCorrectly() {
        Path configPath = Paths.get("config/app with spaces & symbols.properties");
        Path backupPath = SecureBackupPath.createBackupPath(configPath);
        
        assertEquals("config", backupPath.getParent().toString());
        assertEquals("app with spaces & symbols.properties.backup", backupPath.getFileName().toString());
    }
}