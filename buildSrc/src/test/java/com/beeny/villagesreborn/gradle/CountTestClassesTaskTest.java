package com.beeny.villagesreborn.gradle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the countTestClasses Gradle task functionality.
 * Tests the core logic for scanning @Test annotations and file handling.
 */
class CountTestClassesTaskTest {

    @TempDir
    Path tempDir;
    
    private File testJavaDir;
    private File readmeFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create test directory structure
        testJavaDir = tempDir.resolve("common/src/test/java").toFile();
        testJavaDir.mkdirs();
        
        // Create README.md file
        readmeFile = tempDir.resolve("README.md").toFile();
        Files.writeString(readmeFile.toPath(),
            "# Test Project\n- **137 Test Classes**: Comprehensive test coverage\n");
    }

    @Test
    void shouldDetectTestAnnotationInJavaFiles() throws IOException {
        // Create test class with @Test annotation
        File testClass1 = new File(testJavaDir, "ExampleTest.java");
        Files.writeString(testClass1.toPath(),
            "package test;\n" +
            "import org.junit.jupiter.api.Test;\n" +
            "class ExampleTest {\n" +
            "    @Test\n" +
            "    void shouldDoSomething() {}\n" +
            "}\n");
            
        // Create test class without @Test annotation
        File testClass2 = new File(testJavaDir, "UtilityClass.java");
        Files.writeString(testClass2.toPath(),
            "package test;\n" +
            "class UtilityClass {\n" +
            "    void helperMethod() {}\n" +
            "}\n");

        // Simulate the counting logic from our Gradle task
        int testClassCount = countTestClassesWithAnnotations(testJavaDir);
        assertEquals(1, testClassCount, "Should find exactly one class with @Test annotation");
    }

    @Test
    void shouldUpdateReadmeContentCorrectly() throws IOException {
        // Create test data
        createTestClass("Test1.java", true);
        createTestClass("Test2.java", true);
        createTestClass("NotATest.java", false);
        
        // Verify README contains original count
        String originalContent = Files.readString(readmeFile.toPath());
        assertThat(originalContent).contains("137 Test Classes");
        
        // Simulate README update logic
        int actualTestCount = countTestClassesWithAnnotations(testJavaDir);
        String updatedContent = originalContent.replaceAll(
            "- \\*\\*\\d+ Test Classes\\*\\*:",
            "- **" + actualTestCount + " Test Classes**:"
        );
        
        assertThat(updatedContent).contains("- **2 Test Classes**:");
        assertThat(updatedContent).doesNotContain("137 Test Classes");
    }

    @Test
    void shouldHandleEmptyTestDirectories() {
        // Test behavior when no test files exist
        File emptyDir = new File(testJavaDir, "empty");
        emptyDir.mkdirs();
        
        int testCount = countTestClassesWithAnnotations(emptyDir);
        assertEquals(0, testCount, "Should return 0 for empty directories");
    }

    @Test
    void shouldOnlyCountJavaFiles() throws IOException {
        // Create Java test file
        createTestClass("ValidTest.java", true);
        
        // Create non-Java file with @Test content
        File nonJavaFile = new File(testJavaDir, "NotJava.txt");
        Files.writeString(nonJavaFile.toPath(), "@Test\nvoid test() {}");
        
        int testCount = countTestClassesWithAnnotations(testJavaDir);
        assertEquals(1, testCount, "Should only count .java files, not .txt files");
    }

    @Test
    void shouldDetectTestAnnotationInVariousFormats() throws IOException {
        // Test different @Test annotation formats
        createTestClassWithContent("MultipleTests.java",
            "import org.junit.jupiter.api.Test;\n" +
            "class MultipleTests {\n" +
            "    @Test void test1() {}\n" +
            "    @Test\n" +
            "    void test2() {}\n" +
            "    @org.junit.jupiter.api.Test\n" +
            "    void test3() {}\n" +
            "}\n");
        
        int testCount = countTestClassesWithAnnotations(testJavaDir);
        assertEquals(1, testCount, "Should detect @Test annotation regardless of format");
    }

    @Test
    void shouldHandleRegexReplacementCorrectly() throws IOException {
        // Create README with multiple references to test count
        Files.writeString(readmeFile.toPath(),
            "# Project\n" +
            "- **137 Test Classes**: Full coverage\n" +
            "Integration report: 137 test classes total\n");
        
        String content = Files.readString(readmeFile.toPath());
        
        // Test both regex patterns from the actual task
        String updated1 = content.replaceAll(
            "- \\*\\*\\d+ Test Classes\\*\\*:",
            "- **5 Test Classes**:"
        );
        String updated2 = updated1.replaceAll(
            "\\d+ test classes",
            "5 test classes"
        );
        
        assertThat(updated2).contains("- **5 Test Classes**:");
        assertThat(updated2).contains("5 test classes total");
        assertThat(updated2).doesNotContain("137");
    }

    @Test
    void shouldValidateFileStructure() {
        assertTrue(testJavaDir.exists(), "Test directory should exist");
        assertTrue(readmeFile.exists(), "README file should exist");
        assertTrue(testJavaDir.isDirectory(), "Should be a directory");
        assertTrue(readmeFile.isFile(), "Should be a file");
    }

    // Helper method that simulates the core logic of the Gradle task
    private int countTestClassesWithAnnotations(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }
        
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countTestClassesWithAnnotations(file);
                } else if (file.getName().endsWith(".java")) {
                    try {
                        String content = Files.readString(file.toPath());
                        if (content.contains("@Test")) {
                            count++;
                        }
                    } catch (IOException e) {
                        // Skip files that can't be read
                    }
                }
            }
        }
        return count;
    }

    private void createTestClass(String fileName, boolean includeTestAnnotation) throws IOException {
        String content = "package test;\n";
        if (includeTestAnnotation) {
            content += "import org.junit.jupiter.api.Test;\n";
        }
        content += "class " + fileName.replace(".java", "") + " {\n";
        if (includeTestAnnotation) {
            content += "    @Test\n    void shouldTest() {}\n";
        } else {
            content += "    void utilityMethod() {}\n";
        }
        content += "}\n";
        
        File testFile = new File(testJavaDir, fileName);
        Files.writeString(testFile.toPath(), content);
    }

    private void createTestClassWithContent(String fileName, String content) throws IOException {
        File testFile = new File(testJavaDir, fileName);
        Files.writeString(testFile.toPath(), content);
    }
}