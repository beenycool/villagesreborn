package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.ai.VillagerBrainManager;
import com.beeny.villagesreborn.core.ai.VillagerProximityDetector;
import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.ServerChatEvent;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Enhanced tests for Phase 4 ChatEventHandler features
 */
@DisplayName("Enhanced Chat Event Handler Tests")
class ChatEventHandlerEnhancedTest {

    @Mock
    private VillagerProximityDetector proximityDetector;
    
    @Mock
    private VillagerBrainManager brainManager;
    
    @Mock
    private ConversationRouter conversationRouter;
    
    @Mock
    private ServerChatEvent chatEvent;
    
    @Mock
    private Player player;
    
    @Mock
    private VillagerEntity villager;

    private ChatEventHandler chatEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatEventHandler = new ChatEventHandler(proximityDetector, brainManager, conversationRouter);
        
        // Setup default mocks
        when(player.getBlockPos()).thenReturn(new BlockPos(0, 64, 0));
        when(player.getUUID()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("TestPlayer");
        when(chatEvent.getPlayer()).thenReturn(player);
        when(chatEvent.getTimestamp()).thenReturn(System.currentTimeMillis());
        when(chatEvent.getWorld()).thenReturn("overworld");
        when(villager.getUUID()).thenReturn(UUID.randomUUID());
        when(villager.getBlockPos()).thenReturn(new BlockPos(5, 64, 0));
        when(villager.getName()).thenReturn("TestVillager");
        when(villager.getProfession()).thenReturn("farmer");
    }

    @Test
    @DisplayName("Should trigger AI with default patterns")
    void shouldTriggerAIWithDefaultPatterns() {
        assertTrue(chatEventHandler.shouldTriggerAI("hello"));
        assertTrue(chatEventHandler.shouldTriggerAI("Hi there"));
        assertTrue(chatEventHandler.shouldTriggerAI("@villager"));
        assertTrue(chatEventHandler.shouldTriggerAI("Can you help?"));
        assertTrue(chatEventHandler.shouldTriggerAI("farmer, hello"));
        assertFalse(chatEventHandler.shouldTriggerAI("just normal text"));
        assertFalse(chatEventHandler.shouldTriggerAI(""));
        assertFalse(chatEventHandler.shouldTriggerAI(null));
    }

    @Test
    @DisplayName("Should handle custom trigger patterns")
    void shouldHandleCustomTriggerPatterns() {
        List<String> customPatterns = Arrays.asList("(?i)trade", "(?i)buy");
        chatEventHandler.updateTriggerPatterns(customPatterns);
        
        assertTrue(chatEventHandler.shouldTriggerAI("I want to trade"));
        assertTrue(chatEventHandler.shouldTriggerAI("buy something"));
        assertFalse(chatEventHandler.shouldTriggerAI("hello")); // Old pattern no longer works
    }

    @Test
    @DisplayName("Should handle async conversation routing")
    void shouldHandleAsyncConversationRouting() {
        when(chatEvent.getMessage()).thenReturn("hello villager");
        
        chatEventHandler.onServerChatEvent(chatEvent);
        
        // Verify that the conversation router is set up for async call
        verify(chatEvent).getPlayer();
        verify(chatEvent).getMessage();
        verify(chatEvent).getTimestamp();
        verify(chatEvent).getWorld();
    }

    @Test
    @DisplayName("Should not trigger AI for non-matching messages")
    void shouldNotTriggerAIForNonMatchingMessages() {
        when(chatEvent.getMessage()).thenReturn("just normal chat");
        
        chatEventHandler.onServerChatEvent(chatEvent);
        
        // Should not trigger AI processing
        verify(chatEvent).getMessage();
        verifyNoMoreInteractions(conversationRouter);
    }

    @Test
    @DisplayName("Should maintain backward compatibility")
    void shouldMaintainBackwardCompatibility() {
        // Test backward compatibility with legacy method
        assertTrue(chatEventHandler.isDirectedAtVillager("hello"));
        assertTrue(chatEventHandler.isDirectedAtVillager("@villager"));
        assertFalse(chatEventHandler.isDirectedAtVillager("normal text"));
        
        // Legacy method should delegate to new implementation
        when(chatEvent.getMessage()).thenReturn("hello");
        chatEventHandler.onServerChatReceived(chatEvent);
        verify(chatEvent).getMessage();
    }

    @Test
    @DisplayName("Should handle multiple pattern matches")
    void shouldHandleMultiplePatternMatches() {
        assertTrue(chatEventHandler.shouldTriggerAI("Hello, can you help me?")); // Multiple matches
        assertTrue(chatEventHandler.shouldTriggerAI("Hi farmer!")); // Multiple matches
    }

    @Test
    @DisplayName("Should gracefully handle invalid regex patterns")
    void shouldGracefullyHandleInvalidRegexPatterns() {
        // Test that invalid regex patterns are skipped gracefully
        List<String> invalidPatterns = Arrays.asList("(?i)valid", "[invalid", "(?i)another");
        chatEventHandler.updateTriggerPatterns(invalidPatterns);
        
        assertTrue(chatEventHandler.shouldTriggerAI("valid message"));
        assertTrue(chatEventHandler.shouldTriggerAI("another message"));
        // Invalid pattern should be skipped without crashing
    }

    @Test
    @DisplayName("Should handle empty or null messages")
    void shouldHandleEmptyOrNullMessages() {
        assertFalse(chatEventHandler.shouldTriggerAI(null));
        assertFalse(chatEventHandler.shouldTriggerAI(""));
        assertFalse(chatEventHandler.shouldTriggerAI("   ")); // Whitespace only
    }

    @Test
    @DisplayName("Should handle case insensitive matching")
    void shouldHandleCaseInsensitiveMatching() {
        assertTrue(chatEventHandler.shouldTriggerAI("HELLO"));
        assertTrue(chatEventHandler.shouldTriggerAI("Hello"));
        assertTrue(chatEventHandler.shouldTriggerAI("HeLLo"));
        assertTrue(chatEventHandler.shouldTriggerAI("FARMER"));
        assertTrue(chatEventHandler.shouldTriggerAI("farmer"));
    }

    @Test
    @DisplayName("Should handle question patterns")
    void shouldHandleQuestionPatterns() {
        assertTrue(chatEventHandler.shouldTriggerAI("What time is it?"));
        assertTrue(chatEventHandler.shouldTriggerAI("Can you help me?"));
        assertTrue(chatEventHandler.shouldTriggerAI("How are you?"));
        assertFalse(chatEventHandler.shouldTriggerAI("This is a statement."));
    }
}