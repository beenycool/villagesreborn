package com.beeny.villagesreborn.core.expansion;

import java.util.List;

/**
 * Represents a phase in village expansion planning
 */
public class ExpansionPhase {
    private final String name;
    private final List<BuildingRequest> buildings;
    private final int phaseNumber;
    private final String description;
    private final int estimatedDuration; // in game ticks or days
    
    public ExpansionPhase(String name, List<BuildingRequest> buildings, int phaseNumber) {
        this(name, buildings, phaseNumber, "", 0);
    }
    
    public ExpansionPhase(String name, List<BuildingRequest> buildings, int phaseNumber, 
                         String description, int estimatedDuration) {
        this.name = name;
        this.buildings = buildings;
        this.phaseNumber = phaseNumber;
        this.description = description;
        this.estimatedDuration = estimatedDuration;
    }
    
    public String getName() {
        return name;
    }
    
    public List<BuildingRequest> getBuildings() {
        return buildings;
    }
    
    public int getPhaseNumber() {
        return phaseNumber;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getEstimatedDuration() {
        return estimatedDuration;
    }
    
    public int getBuildingCount() {
        return buildings.stream().mapToInt(BuildingRequest::getQuantity).sum();
    }
    
    @Override
    public String toString() {
        return String.format("ExpansionPhase{name='%s', phase=%d, buildings=%d, duration=%d}", 
                           name, phaseNumber, getBuildingCount(), estimatedDuration);
    }
}