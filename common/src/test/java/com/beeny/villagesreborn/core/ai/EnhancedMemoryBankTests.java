package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the enhanced MemoryBank functionality
 */
public class EnhancedMemoryBankTests {
    
    private MemoryBank memoryBank;
    private UUID playerUUID;
    private UUID villagerUUID;
    
    @BeforeEach
    void setUp() {
        memoryBank = new MemoryBank();
        playerUUID = UUID.randomUUID();
        villagerUUID = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should record and retrieve village events")
    void testVillageEventRecording() {
        // Given
        String eventType = "festival";
        String description = "Annual harvest festival brought joy to the village";
        List<UUID> participants = Arrays.asList(playerUUID, villagerUUID);
        EventImpact impact = EventImpact.VERY_POSITIVE;
        
        // When
        memoryBank.recordVillageEvent(eventType, description, participants, impact);
        
        // Then
        List<VillageEvent> events = memoryBank.getVillageEvents();
        assertEquals(1, events.size());
        
        VillageEvent event = events.get(0);
        assertEquals(eventType, event.getEventType());
        assertEquals(description, event.getDescription());
        assertEquals(participants.size(), event.getParticipantsInvolved().size());
        assertEquals(impact, event.getImpact());
        assertTrue(event.getParticipantsInvolved().containsAll(participants));
    }
    
    @Test
    @DisplayName("Should record detailed player interactions")
    void testPlayerInteractionRecording() {
        // Given
        String playerName = "TestPlayer";
        String interactionType = "trade";
        String context = "Bought emeralds for wheat";
        String response = "Thank you for the trade!";
        float sentimentChange = 0.3f;
        
        // When
        memoryBank.recordPlayerInteraction(playerUUID, playerName, interactionType, 
                                         context, response, sentimentChange);
        
        // Then
        Map<UUID, List<DetailedInteraction>> interactions = memoryBank.getPlayerInteractions();
        assertTrue(interactions.containsKey(playerUUID));
        
        List<DetailedInteraction> playerInteractions = interactions.get(playerUUID);
        assertEquals(1, playerInteractions.size());
        
        DetailedInteraction interaction = playerInteractions.get(0);
        assertEquals(playerUUID, interaction.getPlayerUUID());
        assertEquals(playerName, interaction.getPlayerName());
        assertEquals(interactionType, interaction.getInteractionType());
        assertEquals(context, interaction.getContext());
        assertEquals(response, interaction.getVillagerResponse());
        assertEquals(sentimentChange, interaction.getSentimentChange(), 0.01f);
    }
    
    @Test
    @DisplayName("Should manage villager relationships")
    void testVillagerRelationshipManagement() {
        // Given
        String villagerName = "Bob";
        RelationshipType relationshipType = RelationshipType.FRIEND;
        String lastInteraction = "Shared stories by the fire";
        float relationshipStrength = 0.7f;
        
        // When
        memoryBank.recordVillagerRelationship(villagerUUID, villagerName, 
                                            relationshipType, lastInteraction, relationshipStrength);
        
        // Then
        Map<UUID, VillagerRelationshipMemory> relationships = memoryBank.getVillagerRelationships();
        assertTrue(relationships.containsKey(villagerUUID));
        
        VillagerRelationshipMemory relationship = relationships.get(villagerUUID);
        assertEquals(villagerUUID, relationship.getVillagerUUID());
        assertEquals(villagerName, relationship.getVillagerName());
        assertEquals(relationshipType, relationship.getRelationshipType());
        assertEquals(lastInteraction, relationship.getLastInteraction());
        assertEquals(relationshipStrength, relationship.getRelationshipStrength(), 0.01f);
    }
    
    @Test
    @DisplayName("Should store and categorize personal stories")
    void testPersonalStoryManagement() {
        // Given
        String title = "The Great Adventure";
        String narrative = "I once traveled to the neighboring village and discovered a hidden cave";
        List<String> keyCharacters = Arrays.asList("Old Merchant", "Cave Spider");
        EmotionalTone tone = EmotionalTone.EXCITED;
        boolean isOngoing = false;
        
        // When
        memoryBank.addPersonalStory(title, narrative, keyCharacters, tone, isOngoing);
        
        // Then
        List<PersonalStory> stories = memoryBank.getPersonalStories();
        assertEquals(1, stories.size());
        
        PersonalStory story = stories.get(0);
        assertEquals(title, story.getTitle());
        assertEquals(narrative, story.getNarrative());
        assertEquals(keyCharacters.size(), story.getKeyCharacters().size());
        assertEquals(tone, story.getTone());
        assertEquals(isOngoing, story.isOngoing());
        assertTrue(story.getKeyCharacters().containsAll(keyCharacters));
    }
    
    @Test
    @DisplayName("Should record and track traumatic events")
    void testTraumaticEventRecording() {
        // Given
        String description = "Zombie attack on the village";
        String trigger = "nightfall";
        float severity = 0.8f;
        List<UUID> involvedEntities = Arrays.asList(villagerUUID);
        
        // When
        memoryBank.recordTrauma(description, trigger, severity, involvedEntities);
        
        // Then
        List<TraumaticEvent> traumas = memoryBank.getTraumaticEvents();
        assertEquals(1, traumas.size());
        
        TraumaticEvent trauma = traumas.get(0);
        assertEquals(description, trauma.getDescription());
        assertEquals(trigger, trauma.getTrigger());
        assertEquals(severity, trauma.getSeverity(), 0.01f);
        assertEquals(involvedEntities.size(), trauma.getInvolvedEntities().size());
        assertTrue(trauma.getInvolvedEntities().containsAll(involvedEntities));
    }
    
    @Test
    @DisplayName("Should record joyful memories")
    void testJoyfulMemoryRecording() {
        // Given
        String description = "Wedding celebration in the village square";
        List<UUID> sharedWith = Arrays.asList(playerUUID, villagerUUID);
        float happinessLevel = 0.9f;
        
        // When
        memoryBank.recordJoyfulMemory(description, sharedWith, happinessLevel);
        
        // Then
        List<JoyfulMemory> joyfulMemories = memoryBank.getJoyfulMemories();
        assertEquals(1, joyfulMemories.size());
        
        JoyfulMemory memory = joyfulMemories.get(0);
        assertEquals(description, memory.getDescription());
        assertEquals(happinessLevel, memory.getHappinessLevel(), 0.01f);
        assertEquals(sharedWith.size(), memory.getSharedWith().size());
        assertTrue(memory.getSharedWith().containsAll(sharedWith));
    }
    
    @Test
    @DisplayName("Should retrieve memories by category")
    void testMemoryCategorization() {
        // Given - add memories of different types
        memoryBank.recordVillageEvent("festival", "Village festival", Arrays.asList(playerUUID), EventImpact.POSITIVE);
        memoryBank.recordPlayerInteraction(playerUUID, "Player", "greeting", "Hello", "Hi there!", 0.1f);
        memoryBank.addPersonalStory("My Story", "A tale", Arrays.asList("Character"), EmotionalTone.HAPPY, false);
        
        // When
        List<Memory> villageEventMemories = memoryBank.getMemoriesByCategory(MemoryCategory.VILLAGE_EVENTS);
        List<Memory> playerInteractionMemories = memoryBank.getMemoriesByCategory(MemoryCategory.PLAYER_INTERACTIONS);
        List<Memory> personalStoryMemories = memoryBank.getMemoriesByCategory(MemoryCategory.PERSONAL_STORIES);
        
        // Then
        assertEquals(1, villageEventMemories.size());
        assertEquals(1, playerInteractionMemories.size());
        assertEquals(1, personalStoryMemories.size());
        
        assertEquals("festival", villageEventMemories.get(0).getType());
        assertEquals("greeting", playerInteractionMemories.get(0).getType());
        assertEquals("personal_story", personalStoryMemories.get(0).getType());
    }
    
    @Test
    @DisplayName("Should retrieve memories from specific time periods")
    void testTimeBasedMemoryRetrieval() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime tomorrow = now.plusDays(1);
        
        // Add a memory
        memoryBank.recordPlayerInteraction(playerUUID, "Player", "greeting", "Hello", "Hi!", 0.1f);
        
        // When
        List<Memory> memoriesFromYesterday = memoryBank.getMemoriesFromPeriod(yesterday, tomorrow);
        List<Memory> memoriesFromFuture = memoryBank.getMemoriesFromPeriod(tomorrow, tomorrow.plusDays(1));
        
        // Then
        assertEquals(1, memoriesFromYesterday.size());
        assertEquals(0, memoriesFromFuture.size());
    }
    
