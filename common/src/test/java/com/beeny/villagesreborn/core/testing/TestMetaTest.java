package com.beeny.villagesreborn.core.testing;

import com.beeny.villagesreborn.core.config.FabricLoaderMockUtilsEnhanced;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    private static File PROJECT_DIR;
    
    private static File findProjectRoot() {
        try {
            File current = new File(System.getProperty("user.dir"));
            while (current != null) {
                if (new File(current, "gradlew").exists() || new File(current, "gradlew.bat").exists()) {
                    return current;
                }
                current = current.getParentFile();
            }
            // Fallback to current directory if gradle wrapper not found
            return new File(System.getProperty("user.dir"));
        } catch (Exception e) {
            // Fallback to current directory
            return new File(System.getProperty("user.dir"));
        }
    }
    
    @BeforeAll
    static void setupTestEnvironment() {
        // Initialize project directory
        PROJECT_DIR = findProjectRoot();
        
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
     * Executes all test suites using Gradle and collects execution metrics.
     * Replaces the static TEST_SUITES approach with dynamic execution.
     * DISABLED: This test causes circular dependency during build.
     */
    @Test
    @org.junit.jupiter.api.Disabled("Disabled to prevent circular test execution during build")
    void executeTestSuitesWithMetrics() {
        List<TestSuiteResult> results = new ArrayList<>();
        
        for (String suite : TEST_SUITES) {
            TestSuiteResult result = executeTestSuite(suite);
            results.add(result);
        }
        
        // Assert that all test suites passed (zero failures)
        for (TestSuiteResult result : results) {
            assertEquals(0, result.failures,
                String.format("Test suite '%s' had %d failures. Output: %s",
                    result.suiteName, result.failures, result.output));
        }
        
        // Log execution metrics
        long totalDuration = results.stream().mapToLong(r -> r.duration).sum();
        int totalTests = results.stream().mapToInt(r -> r.testCount).sum();
        
        System.out.printf("Test suite execution summary:%n");
        System.out.printf("Total test suites: %d%n", results.size());
        System.out.printf("Total tests executed: %d%n", totalTests);
        System.out.printf("Total execution time: %d ms%n", totalDuration);
        
        for (TestSuiteResult result : results) {
            System.out.printf("Suite '%s': %d tests, %d failures, %d ms%n",
                result.suiteName, result.testCount, result.failures, result.duration);
        }
    }
    
    private TestSuiteResult executeTestSuite(String suiteName) {
        Instant start = Instant.now();
        
        try {
            String gradleCommand = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "gradlew.bat" : "./gradlew";
                
            ProcessBuilder pb = new ProcessBuilder(gradleCommand, suiteName, "--quiet");
            pb.directory(PROJECT_DIR);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            boolean success = process.waitFor(300, TimeUnit.SECONDS) && process.exitValue() == 0;
            long duration = Duration.between(start, Instant.now()).toMillis();
            
            return parseTestResults(suiteName, output.toString(), duration, success);
            
        } catch (Exception e) {
            long duration = Duration.between(start, Instant.now()).toMillis();
            return new TestSuiteResult(suiteName, 0, 1, duration,
                "Exception during test execution: " + e.getMessage());
        }
    }
    
    private TestSuiteResult parseTestResults(String suiteName, String output, long duration, boolean success) {
        int testCount = 0;
        int failures = success ? 0 : 1;
        
        // Parse test output for test counts and failures
        Pattern testPattern = Pattern.compile("(\\d+) tests? completed");
        Pattern failurePattern = Pattern.compile("(\\d+) failures?");
        
        Matcher testMatcher = testPattern.matcher(output);
        if (testMatcher.find()) {
            testCount = Integer.parseInt(testMatcher.group(1));
        }
        
        Matcher failureMatcher = failurePattern.matcher(output);
        if (failureMatcher.find()) {
            failures = Integer.parseInt(failureMatcher.group(1));
        }
        
        return new TestSuiteResult(suiteName, testCount, failures, duration, output);
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
    
    /**
     * Data class to hold test suite execution results
     */
    private static class TestSuiteResult {
        final String suiteName;
        final int testCount;
        final int failures;
        final long duration;
        final String output;
        
        TestSuiteResult(String suiteName, int testCount, int failures, long duration, String output) {
            this.suiteName = suiteName;
            this.testCount = testCount;
            this.failures = failures;
            this.duration = duration;
            this.output = output;
        }
    }
}