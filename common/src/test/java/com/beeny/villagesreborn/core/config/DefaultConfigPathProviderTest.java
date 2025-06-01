package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultConfigPathProvider verifying delegation to Fabric provider
 * and fallback behavior.
 */
class DefaultConfigPathProviderTest {

    private TestableConfigPathProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TestableConfigPathProvider();
    }

    // Testable subclass that allows controlling all dependencies
    private static class TestableConfigPathProvider extends DefaultConfigPathProvider {
        private boolean fabricAvailable = false;
        private Path fabricPath = null;
        private RuntimeException fabricException = null;
        private String userDir = "/default/dir";

        @Override
        FabricConfigPathProvider getFabricProvider() {
            return new FabricConfigPathProvider() {
                @Override
                public boolean isAvailable() {
                    return fabricAvailable;
                }

                @Override
                public Path getConfigPath() {
                    if (fabricException != null) {
                        throw fabricException;
                    }
                    return fabricPath;
                }
            };
        }

        @Override
        protected String getUserDir() {
            return userDir;
        }

        void setUserDir(String userDir) {
            this.userDir = userDir;
        }
    }

    @Test
    void shouldAlwaysBeAvailable() {
        assertTrue(provider.isAvailable());
    }

    @Test
    void shouldHaveLowerPriorityThanFabricProvider() {
        assertEquals(1, provider.getPriority());
        assertTrue(provider.getPriority() < 100); // Fabric provider has priority 100
    }

    @Test
    void shouldDelegateToFabricProviderWhenAvailable() {
        Path expectedFabricPath = Paths.get("/fabric/config/villagesreborn_setup.properties");
        provider.fabricAvailable = true;
        provider.fabricPath = expectedFabricPath;

        Path result = provider.getConfigPath();

        assertEquals(expectedFabricPath, result);
    }

    @Test
    void shouldFallbackToUserDirWhenFabricProviderNotAvailable() {
        provider.fabricAvailable = false;
        provider.setUserDir("/test/dir");

        Path result = provider.getConfigPath();

        assertEquals(Paths.get("/test/dir/villagesreborn_setup.properties"), result);
    }

    @Test
    void shouldFallbackWhenFabricProviderThrowsUnsupportedOperation() {
        provider.fabricAvailable = true;
        provider.fabricException = new UnsupportedOperationException("Fabric not available");
        provider.setUserDir("/fallback/dir");

        Path result = provider.getConfigPath();

        assertEquals(Paths.get("/fallback/dir/villagesreborn_setup.properties"), result);
    }

    @Test
    void shouldFallbackWhenFabricProviderThrowsUnexpectedException() {
        provider.fabricAvailable = true;
        provider.fabricException = new RuntimeException("Unexpected error");
        provider.setUserDir("/error/fallback");

        Path result = provider.getConfigPath();

        assertEquals(Paths.get("/error/fallback/villagesreborn_setup.properties"), result);
    }

    @Test
    void shouldFallbackWhenFabricProviderReturnsNull() {
        provider.fabricAvailable = true;
        provider.fabricPath = null;
        provider.setUserDir("/null/fallback");

        Path result = provider.getConfigPath();

        assertEquals(Paths.get("/null/fallback/villagesreborn_setup.properties"), result);
    }

    @Test
    void shouldUseCorrectConfigFileName() {
        provider.fabricAvailable = false;
        provider.setUserDir("/test");
        
        Path result = provider.getConfigPath();
        
        assertTrue(result.toString().endsWith("villagesreborn_setup.properties"));
    }

    @Test
    void shouldHandleEmptyUserDirProperty() {
        provider.fabricAvailable = false;
        provider.setUserDir("");
        
        Path result = provider.getConfigPath();
        
        assertEquals(Paths.get("villagesreborn_setup.properties"), result);
    }

    @Test
    void shouldCreateFabricProviderInConstructor() {
        DefaultConfigPathProvider newProvider = new DefaultConfigPathProvider();
        assertNotNull(newProvider.getFabricProvider());
        assertInstanceOf(FabricConfigPathProvider.class, newProvider.getFabricProvider());
    }
}