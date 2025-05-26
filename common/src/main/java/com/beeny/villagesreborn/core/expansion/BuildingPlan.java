package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.common.BlockPos;

/**
 * Represents a planned building in an expansion proposal
 */
public class BuildingPlan {
    
    public enum BuildingType {
        HOUSE,
        WORKSHOP,
        FARM,
        DEFENSE_TOWER,
        MARKET,
        STORAGE,
        ROAD,
        DECORATION
    }
    
    private final BuildingType type;
    private final BlockPos targetPosition;
    private final int priority;
    private final ResourceCost cost;
    
    public BuildingPlan(BuildingType type, BlockPos targetPosition, int priority, ResourceCost cost) {
        this.type = type;
        this.targetPosition = targetPosition;
        this.priority = priority;
        this.cost = cost;
    }
    
    public BuildingType getType() {
        return type;
    }
    
    public BlockPos getTargetPosition() {
        return targetPosition;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public ResourceCost getCost() {
        return cost;
    }
    
    @Override
    public String toString() {
        return String.format("BuildingPlan{type=%s, position=%s, priority=%d}", 
                           type, targetPosition, priority);
    }
}