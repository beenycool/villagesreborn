package com.beeny.villagesreborn.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves config file paths using a priority-based strategy pattern.
 * Tries providers in order of priority until one succeeds.
 */
public class ConfigPathResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPathResolver.class);
    
    private final List<ConfigPathStrategy> providers;
    
    public ConfigPathResolver() {
        this(createDefaultProviders());
    }
    
    // Package-private constructor for testing
    ConfigPathResolver(List<ConfigPathStrategy> providers) {
        this.providers = new ArrayList<>(providers);
        // Sort by priority (higher priority first)
        this.providers.sort(Comparator.comparingInt(ConfigPathStrategy::getPriority).reversed());
    }
    
    private static List<ConfigPathStrategy> createDefaultProviders() {
        return Arrays.asList(
            new FabricConfigPathProvider(),
            new DefaultConfigPathProvider()
        );
    }
    
    /**
     * Resolve the config file path using available providers.
     * 
     * @return the resolved config file path
     * @throws IllegalStateException if no provider is available
     */
    public Path resolveConfigPath() {
        for (ConfigPathStrategy provider : providers) {
            if (provider.isAvailable()) {
                try {
                    Path path = provider.getConfigPath();
                    LOGGER.debug("Config path resolved using {}: {}", 
                               provider.getClass().getSimpleName(), path);
                    return path;
                } catch (Exception e) {
                    LOGGER.warn("Config path provider {} failed: {}", 
                              provider.getClass().getSimpleName(), e.getMessage());
                    // Continue to next provider
                }
            } else {
                LOGGER.debug("Config path provider {} not available", 
                           provider.getClass().getSimpleName());
            }
        }
        
        throw new IllegalStateException("No config path provider available");
    }
}