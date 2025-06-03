package com.beeny.villagesreborn.core.expansion.entities;

import com.beeny.villagesreborn.core.expansion.EntityAttributes;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

/**
 * End-adapted villager entity with teleportation abilities and unique AI behaviors
 */
public class EndVillager {
    private final UUID villagerID;
    private final EntityAttributes attributes;
    private final EndBehaviorProfile behaviorProfile;
    
    // End-specific properties
    private double teleportationEnergy;
    private int enderPearlMasteryLevel;
    private boolean hasChorusHeart;
    private List<String> knownEndCities;
    
    public EndVillager(UUID villagerID) {
        this.villagerID = villagerID;
        this.attributes = createEndAttributes();
        this.behaviorProfile = new EndBehaviorProfile();
        this.teleportationEnergy = 100.0;
        this.enderPearlMasteryLevel = 0;
        this.hasChorusHeart = false;
        this.knownEndCities = new ArrayList<>();
    }
    
    /**
     * Creates End-optimized entity attributes
     */
    private EntityAttributes createEndAttributes() {
        return EntityAttributes.builder()
            .maxHealth(25.0) // Adapted to void environment
            .movementSpeed(0.35) // Faster due to teleportation abilities
            .fireResistance(false) // No fire in The End
            .teleportResistance(true) // Masters of teleportation
            .build();
    }
    
    /**
     * AI-powered teleportation system with energy management
     */
    public boolean attemptTeleportation(double distance) {
        double energyCost = distance * 2.0;
        
        if (teleportationEnergy >= energyCost) {
            teleportationEnergy -= energyCost;
            enderPearlMasteryLevel += 1;
            
            // Advanced teleportation unlocks chorus heart evolution
            if (enderPearlMasteryLevel > 200 && Math.random() < 0.05) {
                hasChorusHeart = true;
                teleportationEnergy = 200.0; // Increased energy capacity
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * AI behavior for End city exploration and mapping
     */
    public void exploreEndCity(String cityCoordinates) {
        if (!knownEndCities.contains(cityCoordinates)) {
            knownEndCities.add(cityCoordinates);
            enderPearlMasteryLevel += 10; // Significant experience gain
            
            // Chance to discover chorus heart during exploration
            if (knownEndCities.size() > 5 && Math.random() < 0.15) {
                hasChorusHeart = true;
            }
        }
    }
    
    /**
     * AI-powered trading specializing in End resources
     */
    public boolean canTradeEndResource(String resourceType) {
        return switch (resourceType.toLowerCase()) {
            case "ender_pearl", "end_stone" -> true;
            case "chorus_fruit", "chorus_flower" -> enderPearlMasteryLevel > 20;
            case "purpur_block", "purpur_pillar" -> knownEndCities.size() > 2;
            case "elytra", "shulker_shell" -> hasChorusHeart && knownEndCities.size() > 5;
            case "dragon_egg", "dragon_head" -> hasChorusHeart && enderPearlMasteryLevel > 500;
            default -> false;
        };
    }
    
    /**
     * AI pathfinding for End terrain and void navigation
     */
    public boolean canNavigateEndTerrain(String terrainType) {
        return switch (terrainType) {
            case "void_edges" -> hasChorusHeart; // Chorus heart prevents void damage
            case "end_highlands" -> teleportationEnergy > 20;
            case "end_islands" -> enderPearlMasteryLevel > 50;
            case "chorus_forest" -> true; // Natural habitat
            case "end_city_platform" -> knownEndCities.size() > 0;
            default -> teleportationEnergy > 10;
        };
    }
    
    /**
     * Regenerates teleportation energy over time
     */
    public void regenerateEnergy() {
        double regenRate = hasChorusHeart ? 5.0 : 2.0;
        double maxEnergy = hasChorusHeart ? 200.0 : 100.0;
        
        teleportationEnergy = Math.min(maxEnergy, teleportationEnergy + regenRate);
    }
    
    /**
     * AI decision making for chorus fruit consumption
     */
    public boolean shouldConsumeChorusFruit() {
        return teleportationEnergy < 30.0 || (hasChorusHeart && Math.random() < 0.1);
    }
    
    // Getters
    public UUID getVillagerID() { return villagerID; }
    public EntityAttributes getAttributes() { return attributes; }
    public double getTeleportationEnergy() { return teleportationEnergy; }
    public int getEnderPearlMasteryLevel() { return enderPearlMasteryLevel; }
    public boolean hasChorusHeart() { return hasChorusHeart; }
    public List<String> getKnownEndCities() { return new ArrayList<>(knownEndCities); }
    public EndBehaviorProfile getBehaviorProfile() { return behaviorProfile; }
    
    /**
     * End-specific behavior profile for AI decision making
     */
    public static class EndBehaviorProfile {
        private double curiosityLevel = 0.9; // Extremely curious explorers
        private double voidFearLevel = 0.7; // Healthy respect for the void
        private double isolationTolerance = 0.8; // Comfortable in solitude
        private double territorialInstinct = 0.3; // Less territorial than other dimensions
        
        public double getCuriosityLevel() { return curiosityLevel; }
        public double getVoidFearLevel() { return voidFearLevel; }
        public double getIsolationTolerance() { return isolationTolerance; }
        public double getTerritorialInstinct() { return territorialInstinct; }
        
        public void evolveForChorusHeart() {
            curiosityLevel = 0.95;
            voidFearLevel = 0.2; // Significantly reduced void fear
            isolationTolerance = 0.9;
            territorialInstinct = 0.1;
        }
    }
}