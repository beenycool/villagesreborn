package com.beeny.villagesreborn.core.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default config path provider that uses the current working directory.
 * This provider is always available and serves as a fallback.
 */
public class DefaultConfigPathProvider implements ConfigPathStrategy {
    private static final String SETUP_CONFIG_FILE = "villagesreborn_setup.properties";
    
    @Override
    public boolean isAvailable() {
        return true; // Always available
    }
    
    @Override
    public int getPriority() {
        return 1; // Lower priority than Fabric provider
    }
    
    @Override
    public Path getConfigPath() {
        String userDir = System.getProperty("user.dir");
        return Paths.get(userDir, SETUP_CONFIG_FILE);
    }
}