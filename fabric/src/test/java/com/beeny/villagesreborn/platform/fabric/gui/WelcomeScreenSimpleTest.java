package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.platform.fabric.testing.IntProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified tests for WelcomeScreen without complex mocking
 */
class WelcomeScreenSimpleTest {
    
    @BeforeAll
    static void setUpClass() {
        // Initialize IntProvider to prevent NoClassDefFoundError
        IntProvider.reset();
    }
    
    @Test
    void testWelcomeScreenClassExists() {
        // Verify the class exists and has expected structure
        assertEquals("WelcomeScreen", WelcomeScreen.class.getSimpleName());
        
        // Verify key methods exist
        var methods = WelcomeScreen.class.getDeclaredMethods();
        var methodNames = java.util.Arrays.stream(methods)
            .map(java.lang.reflect.Method::getName)
            .collect(java.util.stream.Collectors.toSet());
            
        assertTrue(methodNames.contains("setSelectedProvider"));
        assertTrue(methodNames.contains("setSelectedModel"));
        assertTrue(methodNames.contains("getSelectedProvider"));
        assertTrue(methodNames.contains("getSelectedModel"));
        assertTrue(methodNames.contains("getValidationErrors"));
    }
    
    @Test
    void testWelcomeScreenConstructor() {
        // Test that the constructor exists and takes the expected parameters
        var constructors = WelcomeScreen.class.getConstructors();
        assertEquals(1, constructors.length);
        
        var constructor = constructors[0];
        assertEquals(3, constructor.getParameterCount());
    }
    
    @Test
    void testWelcomeScreenInheritance() {
        // Verify inheritance from Screen
        assertTrue(net.minecraft.client.gui.screen.Screen.class.isAssignableFrom(WelcomeScreen.class));
    }
}