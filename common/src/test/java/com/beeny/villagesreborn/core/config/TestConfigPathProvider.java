package com.beeny.villagesreborn.core.config;

import java.nio.file.Path;

/**
 * Test-specific config path provider that uses a configurable path
 */
public class TestConfigPathProvider implements ConfigPathStrategy {
    private final Path testPath;
    
    public TestConfigPathProvider(Path testPath) {
        this.testPath = testPath;
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public Path getConfigPath() {
        // If testPath already ends with the config file name, return it directly
        // Otherwise, resolve the config file name within the testPath directory
        if (testPath.getFileName().toString().endsWith(".properties")) {
            return testPath;
        }
        return testPath.resolve("villagesreborn_setup.properties");
    }
    
    @Override
    public int getPriority() {
        return 1000; // Highest priority for tests
    }
}