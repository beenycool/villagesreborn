package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for VillagerProximityDetectorImpl with spatial indexing
 */
@DisplayName("Villager Proximity Detector Tests")
class VillagerProximityDetectorTest {

    @Mock
    private VillagerEntity villager1;
    
    @Mock
    private VillagerEntity villager2;
    
    @Mock
    private VillagerEntity villager3;
    
    @Mock
    private Player player;

    private VillagerProximityDetectorImpl proximityDetector;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        proximityDetector = new VillagerProximityDetectorImpl();
        
        // Set up villager positions
        when(villager1.getUUID()).thenReturn(UUID.randomUUID());
        when(villager1.getBlockPos()).thenReturn(new BlockPos(5, 64, 5));
        
        when(villager2.getUUID()).thenReturn(UUID.randomUUID());
        when(villager2.getBlockPos()).thenReturn(new BlockPos(20, 64, 20));
        
        when(villager3.getUUID()).thenReturn(UUID.randomUUID());
        when(villager3.getBlockPos()).thenReturn(new BlockPos(100, 64, 100));
        
        when(player.getBlockPos()).thenReturn(new BlockPos(0, 64, 0));
    }

    @Test
    @DisplayName("Should find villagers within radius")
    void shouldFindVillagersWithinRadius() {
        // Add villagers to spatial index
        proximityDetector.updateVillagerPosition(villager1);
        proximityDetector.updateVillagerPosition(villager2);
        proximityDetector.updateVillagerPosition(villager3);
        
        BlockPos searchPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(searchPos, 10);
        
        assertEquals(1, nearby.size());
        assertEquals(villager1, nearby.get(0));
    }

    @Test
    @DisplayName("Should return empty list when no villagers nearby")
    void shouldReturnEmptyListWhenNoVillagersNearby() {
        proximityDetector.updateVillagerPosition(villager3); // Far away
        
        BlockPos searchPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(searchPos, 10);
        
        assertTrue(nearby.isEmpty());
    }

    @Test
    @DisplayName("Should sort results by distance")
    void shouldSortResultsByDistance() {
        // Position villagers at different distances
        when(villager1.getBlockPos()).thenReturn(new BlockPos(3, 64, 0)); // Distance ~3
        when(villager2.getBlockPos()).thenReturn(new BlockPos(1, 64, 0)); // Distance ~1
        
        proximityDetector.updateVillagerPosition(villager1);
        proximityDetector.updateVillagerPosition(villager2);
        
        BlockPos searchPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(searchPos, 10);
        
        assertEquals(2, nearby.size());
        assertEquals(villager2, nearby.get(0)); // Closer villager first
        assertEquals(villager1, nearby.get(1)); // Farther villager second
    }

    @Test
    @DisplayName("Should handle villager position updates")
    void shouldHandleVillagerPositionUpdates() {
        proximityDetector.updateVillagerPosition(villager1);
        
        // Move villager to new position
        when(villager1.getBlockPos()).thenReturn(new BlockPos(50, 64, 50));
        proximityDetector.updateVillagerPosition(villager1);
        
        // Should find villager at new position
        BlockPos searchPos = new BlockPos(45, 64, 45);
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(searchPos, 10);
        
        assertEquals(1, nearby.size());
        assertEquals(villager1, nearby.get(0));
        
        // Should not find villager at old position
        BlockPos oldSearchPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> oldNearby = proximityDetector.findNearbyVillagers(oldSearchPos, 10);
        assertTrue(oldNearby.isEmpty());
    }

    @Test
    @DisplayName("Should remove villagers from index")
    void shouldRemoveVillagersFromIndex() {
        proximityDetector.updateVillagerPosition(villager1);
        proximityDetector.removeVillager(villager1);
        
        BlockPos searchPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(searchPos, 10);
        
        assertTrue(nearby.isEmpty());
    }

    @Test
    @DisplayName("Should handle Player-based search")
    void shouldHandlePlayerBasedSearch() {
        proximityDetector.updateVillagerPosition(villager1);
        
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(player, 10.0);
        
        assertEquals(1, nearby.size());
        assertEquals(villager1, nearby.get(0));
    }

    @Test
    @DisplayName("Should get villagers in conversation range")
    void shouldGetVillagersInConversationRange() {
        proximityDetector.updateVillagerPosition(villager1);
        proximityDetector.updateVillagerPosition(villager2);
        
        BlockPos searchPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> inRange = proximityDetector.getVillagersInConversationRange(searchPos);
        
        assertEquals(1, inRange.size());
        assertEquals(villager1, inRange.get(0));
    }

    @Test
    @DisplayName("Should detect conversation overhearing")
    void shouldDetectConversationOverhearing() {
        proximityDetector.updateVillagerPosition(villager1);
        proximityDetector.updateVillagerPosition(villager2);
        
        BlockPos conversationPos = new BlockPos(0, 64, 0);
        
        assertTrue(proximityDetector.canOverhearConversation(villager1, conversationPos));
        assertFalse(proximityDetector.canOverhearConversation(villager2, conversationPos)); // Too far
    }

    @Test
    @DisplayName("Should provide index statistics")
    void shouldProvideIndexStatistics() {
        proximityDetector.updateVillagerPosition(villager1);
        proximityDetector.updateVillagerPosition(villager2);
        
        Map<String, Object> stats = proximityDetector.getIndexStats();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("gridCells"));
        assertTrue(stats.containsKey("totalVillagers"));
        assertTrue(stats.containsKey("averageVillagersPerCell"));
        assertEquals(2, stats.get("totalVillagers"));
    }

    @Test
    @DisplayName("Should clear spatial index")
    void shouldClearSpatialIndex() {
        proximityDetector.updateVillagerPosition(villager1);
        proximityDetector.updateVillagerPosition(villager2);
        
        proximityDetector.clearIndex();
        
        BlockPos searchPos = new BlockPos(0, 64, 0);
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(searchPos, 100);
        
        assertTrue(nearby.isEmpty());
        
        Map<String, Object> stats = proximityDetector.getIndexStats();
        assertEquals(0, stats.get("totalVillagers"));
    }

    @Test
    @DisplayName("Should handle edge cases gracefully")
    void shouldHandleEdgeCasesGracefully() {
        // Test with null villager (should not crash)
        assertDoesNotThrow(() -> proximityDetector.removeVillager(null));
        
        // Test with zero radius
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(new BlockPos(0, 64, 0), 0);
        assertNotNull(nearby);
        
        // Test with negative radius
        nearby = proximityDetector.findNearbyVillagers(new BlockPos(0, 64, 0), -1);
        assertNotNull(nearby);
    }

    @Test
    @DisplayName("Should handle large coordinates")
    void shouldHandleLargeCoordinates() {
        VillagerEntity farVillager = mock(VillagerEntity.class);
        when(farVillager.getUUID()).thenReturn(UUID.randomUUID());
        when(farVillager.getBlockPos()).thenReturn(new BlockPos(1000000, 64, 1000000));
        
        proximityDetector.updateVillagerPosition(farVillager);
        
        BlockPos searchPos = new BlockPos(1000005, 64, 1000005);
        List<VillagerEntity> nearby = proximityDetector.findNearbyVillagers(searchPos, 10);
        
        assertEquals(1, nearby.size());
        assertEquals(farVillager, nearby.get(0));
    }
}