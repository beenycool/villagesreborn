package com.beeny.villagesreborn.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Config path provider that uses FabricLoader's config directory.
 * Falls back gracefully when FabricLoader is not available.
 */
public class FabricConfigPathProvider implements ConfigPathStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricConfigPathProvider.class);
    private static final String SETUP_CONFIG_FILE = "villagesreborn_setup.properties";
    
    private final Optional<Object> fabricLoader;
    
    public FabricConfigPathProvider() {
        this.fabricLoader = initializeFabricLoader();
    }
    
    private Optional<Object> initializeFabricLoader() {
        try {
            // Use reflection to avoid compile-time dependency on FabricLoader
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            LOGGER.debug("FabricLoader detected and initialized successfully");
            return Optional.of(instance);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("FabricLoader not found in classpath, will use fallback");
        } catch (NoClassDefFoundError e) {
            LOGGER.debug("FabricLoader class definition not found, will use fallback");
        } catch (LinkageError e) {
            LOGGER.debug("FabricLoader linkage error, will use fallback: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.debug("Failed to initialize FabricLoader, will use fallback: {}", e.getMessage());
        }
        return Optional.empty();
    }
    
    @Override
    public boolean isAvailable() {
        return fabricLoader.isPresent();
    }
    
    @Override
    public int getPriority() {
        return 100; // Higher priority than default
    }
    
    @Override
    public Path getConfigPath() {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("FabricLoader not available");
        }
        
        try {
            Object loader = fabricLoader.get();
            // Call getConfigDir() using reflection
            Path configDir = (Path) loader.getClass().getMethod("getConfigDir").invoke(loader);
            if (configDir == null) {
                throw new RuntimeException("FabricLoader returned null config directory");
            }
            return configDir.resolve(SETUP_CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.warn("Failed to get config path from FabricLoader: {}", e.getMessage());
            throw new UnsupportedOperationException("Failed to access FabricLoader config directory", e);
        }
    }
}