package com.beeny.data;

import com.beeny.constants.VillagerConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VillagerData core functionality
 */
public class VillagerDataTest {
    
    private VillagerData villagerData;
    
    @BeforeEach
    void setUp() {
        villagerData = new VillagerData();
    }
    
    @Test
    void testDefaultValues() {
        assertEquals(VillagerConstants.Defaults.NAME, villagerData.getName());
        assertEquals(VillagerConstants.Defaults.AGE, villagerData.getAge());
        assertEquals(VillagerConstants.Defaults.GENDER, villagerData.getGender());
        assertEquals(VillagerConstants.PersonalityType.FRIENDLY, villagerData.getPersonality());
        assertNotNull(villagerData.getEmotionalState());
        assertNotNull(villagerData.getAiState());
        assertNotNull(villagerData.getLearningProfile());
    }
    
    @Test
    void testHappinessAdjustment() {
        int initialHappiness = villagerData.getHappiness();
        
        villagerData.adjustHappiness(10);
        assertEquals(initialHappiness + 10, villagerData.getHappiness());
        
        villagerData.adjustHappiness(-5);
        assertEquals(initialHappiness + 5, villagerData.getHappiness());
    }
    
    @Test
    void testHappinessBounds() {
        // Test upper bound
        villagerData.adjustHappiness(1000);
        assertTrue(villagerData.getHappiness() <= 100);
        
        // Test lower bound  
        villagerData.adjustHappiness(-2000);
        assertTrue(villagerData.getHappiness() >= 0);
    }
    
    @Test
    void testNameSetting() {
        String testName = "TestVillager";
        villagerData.setName(testName);
        assertEquals(testName, villagerData.getName());
    }
    
    @Test
    void testAgeSetting() {
        int testAge = 25;
        villagerData.setAge(testAge);
        assertEquals(testAge, villagerData.getAge());
    }
    
    @Test
    void testPersonalitySetting() {
        VillagerConstants.PersonalityType personality = VillagerConstants.PersonalityType.SHY;
        villagerData.setPersonality(personality);
        assertEquals(personality, villagerData.getPersonality());
    }
    
    @Test
    void testRecentEventsManagement() {
        assertTrue(villagerData.getRecentEvents().isEmpty());
        
        villagerData.addRecentEvent("Test event");
        assertEquals(1, villagerData.getRecentEvents().size());
        assertTrue(villagerData.getRecentEvents().contains("Test event"));
    }
    
    @Test
    void testEmotionalStateIntegration() {
        com.beeny.ai.core.VillagerEmotionSystem.EmotionalState emotional = villagerData.getEmotionalState();
        assertNotNull(emotional);
        
        // Test that emotional state changes are preserved
        emotional.adjustEmotion(com.beeny.ai.core.VillagerEmotionSystem.EmotionType.HAPPINESS, 0.5f);
        assertEquals(emotional, villagerData.getEmotionalState());
    }
}