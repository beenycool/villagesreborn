package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.mockito.Mockito.*;

/**
 * Enhanced mock utilities for FabricLoader testing with headless environment support
 */
@ExtendWith(MockitoExtension.class)
public class FabricLoaderMockUtilsEnhanced extends FabricLoaderMockUtils {
    
    public static class FabricTestEnvironment {
        private final MockedStatic<?> mockLoader;
        private final Path gameDirectory;
        private final Path configDirectory;
        private final Runnable cleanup;
        
        public FabricTestEnvironment(MockedStatic<?> mockLoader, Path gameDirectory, 
                                   Path configDirectory, Runnable cleanup) {
            this.mockLoader = mockLoader;
            this.gameDirectory = gameDirectory;
            this.configDirectory = configDirectory;
            this.cleanup = cleanup;
        }
        
        public Path getGameDirectory() { return gameDirectory; }
        public Path getConfigDirectory() { return configDirectory; }
        public Runnable getCleanup() { return cleanup; }
        
        public void cleanup() {
            if (cleanup != null) {
                cleanup.run();
            }
        }
    }
    
    public static class WorldTestEnvironment {
        private final Object world;
        private final Object server;
        private final Object settings;
        
        public WorldTestEnvironment(Object world, Object server, Object settings) {
            this.world = world;
            this.server = server;
            this.settings = settings;
        }
        
        public Object getWorld() { return world; }
        public Object getServer() { return server; }
        public Object getSettings() { return settings; }
    }
    
    public static class TestEnvironment {
        private final FabricTestEnvironment fabricEnvironment;
        private final WorldTestEnvironment worldEnvironment;
        
        public TestEnvironment(FabricTestEnvironment fabricEnvironment, 
                             WorldTestEnvironment worldEnvironment) {
            this.fabricEnvironment = fabricEnvironment;
            this.worldEnvironment = worldEnvironment;
        }
        
        public FabricTestEnvironment getFabricEnvironment() { return fabricEnvironment; }
        public WorldTestEnvironment getWorldEnvironment() { return worldEnvironment; }
    }
    
    /**
     * Creates a headless Fabric test environment for integration testing
     */
    public static FabricTestEnvironment createHeadlessFabricEnvironment() {
        try {
            // Setup headless system properties
            System.setProperty("java.awt.headless", "true");
            System.setProperty("fabric.development", "true");
            System.setProperty("fabric.test.environment", "true");
            
            // Create temporary directories
            Path gameDir = Files.createTempDirectory("fabric-test-game");
            Path configDir = gameDir.resolve("config");
            Files.createDirectories(configDir);
            
            // Create mock configuration
            createMockConfigFile(configDir.resolve("villagesreborn_setup.properties"));
            
            // Mock environment variables
            setupEnvironmentMocks();
            
            Runnable cleanup = () -> {
                try {
                    // Clean up temporary directories
                    Files.walk(gameDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // Ignore cleanup errors
                            }
                        });
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
                
                // Clear system properties
                System.clearProperty("java.awt.headless");
                System.clearProperty("fabric.development");
                System.clearProperty("fabric.test.environment");
            };
            
            return new FabricTestEnvironment(null, gameDir, configDir, cleanup);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to create headless Fabric environment", e);
        }
    }
    
    /**
     * Creates mock world environment for testing
     */
    public static WorldTestEnvironment createMockWorldEnvironment() {
        Object mockWorld = mock(Object.class);
        Object mockServer = mock(Object.class);
        Object mockSettings = mock(Object.class);
        
        return new WorldTestEnvironment(mockWorld, mockServer, mockSettings);
    }
    
    /**
     * Sets up standard test environment with both Fabric and World mocks
     */
    public static TestEnvironment setupStandardTestEnvironment() {
        FabricTestEnvironment fabricEnv = createHeadlessFabricEnvironment();
        WorldTestEnvironment worldEnv = createMockWorldEnvironment();
        
        return new TestEnvironment(fabricEnv, worldEnv);
    }
    
    /**
     * Creates mock configuration file for testing
     */
    private static void createMockConfigFile(Path configPath) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("firstTimeSetupComplete", "true");
        properties.setProperty("llmProvider", "TEST_PROVIDER");
        properties.setProperty("enableAdvancedAI", "false");
        properties.setProperty("testMode", "true");
        
        try (FileOutputStream fos = new FileOutputStream(configPath.toFile())) {
            properties.store(fos, "Test configuration for Villages Reborn");
        }
    }
    
    /**
     * Sets up environment variable mocks
     */
    private static void setupEnvironmentMocks() {
        System.setProperty("MINECRAFT_VERSION", "1.21.4");
        System.setProperty("FABRIC_VERSION", "0.100.0");
        System.setProperty("VILLAGESREBORN_TEST_MODE", "true");
    }
}