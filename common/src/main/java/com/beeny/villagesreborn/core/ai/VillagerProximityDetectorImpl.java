package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced VillagerProximityDetector with spatial indexing for efficient proximity queries.
 * This implementation uses a grid-based spatial index to quickly find nearby villagers,
 * minimizing performance impact from frequent proximity checks. It is optimized for scenarios
 * with a high density of villagers.
 */
public class VillagerProximityDetectorImpl implements VillagerProximityDetector {
    
    private static final int GRID_SIZE = 16; // 16x16 block grid cells
    private final Map<String, Set<VillagerEntity>> spatialGrid = new HashMap<>();
    private final Map<UUID, String> villagerGridKeys = new ConcurrentHashMap<>();
    
    /**
     * Updates the spatial index with a villager's current position.
     * This method is optimized to avoid redundant lookups and ensure villagers are correctly
     * positioned in the spatial grid.
     *
     * @param villager The villager whose position is being updated.
     */
    public void updateVillagerPosition(VillagerEntity villager) {
        String oldGridKey = villagerGridKeys.get(villager.getUUID());
        String newGridKey = getGridKey(villager.getBlockPos());

        if (Objects.equals(oldGridKey, newGridKey)) {
            return; // No need to update if the villager hasn't changed grid cells
        }

        // Remove from old grid cell
        if (oldGridKey != null) {
            spatialGrid.computeIfPresent(oldGridKey, (key, villagers) -> {
                villagers.remove(villager);
                return villagers.isEmpty() ? null : villagers;
            });
        }

        // Add to new grid cell and update the key mapping
        spatialGrid.computeIfAbsent(newGridKey, k -> new HashSet<>()).add(villager);
        villagerGridKeys.put(villager.getUUID(), newGridKey);
    }
    
    /**
     * Removes a villager from the spatial index.
     *
     * @param villager The villager to remove.
     */
    public void removeVillager(VillagerEntity villager) {
        if (villager == null) {
            return;
        }
        String gridKey = villagerGridKeys.remove(villager.getUUID());
        if (gridKey != null) {
            spatialGrid.computeIfPresent(gridKey, (key, villagers) -> {
                villagers.remove(villager);
                return villagers.isEmpty() ? null : villagers;
            });
        }
    }
    
    @Override
    public List<VillagerEntity> findNearbyVillagers(BlockPos location, int radius) {
        return findNearbyVillagers(location, radius, true);
    }
    
    /**
     * Finds nearby villagers with an option to sort by distance.
     *
     * @param location The center of the search area.
     * @param radius   The search radius.
     * @param sorted   Whether to sort the results by distance.
     * @return A list of nearby villagers.
     */
    public List<VillagerEntity> findNearbyVillagers(BlockPos location, int radius, boolean sorted) {
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
        
        // Filter by actual distance and map to a custom object with distance squared
        List<VillagerDistance> villagersWithDistance = candidates.stream()
                .map(villager -> {
                    double distSq = getDistanceSq(location, villager.getBlockPos());
                    return new VillagerDistance(villager, distSq);
                })
                .filter(vd -> vd.distanceSq <= radius * radius)
                .collect(Collectors.toList());

        if (sorted) {
            // Sort by distance squared (faster than sorting by distance)
            villagersWithDistance.sort(Comparator.comparingDouble(vd -> vd.distanceSq));
        }

        return villagersWithDistance.stream()
                .map(vd -> vd.villager)
                .collect(Collectors.toList());
    }
    
    /**
     * Finds nearby villagers for a player, with optimized performance.
     *
     * @param player The player to search around.
     * @param range  The search range.
     * @return A list of nearby villagers.
     */
    public List<VillagerEntity> findNearbyVillagers(com.beeny.villagesreborn.core.common.Player player, double range) {
        return findNearbyVillagers(player.getBlockPos(), (int) Math.ceil(range), true);
    }
    
    /**
     * Gets all villagers within conversation range, sorted by distance.
     *
     * @param location The location to check from.
     * @return A sorted list of villagers within conversation range.
     */
    public List<VillagerEntity> getVillagersInConversationRange(BlockPos location) {
        return findNearbyVillagers(location, 8, true); // Standard conversation range
    }
    
    /**
     * Checks if a villager is within range for overhearing conversations.
     * This uses a squared-distance check for performance.
     *
     * @param villager             The villager who might overhear.
     * @param conversationLocation The location of the conversation.
     * @return True if the villager can overhear, false otherwise.
     */
    public boolean canOverhearConversation(VillagerEntity villager, BlockPos conversationLocation) {
        return isWithinRadius(conversationLocation, villager.getBlockPos(), 16);
    }
    
    private void removeFromGrid(VillagerEntity villager) {
        String gridKey = villagerGridKeys.remove(villager.getUUID());
        if (gridKey != null) {
            Set<VillagerEntity> villagers = spatialGrid.get(gridKey);
            if (villagers != null) {
                villagers.remove(villager);
                if (villagers.isEmpty()) {
                    spatialGrid.remove(gridKey);
                }
            }
        }
    }
    
    private String getGridKey(BlockPos pos) {
        int gridX = pos.getX() / GRID_SIZE;
        int gridZ = pos.getZ() / GRID_SIZE;
        return gridX + "," + gridZ;
    }
    
    private boolean isWithinRadius(BlockPos center, BlockPos target, int radius) {
        return getDistanceSq(center, target) <= radius * radius;
    }
    
    private double getDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(getDistanceSq(pos1, pos2));
    }
    
    private double getDistanceSq(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dz = pos1.getZ() - pos2.getZ();
        return dx * dx + dz * dz;
    }
    
    /**
     * Gets statistics about the spatial index.
     *
     * @return A map containing statistics about the spatial index.
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
     * Clears the spatial index (for testing or world unload).
     */
    public void clearIndex() {
        spatialGrid.clear();
        villagerGridKeys.clear();
    }

    /**
     * A helper class to store a villager and their distance squared from a location.
     */
    private static class VillagerDistance {
        final VillagerEntity villager;
        final double distanceSq;

        VillagerDistance(VillagerEntity villager, double distanceSq) {
            this.villager = villager;
            this.distanceSq = distanceSq;
        }
    }
}