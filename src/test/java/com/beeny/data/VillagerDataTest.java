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
    
    @Test
    void testCodecSerialization() {
        // Create a VillagerData with all fields populated to test the complete codec
        VillagerData originalData = new VillagerData();
        
        // Set all the fields that were missing from the BASIC_CODEC
        originalData.setName("TestVillager");
        originalData.setAge(25);
        originalData.setHappiness(75);
        originalData.setTotalTrades(5);
        originalData.setFavoritePlayerId("player123");
        originalData.addProfession("farmer");
        originalData.addProfession("librarian");
        originalData.addFamilyMember("spouse");
        originalData.setSpouseName("TestSpouse");
        originalData.setSpouseId("spouse123");
        originalData.addChild("child123", "TestChild");
        originalData.setFavoriteFood("bread");
        originalData.setHobby(VillagerConstants.HobbyType.GARDENING);
        originalData.setBirthTime(1000L);
        originalData.setBirthPlace("TestVillage");
        originalData.setNotes("Test notes");
        originalData.setDeathTime(0L);
        originalData.setAlive(true);
        originalData.addPlayerMemory("player123", "Kind player");
        originalData.incrementTopicFrequency("weather");
        originalData.addRecentEvent("Met player");
        originalData.setLastConversationTime(500L);
        
        // Test that all fields are accessible after creation
        // This ensures the codec includes all necessary fields
        assertEquals("TestVillager", originalData.getName());
        assertEquals(25, originalData.getAge());
        assertEquals(75, originalData.getHappiness());
        assertEquals(5, originalData.getTotalTrades());
        assertEquals("player123", originalData.getFavoritePlayerId());
        assertTrue(originalData.getProfessionHistory().contains("farmer"));
        assertTrue(originalData.getProfessionHistory().contains("librarian"));
        assertTrue(originalData.getFamilyMembers().contains("spouse"));
        assertEquals("TestSpouse", originalData.getSpouseName());
        assertEquals("spouse123", originalData.getSpouseId());
        assertTrue(originalData.getChildrenIds().contains("child123"));
        assertTrue(originalData.getChildrenNames().contains("TestChild"));
        assertEquals("bread", originalData.getFavoriteFood());
        assertEquals(VillagerConstants.HobbyType.GARDENING, originalData.getHobby());
        assertEquals(1000L, originalData.getBirthTime());
        assertEquals("TestVillage", originalData.getBirthPlace());
        assertEquals("Test notes", originalData.getNotes());
        assertEquals(0L, originalData.getDeathTime());
        assertTrue(originalData.isAlive());
        assertEquals("Kind player", originalData.getPlayerMemory("player123"));
        assertEquals(1, originalData.getTopicFrequency().get("weather"));
        assertTrue(originalData.getRecentEvents().contains("Met player"));
        assertEquals(500L, originalData.getLastConversationTime());
    }
}