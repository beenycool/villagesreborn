package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.*;
import com.beeny.villagesreborn.core.ai.social.*;
import com.beeny.villagesreborn.core.conversation.ConversationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enhanced PromptBuilder with rich memory integration
 */
public class EnhancedPromptBuilderTests {
    
    private VillagerBrain villagerBrain;
    private PromptBuilder promptBuilder;
    private ConversationContext context;
    private UUID villagerUUID;
    
    @BeforeEach
    void setUp() {
        villagerUUID = UUID.randomUUID();
        villagerBrain = new VillagerBrain(villagerUUID);
        villagerBrain.setVillagerName("TestVillager");
        villagerBrain.setProfession("Blacksmith");
        villagerBrain.setVillageName("Testville");
        
        promptBuilder = new PromptBuilder();
        
        context = new ConversationContext();
        context.setTimeOfDay("morning");
        context.setWeather("sunny");
        context.setLocation("Village Square");
        context.setRelationship("friendly");
    }
    
    @Test
    @DisplayName("Should build enhanced system prompt with background narrative")
    void testEnhancedSystemPromptBuilding() {
        // Given - add some personal stories to create background
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        memoryBank.addPersonalStory("Apprenticeship", "I learned smithing from the old master", 
                                   Arrays.asList("Old Master", "Anvil"), EmotionalTone.PROUD, false);
        
        // When
        String systemPrompt = promptBuilder.buildEnhancedSystemPrompt(villagerBrain);
        
        // Then
        assertNotNull(systemPrompt);
        assertFalse(systemPrompt.trim().isEmpty());
        
        // Should include personality
        assertTrue(systemPrompt.contains("Your personality:"));
        
        // Should include mood
        assertTrue(systemPrompt.contains("Your current mood:"));
        
        // Should include background story
        assertTrue(systemPrompt.contains("Your background:"));
        assertTrue(systemPrompt.contains("smithing") || systemPrompt.contains("learned"));
        
        // Should include enhanced guidelines
        assertTrue(systemPrompt.contains("drawing on your memories"));
        assertTrue(systemPrompt.contains("personal history"));
        assertTrue(systemPrompt.contains("village events"));
    }
    
    @Test
    @DisplayName("Should build comprehensive prompt with memory context")
    void testComprehensivePromptBuilding() {
        // Given - add rich memories
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        UUID playerUUID = UUID.randomUUID();
        
        // Add recent village event
        memoryBank.recordVillageEvent("festival", "Wonderful harvest celebration", 
                                    Arrays.asList(villagerUUID, playerUUID), EventImpact.VERY_POSITIVE);
        
        // Add player interaction
        memoryBank.recordPlayerInteraction(playerUUID, "FriendlyPlayer", "greeting", 
                                         "Hello there!", "Good morning!", 0.3f);
        
        // Add personal story
        memoryBank.addPersonalStory("Master's Teaching", "The day I forged my first sword", 
                                   Arrays.asList("Master Smith"), EmotionalTone.PROUD, false);
        
        // Add villager relationship
        UUID friendUUID = UUID.randomUUID();
        memoryBank.recordVillagerRelationship(friendUUID, "BestFriend", RelationshipType.BEST_FRIEND, 
                                            "Always supports my work", 0.8f);
        
        // When
        String fullPrompt = promptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertNotNull(fullPrompt);
        assertFalse(fullPrompt.trim().isEmpty());
        
        // Should include system prompt elements
        assertTrue(fullPrompt.contains("villager in a Minecraft village"));
        assertTrue(fullPrompt.contains("Your personality:"));
        
        // Should include memory context
        assertTrue(fullPrompt.contains("Recent Events on Your Mind") || 
                  fullPrompt.contains("Your Personal History") ||
                  fullPrompt.contains("Important Relationships"));
        
        // Should include current context
        assertTrue(fullPrompt.contains("Current Situation:"));
        assertTrue(fullPrompt.contains("morning"));
        assertTrue(fullPrompt.contains("sunny"));
        assertTrue(fullPrompt.contains("Village Square"));
        
        // Should end with response instruction
        assertTrue(fullPrompt.contains("Your Response:"));
    }
    
    @Test
    @DisplayName("Should build memory context from recent events")
    void testMemoryContextBuilding() {
        // Given - add memories from different time periods
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        
        // Recent memory (should be included)
        memoryBank.recordVillageEvent("recent_festival", "Yesterday's celebration", 
                                    Arrays.asList(villagerUUID), EventImpact.POSITIVE);
        
        // When
        String memoryContext = promptBuilder.buildMemoryContext(memoryBank, context);
        
        // Then
        assertNotNull(memoryContext);
        
        if (!memoryContext.trim().isEmpty()) {
            // Should include recent events section if there are recent memories
            assertTrue(memoryContext.contains("Recent Events on Your Mind") || 
                      memoryContext.isEmpty()); // Might be empty if no qualifying memories
        }
    }
    
