package com.beeny.villagesreborn.core.expansion.entities;

import com.beeny.villagesreborn.core.expansion.EntityAttributes;
import java.util.UUID;

/**
 * Nether-adapted villager entity with specialized AI behaviors and attributes
 */
public class NetherVillager {
    private final UUID villagerID;
    private final EntityAttributes attributes;
    private final NetherBehaviorProfile behaviorProfile;
    
    // Nether-specific properties
    private double lavaResistance;
    private int blazeRodTradingExperience;
    private boolean isWitherSkeleton;
    
    public NetherVillager(UUID villagerID) {
        this.villagerID = villagerID;
        this.attributes = createNetherAttributes();
        this.behaviorProfile = new NetherBehaviorProfile();
        this.lavaResistance = 100.0; // Full lava immunity
        this.blazeRodTradingExperience = 0;
        this.isWitherSkeleton = false;
    }
    
    /**
     * Creates Nether-optimized entity attributes
     */
    private EntityAttributes createNetherAttributes() {
        return EntityAttributes.builder()
            .maxHealth(30.0) // Higher health for harsh environment
            .movementSpeed(0.28) // Slightly faster than overworld villagers
            .fireResistance(true) // Essential for Nether survival
            .teleportResistance(false) // Can use nether portals
            .build();
    }
    
    /**
     * AI-powered trading behavior specialized for Nether resources
     */
    public boolean canTradeNetherResource(String resourceType) {
        return switch (resourceType.toLowerCase()) {
            case "blaze_rod", "nether_wart", "ghast_tear", "magma_cream" ->
                blazeRodTradingExperience > 10;
            case "quartz", "gold_nugget", "blackstone" -> true;
            case "netherite_scrap" -> blazeRodTradingExperience > 50 && !isWitherSkeleton;
            default -> false;
        };
    }
    
    /**
     * AI behavior for handling hostile Nether environment
     */
    public void adaptToNetherEnvironment() {
        // Increase trading experience based on survival time
        blazeRodTradingExperience += 1;
        
        // Rare chance to evolve into wither skeleton variant
        if (blazeRodTradingExperience > 100 && Math.random() < 0.01) {
            isWitherSkeleton = true;
            // Wither skeletons have enhanced combat capabilities
        }
    }
    
    /**
     * AI-powered pathfinding for Nether terrain
     */
    public boolean canNavigateNetherTerrain(String terrainType) {
        return switch (terrainType) {
            case "lava_ocean", "lava_lake" -> true; // Fire resistance allows lava traversal
            case "soul_sand_valley" -> blazeRodTradingExperience > 5;
            case "basalt_deltas" -> true; // Natural climbers
            case "warped_forest", "crimson_forest" -> true;
            default -> false;
        };
    }
    
    // Getters
    public UUID getVillagerID() { return villagerID; }
    public EntityAttributes getAttributes() { return attributes; }
    public double getLavaResistance() { return lavaResistance; }
    public int getBlazeRodTradingExperience() { return blazeRodTradingExperience; }
    public boolean isWitherSkeleton() { return isWitherSkeleton; }
    public NetherBehaviorProfile getBehaviorProfile() { return behaviorProfile; }
    
    /**
     * Nether-specific behavior profile for AI decision making
     */
    public static class NetherBehaviorProfile {
        private double aggressionLevel = 0.6; // Higher than overworld villagers
        private double resourceHoardingTendency = 0.8; // Scarce resources in Nether
        private double explorationDesire = 0.7; // Brave explorers
        
        public double getAggressionLevel() { return aggressionLevel; }
        public double getResourceHoardingTendency() { return resourceHoardingTendency; }
        public double getExplorationDesire() { return explorationDesire; }
        
        public void evolveForWitherSkeleton() {
            aggressionLevel = 0.9;
            resourceHoardingTendency = 0.5;
            explorationDesire = 0.4;
        }
    }
}