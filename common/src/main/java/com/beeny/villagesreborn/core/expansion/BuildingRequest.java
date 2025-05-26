package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.common.BlockPos;

import java.util.List;

/**
 * Represents a request to build a specific type of structure
 */
public class BuildingRequest {
    private final String buildingType;
    private final int quantity;
    private final BuildingPriority priority;
    private final BlockPos preferredLocation;
    private final List<String> requirements;
    
    public BuildingRequest(String buildingType, int quantity, BuildingPriority priority) {
        this(buildingType, quantity, priority, null, List.of());
    }
    
    public BuildingRequest(String buildingType, int quantity, BuildingPriority priority, 
                          BlockPos preferredLocation, List<String> requirements) {
        this.buildingType = buildingType;
        this.quantity = quantity;
        this.priority = priority;
        this.preferredLocation = preferredLocation;
        this.requirements = requirements;
    }
    
    public String getBuildingType() {
        return buildingType;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public BuildingPriority getPriority() {
        return priority;
    }
    
    public BlockPos getPreferredLocation() {
        return preferredLocation;
    }
    
    public List<String> getRequirements() {
        return requirements;
    }
    
    @Override
    public String toString() {
        return String.format("BuildingRequest{type='%s', quantity=%d, priority=%s}", 
                           buildingType, quantity, priority);
    }
}