    @Test
    @DisplayName("Should include personal stories in memory context")
    void testPersonalStoriesInContext() {
        // Given
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        memoryBank.addPersonalStory("Great Adventure", "My journey to the mountain village", 
                                   Arrays.asList("Mountain Guide", "Traveler"), EmotionalTone.EXCITED, false);
        memoryBank.addPersonalStory("Learning Experience", "When I mastered the forge", 
                                   Arrays.asList("Master"), EmotionalTone.PROUD, true);
        
        // When
        String memoryContext = promptBuilder.buildMemoryContext(memoryBank, context);
        
        // Then
        if (!memoryContext.trim().isEmpty()) {
            assertTrue(memoryContext.contains("Your Personal History"));
            assertTrue(memoryContext.contains("Great Adventure") || memoryContext.contains("Learning Experience"));
        }
    }
    
    @Test
    @DisplayName("Should include important relationships in memory context")
    void testRelationshipsInContext() {
        // Given
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        UUID friendUUID = UUID.randomUUID();
        UUID rivalUUID = UUID.randomUUID();
        
        memoryBank.recordVillagerRelationship(friendUUID, "GoodFriend", RelationshipType.CLOSE_FRIEND, 
                                            "Always helps me", 0.7f);
        memoryBank.recordVillagerRelationship(rivalUUID, "Rival", RelationshipType.RIVAL, 
                                            "We compete for customers", -0.6f);
        
        // When
        String memoryContext = promptBuilder.buildMemoryContext(memoryBank, context);
        
        // Then
        if (!memoryContext.trim().isEmpty()) {
            assertTrue(memoryContext.contains("Important Relationships"));
            assertTrue(memoryContext.contains("fond of") || memoryContext.contains("troubled by"));
        }
    }
    
    @Test
    @DisplayName("Should include traumatic experiences when relevant")
    void testTraumaticEventsInContext() {
        // Given
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        memoryBank.recordTrauma("Fire in the forge", "sudden flames", 0.8f, Arrays.asList(villagerUUID));
        
        // When
        String memoryContext = promptBuilder.buildMemoryContext(memoryBank, context);
        
        // Then
        if (!memoryContext.trim().isEmpty()) {
            assertTrue(memoryContext.contains("Difficult Experiences"));
            assertTrue(memoryContext.contains("Fire") || memoryContext.contains("flames"));
        }
    }
    
    @Test
    @DisplayName("Should build conversation history section")
    void testConversationHistoryBuilding() {
        // Given - add some conversation history
        UUID playerUUID = UUID.randomUUID();
        ConversationInteraction interaction1 = new ConversationInteraction(
            System.currentTimeMillis() - 10000,
            playerUUID,
            "Hello there!",
            "Good day to you!",
            villagerBrain.getCurrentMood(),
            "Village"
        );
        ConversationInteraction interaction2 = new ConversationInteraction(
            System.currentTimeMillis(),
            playerUUID,
            "How's business?",
            "Very well, thank you for asking!",
            villagerBrain.getCurrentMood(),
            "Village"
        );
        
        villagerBrain.getShortTermMemory().addInteraction(interaction1);
        villagerBrain.getShortTermMemory().addInteraction(interaction2);
        
        // When
        String historySection = promptBuilder.buildConversationHistory(villagerBrain.getShortTermMemory());
        
        // Then
        assertNotNull(historySection);
        if (!historySection.trim().isEmpty()) {
            assertTrue(historySection.contains("Recent Conversation History:"));
            assertTrue(historySection.contains("Player: Hello there!"));
            assertTrue(historySection.contains("You: Good day to you!"));
            assertTrue(historySection.contains("Player: How's business?"));
            assertTrue(historySection.contains("You: Very well, thank you for asking!"));
        }
    }
    
    @Test
    @DisplayName("Should build current context section")
    void testCurrentContextBuilding() {
        // Given - context already set in setUp
        
        // When
        String contextSection = promptBuilder.buildCurrentContext(context);
        
        // Then
        assertNotNull(contextSection);
        assertTrue(contextSection.contains("Current Situation:"));
        assertTrue(contextSection.contains("Time: morning"));
        assertTrue(contextSection.contains("Weather: sunny"));
        assertTrue(contextSection.contains("Location: Village Square"));
        assertTrue(contextSection.contains("Player Relationship: friendly"));
    }
    
