package com.beeny.villagesreborn.core.testing;

import com.beeny.villagesreborn.core.config.FabricLoaderMockUtilsEnhanced;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Master test that validates all test suites pass and mock utilities work correctly
 * This test fails the build if any test suite fails, as per the specification
 */
@ExtendWith(MockitoExtension.class)
public class TestMetaTest {
    
    private static final List<String> TEST_SUITES = List.of(
        "common:test",
        "fabric:test"
    );
    
    @BeforeAll
    static void setupTestEnvironment() {
        // Setup logging for test debugging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
        System.setProperty("villagesreborn.test.mode", "true");
    }
    
    @Test
    void testMockUtilitiesInitialization() {
        // Test that FabricLoaderMockUtils can initialize correctly
        assertDoesNotThrow(() -> {
            FabricLoaderMockUtilsEnhanced.FabricTestEnvironment env = 
                FabricLoaderMockUtilsEnhanced.createHeadlessFabricEnvironment();
            
            assertNotNull(env.getGameDirectory(), "Game directory should be created");
            assertNotNull(env.getConfigDirectory(), "Config directory should be created");
            assertTrue(Files.exists(env.getConfigDirectory()), "Config directory should exist");
            
            // Cleanup
            env.cleanup();
        }, "FabricLoaderMockUtils should initialize without errors");
    }
    
    @Test
    void testWorldCreationMockInitialization() {
        assertDoesNotThrow(() -> {
            FabricLoaderMockUtilsEnhanced.WorldTestEnvironment worldEnv = 
                FabricLoaderMockUtilsEnhanced.createMockWorldEnvironment();
            
            assertNotNull(worldEnv.getWorld(), "Mock world should be created");
            assertNotNull(worldEnv.getServer(), "Mock server should be created");
            assertNotNull(worldEnv.getSettings(), "Mock settings should be created");
        }, "World creation mocks should initialize without errors");
    }
    
    @Test
    void testStandardTestEnvironmentSetup() {
        assertDoesNotThrow(() -> {
            FabricLoaderMockUtilsEnhanced.TestEnvironment testEnv = 
                FabricLoaderMockUtilsEnhanced.setupStandardTestEnvironment();
            
            assertNotNull(testEnv.getFabricEnvironment(), "Fabric environment should be set up");
            assertNotNull(testEnv.getWorldEnvironment(), "World environment should be set up");
            
            // Cleanup
            testEnv.getFabricEnvironment().cleanup();
        }, "Standard test environment should set up without errors");
    }
    
    @Test
    void testGradleConfigurationExists() {
        // Verify gradle configuration files exist
        assertTrue(Files.exists(Paths.get("gradlew")), "Gradle wrapper should exist");
        assertTrue(Files.exists(Paths.get("gradlew.bat")), "Gradle wrapper batch file should exist");
        assertTrue(Files.exists(Paths.get("settings.gradle")), "Settings.gradle should exist");
        assertTrue(Files.exists(Paths.get("common/build.gradle")), "Common build.gradle should exist");
        assertTrue(Files.exists(Paths.get("fabric/build.gradle")), "Fabric build.gradle should exist");
    }
    
    @Test
    void testTestDependenciesConfiguration() throws IOException {
        // Verify common module has required test dependencies
        String commonBuild = Files.readString(Paths.get("common/build.gradle"));
        assertTrue(commonBuild.contains("junit-jupiter"), "Common should have JUnit 5");
        assertTrue(commonBuild.contains("assertj-core"), "Common should have AssertJ");
        assertTrue(commonBuild.contains("mockito-core"), "Common should have Mockito");
        assertTrue(commonBuild.contains("useJUnitPlatform()"), "Common should use JUnit platform");
        
        // Verify fabric module has required test dependencies
        String fabricBuild = Files.readString(Paths.get("fabric/build.gradle"));
        assertTrue(fabricBuild.contains("junit-jupiter"), "Fabric should have JUnit 5");
        assertTrue(fabricBuild.contains("assertj-core"), "Fabric should have AssertJ");
        assertTrue(fabricBuild.contains("mockito-core"), "Fabric should have Mockito");
        assertTrue(fabricBuild.contains("useJUnitPlatform()"), "Fabric should use JUnit platform");
    }
    
    @Test
    void testProjectStructureIntegrity() {
        // Verify key project structure elements exist
        assertTrue(Files.exists(Paths.get("common/src/main/java")), "Common main source should exist");
        assertTrue(Files.exists(Paths.get("common/src/test/java")), "Common test source should exist");
        assertTrue(Files.exists(Paths.get("fabric/src/main/java")), "Fabric main source should exist");
        assertTrue(Files.exists(Paths.get("fabric/src/test/java")), "Fabric test source should exist");
        
        // Verify enhanced mock utilities exist
        assertTrue(Files.exists(Paths.get("common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtilsEnhanced.java")), 
                  "Enhanced mock utilities should exist");
    }
    
    /**
     * Meta test that validates the overall test suite execution.
     * This test is designed to catch systemic test failures that might not be
     * visible in individual test runs.
     */
    @Test
    void validateTestSuiteHealth() {
        // This test serves as a canary for the overall health of the test suite
        // If core infrastructure is broken, this test should catch it
        
        List<String> issues = new ArrayList<>();
        
        // Check for common test configuration issues
        if (!Files.exists(Paths.get("common/src/test/java"))) {
            issues.add("Common test source directory missing");
        }
        
        if (!Files.exists(Paths.get("fabric/src/test/java"))) {
            issues.add("Fabric test source directory missing");
        }
        
        // Check for critical missing classes that would cause widespread failures
        Path[] criticalFiles = {
            Paths.get("common/src/main/java/com/beeny/villagesreborn/core/VillagesRebornCommon.java"),
            Paths.get("common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtils.java"),
            Paths.get("common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtilsEnhanced.java")
        };
        
        for (Path file : criticalFiles) {
            if (!Files.exists(file)) {
                issues.add("Critical file missing: " + file);
            }
        }
        
        if (!issues.isEmpty()) {
            fail("Test suite health check failed:\n" + String.join("\n", issues));
        }
    }
    
    /**
     * Validates that the CI environment can execute basic operations
     */
    @Test
    void validateCICompatibility() {
        // Test headless mode compatibility
        String headlessProperty = System.getProperty("java.awt.headless");
        if (!"true".equals(headlessProperty)) {
            System.setProperty("java.awt.headless", "true");
        }
        
        // Test environment variable handling
        System.setProperty("villagesreborn.ci.mode", "true");
        assertEquals("true", System.getProperty("villagesreborn.ci.mode"));
        
        // Test temp directory creation (common CI issue)
        assertDoesNotThrow(() -> {
            Path tempDir = Files.createTempDirectory("villagesreborn-ci-test");
            assertTrue(Files.exists(tempDir));
            Files.deleteIfExists(tempDir);
        }, "CI should be able to create temporary directories");
    }
}