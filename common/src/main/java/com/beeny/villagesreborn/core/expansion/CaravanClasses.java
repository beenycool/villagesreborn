package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.common.BlockPos;
import java.util.List;

// Village Coordinate
class VillageCoordinate {
    private final BlockPos position;
    private final String name;
    
    public VillageCoordinate(BlockPos position, String name) {
        this.position = position;
        this.name = name;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public String getName() {
        return name;
    }
}

// Pathfinding Engine Interface
interface PathfindingEngine {
    List<BlockPos> findPath(BlockPos start, BlockPos end, PathfindingOptions options);
    List<BlockPos> findAlternativePath(BlockPos start, BlockPos end, PathfindingOptions options);
}

// Terrain Analyzer Interface
interface TerrainAnalyzer {
    boolean isPathTraversable(BlockPos from, BlockPos to);
}

// Pathfinding Options
class PathfindingOptions {
    private final double maxSlopeAngle;
    private final int minimumWidth;
    private final boolean avoidWater;
    private final boolean preferRoads;
    
    private PathfindingOptions(Builder builder) {
        this.maxSlopeAngle = builder.maxSlopeAngle;
        this.minimumWidth = builder.minimumWidth;
        this.avoidWater = builder.avoidWater;
        this.preferRoads = builder.preferRoads;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public double getMaxSlopeAngle() {
        return maxSlopeAngle;
    }
    
    public int getMinimumWidth() {
        return minimumWidth;
    }
    
    public boolean shouldAvoidWater() {
        return avoidWater;
    }
    
    public boolean shouldPreferRoads() {
        return preferRoads;
    }
    
    public static class Builder {
        private double maxSlopeAngle = 45.0;
        private int minimumWidth = 2;
        private boolean avoidWater = false;
        private boolean preferRoads = false;
        
        public Builder maxSlopeAngle(double maxSlopeAngle) {
            this.maxSlopeAngle = maxSlopeAngle;
            return this;
        }
        
        public Builder minimumWidth(int minimumWidth) {
            this.minimumWidth = minimumWidth;
            return this;
        }
        
        public Builder avoidWater(boolean avoidWater) {
            this.avoidWater = avoidWater;
            return this;
        }
        
        public Builder preferRoads(boolean preferRoads) {
            this.preferRoads = preferRoads;
            return this;
        }
        
        public PathfindingOptions build() {
            return new PathfindingOptions(this);
        }
    }
}

// Caravan Movement Profile
class CaravanMovementProfile {
    private final double maxSlopeAngle;
    private final int minimumPathWidth;
    private final boolean avoidWater;
    private final boolean preferRoads;
    
    private CaravanMovementProfile(Builder builder) {
        this.maxSlopeAngle = builder.maxSlopeAngle;
        this.minimumPathWidth = builder.minimumPathWidth;
        this.avoidWater = builder.avoidWater;
        this.preferRoads = builder.preferRoads;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public double getMaxSlopeAngle() {
        return maxSlopeAngle;
    }
    
    public int getMinimumPathWidth() {
        return minimumPathWidth;
    }
    
    public boolean shouldAvoidWater() {
        return avoidWater;
    }
    
    public boolean shouldPreferRoads() {
        return preferRoads;
    }
    
    public static class Builder {
        private double maxSlopeAngle = 30.0;
        private int minimumPathWidth = 2;
        private boolean avoidWater = true;
        private boolean preferRoads = true;
        
        public Builder maxSlopeAngle(double maxSlopeAngle) {
            this.maxSlopeAngle = maxSlopeAngle;
            return this;
        }
        
        public Builder minimumPathWidth(int minimumPathWidth) {
            this.minimumPathWidth = minimumPathWidth;
            return this;
        }
        
        public Builder avoidWater(boolean avoidWater) {
            this.avoidWater = avoidWater;
            return this;
        }
        
        public Builder preferRoads(boolean preferRoads) {
            this.preferRoads = preferRoads;
            return this;
        }
        
        public CaravanMovementProfile build() {
            return new CaravanMovementProfile(this);
        }
    }
}