    @Test
    @DisplayName("Should identify grudges correctly")
    void testGrudgeDetection() {
        // Given - record negative interaction
        memoryBank.recordPlayerInteraction(playerUUID, "BadPlayer", "insult", "You're stupid", "That hurt", -0.7f);
        
        // When
        boolean holdsGrudge = memoryBank.holdsGrudge(playerUUID);
        
        // Then
        assertTrue(holdsGrudge);
    }
    
    @Test
    @DisplayName("Should not identify grudge for positive interactions")
    void testNoGrudgeForPositiveInteractions() {
        // Given - record positive interaction
        memoryBank.recordPlayerInteraction(playerUUID, "GoodPlayer", "compliment", "You're helpful", "Thank you!", 0.5f);
        
        // When
        boolean holdsGrudge = memoryBank.holdsGrudge(playerUUID);
        
        // Then
        assertFalse(holdsGrudge);
    }
    
    @Test
    @DisplayName("Should get relationship strength")
    void testRelationshipStrengthRetrieval() {
        // Given
        float expectedStrength = 0.6f;
        memoryBank.recordVillagerRelationship(villagerUUID, "Friend", RelationshipType.FRIEND, 
                                            "Had coffee together", expectedStrength);
        
        // When
        float actualStrength = memoryBank.getRelationshipStrength(villagerUUID);
        
        // Then
        assertEquals(expectedStrength, actualStrength, 0.01f);
    }
    
