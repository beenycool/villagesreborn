package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.common.BlockPos;
import java.util.List;
import java.util.Map;

/**
 * Represents terrain analysis data for expansion planning
 */
public class TerrainData {
    
    public enum TerrainType {
        FLAT,
        HILLY,
        MOUNTAINOUS,
        WATER,
        FOREST,
        DESERT,
        SWAMP
    }
    
    public enum SuitabilityLevel {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        UNSUITABLE
    }
    
    private final Map<TerrainType, Float> terrainComposition;
    private final List<BlockPos> buildableSites;
    private final List<BlockPos> resourceLocations;
    private final SuitabilityLevel overallSuitability;
    private final float elevationVariance;
    
    public TerrainData(Map<TerrainType, Float> terrainComposition,
                      List<BlockPos> buildableSites,
                      List<BlockPos> resourceLocations,
                      SuitabilityLevel overallSuitability,
                      float elevationVariance) {
        this.terrainComposition = terrainComposition;
        this.buildableSites = buildableSites;
        this.resourceLocations = resourceLocations;
        this.overallSuitability = overallSuitability;
        this.elevationVariance = elevationVariance;
    }
    
    public Map<TerrainType, Float> getTerrainComposition() {
        return terrainComposition;
    }
    
    public List<BlockPos> getBuildableSites() {
        return buildableSites;
    }
    
    public List<BlockPos> getResourceLocations() {
        return resourceLocations;
    }
    
    public SuitabilityLevel getOverallSuitability() {
        return overallSuitability;
    }
    
    public float getElevationVariance() {
        return elevationVariance;
    }
    
    public float getTerrainPercentage(TerrainType type) {
        return terrainComposition.getOrDefault(type, 0.0f);
    }
    
    public boolean hasTerrainType(TerrainType type) {
        return getTerrainPercentage(type) > 0.0f;
    }
    
    @Override
    public String toString() {
        return String.format("TerrainData{suitability=%s, buildable=%d, resources=%d, elevation=%.2f}",
                           overallSuitability, buildableSites.size(), resourceLocations.size(), elevationVariance);
    }
}