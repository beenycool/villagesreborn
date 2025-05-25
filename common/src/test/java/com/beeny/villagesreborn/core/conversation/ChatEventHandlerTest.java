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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for ChatEventHandler
 * Tests ServerChatEvents.RECEIVED interception, villager proximity detection,
 * and addressed vs. overheard message handling
 */
@DisplayName("Chat Event Handler Tests")
class ChatEventHandlerTest {

    @Mock
    private VillagerProximityDetector proximityDetector;
    
    @Mock
    private VillagerBrainManager brainManager;
    
    @Mock
    private ConversationRouter conversationRouter;
    
    @Mock
    private ServerChatEvent mockChatEvent;
    
    @Mock
    private Player mockPlayer;
    
    @Mock
    private VillagerEntity mockVillager;

    private ChatEventHandler chatEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatEventHandler = new ChatEventHandler(proximityDetector, brainManager, conversationRouter);
    }

    @Test
    @DisplayName("Should detect villager addressing with @villager prefix")
    void shouldDetectVillagerAddressingWithPrefix() {
        // Given: Message with @villager prefix
        String message = "@villager hello there!";

        // When: Checking if directed at villager
        boolean isDirected = chatEventHandler.isDirectedAtVillager(message);

        // Then: Should be detected as directed
        assertTrue(isDirected);
    }

    @Test
    @DisplayName("Should not detect villager addressing in normal chat")
    void shouldNotDetectVillagerAddressingInNormalChat() {
        // Given: Normal chat message
        String message = "hello everyone";

        // When: Checking if directed at villager
        boolean isDirected = chatEventHandler.isDirectedAtVillager(message);

        // Then: Should not be detected as directed
        assertFalse(isDirected);
    }

    @Test
    @DisplayName("Should detect villager addressing with name patterns")
    void shouldDetectVillagerAddressingWithNamePatterns() {
        // Given: Messages addressing villagers by role
        String[] messages = {
            "hey farmer, how's the crops?",
            "librarian, do you have any books?",
            "blacksmith, can you fix this?"
        };

        // When & Then: All should be detected as directed
        for (String message : messages) {
            assertTrue(chatEventHandler.isDirectedAtVillager(message), 
                      "Should detect: " + message);
        }
    }

    @Test
    @DisplayName("Should find nearby villagers within chat radius")
    void shouldFindNearbyVillagersWithinChatRadius() {
        // Given: Player position and nearby villagers
        BlockPos playerPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> nearbyVillagers = List.of(mockVillager);
        when(mockPlayer.getBlockPos()).thenReturn(playerPos);
        when(proximityDetector.findNearbyVillagers(playerPos, ChatEventHandler.CHAT_RADIUS))
            .thenReturn(nearbyVillagers);

        // When: Finding nearby villagers
        List<VillagerEntity> found = chatEventHandler.findNearbyVillagers(mockPlayer);

        // Then: Should return nearby villagers
        assertEquals(1, found.size());
        assertEquals(mockVillager, found.get(0));
    }

    @Test
    @DisplayName("Should cancel chat event when directed at villager")
    void shouldCancelChatEventWhenDirectedAtVillager() {
        // Given: Chat event with directed message and nearby villager
        String directedMessage = "@villager hello";
        BlockPos playerPos = new BlockPos(0, 64, 0);
        BlockPos villagerPos = new BlockPos(5, 64, 0);
        List<VillagerEntity> nearbyVillagers = List.of(mockVillager);
        
        when(mockChatEvent.getMessage()).thenReturn(directedMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockPlayer);
        when(mockPlayer.getBlockPos()).thenReturn(playerPos);
        when(mockVillager.getBlockPos()).thenReturn(villagerPos);
        when(proximityDetector.findNearbyVillagers(playerPos, ChatEventHandler.CHAT_RADIUS))
            .thenReturn(nearbyVillagers);

        // When: Processing chat event
        chatEventHandler.onServerChatReceived(mockChatEvent);

        // Then: Should cancel the event
        verify(mockChatEvent).cancel();
    }

    @Test
    @DisplayName("Should route directed message to target villager")
    void shouldRouteDirectedMessageToTargetVillager() {
        // Given: Chat event with directed message and nearby villager
        String directedMessage = "@villager hello";
        BlockPos playerPos = new BlockPos(0, 64, 0);
        BlockPos villagerPos = new BlockPos(5, 64, 0);
        List<VillagerEntity> nearbyVillagers = List.of(mockVillager);
        
        when(mockChatEvent.getMessage()).thenReturn(directedMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockPlayer);
        when(mockPlayer.getBlockPos()).thenReturn(playerPos);
        when(mockVillager.getBlockPos()).thenReturn(villagerPos);
        when(proximityDetector.findNearbyVillagers(playerPos, ChatEventHandler.CHAT_RADIUS))
            .thenReturn(nearbyVillagers);

        // When: Processing chat event
        chatEventHandler.onServerChatReceived(mockChatEvent);

        // Then: Should route message to conversation system
        verify(conversationRouter).routeMessage(mockPlayer, mockVillager, directedMessage);
    }

    @Test
    @DisplayName("Should process overheard messages for nearby villagers")
    void shouldProcessOverheardMessagesForNearbyVillagers() {
        // Given: Normal chat with nearby villagers
        String normalMessage = "the weather is nice today";
        BlockPos playerPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> nearbyVillagers = List.of(mockVillager);
        
        when(mockChatEvent.getMessage()).thenReturn(normalMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockPlayer);
        when(mockPlayer.getBlockPos()).thenReturn(playerPos);
        when(proximityDetector.findNearbyVillagers(playerPos, ChatEventHandler.CHAT_RADIUS))
            .thenReturn(nearbyVillagers);

        // When: Processing chat event
        chatEventHandler.onServerChatReceived(mockChatEvent);

        // Then: Should process as overheard message
        verify(brainManager).processOverheardMessage(mockVillager, mockPlayer, normalMessage);
        verify(mockChatEvent, never()).cancel();
    }

    @Test
    @DisplayName("Should select closest villager as target")
    void shouldSelectClosestVillagerAsTarget() {
        // Given: Multiple villagers at different distances
        VillagerEntity villager1 = mock(VillagerEntity.class);
        VillagerEntity villager2 = mock(VillagerEntity.class);
        BlockPos playerPos = new BlockPos(0, 64, 0);
        BlockPos villager1Pos = new BlockPos(5, 64, 0);  // Distance: 5
        BlockPos villager2Pos = new BlockPos(3, 64, 0);  // Distance: 3 (closer)
        
        when(villager1.getBlockPos()).thenReturn(villager1Pos);
        when(villager2.getBlockPos()).thenReturn(villager2Pos);
        when(mockPlayer.getBlockPos()).thenReturn(playerPos);
        
        List<VillagerEntity> villagers = List.of(villager1, villager2);

        // When: Selecting target villager
        VillagerEntity target = chatEventHandler.selectTargetVillager(villagers, mockPlayer, "@villager hello");

        // Then: Should select the closest villager
        assertEquals(villager2, target);
    }

    @Test
    @DisplayName("Should handle empty villager list gracefully")
    void shouldHandleEmptyVillagerListGracefully() {
        // Given: Chat event with no nearby villagers
        String directedMessage = "@villager hello";
        BlockPos playerPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> emptyList = List.of();
        
        when(mockChatEvent.getMessage()).thenReturn(directedMessage);
        when(mockChatEvent.getPlayer()).thenReturn(mockPlayer);
        when(mockPlayer.getBlockPos()).thenReturn(playerPos);
        when(proximityDetector.findNearbyVillagers(playerPos, ChatEventHandler.CHAT_RADIUS))
            .thenReturn(emptyList);

        // When: Processing chat event
        chatEventHandler.onServerChatReceived(mockChatEvent);

        // Then: Should not cancel event or route message
        verify(mockChatEvent, never()).cancel();
        verify(conversationRouter, never()).routeMessage(any(), any(), any());
    }

    @Test
    @DisplayName("Should detect villager addressing case-insensitively")
    void shouldDetectVillagerAddressingCaseInsensitively() {
        // Given: Messages with different cases
        String[] messages = {
            "@VILLAGER hello",
            "@Villager hello", 
            "Hey FARMER, what's up?",
            "LIBRARIAN, help me"
        };

        // When & Then: All should be detected regardless of case
        for (String message : messages) {
            assertTrue(chatEventHandler.isDirectedAtVillager(message),
                      "Should detect case-insensitive: " + message);
        }
    }

    @Test
    @DisplayName("Should handle null or empty messages safely")
    void shouldHandleNullOrEmptyMessagesSafely() {
        // Given: Null and empty messages
        String nullMessage = null;
        String emptyMessage = "";

        // When & Then: Should not crash and return false
        assertFalse(chatEventHandler.isDirectedAtVillager(nullMessage));
        assertFalse(chatEventHandler.isDirectedAtVillager(emptyMessage));
    }

    @Test
    @DisplayName("Should respect chat radius configuration")
    void shouldRespectChatRadiusConfiguration() {
        // Given: Player position and custom chat radius
        BlockPos playerPos = new BlockPos(0, 64, 0);
        int customRadius = 32;
        
        when(mockPlayer.getBlockPos()).thenReturn(playerPos);

        // When: Finding nearby villagers with custom radius
        chatEventHandler.findNearbyVillagers(mockPlayer, customRadius);

        // Then: Should use the specified radius
        verify(proximityDetector).findNearbyVillagers(playerPos, customRadius);
    }
}