    @Test
    @DisplayName("Should return zero for unknown relationships")
    void testUnknownRelationshipStrength() {
        // Given - no relationship recorded
        UUID unknownVillager = UUID.randomUUID();
        
        // When
        float strength = memoryBank.getRelationshipStrength(unknownVillager);
        
        // Then
        assertEquals(0.0f, strength, 0.01f);
    }
    
    @Test
    @DisplayName("Should generate life story summary")
    void testLifeStorySummaryGeneration() {
        // Given - add various types of memories
        memoryBank.addPersonalStory("Childhood", "I grew up in this village", Arrays.asList("Mother", "Father"), EmotionalTone.NOSTALGIC, false);
        memoryBank.recordVillageEvent("festival", "Great harvest festival", Arrays.asList(playerUUID), EventImpact.POSITIVE);
        memoryBank.recordVillagerRelationship(villagerUUID, "BestFriend", RelationshipType.BEST_FRIEND, "Always there for me", 0.8f);
        
        // When
        String lifeStory = memoryBank.generateLifeStorySummary();
        
        // Then
        assertNotNull(lifeStory);
        assertFalse(lifeStory.trim().isEmpty());
        assertTrue(lifeStory.contains("My Life in the Village"));
        assertTrue(lifeStory.contains("Personal Chronicles"));
        assertTrue(lifeStory.contains("Village History I Witnessed"));
        assertTrue(lifeStory.contains("Important People in My Life"));
        assertTrue(lifeStory.contains("Childhood"));
        assertTrue(lifeStory.contains("BestFriend"));
    }
    
    @Test
    @DisplayName("Should get most significant memories")
    void testMostSignificantMemories() {
        // Given - add memories with different emotional intensities
        memoryBank.recordPlayerInteraction(playerUUID, "Player1", "greeting", "Hi", "Hello", 0.1f);
        memoryBank.recordTrauma("Big scary event", "trigger", 0.9f, Arrays.asList(villagerUUID));
        memoryBank.recordJoyfulMemory("Amazing celebration", Arrays.asList(playerUUID), 0.8f);
        
        // When
        List<Memory> significantMemories = memoryBank.getMostSignificantMemories(2);
        
        // Then
        assertEquals(2, significantMemories.size());
        // Should be ordered by emotional intensity (highest first)
        assertTrue(significantMemories.get(0).getEmotionalIntensity() >= significantMemories.get(1).getEmotionalIntensity());
    }
    
    @Test
    @DisplayName("Should maintain memory limits")
    void testMemoryLimits() {
        // Given - add many village events (more than the limit)
        for (int i = 0; i < 35; i++) {
            memoryBank.recordVillageEvent("event" + i, "Description " + i, Arrays.asList(playerUUID), EventImpact.NEUTRAL);
        }
        
        // When
        List<VillageEvent> events = memoryBank.getVillageEvents();
        
        // Then
        assertTrue(events.size() <= 30); // Should respect MAX_VILLAGE_EVENTS limit
    }
    
    @Test
    @DisplayName("Should support legacy significant memories")
    void testLegacySignificantMemories() {
        // Given
        String memory1 = "I learned to farm";
        String memory2 = "I met my best friend";
        
        // When
        memoryBank.addSignificantMemory(memory1);
        memoryBank.addSignificantMemory(memory2);
        
        // Then
        List<String> memories = memoryBank.getSignificantMemories();
        assertEquals(2, memories.size());
        assertTrue(memories.contains(memory1));
        assertTrue(memories.contains(memory2));
    }
}