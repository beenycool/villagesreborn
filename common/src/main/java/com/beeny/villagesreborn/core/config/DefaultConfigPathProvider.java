package com.beeny.villagesreborn.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default config path provider that delegates to FabricConfigPathProvider when available,
 * and falls back to the current working directory only if Fabric provider returns empty.
 */
public class DefaultConfigPathProvider implements ConfigPathStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigPathProvider.class);
    private static final String SETUP_CONFIG_FILE = "villagesreborn_setup.properties";
    
    private final FabricConfigPathProvider fabricProvider;
    
    public DefaultConfigPathProvider() {
        this.fabricProvider = new FabricConfigPathProvider();
    }
    
    @Override
    public boolean isAvailable() {
        return true; // Always available as fallback
    }
    
    @Override
    public int getPriority() {
        return 1; // Lower priority than Fabric provider
    }
    
    @Override
    public Path getConfigPath() {
        // First try to delegate to Fabric provider
        if (fabricProvider.isAvailable()) {
            try {
                Path fabricPath = fabricProvider.getConfigPath();
                if (fabricPath != null) {
                    LOGGER.debug("Using Fabric config path: {}", fabricPath);
                    return fabricPath;
                }
            } catch (UnsupportedOperationException e) {
                LOGGER.debug("Fabric provider failed, falling back to default: {}", e.getMessage());
            } catch (Exception e) {
                LOGGER.warn("Unexpected error from Fabric provider, falling back to default: {}", e.getMessage());
            }
        }
        
        // Fall back to user.dir if Fabric provider is not available or returns empty
        String userDir = System.getProperty("user.dir");
        Path fallbackPath = Paths.get(userDir, SETUP_CONFIG_FILE);
        LOGGER.debug("Using fallback config path: {}", fallbackPath);
        return fallbackPath;
    }
    
    /**
     * Get the Fabric provider instance for testing purposes
     */
    FabricConfigPathProvider getFabricProvider() {
        return fabricProvider;
    }
}