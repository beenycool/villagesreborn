package com.beeny.villagesreborn.core;

import com.beeny.villagesreborn.core.config.ModConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for VillagesRebornCommon module
 * Validates initialization, configuration loading, and core functionality
 */
@ExtendWith(MockitoExtension.class)
class VillagesRebornCommonTest {
    
    @BeforeEach
    void setUp() {
        // Reset the initialization state before each test
        VillagesRebornCommon.resetForTesting();
    }
    
    @Test
    void testCommonModuleInitialization() {
        // Mock the config loading
        try (MockedStatic<ModConfig> mockedConfig = Mockito.mockStatic(ModConfig.class)) {
            ModConfig mockConfig = mock(ModConfig.class);
            mockedConfig.when(ModConfig::load).thenReturn(mockConfig);
            
            // Test initialization
            assertDoesNotThrow(() -> VillagesRebornCommon.initialize());
            assertTrue(VillagesRebornCommon.isInitialized());
        }
    }
    
    @Test
    void testGetConfigBeforeInitialization() {
        // Test that getting config before initialization throws exception
        assertThrows(IllegalStateException.class, () -> VillagesRebornCommon.getConfig());
    }
    
    @Test
    void testDoubleInitialization() {
        // Mock the config loading
        try (MockedStatic<ModConfig> mockedConfig = Mockito.mockStatic(ModConfig.class)) {
            ModConfig mockConfig = mock(ModConfig.class);
            mockedConfig.when(ModConfig::load).thenReturn(mockConfig);
            
            // Initialize once
            VillagesRebornCommon.initialize();
            
            // Second initialization should not throw but should log warning
            assertDoesNotThrow(() -> VillagesRebornCommon.initialize());
        }
    }
    
    @Test
    void testConfigurationAccess() {
        // Mock the config loading
        try (MockedStatic<ModConfig> mockedConfig = Mockito.mockStatic(ModConfig.class)) {
            ModConfig mockConfig = mock(ModConfig.class);
            when(mockConfig.isDevelopmentMode()).thenReturn(false);
            when(mockConfig.getLogLevel()).thenReturn(1);
            mockedConfig.when(ModConfig::load).thenReturn(mockConfig);
            
            // Initialize and test config access
            VillagesRebornCommon.initialize();
            ModConfig config = VillagesRebornCommon.getConfig();
            
            assertNotNull(config);
            assertFalse(config.isDevelopmentMode());
            assertEquals(1, config.getLogLevel());
        }
    }
    
    @Test
    void testInitializationFailure() {
        // Mock config loading to throw exception
        try (MockedStatic<ModConfig> mockedConfig = Mockito.mockStatic(ModConfig.class)) {
            mockedConfig.when(ModConfig::load).thenThrow(new RuntimeException("Config load failed"));
            
            // Test that initialization failure is properly handled
            assertThrows(RuntimeException.class, () -> VillagesRebornCommon.initialize());
            assertFalse(VillagesRebornCommon.isInitialized());
        }
    }
}