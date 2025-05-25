package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.conversation.ConversationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for PromptBuilder class
 */
@DisplayName("PromptBuilder Tests")
class PromptBuilderTest {
    
    private VillagerBrain villagerBrain;
    private ConversationContext context;
    
    @BeforeEach
    void setUp() {
        villagerBrain = new VillagerBrain(UUID.randomUUID());
        context = new ConversationContext();
        context.setTimeOfDay("Morning");
        context.setWeather("Sunny");
        context.setLocation("Village Square");
        context.setRelationship("Friendly");
    }
    
    @Test
    @DisplayName("Should build basic prompt with system prompt and context")
    void shouldBuildBasicPrompt() {
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        assertTrue(prompt.contains("villager"));
        assertTrue(prompt.contains("Morning"));
        assertTrue(prompt.contains("Sunny"));
        assertTrue(prompt.contains("Village Square"));
    }
    
    @Test
    @DisplayName("Should include personality traits in system prompt")
    void shouldIncludePersonalityTraits() {
        // Given: Villager with specific personality
        villagerBrain.getPersonalityTraits().adjustTrait(TraitType.FRIENDLINESS, 0.8f);
        villagerBrain.getPersonalityTraits().adjustTrait(TraitType.CURIOSITY, 0.6f);
        
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertTrue(prompt.contains("personality"));
        // Should include some representation of personality traits
    }
    
    @Test
    @DisplayName("Should include conversation history when available")
    void shouldIncludeConversationHistory() {
        // Given: Villager with conversation history
        ConversationInteraction interaction = new ConversationInteraction(
            System.currentTimeMillis() - 1000,
            UUID.randomUUID(),
            "Hello there!",
            "Greetings, traveler!",
            villagerBrain.getCurrentMood().copy(),
            "Village"
        );
        villagerBrain.getShortTermMemory().addInteraction(interaction);
        
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertTrue(prompt.contains("Hello there!"));
        assertTrue(prompt.contains("Greetings, traveler!"));
    }
    
    @Test
    @DisplayName("Should include current mood in system prompt")
    void shouldIncludeCurrentMood() {
        // Given: Villager with specific mood
        villagerBrain.getCurrentMood().setHappiness(0.8f);
        villagerBrain.getCurrentMood().setEnergy(0.6f);
        
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertTrue(prompt.contains("mood"));
    }
    
    @Test
    @DisplayName("Should build context section with location and time")
    void shouldBuildContextSection() {
        // When
        String contextSection = PromptBuilder.buildCurrentContext(context);
        
        // Then
        assertNotNull(contextSection);
        assertTrue(contextSection.contains("Morning"));
        assertTrue(contextSection.contains("Sunny"));
        assertTrue(contextSection.contains("Village Square"));
        assertTrue(contextSection.contains("Friendly"));
    }
    
    @Test
    @DisplayName("Should build conversation history section")
    void shouldBuildConversationHistorySection() {
        // Given: Multiple interactions
        ConversationInteraction interaction1 = new ConversationInteraction(
            System.currentTimeMillis() - 2000,
            UUID.randomUUID(),
            "What's your name?",
            "I'm Bob the blacksmith.",
            villagerBrain.getCurrentMood().copy(),
            "Village"
        );
        ConversationInteraction interaction2 = new ConversationInteraction(
            System.currentTimeMillis() - 1000,
            UUID.randomUUID(),
            "How are you today?",
            "I'm doing well, thank you!",
            villagerBrain.getCurrentMood().copy(),
            "Village"
        );
        
        villagerBrain.getShortTermMemory().addInteraction(interaction1);
        villagerBrain.getShortTermMemory().addInteraction(interaction2);
        
        // When
        String historySection = PromptBuilder.buildConversationHistory(villagerBrain.getShortTermMemory());
        
        // Then
        assertNotNull(historySection);
        assertTrue(historySection.contains("What's your name?"));
        assertTrue(historySection.contains("I'm Bob the blacksmith."));
        assertTrue(historySection.contains("How are you today?"));
        assertTrue(historySection.contains("I'm doing well, thank you!"));
    }
    
    @Test
    @DisplayName("Should limit conversation history to prevent token overflow")
    void shouldLimitConversationHistory() {
        // Given: Many interactions
        for (int i = 0; i < 100; i++) {
            ConversationInteraction interaction = new ConversationInteraction(
                System.currentTimeMillis() - (100 - i) * 1000,
                UUID.randomUUID(),
                "Message " + i,
                "Response " + i,
                villagerBrain.getCurrentMood().copy(),
                "Village"
            );
            villagerBrain.getShortTermMemory().addInteraction(interaction);
        }
        
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertNotNull(prompt);
        // Should not be excessively long - basic token limiting
        assertTrue(prompt.length() < 10000); // Reasonable upper bound
    }
    
    @Test
    @DisplayName("Should truncate oldest entries when enforcing token limits")
    void shouldTruncateOldestEntries() {
        // Given: Many interactions to force truncation
        for (int i = 0; i < 50; i++) {
            ConversationInteraction interaction = new ConversationInteraction(
                System.currentTimeMillis() - (50 - i) * 1000,
                UUID.randomUUID(),
                "Old message " + i,
                "Old response " + i,
                villagerBrain.getCurrentMood().copy(),
                "Village"
            );
            villagerBrain.getShortTermMemory().addInteraction(interaction);
        }
        
        // Add recent interaction
        ConversationInteraction recentInteraction = new ConversationInteraction(
            System.currentTimeMillis(),
            UUID.randomUUID(),
            "Recent message",
            "Recent response",
            villagerBrain.getCurrentMood().copy(),
            "Village"
        );
        villagerBrain.getShortTermMemory().addInteraction(recentInteraction);
        
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertTrue(prompt.contains("Recent message"));
        assertTrue(prompt.contains("Recent response"));
        // Should not contain very old messages if truncated
    }
    
    @Test
    @DisplayName("Should handle empty conversation history gracefully")
    void shouldHandleEmptyConversationHistory() {
        // Given: Villager with no conversation history (default state)
        
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertNotNull(prompt);
        assertFalse(prompt.isEmpty());
        // Should still contain system prompt and context
        assertTrue(prompt.contains("villager"));
        assertTrue(prompt.contains("Morning"));
    }
    
    @Test
    @DisplayName("Should include dynamic context layers")
    void shouldIncludeDynamicContextLayers() {
        // Given: Context with nearby villagers
        context.setHasNearbyVillagers(true);
        context.setNearbyVillagers("Alice the farmer, Charlie the baker");
        
        // When
        String prompt = PromptBuilder.buildPrompt(villagerBrain, context);
        
        // Then
        assertTrue(prompt.contains("Alice the farmer"));
        assertTrue(prompt.contains("Charlie the baker"));
    }
}