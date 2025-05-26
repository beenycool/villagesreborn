package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced VillagerProximityDetector with spatial indexing for efficient proximity queries
 * Uses a grid-based spatial index to quickly find nearby villagers
 */
public class VillagerProximityDetectorImpl implements VillagerProximityDetector {
    
    private static final int GRID_SIZE = 16; // 16x16 block grid cells
    private final Map<String, Set<VillagerEntity>> spatialGrid = new ConcurrentHashMap<>();
    
    /**
     * Updates the spatial index with a villager's current position
     */
    public void updateVillagerPosition(VillagerEntity villager) {
        // Remove from old grid cell
        removeFromGrid(villager);
        
        // Add to new grid cell
        String gridKey = getGridKey(villager.getBlockPos());
        spatialGrid.computeIfAbsent(gridKey, k -> ConcurrentHashMap.newKeySet()).add(villager);
    }
    
    /**
     * Removes a villager from the spatial index
     */
    public void removeVillager(VillagerEntity villager) {
        removeFromGrid(villager);
    }
    
    @Override
    public List<VillagerEntity> findNearbyVillagers(BlockPos location, int radius) {
        Set<VillagerEntity> candidates = new HashSet<>();
        
        // Calculate grid bounds to search
        int minGridX = (location.getX() - radius) / GRID_SIZE;
        int maxGridX = (location.getX() + radius) / GRID_SIZE;
        int minGridZ = (location.getZ() - radius) / GRID_SIZE;
        int maxGridZ = (location.getZ() + radius) / GRID_SIZE;
        
        // Collect candidates from relevant grid cells
        for (int gridX = minGridX; gridX <= maxGridX; gridX++) {
            for (int gridZ = minGridZ; gridZ <= maxGridZ; gridZ++) {
                String gridKey = gridX + "," + gridZ;
                Set<VillagerEntity> cellVillagers = spatialGrid.get(gridKey);
                if (cellVillagers != null) {
                    candidates.addAll(cellVillagers);
                }
            }
        }
        
        // Filter by actual distance
        return candidates.stream()
            .filter(villager -> isWithinRadius(location, villager.getBlockPos(), radius))
            .sorted((v1, v2) -> {
                double dist1 = getDistance(location, v1.getBlockPos());
                double dist2 = getDistance(location, v2.getBlockPos());
                return Double.compare(dist1, dist2);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Enhanced proximity detection for players (using Player.getBlockPos())
     */
    public List<VillagerEntity> findNearbyVillagers(com.beeny.villagesreborn.core.common.Player player, double range) {
        return findNearbyVillagers(player.getBlockPos(), (int) Math.ceil(range));
    }
    
    /**
     * Gets all villagers within conversation range, sorted by distance
     */
    public List<VillagerEntity> getVillagersInConversationRange(BlockPos location) {
        return findNearbyVillagers(location, 8); // Standard conversation range
    }
    
    /**
     * Checks if a villager is within range for overhearing conversations
     */
    public boolean canOverhearConversation(VillagerEntity villager, BlockPos conversationLocation) {
        return isWithinRadius(conversationLocation, villager.getBlockPos(), 16);
    }
    
    private void removeFromGrid(VillagerEntity villager) {
        spatialGrid.values().forEach(set -> set.remove(villager));
    }
    
    private String getGridKey(BlockPos pos) {
        int gridX = pos.getX() / GRID_SIZE;
        int gridZ = pos.getZ() / GRID_SIZE;
        return gridX + "," + gridZ;
    }
    
    private boolean isWithinRadius(BlockPos center, BlockPos target, int radius) {
        return getDistance(center, target) <= radius;
    }
    
    private double getDistance(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Gets statistics about the spatial index
     */
    public Map<String, Object> getIndexStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("gridCells", spatialGrid.size());
        stats.put("totalVillagers", spatialGrid.values().stream().mapToInt(Set::size).sum());
        stats.put("averageVillagersPerCell", 
            spatialGrid.isEmpty() ? 0 : 
            spatialGrid.values().stream().mapToInt(Set::size).average().orElse(0));
        return stats;
    }
    
    /**
     * Clears the spatial index (for testing)
     */
    public void clearIndex() {
        spatialGrid.clear();
    }
}