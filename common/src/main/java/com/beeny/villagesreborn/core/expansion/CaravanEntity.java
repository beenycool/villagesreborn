package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.common.BlockPos;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CaravanEntity {
    private final World world;
    private final PathfindingEngine pathfinder;
    private final TerrainAnalyzer terrainAnalyzer;
    private CaravanMovementProfile movementProfile;
    private final Map<String, List<BlockPos>> pathCache = new HashMap<>();
    
    public CaravanEntity(World world, PathfindingEngine pathfinder, TerrainAnalyzer terrainAnalyzer) {
        this.world = world;
        this.pathfinder = pathfinder;
        this.terrainAnalyzer = terrainAnalyzer;
        this.movementProfile = CaravanMovementProfile.builder().build();
    }
    
    public List<BlockPos> computePath(VillageCoordinate source, VillageCoordinate target) {
        String cacheKey = source.getName() + "->" + target.getName();
        
        if (pathCache.containsKey(cacheKey)) {
            return pathCache.get(cacheKey);
        }
        
        PathfindingOptions options = PathfindingOptions.builder()
                .maxSlopeAngle(movementProfile.getMaxSlopeAngle())
                .minimumWidth(movementProfile.getMinimumPathWidth())
                .avoidWater(movementProfile.shouldAvoidWater())
                .preferRoads(movementProfile.shouldPreferRoads())
                .build();
        
        List<BlockPos> path = pathfinder.findPath(source.getPosition(), target.getPosition(), options);
        
        if (path == null || path.isEmpty()) {
            // Try fallback pathfinding
            path = pathfinder.findAlternativePath(source.getPosition(), target.getPosition(), options);
        }
        
        if (path == null) {
            return List.of(); // Return empty list for unreachable destination
        }
        
        pathCache.put(cacheKey, path);
        return path;
    }
    
    public boolean validatePath(List<BlockPos> path) {
        if (path.isEmpty()) return false;
        
        for (int i = 0; i < path.size() - 1; i++) {
            BlockPos current = path.get(i);
            BlockPos next = path.get(i + 1);
            
            if (!terrainAnalyzer.isPathTraversable(current, next)) {
                return false;
            }
        }
        
        return true;
    }
    
    public void setMovementProfile(CaravanMovementProfile profile) {
        this.movementProfile = profile;
    }
    
    public void invalidatePathCache() {
        pathCache.clear();
    }
}