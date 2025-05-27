package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.conversation.ConversationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for PromptBuilder
 * Tests system prompt assembly with personality, mood, context layers,
 * history concatenation and token-length enforcement
 */
@DisplayName("Prompt Builder Tests")
class PromptBuilderTest {

    @Mock
    private VillagerBrain mockBrain;
    
    @Mock
    private PersonalityProfile mockPersonality;
    
    @Mock
    private MoodState mockMood;
    
    @Mock
    private ConversationHistory mockHistory;
    
    @Mock
    private ConversationContext mockContext;

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        promptBuilder = new PromptBuilder();
    }

    @Test
    @DisplayName("Should build complete prompt with all components")
    void shouldBuildCompletePromptWithAllComponents() {
        // Given: Complete villager brain setup
        when(mockBrain.getPersonalityTraits()).thenReturn(mockPersonality);
        when(mockBrain.getCurrentMood()).thenReturn(mockMood);
        when(mockBrain.getShortTermMemory()).thenReturn(mockHistory);
        when(mockBrain.getVillagerName()).thenReturn("Bob");
        when(mockBrain.getProfession()).thenReturn("Farmer");
        when(mockBrain.getVillageName()).thenReturn("Testville");
        
        when(mockPersonality.generateDescription()).thenReturn("Friendly and helpful");
        when(mockMood.getOverallMood()).thenReturn(MoodCategory.HAPPY);
        when(mockHistory.getRecent(anyInt())).thenReturn(List.of());

        // When: Building prompt
        String prompt = promptBuilder.buildPrompt(mockBrain, mockContext);

        // Then: Should contain all components
        assertNotNull(prompt);
        assertTrue(prompt.contains("Friendly and helpful"));
        assertTrue(prompt.contains("HAPPY"));
    }

    @Test
    @DisplayName("Should generate system prompt with personality traits")
    void shouldGenerateSystemPromptWithPersonalityTraits() {
        // Given: Villager with specific personality
        when(mockBrain.getPersonalityTraits()).thenReturn(mockPersonality);
        when(mockBrain.getCurrentMood()).thenReturn(mockMood);
        when(mockBrain.getVillagerName()).thenReturn("Alice");
        when(mockBrain.getProfession()).thenReturn("Librarian");
        when(mockBrain.getVillageName()).thenReturn("Booktown");
        
        when(mockPersonality.generateDescription()).thenReturn("Scholarly and introverted");
        when(mockMood.getOverallMood()).thenReturn(MoodCategory.CONTENT);

        // When: Building system prompt
        String systemPrompt = promptBuilder.buildSystemPrompt(mockBrain);

        // Then: Should include personality description
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("Scholarly and introverted"));
        assertTrue(systemPrompt.contains("CONTENT"));
        assertTrue(systemPrompt.length() > 100);
    }

    @Test
    @DisplayName("Should build conversation history from recent interactions")
    void shouldBuildConversationHistoryFromRecentInteractions() {
        // Given: Conversation history with interactions
        ConversationInteraction interaction1 = createMockInteraction("Hello!", "Hi there!");
        ConversationInteraction interaction2 = createMockInteraction("How are you?", "I'm doing well, thanks!");
        List<ConversationInteraction> interactions = List.of(interaction1, interaction2);
        
        when(mockHistory.getRecent(PromptBuilder.CONTEXT_WINDOW_SIZE)).thenReturn(interactions);

        // When: Building conversation history
        String history = promptBuilder.buildConversationHistory(mockHistory);

        // Then: Should format interactions correctly
        assertNotNull(history);
        assertTrue(history.contains("Player: Hello!"));
        assertTrue(history.contains("You: Hi there!"));
        assertTrue(history.contains("Player: How are you?"));
        assertTrue(history.contains("You: I'm doing well, thanks!"));
        
        // Should have proper structure (2 interactions = 4 lines)
        String[] lines = history.split("\n");
        assertEquals(5, lines.length); // Header + 4 lines
    }

    @Test
    @DisplayName("Should build current context with environmental data")
    void shouldBuildCurrentContextWithEnvironmentalData() {
        // Given: Conversation context with environment
        when(mockContext.getTimeOfDay()).thenReturn("Morning");
        when(mockContext.getWeather()).thenReturn("Sunny");
        when(mockContext.getLocation()).thenReturn("Village Square");
        when(mockContext.getRelationship()).thenReturn("Friendly");
        when(mockContext.hasNearbyVillagers()).thenReturn(true);
        when(mockContext.getNearbyVillagers()).thenReturn("Tom, Jerry");

        // When: Building current context
        String context = promptBuilder.buildCurrentContext(mockContext);

        // Then: Should include all environmental data
        assertNotNull(context);
        assertTrue(context.contains("Current Situation:"));
        assertTrue(context.contains("Time: Morning"));
        assertTrue(context.contains("Weather: Sunny"));
        assertTrue(context.contains("Location: Village Square"));
        assertTrue(context.contains("Player Relationship: Friendly"));
        assertTrue(context.contains("Other villagers nearby: Tom, Jerry"));
    }

    @Test
    @DisplayName("Should enforce maximum prompt length limits")
    void shouldEnforceMaximumPromptLengthLimits() {
        // Given: Villager brain with extensive history
        when(mockBrain.getPersonalityTraits()).thenReturn(mockPersonality);
        when(mockBrain.getCurrentMood()).thenReturn(mockMood);
        when(mockBrain.getShortTermMemory()).thenReturn(mockHistory);
        when(mockBrain.getVillagerName()).thenReturn("Bob");
        when(mockBrain.getProfession()).thenReturn("Farmer");
        when(mockBrain.getVillageName()).thenReturn("Testville");
        
        when(mockPersonality.generateDescription()).thenReturn("Very detailed personality");
        when(mockMood.getOverallMood()).thenReturn(MoodCategory.HAPPY);
        
        // Create extensive history that would exceed limits
        List<ConversationInteraction> longHistory = createLongHistoryList(100);
        when(mockHistory.getRecent(anyInt())).thenReturn(longHistory);

        // When: Building prompt
        String prompt = promptBuilder.buildPrompt(mockBrain, mockContext);

        // Then: Should respect maximum length
        assertNotNull(prompt);
        assertTrue(prompt.length() <= PromptBuilder.MAX_PROMPT_LENGTH);
    }

    @Test
    @DisplayName("Should handle empty conversation history gracefully")
    void shouldHandleEmptyConversationHistoryGracefully() {
        // Given: Empty conversation history
        when(mockHistory.getRecent(anyInt())).thenReturn(List.of());

        // When: Building conversation history
        String history = promptBuilder.buildConversationHistory(mockHistory);

        // Then: Should return empty or minimal content
        assertNotNull(history);
        assertTrue(history.isEmpty() || history.trim().isEmpty());
    }

    @Test
    @DisplayName("Should include response guidelines in prompt")
    void shouldIncludeResponseGuidelinesInPrompt() {
        // Given: Complete villager brain
        setupMockBrain();

        // When: Building prompt
        String prompt = promptBuilder.buildPrompt(mockBrain, mockContext);

        // Then: Should include response guidelines
        assertNotNull(prompt);
        assertTrue(prompt.contains("Response Requirements:") || 
                  prompt.contains("Guidelines:") ||
                  prompt.contains("Stay in character"));
    }

    @Test
    @DisplayName("Should truncate history when approaching token limits")
    void shouldTruncateHistoryWhenApproachingTokenLimits() {
        // Given: Brain with moderate history that needs truncation
        setupMockBrain();
        List<ConversationInteraction> moderateHistory = createLongHistoryList(20);
        when(mockHistory.getRecent(anyInt())).thenReturn(moderateHistory);

        // When: Building prompt
        String prompt = promptBuilder.buildPrompt(mockBrain, mockContext);

        // Then: Should be within limits and history should be truncated appropriately
        assertNotNull(prompt);
        assertTrue(prompt.length() <= PromptBuilder.MAX_PROMPT_LENGTH);
        
        // Should still contain essential components
        assertTrue(prompt.contains("Guidelines:"));
    }

    @Test
    @DisplayName("Should prioritize recent interactions in truncated history")
    void shouldPrioritizeRecentInteractionsInTruncatedHistory() {
        // Given: History with many interactions
        ConversationInteraction oldInteraction = createMockInteraction("Old message", "Old response");
        ConversationInteraction recentInteraction = createMockInteraction("Recent message", "Recent response");
        
        List<ConversationInteraction> history = List.of(oldInteraction, recentInteraction);
        when(mockHistory.getRecent(anyInt())).thenReturn(history);

        // When: Building conversation history
        String historyString = promptBuilder.buildConversationHistory(mockHistory);

        // Then: Should include both but prioritize structure
        assertNotNull(historyString);
        assertTrue(historyString.contains("Recent message"));
        assertTrue(historyString.contains("Recent response"));
    }

    @Test
    @DisplayName("Should handle null mood and personality gracefully")
    void shouldHandleNullMoodAndPersonalityGracefully() {
        // Given: Brain with null components
        when(mockBrain.getPersonalityTraits()).thenReturn(null);
        when(mockBrain.getCurrentMood()).thenReturn(null);
        when(mockBrain.getShortTermMemory()).thenReturn(mockHistory);
        when(mockBrain.getVillagerName()).thenReturn("Bob");
        when(mockBrain.getProfession()).thenReturn("Farmer");
        when(mockBrain.getVillageName()).thenReturn("Testville");
        when(mockHistory.getRecent(anyInt())).thenReturn(List.of());

        // When: Building prompt
        String prompt = promptBuilder.buildPrompt(mockBrain, mockContext);

        // Then: Should not crash and provide fallback values
        assertNotNull(prompt);
        assertTrue(prompt.contains("Unknown personality"));
        assertTrue(prompt.contains("NEUTRAL"));
    }

    @Test
    @DisplayName("Should format prompt sections with proper separators")
    void shouldFormatPromptSectionsWithProperSeparators() {
        // Given: Complete brain setup
        setupMockBrain();

        // When: Building prompt
        String prompt = promptBuilder.buildPrompt(mockBrain, mockContext);

        // Then: Should have proper section separators
        assertNotNull(prompt);
        assertTrue(prompt.contains("\n\n")); // Section separators
        
        // Should be well-structured
        String[] sections = prompt.split("\n\n");
        assertTrue(sections.length >= 2); // At least system prompt and guidelines
    }

    // Helper methods
    private ConversationInteraction createMockInteraction(String playerMessage, String villagerResponse) {
        ConversationInteraction interaction = mock(ConversationInteraction.class);
        when(interaction.getPlayerMessage()).thenReturn(playerMessage);
        when(interaction.getVillagerResponse()).thenReturn(villagerResponse);
        return interaction;
    }

    private List<ConversationInteraction> createLongHistoryList(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createMockInteraction("Message " + i, "Response " + i))
            .toList();
    }

    private void setupMockBrain() {
        when(mockBrain.getPersonalityTraits()).thenReturn(mockPersonality);
        when(mockBrain.getCurrentMood()).thenReturn(mockMood);
        when(mockBrain.getShortTermMemory()).thenReturn(mockHistory);
        when(mockBrain.getVillagerName()).thenReturn("Bob");
        when(mockBrain.getProfession()).thenReturn("Farmer");
        when(mockBrain.getVillageName()).thenReturn("Testville");
        
        when(mockPersonality.generateDescription()).thenReturn("Friendly and helpful");
        when(mockMood.getOverallMood()).thenReturn(MoodCategory.HAPPY);
        when(mockHistory.getRecent(anyInt())).thenReturn(List.of());
    }
}