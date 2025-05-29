package com.beeny.villagesreborn.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Custom Gradle task to count test classes containing @Test annotations
 * and update the README.md file with the accurate count.
 */
public abstract class CountTestClassesTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getCommonTestDir();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getFabricTestDir();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getReadmeFile();

    @OutputFile
    @Optional
    public abstract RegularFileProperty getUpdatedReadmeFile();

    @Input
    public abstract Property<String> getProjectDescription();

    public CountTestClassesTask() {
        setGroup("verification");
        setDescription("Counts test classes annotated with @Test and updates README.md");
        
        // Set default values
        getCommonTestDir().convention(getProject().getLayout().getProjectDirectory().dir("common/src/test/java"));
        getFabricTestDir().convention(getProject().getLayout().getProjectDirectory().dir("fabric/src/test/java"));
        getReadmeFile().convention(getProject().getLayout().getProjectDirectory().file("README.md"));
        getUpdatedReadmeFile().convention(getReadmeFile());
        getProjectDescription().convention("Villages Reborn Test Suite");
    }

    @TaskAction
    public void countTestClasses() {
        List<Directory> testDirs = List.of(
            getCommonTestDir().get(),
            getFabricTestDir().get()
        );
        
        int totalTestClasses = 0;
        
        for (Directory dir : testDirs) {
            File dirFile = dir.getAsFile();
            if (dirFile.exists()) {
                totalTestClasses += countTestClassesRecursively(dirFile);
            }
        }
        
        getLogger().info("Found {} test classes with @Test annotations", totalTestClasses);
        
        updateReadmeFile(totalTestClasses);
    }

    private int countTestClassesRecursively(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return 0;
        }
        
        int count = 0;
        File[] files = directory.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countTestClassesRecursively(file);
                } else if (file.getName().endsWith(".java")) {
                    if (containsTestAnnotation(file)) {
                        count++;
                        getLogger().debug("Found test class: {}", file.getPath());
                    }
                }
            }
        }
        
        return count;
    }

    private boolean containsTestAnnotation(File javaFile) {
        try {
            String content = Files.readString(javaFile.toPath());
            return content.contains("@Test");
        } catch (IOException e) {
            getLogger().warn("Could not read file: {}", javaFile.getPath(), e);
            return false;
        }
    }

    private void updateReadmeFile(int testClassCount) {
        RegularFile readmeFile = getReadmeFile().get();
        File file = readmeFile.getAsFile();
        
        if (!file.exists()) {
            getLogger().warn("README.md file not found at: {}", file.getPath());
            return;
        }
        
        try {
            String content = Files.readString(file.toPath());
            String updatedContent = updateTestClassReferences(content, testClassCount);
            
            Files.writeString(file.toPath(), updatedContent);
            getLogger().info("Updated README.md with {} test classes", testClassCount);
            
        } catch (IOException e) {
            getLogger().error("Failed to update README.md", e);
            throw new TaskExecutionException(this, e);
        }
    }

    String updateTestClassReferences(String content, int testClassCount) {
        // Update "**X Test Classes**:" pattern
        String updated = content.replaceAll(
            "- \\*\\*\\d+ Test Classes\\*\\*:",
            "- **" + testClassCount + " Test Classes**:"
        );
        
        // Update "X test classes" pattern (case insensitive)
        updated = updated.replaceAll(
            "\\d+ test classes",
            testClassCount + " test classes"
        );
        
        return updated;
    }
}