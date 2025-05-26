package com.beeny.villagesreborn.core.config;

import org.mockito.MockedStatic;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 * Utility class for mocking FabricLoader in tests.
 * Provides common mocking patterns for different test scenarios.
 *
 * Note: This is a simplified mock utility. In practice, testing the actual
 * FabricLoader integration would require more sophisticated mocking or
 * integration tests in the fabric module.
 */
public class FabricLoaderMockUtils {
    
    /**
     * Create a mock config path provider that simulates FabricLoader behavior.
     * This is used instead of complex static mocking.
     *
     * @param configDir the config directory to return
     * @return a FabricConfigPathProvider that returns the specified directory
     */
    public static FabricConfigPathProvider createMockFabricProvider(Path configDir) {
        return new FabricConfigPathProvider() {
            @Override
            public boolean isAvailable() {
                return true;
            }
            
            @Override
            public Path getConfigPath() {
                return configDir.resolve("villagesreborn_setup.properties");
            }
        };
    }
    
    /**
     * Create a mock config path provider that simulates FabricLoader being unavailable.
     *
     * @return a FabricConfigPathProvider that reports as unavailable
     */
    public static FabricConfigPathProvider createUnavailableFabricProvider() {
        return new FabricConfigPathProvider() {
            @Override
            public boolean isAvailable() {
                return false;
            }
            
            @Override
            public Path getConfigPath() {
                throw new UnsupportedOperationException("FabricLoader not available");
            }
        };
    }
    
    /**
     * Create a mock config path provider that throws exceptions.
     *
     * @param exception the exception to throw
     * @return a FabricConfigPathProvider that throws the specified exception
     */
    public static FabricConfigPathProvider createFaultyFabricProvider(RuntimeException exception) {
        return new FabricConfigPathProvider() {
            @Override
            public boolean isAvailable() {
                return true; // Claims to be available but fails
            }
            
            @Override
            public Path getConfigPath() {
                throw exception;
            }
        };
    }
}