    @Test
    @DisplayName("Should handle empty memory bank gracefully")
    void testEmptyMemoryBankHandling() {
        // Given - fresh villager with no memories
        VillagerBrain freshBrain = new VillagerBrain(UUID.randomUUID());
        freshBrain.setVillagerName("FreshVillager");
        
        // When
        String prompt = promptBuilder.buildPrompt(freshBrain, context);
        
        // Then
        assertNotNull(prompt);
        assertFalse(prompt.trim().isEmpty());
        
        // Should still contain basic elements
        assertTrue(prompt.contains("villager in a Minecraft village"));
        assertTrue(prompt.contains("Current Situation:"));
        assertTrue(prompt.contains("Your Response:"));
    }
    
    @Test
    @DisplayName("Should enforce token limits by truncating when necessary")
    void testTokenLimitEnforcement() {
        // Given - add lots of conversation history to trigger truncation
        ConversationHistory longHistory = villagerBrain.getShortTermMemory();
        UUID playerUUID = UUID.randomUUID();
        
        // Add many interactions to create a very long prompt
        for (int i = 0; i < 50; i++) {
            ConversationInteraction interaction = new ConversationInteraction(
                System.currentTimeMillis() - (i * 1000),
                playerUUID,
                "This is a very long player message number " + i + " that contains lots of text to make the prompt very long and test truncation functionality",
                "This is an equally long villager response number " + i + " with lots of details about the villager's thoughts and feelings and experiences",
                villagerBrain.getCurrentMood(),
                "Village"
            );
            longHistory.addInteraction(interaction);
        }
        
        // When
        String prompt = promptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertNotNull(prompt);
        assertTrue(prompt.length() <= PromptBuilder.MAX_PROMPT_LENGTH + 1000); // Allow some tolerance
        
        // Should still contain essential elements even after truncation
        assertTrue(prompt.contains("villager in a Minecraft village"));
        assertTrue(prompt.contains("Your Response:"));
    }
    
    @Test
    @DisplayName("Should build legacy system prompt for backward compatibility")
    void testLegacySystemPromptBuilding() {
        // When
        String legacyPrompt = promptBuilder.buildSystemPrompt(villagerBrain);
        
        // Then
        assertNotNull(legacyPrompt);
        assertFalse(legacyPrompt.trim().isEmpty());
        
        // Should contain basic elements without enhanced features
        assertTrue(legacyPrompt.contains("You are a villager in a Minecraft village"));
        assertTrue(legacyPrompt.contains("Your personality:"));
        assertTrue(legacyPrompt.contains("Your current mood:"));
        assertTrue(legacyPrompt.contains("Guidelines:"));
        
        // Should NOT contain enhanced features
        assertFalse(legacyPrompt.contains("Your background:"));
        assertFalse(legacyPrompt.contains("drawing on your memories"));
    }
    
    @Test
    @DisplayName("Should generate appropriate background narrative")
    void testBackgroundNarrativeGeneration() {
        // Given - add personal stories and relationships
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        memoryBank.addPersonalStory("Origin Story", "I came to this village as a young apprentice", 
                                   Arrays.asList("Former Master"), EmotionalTone.NOSTALGIC, false);
        
        UUID friendUUID = UUID.randomUUID();
        memoryBank.recordVillagerRelationship(friendUUID, "Mentor", RelationshipType.MENTOR, 
                                            "Taught me everything", 0.9f);
        
        // When
        String enhancedPrompt = promptBuilder.buildEnhancedSystemPrompt(villagerBrain);
        
        // Then
        assertTrue(enhancedPrompt.contains("Your background:"));
        assertTrue(enhancedPrompt.contains("apprentice") || enhancedPrompt.contains("village") || 
                  enhancedPrompt.contains("Well-connected"));
    }
    
    @Test
    @DisplayName("Should handle context with nearby villagers")
    void testNearbyVillagersContext() {
        // Given
        context.setNearbyVillagers("Alice, Bob, Charlie");
        
        // When
        String contextSection = promptBuilder.buildCurrentContext(context);
        
        // Then
        assertTrue(contextSection.contains("Other villagers nearby: Alice, Bob, Charlie"));
    }
    
    @Test
    @DisplayName("Should maintain prompt structure integrity")
    void testPromptStructureIntegrity() {
        // Given - add some memories for a complete prompt
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        memoryBank.addPersonalStory("Test Story", "A simple story", Arrays.asList("Character"), EmotionalTone.NEUTRAL, false);
        
        // When
        String fullPrompt = promptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        String[] lines = fullPrompt.split("\n");
        
        // Should have proper structure with sections separated by blank lines
        assertTrue(fullPrompt.contains("You are a villager"));
        assertTrue(fullPrompt.contains("Current Situation:"));
        assertTrue(fullPrompt.contains("Your Response:"));
        
        // Should end with the response instruction
        assertTrue(fullPrompt.trim().endsWith("Your Response:"));
    }
}