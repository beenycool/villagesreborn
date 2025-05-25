package com.beeny.villagesreborn.core.expansion;

public class BuildingPlacement {
    private final String buildingType;
    private final long tick;
    
    public BuildingPlacement(String buildingType, long tick) {
        this.buildingType = buildingType;
        this.tick = tick;
    }
    
    public String getBuildingType() {
        return buildingType;
    }
    
    public long getTick() {
        return tick;
    }
}