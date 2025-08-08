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
        
        // CRITICAL: Actually test the codec by performing encode-decode round-trip
        try {
            // Serialize the data to JSON
            com.mojang.serialization.JsonOps jsonOps = com.mojang.serialization.JsonOps.INSTANCE;
            com.google.gson.JsonElement encoded = VillagerData.CODEC.encodeStart(jsonOps, originalData)
                .getOrThrow(false, msg -> fail("Encoding failed: " + msg));
            
            // Deserialize it back
            VillagerData deserializedData = VillagerData.CODEC.parse(jsonOps, encoded)
                .getOrThrow(false, msg -> fail("Decoding failed: " + msg));
            
            // Assert that all fields are preserved after round-trip serialization
            assertEquals(originalData.getName(), deserializedData.getName());
            assertEquals(originalData.getAge(), deserializedData.getAge());
            assertEquals(originalData.getHappiness(), deserializedData.getHappiness());
            assertEquals(originalData.getTotalTrades(), deserializedData.getTotalTrades());
            assertEquals(originalData.getFavoritePlayerId(), deserializedData.getFavoritePlayerId());
            assertTrue(deserializedData.getProfessionHistory().contains("farmer"));
            assertTrue(deserializedData.getProfessionHistory().contains("librarian"));
            assertTrue(deserializedData.getFamilyMembers().contains("spouse"));
            assertEquals(originalData.getSpouseName(), deserializedData.getSpouseName());
            assertEquals(originalData.getSpouseId(), deserializedData.getSpouseId());
            assertTrue(deserializedData.getChildrenIds().contains("child123"));
            assertTrue(deserializedData.getChildrenNames().contains("TestChild"));
            assertEquals(originalData.getFavoriteFood(), deserializedData.getFavoriteFood());
            assertEquals(originalData.getHobby(), deserializedData.getHobby());
            assertEquals(originalData.getBirthTime(), deserializedData.getBirthTime());
            assertEquals(originalData.getBirthPlace(), deserializedData.getBirthPlace());
            assertEquals(originalData.getNotes(), deserializedData.getNotes());
            assertEquals(originalData.getDeathTime(), deserializedData.getDeathTime());
            assertEquals(originalData.isAlive(), deserializedData.isAlive());
            assertEquals("Kind player", deserializedData.getPlayerMemory("player123"));
            assertEquals(Integer.valueOf(1), deserializedData.getTopicFrequency().get("weather"));
            assertTrue(deserializedData.getRecentEvents().contains("Met player"));
            assertEquals(originalData.getLastConversationTime(), deserializedData.getLastConversationTime());
        } catch (Exception e) {
            fail("Codec round-trip test failed with exception: " + e.getMessage());
        }
    }
}