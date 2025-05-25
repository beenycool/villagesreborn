package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.core.api.Platform;
import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for FabricPlatform implementation
 * Validates platform-specific functionality and Fabric integration
 */
@ExtendWith(MockitoExtension.class)
class FabricPlatformTest {
    
    @Mock
    private FabricLoader mockFabricLoader;
    
    private FabricPlatform platform;
    
    @BeforeEach
    void setUp() {
        // Create platform instance with mocked Fabric loader
        platform = new FabricPlatform();
    }
    
    @Test
    void testPlatformName() {
        assertEquals("Fabric", platform.getPlatformName());
    }
    
    @Test
    void testFeatureSupport() {
        // Test supported features
        assertTrue(platform.supportsFeature("mixins"));
        assertTrue(platform.supportsFeature("fabric-api"));
        assertTrue(platform.supportsFeature("hot-reload"));
        assertTrue(platform.supportsFeature("development-tools"));
        
        // Test unsupported features
        assertFalse(platform.supportsFeature("forge-compatibility"));
        
        // Test unknown features
        assertFalse(platform.supportsFeature("unknown-feature"));
    }
    
    @Test
    void testHookRegistrationAndExecution() {
        // Test hook registration
        boolean[] hookExecuted = {false};
        Runnable testHook = () -> hookExecuted[0] = true;
        
        platform.registerHook("test-hook", testHook);
        
        // Test hook execution
        platform.executeHook("test-hook");
        assertTrue(hookExecuted[0]);
    }
    
    @Test
    void testHookExecutionWithException() {
        // Register a hook that throws an exception
        Runnable failingHook = () -> {
            throw new RuntimeException("Test exception");
        };
        
        platform.registerHook("failing-hook", failingHook);
        
        // Should not throw exception, but log error
        assertDoesNotThrow(() -> platform.executeHook("failing-hook"));
    }
    
    @Test
    void testExecuteNonExistentHook() {
        // Should not throw exception for non-existent hook
        assertDoesNotThrow(() -> platform.executeHook("non-existent-hook"));
    }
    
    @Test
    void testHookRegistrationValidation() {
        // Test null hook name
        assertThrows(IllegalArgumentException.class, 
            () -> platform.registerHook(null, () -> {}));
        
        // Test null hook implementation
        assertThrows(IllegalArgumentException.class, 
            () -> platform.registerHook("test", null));
    }
    
    @Test
    void testPlatformIntegration() {
        try (MockedStatic<FabricLoader> mockedLoader = Mockito.mockStatic(FabricLoader.class)) {
            when(mockFabricLoader.isDevelopmentEnvironment()).thenReturn(true);
            mockedLoader.when(FabricLoader::getInstance).thenReturn(mockFabricLoader);
            
            FabricPlatform testPlatform = new FabricPlatform();
            assertTrue(testPlatform.isDevelopmentEnvironment());
        }
    }
    
    @Test
    void testModLoadedCheck() {
        try (MockedStatic<FabricLoader> mockedLoader = Mockito.mockStatic(FabricLoader.class)) {
            when(mockFabricLoader.isModLoaded("fabric-api")).thenReturn(true);
            when(mockFabricLoader.isModLoaded("unknown-mod")).thenReturn(false);
            mockedLoader.when(FabricLoader::getInstance).thenReturn(mockFabricLoader);
            
            FabricPlatform testPlatform = new FabricPlatform();
            assertTrue(testPlatform.isModLoaded("fabric-api"));
            assertFalse(testPlatform.isModLoaded("unknown-mod"));
        }
    }
    
    @Test
    void testPlatformAsInterface() {
        // Test that FabricPlatform correctly implements Platform interface
        Platform platformInterface = platform;
        
        assertNotNull(platformInterface.getPlatformName());
        assertTrue(platformInterface.supportsFeature("mixins"));
        
        // Test hook functionality through interface
        boolean[] executed = {false};
        platformInterface.registerHook("interface-test", () -> executed[0] = true);
        platformInterface.executeHook("interface-test");
        assertTrue(executed[0]);
    }
}