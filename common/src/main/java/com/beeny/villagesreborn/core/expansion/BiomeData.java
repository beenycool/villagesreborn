package com.beeny.villagesreborn.core.expansion;

import java.util.List;
import java.util.Map;

/**
 * Represents biome-specific data for expansion planning
 */
public class BiomeData {
    
    public enum BiomeType {
        PLAINS,
        FOREST,
        DESERT,
        MOUNTAIN,
        SWAMP,
        TUNDRA,
        JUNGLE,
        OCEAN,
        NETHER,
        END
    }
    
    public enum ClimateType {
        TEMPERATE,
        HOT,
        COLD,
        WET,
        DRY,
        EXTREME
    }
    
    private final BiomeType biomeType;
    private final ClimateType climate;
    private final Map<String, Float> resourceAvailability;
    private final List<String> buildingRestrictions;
    private final float expansionDifficulty;
    private final List<String> naturalHazards;
    
    public BiomeData(BiomeType biomeType,
                    ClimateType climate,
                    Map<String, Float> resourceAvailability,
                    List<String> buildingRestrictions,
                    float expansionDifficulty,
                    List<String> naturalHazards) {
        this.biomeType = biomeType;
        this.climate = climate;
        this.resourceAvailability = resourceAvailability;
        this.buildingRestrictions = buildingRestrictions;
        this.expansionDifficulty = expansionDifficulty;
        this.naturalHazards = naturalHazards;
    }
    
    public BiomeType getBiomeType() {
        return biomeType;
    }
    
    public ClimateType getClimate() {
        return climate;
    }
    
    public Map<String, Float> getResourceAvailability() {
        return resourceAvailability;
    }
    
    public List<String> getBuildingRestrictions() {
        return buildingRestrictions;
    }
    
    public float getExpansionDifficulty() {
        return expansionDifficulty;
    }
    
    public List<String> getNaturalHazards() {
        return naturalHazards;
    }
    
    public float getResourceAvailability(String resource) {
        return resourceAvailability.getOrDefault(resource, 0.0f);
    }
    
    public boolean hasResource(String resource) {
        return getResourceAvailability(resource) > 0.0f;
    }
    
    public boolean allowsBuilding(String buildingType) {
        return !buildingRestrictions.contains(buildingType);
    }
    
    public boolean hasHazard(String hazard) {
        return naturalHazards.contains(hazard);
    }
    
    @Override
    public String toString() {
        return String.format("BiomeData{type=%s, climate=%s, difficulty=%.2f, hazards=%d}",
                           biomeType, climate, expansionDifficulty, naturalHazards.size());
    }
}