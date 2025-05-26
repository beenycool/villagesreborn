package com.beeny.villagesreborn.platform.fabric.testing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.*;

/**
 * Headless test runner for Fabric integration tests
 * Provides a complete headless environment for running Fabric-specific tests
 */
@ExtendWith(MockitoExtension.class)
public class FabricHeadlessTestRunner {
    
    public static class FabricTestEnvironment {
        private final Path gameDirectory;
        private final Path configDirectory;
        private final Runnable cleanup;
        
        public FabricTestEnvironment(Path gameDirectory, Path configDirectory, Runnable cleanup) {
            this.gameDirectory = gameDirectory;
            this.configDirectory = configDirectory;
            this.cleanup = cleanup;
        }
        
        public Path getGameDirectory() { return gameDirectory; }
        public Path getConfigDirectory() { return configDirectory; }
        
        public void cleanup() {
            if (cleanup != null) {
                cleanup.run();
            }
        }
    }
    
    public static class HeadlessEnvironment {
        private final FabricTestEnvironment fabricEnvironment;
        private final Properties systemProperties;
        private final List<String> classpath;
        
        public HeadlessEnvironment(FabricTestEnvironment fabricEnvironment,
                                 Properties systemProperties, List<String> classpath) {
            this.fabricEnvironment = fabricEnvironment;
            this.systemProperties = systemProperties;
            this.classpath = classpath;
        }
        
        public FabricTestEnvironment getFabricEnvironment() {
            return fabricEnvironment;
        }
        
        public Properties getSystemProperties() {
            return systemProperties;
        }
        
        public List<String> getClasspath() {
            return classpath;
        }
        
        public void cleanup() {
            fabricEnvironment.cleanup();
            
            // Restore system properties
            systemProperties.forEach((key, value) -> {
                System.clearProperty(key.toString());
            });
        }
    }
    
    public static class TestExecutionReport {
        private final HeadlessEnvironment environment;
        private final List<TestResult> results;
        private final boolean success;
        private final long executionTimeMs;
        
        public TestExecutionReport(HeadlessEnvironment environment, List<TestResult> results, 
                                 boolean success, long executionTimeMs) {
            this.environment = environment;
            this.results = results;
            this.success = success;
            this.executionTimeMs = executionTimeMs;
        }
        
        public HeadlessEnvironment getEnvironment() { return environment; }
        public List<TestResult> getResults() { return results; }
        public boolean isSuccess() { return success; }
        public long getExecutionTimeMs() { return executionTimeMs; }
    }
    
    public static class TestResult {
        private final String testClass;
        private final String testMethod;
        private final boolean success;
        private final String errorMessage;
        
        public TestResult(String testClass, String testMethod, boolean success, String errorMessage) {
            this.testClass = testClass;
            this.testMethod = testMethod;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public String getTestClass() { return testClass; }
        public String getTestMethod() { return testMethod; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        
        public static TestResult success(String testClass, String testMethod) {
            return new TestResult(testClass, testMethod, true, null);
        }
        
        public static TestResult failure(String testClass, String testMethod, String error) {
            return new TestResult(testClass, testMethod, false, error);
        }
    }
    
    @BeforeAll
    static void setupHeadlessEnvironment() {
        // Setup headless mode for CI/test environments
        System.setProperty("java.awt.headless", "true");
        System.setProperty("fabric.development", "true");
        System.setProperty("fabric.test.environment", "true");
        System.setProperty("villagesreborn.test.mode", "true");
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
            
            return new FabricTestEnvironment(gameDir, configDir, cleanup);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to create headless Fabric environment", e);
        }
    }
    
    /**
     * Initializes a complete headless Fabric environment for integration testing
     */
    public static HeadlessEnvironment initializeHeadlessEnvironment() {
        Properties systemProps = new Properties();
        
        // Setup headless system properties
        String[] headlessProps = {
            "java.awt.headless=true",
            "fabric.development=true", 
            "fabric.test.environment=true",
            "villagesreborn.test.mode=true",
            "minecraft.version=1.21.4",
            "fabric.version=0.100.0"
        };
        
        for (String prop : headlessProps) {
            String[] parts = prop.split("=", 2);
            if (parts.length == 2) {
                System.setProperty(parts[0], parts[1]);
                systemProps.setProperty(parts[0], parts[1]);
            }
        }
        
        // Initialize Fabric test environment
        FabricTestEnvironment fabricEnv = createHeadlessFabricEnvironment();
        
        // Setup integration test classpath
        List<String> classpath = buildIntegrationTestClasspath();
        
        return new HeadlessEnvironment(fabricEnv, systemProps, classpath);
    }
    
    /**
     * Validates the CI environment can run headless tests
     */
    public static boolean validateCIEnvironment() {
        try {
            // Test headless mode
            if (!"true".equals(System.getProperty("java.awt.headless"))) {
                return false;
            }
            
            // Test temporary directory creation
            Path tempDir = Files.createTempDirectory("ci-test");
            boolean canCreateTemp = Files.exists(tempDir);
            Files.deleteIfExists(tempDir);
            
            if (!canCreateTemp) {
                return false;
            }
            
            // Test environment setup
            HeadlessEnvironment env = initializeHeadlessEnvironment();
            boolean envValid = env.getFabricEnvironment() != null;
            env.cleanup();
            
            return envValid;
            
        } catch (Exception e) {
            return false;
        }
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
        
        properties.store(Files.newOutputStream(configPath), "Test configuration for Villages Reborn");
    }
    
    private static List<String> buildIntegrationTestClasspath() {
        // In a real implementation, this would build the complete classpath
        // For now, return a basic classpath
        return List.of(
            "fabric/build/classes/java/main",
            "fabric/build/classes/java/test", 
            "common/build/classes/java/main",
            "common/build/classes/java/test"
        );
    }
}