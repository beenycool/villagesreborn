package com.beeny.villagesreborn.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultConfigPathProvider verifying delegation to Fabric provider
 * and fallback behavior.
 */
@ExtendWith(MockitoExtension.class)
class DefaultConfigPathProviderTest {

    @Mock
    private FabricConfigPathProvider mockFabricProvider;

    private DefaultConfigPathProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DefaultConfigPathProvider();
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
        // Create a test provider with mocked Fabric provider
        DefaultConfigPathProvider testProvider = new DefaultConfigPathProvider() {
            @Override
            FabricConfigPathProvider getFabricProvider() {
                return mockFabricProvider;
            }
        };

        Path expectedFabricPath = Paths.get("/fabric/config/villagesreborn_setup.properties");
        when(mockFabricProvider.isAvailable()).thenReturn(true);
        when(mockFabricProvider.getConfigPath()).thenReturn(expectedFabricPath);

        Path result = testProvider.getConfigPath();

        assertEquals(expectedFabricPath, result);
        verify(mockFabricProvider).isAvailable();
        verify(mockFabricProvider).getConfigPath();
    }

    @Test
    void shouldFallbackToUserDirWhenFabricProviderNotAvailable() {
        // Create a test provider with mocked Fabric provider
        DefaultConfigPathProvider testProvider = new DefaultConfigPathProvider() {
            @Override
            FabricConfigPathProvider getFabricProvider() {
                return mockFabricProvider;
            }
        };

        when(mockFabricProvider.isAvailable()).thenReturn(false);

        try (MockedStatic<System> systemMock = mockStatic(System.class)) {
            systemMock.when(() -> System.getProperty("user.dir")).thenReturn("/test/dir");

            Path result = testProvider.getConfigPath();

            assertEquals(Paths.get("/test/dir/villagesreborn_setup.properties"), result);
            verify(mockFabricProvider).isAvailable();
            verify(mockFabricProvider, never()).getConfigPath();
        }
    }

    @Test
    void shouldFallbackWhenFabricProviderThrowsUnsupportedOperation() {
        // Create a test provider with mocked Fabric provider
        DefaultConfigPathProvider testProvider = new DefaultConfigPathProvider() {
            @Override
            FabricConfigPathProvider getFabricProvider() {
                return mockFabricProvider;
            }
        };

        when(mockFabricProvider.isAvailable()).thenReturn(true);
        when(mockFabricProvider.getConfigPath()).thenThrow(new UnsupportedOperationException("Fabric not available"));

        try (MockedStatic<System> systemMock = mockStatic(System.class)) {
            systemMock.when(() -> System.getProperty("user.dir")).thenReturn("/fallback/dir");

            Path result = testProvider.getConfigPath();

            assertEquals(Paths.get("/fallback/dir/villagesreborn_setup.properties"), result);
            verify(mockFabricProvider).isAvailable();
            verify(mockFabricProvider).getConfigPath();
        }
    }

    @Test
    void shouldFallbackWhenFabricProviderThrowsUnexpectedException() {
        // Create a test provider with mocked Fabric provider
        DefaultConfigPathProvider testProvider = new DefaultConfigPathProvider() {
            @Override
            FabricConfigPathProvider getFabricProvider() {
                return mockFabricProvider;
            }
        };

        when(mockFabricProvider.isAvailable()).thenReturn(true);
        when(mockFabricProvider.getConfigPath()).thenThrow(new RuntimeException("Unexpected error"));

        try (MockedStatic<System> systemMock = mockStatic(System.class)) {
            systemMock.when(() -> System.getProperty("user.dir")).thenReturn("/error/fallback");

            Path result = testProvider.getConfigPath();

            assertEquals(Paths.get("/error/fallback/villagesreborn_setup.properties"), result);
            verify(mockFabricProvider).isAvailable();
            verify(mockFabricProvider).getConfigPath();
        }
    }

    @Test
    void shouldFallbackWhenFabricProviderReturnsNull() {
        // Create a test provider with mocked Fabric provider
        DefaultConfigPathProvider testProvider = new DefaultConfigPathProvider() {
            @Override
            FabricConfigPathProvider getFabricProvider() {
                return mockFabricProvider;
            }
        };

        when(mockFabricProvider.isAvailable()).thenReturn(true);
        when(mockFabricProvider.getConfigPath()).thenReturn(null);

        try (MockedStatic<System> systemMock = mockStatic(System.class)) {
            systemMock.when(() -> System.getProperty("user.dir")).thenReturn("/null/fallback");

            Path result = testProvider.getConfigPath();

            assertEquals(Paths.get("/null/fallback/villagesreborn_setup.properties"), result);
            verify(mockFabricProvider).isAvailable();
            verify(mockFabricProvider).getConfigPath();
        }
    }

    @Test
    void shouldUseCorrectConfigFileName() {
        try (MockedStatic<System> systemMock = mockStatic(System.class)) {
            systemMock.when(() -> System.getProperty("user.dir")).thenReturn("/test");

            // Test with real provider when Fabric is not available
            DefaultConfigPathProvider realProvider = new DefaultConfigPathProvider();
            
            // This will use real FabricConfigPathProvider which should not be available in test
            Path result = realProvider.getConfigPath();
            
            assertTrue(result.toString().endsWith("villagesreborn_setup.properties"));
        }
    }

    @Test
    void shouldHandleEmptyUserDirProperty() {
        // Create a test provider with mocked Fabric provider
        DefaultConfigPathProvider testProvider = new DefaultConfigPathProvider() {
            @Override
            FabricConfigPathProvider getFabricProvider() {
                return mockFabricProvider;
            }
        };

        when(mockFabricProvider.isAvailable()).thenReturn(false);

        try (MockedStatic<System> systemMock = mockStatic(System.class)) {
            systemMock.when(() -> System.getProperty("user.dir")).thenReturn("");

            Path result = testProvider.getConfigPath();

            assertEquals(Paths.get("villagesreborn_setup.properties"), result);
        }
    }

    @Test
    void shouldCreateFabricProviderInConstructor() {
        DefaultConfigPathProvider newProvider = new DefaultConfigPathProvider();
        assertNotNull(newProvider.getFabricProvider());
        assertInstanceOf(FabricConfigPathProvider.class, newProvider.getFabricProvider());
    }
}