package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ResponseDeliveryManager
 */
@DisplayName("Response Delivery Manager Tests")
class ResponseDeliveryManagerTest {

    @Mock
    private Player player;
    
    @Mock
    private VillagerEntity villager;

    private ResponseDeliveryManager deliveryManager;
    private UUID villagerUUID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        deliveryManager = new ResponseDeliveryManager();
        
        villagerUUID = UUID.randomUUID();
        
        when(player.getName()).thenReturn("TestPlayer");
        when(villager.getUUID()).thenReturn(villagerUUID);
        when(villager.getName()).thenReturn("TestVillager");
        when(villager.getProfession()).thenReturn("farmer");
        when(villager.getBlockPos()).thenReturn(new BlockPos(10, 64, 10));
    }

    @Test
    @DisplayName("Should deliver response to player")
    void shouldDeliverResponseToPlayer() {
        String response = "Hello there, friend!";
        
        assertDoesNotThrow(() -> {
            deliveryManager.deliverResponse(villager, player, response);
        });
        
        // Verify interaction through mocks
        verify(player).getName();
        verify(villager).getName();
    }

    @Test
    @DisplayName("Should format response with villager name")
    void shouldFormatResponseWithVillagerName() {
        String response = "Hello there!";
        
        deliveryManager.deliverResponse(villager, player, response);
        
        verify(villager).getName();
        verify(villager).getProfession();
    }

    @Test
    @DisplayName("Should handle rate limiting")
    void shouldHandleRateLimiting() {
        String response = "Hello!";
        
        // First response should go through
        deliveryManager.deliverResponse(villager, player, response);
        verify(player, times(1)).getName();
        
        // Immediate second response should be rate limited
        deliveryManager.deliverResponse(villager, player, response);
        verify(player, times(1)).getName(); // Should not be called again
    }

    @Test
    @DisplayName("Should truncate long responses")
    void shouldTruncateLongResponses() {
        StringBuilder longResponse = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longResponse.append("word ");
        }
        
        assertDoesNotThrow(() -> {
            deliveryManager.deliverResponse(villager, player, longResponse.toString());
        });
        
        verify(player).getName();
    }

    @Test
    @DisplayName("Should handle villager without name")
    void shouldHandleVillagerWithoutName() {
        when(villager.getName()).thenReturn(null);
        when(villager.getProfession()).thenReturn("farmer");
        
        String response = "Hello!";
        
        assertDoesNotThrow(() -> {
            deliveryManager.deliverResponse(villager, player, response);
        });
        
        verify(villager).getName();
        verify(villager).getProfession();
    }

    @Test
    @DisplayName("Should handle villager without profession")
    void shouldHandleVillagerWithoutProfession() {
        when(villager.getName()).thenReturn(null);
        when(villager.getProfession()).thenReturn(null);
        
        String response = "Hello!";
        
        assertDoesNotThrow(() -> {
            deliveryManager.deliverResponse(villager, player, response);
        });
        
        verify(villager).getName();
        verify(villager).getProfession();
    }

    @Test
    @DisplayName("Should generate villager ID from UUID")
    void shouldGenerateVillagerIdFromUUID() {
        String response = "Hello!";
        
        deliveryManager.deliverResponse(villager, player, response);
        
        verify(villager).getUUID();
    }

    @Test
    @DisplayName("Should generate villager ID from position when UUID is null")
    void shouldGenerateVillagerIdFromPositionWhenUUIDIsNull() {
        when(villager.getUUID()).thenReturn(null);
        
        String response = "Hello!";
        
        deliveryManager.deliverResponse(villager, player, response);
        
        verify(villager).getUUID();
        verify(villager).getBlockPos();
    }

    @Test
    @DisplayName("Should clear rate limiting")
    void shouldClearRateLimiting() {
        String response = "Hello!";
        
        // Send a response
        deliveryManager.deliverResponse(villager, player, response);
        verify(player, times(1)).getName();
        
        // Clear rate limiting
        deliveryManager.clearRateLimiting();
        
        // Should be able to send immediately
        deliveryManager.deliverResponse(villager, player, response);
        verify(player, times(2)).getName();
    }

    @Test
    @DisplayName("Should set custom rate limit for testing")
    void shouldSetCustomRateLimitForTesting() {
        String response = "Hello!";
        String villagerId = villagerUUID.toString();
        
        // Set custom rate limit (allow immediate delivery)
        deliveryManager.setRateLimit(villagerId, 5000); // 5 seconds ago
        
        // Should be able to send multiple messages
        deliveryManager.deliverResponse(villager, player, response);
        deliveryManager.deliverResponse(villager, player, response);
        
        verify(player, times(2)).getName();
    }

    @Test
    @DisplayName("Should handle empty responses")
    void shouldHandleEmptyResponses() {
        assertDoesNotThrow(() -> {
            deliveryManager.deliverResponse(villager, player, "");
        });
        
        assertDoesNotThrow(() -> {
            deliveryManager.deliverResponse(villager, player, null);
        });
    }

    @Test
    @DisplayName("Should handle concurrent delivery attempts")
    void shouldHandleConcurrentDeliveryAttempts() throws InterruptedException {
        String response = "Hello!";
        
        // Create multiple threads trying to deliver responses
        Thread thread1 = new Thread(() -> deliveryManager.deliverResponse(villager, player, response));
        Thread thread2 = new Thread(() -> deliveryManager.deliverResponse(villager, player, response));
        
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        
        // Should handle concurrent access without crashing
        verify(player, atLeastOnce()).getName();
    }

    @Test
    @DisplayName("Should handle different villagers independently")
    void shouldHandleDifferentVillagersIndependently() {
        VillagerEntity villager2 = mock(VillagerEntity.class);
        when(villager2.getUUID()).thenReturn(UUID.randomUUID());
        when(villager2.getName()).thenReturn("OtherVillager");
        when(villager2.getProfession()).thenReturn("librarian");
        when(villager2.getBlockPos()).thenReturn(new BlockPos(20, 64, 20));
        
        String response = "Hello!";
        
        // Both villagers should be able to send responses
        deliveryManager.deliverResponse(villager, player, response);
        deliveryManager.deliverResponse(villager2, player, response);
        
        verify(player, times(2)).getName();
    }
}