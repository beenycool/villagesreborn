package com.beeny.villagesreborn.core.config;

import java.nio.file.Path;

/**
 * Strategy interface for resolving config file paths.
 * Implementations provide different methods for determining where configuration files should be stored.
 */
public interface ConfigPathStrategy {
    
    /**
     * Get the config file path using this strategy.
     * 
     * @return the resolved config file path
     * @throws UnsupportedOperationException if this strategy is not available
     */
    Path getConfigPath();
    
    /**
     * Check if this strategy is available and can be used.
     * 
     * @return true if this strategy can provide a config path, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Get the priority of this strategy. Higher values indicate higher priority.
     * 
     * @return the priority value (higher = more preferred)
     */
    int getPriority();
}