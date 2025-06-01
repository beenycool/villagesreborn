package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigPathResolver functionality.
 * Tests the strategy pattern implementation for config path resolution.
 */
class ConfigPathResolverTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void testResolverUsesHighestPriorityAvailableProvider() {
        // GIVEN: Providers with different priorities
        ConfigPathStrategy highPriority = createTestProvider(100, true, "high-priority-config.properties");
        ConfigPathStrategy lowPriority = createTestProvider(10, true, "low-priority-config.properties");
        
        ConfigPathResolver resolver = new ConfigPathResolver(Arrays.asList(lowPriority, highPriority));
        
        // WHEN: Resolving config path
        Path path = resolver.resolveConfigPath();
        
        // THEN: Should use highest priority provider
        assertTrue(path.toString().contains("high-priority-config.properties"));
    }
    
    @Test
    void testResolverSkipsUnavailableProviders() {
        // GIVEN: Mix of available and unavailable providers
        ConfigPathStrategy unavailable = createTestProvider(100, false, "unavailable.properties");
        ConfigPathStrategy available = createTestProvider(50, true, "available.properties");
        
        ConfigPathResolver resolver = new ConfigPathResolver(Arrays.asList(unavailable, available));
        
        // WHEN: Resolving config path
        Path path = resolver.resolveConfigPath();
        
        // THEN: Should use available provider even with lower priority
        assertTrue(path.toString().contains("available.properties"));
    }
    
    @Test
    void testResolverHandlesProviderExceptions() {
        // GIVEN: Provider that throws exception
        ConfigPathStrategy faultyProvider = createFaultyProvider(100);
        ConfigPathStrategy workingProvider = createTestProvider(50, true, "working.properties");
        
        ConfigPathResolver resolver = new ConfigPathResolver(Arrays.asList(faultyProvider, workingProvider));
        
        // WHEN: Resolving config path
        Path path = resolver.resolveConfigPath();
        
        // THEN: Should fall back to working provider
        assertTrue(path.toString().contains("working.properties"));
    }
    
    @Test
    void testResolverWithNoAvailableProviders() {
        // GIVEN: No available providers
        ConfigPathResolver resolver = new ConfigPathResolver(Arrays.asList());
        
        // WHEN: Resolving config path
        // THEN: Should throw exception
        assertThrows(IllegalStateException.class, resolver::resolveConfigPath);
    }
    
    @Test
    void testDefaultProviders() {
        // GIVEN: Default resolver (no custom providers)
        ConfigPathResolver resolver = new ConfigPathResolver();
        
        // WHEN: Resolving config path
        Path path = resolver.resolveConfigPath();
        
        // THEN: Should return a valid path
        assertNotNull(path);
        assertTrue(path.toString().endsWith("villagesreborn_setup.properties"));
    }
    
    @Test
    void testFabricConfigPathProvider() {
        // GIVEN: Mock Fabric provider
        ConfigPathStrategy provider = FabricLoaderMockUtils.createMockFabricProvider(tempDir);
        
        // WHEN: Getting config path
        Path path = provider.getConfigPath();
        
        // THEN: Should return expected path
        assertEquals(tempDir.resolve("villagesreborn_setup.properties"), path);
        assertTrue(provider.isAvailable());
        assertEquals(100, provider.getPriority());
    }
    
    @Test
    void testDefaultConfigPathProvider() {
        // GIVEN: Default provider
        DefaultConfigPathProvider provider = new DefaultConfigPathProvider();
        
        // WHEN: Getting config path
        Path path = provider.getConfigPath();
        
        // THEN: Should return user.dir based path
        assertNotNull(path);
        assertTrue(path.toString().endsWith("villagesreborn_setup.properties"));
        assertTrue(provider.isAvailable());
        assertEquals(1, provider.getPriority());
    }
    
    private ConfigPathStrategy createTestProvider(int priority, boolean available, String filename) {
        return new ConfigPathStrategy() {
            @Override
            public boolean isAvailable() {
                return available;
            }
            
            @Override
            public int getPriority() {
                return priority;
            }
            
            @Override
            public Path getConfigPath() {
                if (!available) {
                    throw new UnsupportedOperationException("Provider not available");
                }
                return tempDir.resolve(filename);
            }
        };
    }
    
    private ConfigPathStrategy createFaultyProvider(int priority) {
        return new ConfigPathStrategy() {
            @Override
            public boolean isAvailable() {
                return true; // Claims to be available but fails
            }
            
            @Override
            public int getPriority() {
                return priority;
            }
            
            @Override
            public Path getConfigPath() {
                throw new RuntimeException("Simulated provider failure");
            }
        };
    }
}