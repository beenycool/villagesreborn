package com.beeny.villagesreborn.core.expansion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.beeny.villagesreborn.core.common.BlockPos;
import java.util.List;
import java.util.Arrays;

class CaravanPathfindingTests {
    
    @Mock
    private World mockWorld;
    
    @Mock
    private PathfindingEngine mockPathfinder;
    
    @Mock
    private TerrainAnalyzer mockTerrainAnalyzer;
    
    private CaravanEntity caravanEntity;
    private VillageCoordinate sourceVillage;
    private VillageCoordinate targetVillage;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        caravanEntity = new CaravanEntity(mockWorld, mockPathfinder, mockTerrainAnalyzer);
        sourceVillage = new VillageCoordinate(new BlockPos(100, 64, 100), "SourceVillage");
        targetVillage = new VillageCoordinate(new BlockPos(300, 64, 200), "TargetVillage");
    }
    
    @Test
    @DisplayName("Should compute valid path between two village coordinates")
    void testValidPathComputation() {
        // Given: two villages with clear path between them
        List<BlockPos> expectedPath = Arrays.asList(
                new BlockPos(100, 64, 100), // Start
                new BlockPos(150, 64, 125),
                new BlockPos(200, 64, 150),
                new BlockPos(250, 64, 175),
                new BlockPos(300, 64, 200)  // End
        );
        
        when(mockPathfinder.findPath(
                eq(sourceVillage.getPosition()),
                eq(targetVillage.getPosition()),
                any(PathfindingOptions.class)
        )).thenReturn(expectedPath);
        
        when(mockTerrainAnalyzer.isPathTraversable(any(), any())).thenReturn(true);
        
        // When: computing path between villages
        List<BlockPos> computedPath = caravanEntity.computePath(sourceVillage, targetVillage);
        
        // Then: path should be valid and complete
        assertNotNull(computedPath, "Path should not be null");
        assertFalse(computedPath.isEmpty(), "Path should not be empty");
        assertEquals(expectedPath.size(), computedPath.size(), "Path should have expected length");
        assertEquals(sourceVillage.getPosition(), computedPath.get(0), "Path should start at source village");
        assertEquals(targetVillage.getPosition(), computedPath.get(computedPath.size() - 1), "Path should end at target village");
    }
    
    @Test
    @DisplayName("Should validate path segments for traversability")
    void testPathTraversabilityValidation() {
        // Given: path with some non-traversable segments
        List<BlockPos> proposedPath = Arrays.asList(
                new BlockPos(100, 64, 100),
                new BlockPos(150, 64, 125), // Traversable
                new BlockPos(200, 100, 150), // High elevation - non-traversable
                new BlockPos(250, 64, 175),
                new BlockPos(300, 64, 200)
        );
        
        when(mockPathfinder.findPath(any(), any(), any())).thenReturn(proposedPath);
        when(mockTerrainAnalyzer.isPathTraversable(
                eq(new BlockPos(100, 64, 100)),
                eq(new BlockPos(150, 64, 125))
        )).thenReturn(true);
        when(mockTerrainAnalyzer.isPathTraversable(
                eq(new BlockPos(150, 64, 125)),
                eq(new BlockPos(200, 100, 150))
        )).thenReturn(false); // Blocked segment
        
        // When: validating path
        boolean isValid = caravanEntity.validatePath(proposedPath);
        
        // Then: path should be marked as invalid
        assertFalse(isValid, "Path with non-traversable segments should be invalid");
    }
    
    @Test
    @DisplayName("Should trigger fallback behavior when primary path is blocked")
    void testBlockedPathFallbackBehavior() {
        // Given: primary path is blocked
        when(mockPathfinder.findPath(
                eq(sourceVillage.getPosition()),
                eq(targetVillage.getPosition()),
                any(PathfindingOptions.class)
        )).thenReturn(null); // No path found
        
        // Fallback path using different routing
        List<BlockPos> fallbackPath = Arrays.asList(
                new BlockPos(100, 64, 100),
                new BlockPos(120, 64, 80),  // Different route
                new BlockPos(180, 64, 120),
                new BlockPos(280, 64, 180),
                new BlockPos(300, 64, 200)
        );
        
        when(mockPathfinder.findAlternativePath(
                eq(sourceVillage.getPosition()),
                eq(targetVillage.getPosition()),
                any(PathfindingOptions.class)
        )).thenReturn(fallbackPath);
        
        // When: computing path with blocked primary route
        List<BlockPos> computedPath = caravanEntity.computePath(sourceVillage, targetVillage);
        
        // Then: fallback path should be used
        assertNotNull(computedPath, "Fallback path should be available");
        assertEquals(fallbackPath.size(), computedPath.size(), "Should use fallback path");
        
        // Verify fallback was attempted
        verify(mockPathfinder).findAlternativePath(any(), any(), any());
    }
    
    @Test
    @DisplayName("Should handle unreachable destinations gracefully")
    void testUnreachableDestinationHandling() {
        // Given: no path exists between villages
        when(mockPathfinder.findPath(any(), any(), any())).thenReturn(null);
        when(mockPathfinder.findAlternativePath(any(), any(), any())).thenReturn(null);
        
        // When: attempting to compute path to unreachable destination
        List<BlockPos> computedPath = caravanEntity.computePath(sourceVillage, targetVillage);
        
        // Then: should handle gracefully
        assertNotNull(computedPath, "Should return empty path rather than null");
        assertTrue(computedPath.isEmpty(), "Path should be empty for unreachable destination");
        
        // Should attempt both primary and fallback pathfinding
        verify(mockPathfinder).findPath(any(), any(), any());
        verify(mockPathfinder).findAlternativePath(any(), any(), any());
    }
    
    @Test
    @DisplayName("Should optimize path for caravan movement constraints")
    void testCaravanMovementOptimization() {
        // Given: caravan with specific movement constraints
        CaravanMovementProfile movementProfile = CaravanMovementProfile.builder()
                .maxSlopeAngle(30.0)
                .minimumPathWidth(3)
                .avoidWater(true)
                .preferRoads(true)
                .build();
        
        caravanEntity.setMovementProfile(movementProfile);
        
        List<BlockPos> optimizedPath = Arrays.asList(
                new BlockPos(100, 64, 100),
                new BlockPos(140, 65, 110), // Gentle slope
                new BlockPos(180, 66, 120),
                new BlockPos(220, 67, 140),
                new BlockPos(260, 68, 160),
                new BlockPos(300, 64, 200)
        );
        
        when(mockPathfinder.findPath(any(), any(), argThat(options ->
                options.getMaxSlopeAngle() == 30.0 &&
                options.getMinimumWidth() == 3 &&
                options.shouldAvoidWater() &&
                options.shouldPreferRoads()
        ))).thenReturn(optimizedPath);
        
        // When: computing path with movement constraints
        List<BlockPos> computedPath = caravanEntity.computePath(sourceVillage, targetVillage);
        
        // Then: path should respect movement constraints
        assertNotNull(computedPath);
        assertEquals(optimizedPath.size(), computedPath.size());
        
        // Verify pathfinding was called with correct options
        verify(mockPathfinder).findPath(any(), any(), argThat(options ->
                options.getMaxSlopeAngle() == 30.0
        ));
    }
    
    @Test
    @DisplayName("Should cache computed paths for performance")
    void testPathCaching() {
        // Given: computed path for first request
        List<BlockPos> cachedPath = Arrays.asList(
                new BlockPos(100, 64, 100),
                new BlockPos(200, 64, 150),
                new BlockPos(300, 64, 200)
        );
        
        when(mockPathfinder.findPath(any(), any(), any()))
                .thenReturn(cachedPath);
        
        // When: computing same path multiple times
        List<BlockPos> firstPath = caravanEntity.computePath(sourceVillage, targetVillage);
        List<BlockPos> secondPath = caravanEntity.computePath(sourceVillage, targetVillage);
        List<BlockPos> thirdPath = caravanEntity.computePath(sourceVillage, targetVillage);
        
        // Then: pathfinder should only be called once
        verify(mockPathfinder, times(1)).findPath(any(), any(), any());
        
        // And: all paths should be identical
        assertEquals(firstPath, secondPath, "Cached path should be identical");
        assertEquals(secondPath, thirdPath, "Cached path should be identical");
    }
    
    @Test
    @DisplayName("Should invalidate cache when world terrain changes")
    void testCacheInvalidationOnTerrainChange() {
        // Given: initial cached path
        List<BlockPos> initialPath = Arrays.asList(
                new BlockPos(100, 64, 100),
                new BlockPos(200, 64, 150),
                new BlockPos(300, 64, 200)
        );
        
        List<BlockPos> updatedPath = Arrays.asList(
                new BlockPos(100, 64, 100),
                new BlockPos(180, 64, 130), // Different route after terrain change
                new BlockPos(280, 64, 180),
                new BlockPos(300, 64, 200)
        );
        
        when(mockPathfinder.findPath(any(), any(), any()))
                .thenReturn(initialPath)
                .thenReturn(updatedPath);
        
        // When: computing path, then terrain changes, then computing again
        List<BlockPos> firstPath = caravanEntity.computePath(sourceVillage, targetVillage);
        caravanEntity.invalidatePathCache(); // Simulate terrain change
        List<BlockPos> secondPath = caravanEntity.computePath(sourceVillage, targetVillage);
        
        // Then: pathfinder should be called twice
        verify(mockPathfinder, times(2)).findPath(any(), any(), any());
        
        // And: paths should be different
        assertNotEquals(firstPath, secondPath, "Path should change after cache invalidation");
    }
}