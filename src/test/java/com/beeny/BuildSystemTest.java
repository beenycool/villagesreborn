package com.beeny;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify that the build system can compile and run tests successfully.
 * This test validates that all dependencies are correctly configured and that
 * the basic project structure is sound.
 */
public class BuildSystemTest {
    
    @Test
    @DisplayName("Build system can compile and run basic tests")
    void testBasicFunctionality() {
        // Test basic Java functionality
        String testString = "VillagersReborn";
        assertNotNull(testString);
        assertEquals("VillagersReborn", testString);
        assertTrue(testString.length() > 0);
    }
    
    @Test
    @DisplayName("Main mod constants are accessible")
    void testModConstants() {
        assertNotNull(Villagersreborn.MOD_ID);
        assertNotNull(Villagersreborn.LOGGER);
        assertNotNull(Villagersreborn.VILLAGER_DATA);
        assertNotNull(Villagersreborn.VILLAGER_NAME);
        
        assertEquals("villagersreborn", Villagersreborn.MOD_ID);
    }
    
    @Test
    @DisplayName("VillagerData can be instantiated")
    void testVillagerDataInstantiation() {
        com.beeny.data.VillagerData data = new com.beeny.data.VillagerData();
        assertNotNull(data);
        
        // Test some basic data operations
        assertTrue(data.getName().isEmpty());
        assertEquals(50, data.getHappiness()); // Default happiness
        assertNotNull(data.getPersonality());
    }
    
    @Test
    @DisplayName("AI core classes can be instantiated")
    void testAICoreInstantiation() {
        // Test that core AI classes can be created without errors
        assertDoesNotThrow(() -> {
            new com.beeny.system.VillagerHobbySystem();
            new com.beeny.system.VillagerScheduleManager();
            new com.beeny.system.VillagerPersonalityBehavior();
        });
    }
    
    @Test
    @DisplayName("Configuration classes work correctly")
    void testConfigurationClasses() {
        assertDoesNotThrow(() -> {
            com.beeny.config.VillagersRebornConfig.getBoundingBoxSize();
            com.beeny.config.VillagersRebornConfig.getEnableAISystem();
        });
    }
}