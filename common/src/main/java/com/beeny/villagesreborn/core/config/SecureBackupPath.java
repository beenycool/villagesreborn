package com.beeny.villagesreborn.core.config;

import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.Objects;

/**
 * Utility class for creating secure backup paths that prevent directory traversal attacks
 * and handle cross-platform path normalization correctly.
 * 
 * This class addresses Windows-specific path resolution issues and ensures that backup
 * files are always created in the same directory as the original file, regardless of
 * the platform or path format used.
 */
public final class SecureBackupPath {
    
    private SecureBackupPath() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a secure backup path for the given configuration file path.
     * 
     * This method:
     * - Normalizes the input path to resolve any ".." or "." components
     * - Uses resolveSibling to ensure the backup stays in the same directory
     * - Handles Windows-specific path separators and drive letters correctly
     * - Prevents directory traversal attacks
     * 
     * @param configPath the path to the configuration file
     * @return a secure backup path in the same directory as the config file
     * @throws IllegalArgumentException if configPath is null
     * @throws InvalidPathException if the path cannot be normalized or processed
     */
    public static Path createBackupPath(Path configPath) {
        Objects.requireNonNull(configPath, "Config path cannot be null");
        
        try {
            // Normalize the path to resolve any .. or . components
            // This is crucial for security and Windows path handling
            Path normalizedPath = configPath.normalize();
            
            // Get the filename - handle edge case where path ends with separator
            Path fileName = normalizedPath.getFileName();
            if (fileName == null || fileName.toString().isEmpty() || fileName.toString().equals(".") || fileName.toString().equals("..")) {
                // Path represents a root, current directory, parent directory, or has no filename (e.g., "C:\\" or "/")
                throw new IllegalArgumentException("Config path must point to a file, not a directory: " + configPath);
            }
            
            // Create backup filename by appending .backup
            String backupFileName = fileName.toString() + ".backup";
            
            // Use resolveSibling to ensure backup is in the same directory as config
            // This method is safe and prevents directory traversal as it stays within the parent directory
            return normalizedPath.resolveSibling(backupFileName);
            
        } catch (InvalidPathException e) {
            throw new InvalidPathException(configPath.toString(), 
                "Failed to create secure backup path: " + e.getReason());
        }
    }
    
    /**
     * Creates a secure backup path with a custom backup suffix.
     * 
     * @param configPath the path to the configuration file
     * @param backupSuffix the suffix to append (e.g., ".backup", ".bak")
     * @return a secure backup path with the specified suffix
     * @throws IllegalArgumentException if configPath is null or backupSuffix is null/empty
     * @throws InvalidPathException if the path cannot be normalized or processed
     */
    public static Path createBackupPath(Path configPath, String backupSuffix) {
        Objects.requireNonNull(configPath, "Config path cannot be null");
        Objects.requireNonNull(backupSuffix, "Backup suffix cannot be null");
        
        if (backupSuffix.trim().isEmpty()) {
            throw new IllegalArgumentException("Backup suffix cannot be empty");
        }
        
        try {
            Path normalizedPath = configPath.normalize();
            Path fileName = normalizedPath.getFileName();
            
            if (fileName == null) {
                throw new IllegalArgumentException("Config path must point to a file, not a directory: " + configPath);
            }
            
            // Ensure suffix starts with a dot if not already present
            String suffix = backupSuffix.startsWith(".") ? backupSuffix : "." + backupSuffix;
            String backupFileName = fileName.toString() + suffix;
            
            return normalizedPath.resolveSibling(backupFileName);
            
        } catch (InvalidPathException e) {
            throw new InvalidPathException(configPath.toString(), 
                "Failed to create secure backup path with suffix '" + backupSuffix + "': " + e.getReason());
        }
    }
    
    /**
     * Validates that a path is safe for backup operations.
     * 
     * @param path the path to validate
     * @return true if the path is safe for backup operations
     */
    public static boolean isSecurePath(Path path) {
        if (path == null) {
            return false;
        }
        
        try {
            Path normalizedPath = path.normalize();
            String pathString = normalizedPath.toString();
            
            // Check for directory traversal attempts
            if (pathString.contains("..")) {
                return false;
            }
            
            // Check for filename - reject directory paths
            Path fileName = normalizedPath.getFileName();
            if (fileName == null || fileName.toString().isEmpty() || fileName.toString().equals(".") || fileName.toString().equals("..")) {
                return false;
            }
            
            return true;
            
        } catch (InvalidPathException e) {
            return false;
        }
    }
    
    /**
     * Gets the backup directory for a given config path.
     * This is always the same as the config file's parent directory.
     * 
     * @param configPath the configuration file path
     * @return the directory where backup files should be created
     * @throws IllegalArgumentException if configPath is null or invalid
     */
    public static Path getBackupDirectory(Path configPath) {
        Objects.requireNonNull(configPath, "Config path cannot be null");
        
        Path normalizedPath = configPath.normalize();
        Path parent = normalizedPath.getParent();
        
        if (parent == null) {
            throw new IllegalArgumentException("Config path has no parent directory: " + configPath);
        }
        
        return parent;
    }
}