package com.beeny.villagesreborn.core.expansion;

import java.util.Random;

/**
 * Dynamic expansion configuration that generates values based on AI influence and randomization
 * rather than using hardcoded constants. This creates more varied and immersive gameplay.
 */
public class DynamicExpansionConfig {
    private static final Random RANDOM = new Random();
    
    private final float baseExpansionRate;
    private final long baseCheckInterval;
    private final float biomeVariationFactor;
    private final float populationInfluenceFactor;
    private final float resourceInfluenceFactor;
    
    public DynamicExpansionConfig() {
        // Generate dynamic base values with AI influence
        this.baseExpansionRate = 0.8f + RANDOM.nextFloat() * 0.4f; // 0.8 to 1.2
        this.baseCheckInterval = 3000L + RANDOM.nextInt(4000); // 3-7 seconds
        this.biomeVariationFactor = 0.3f + RANDOM.nextFloat() * 0.4f; // 0.3 to 0.7
        this.populationInfluenceFactor = 0.5f + RANDOM.nextFloat() * 0.3f; // 0.5 to 0.8
        this.resourceInfluenceFactor = 0.4f + RANDOM.nextFloat() * 0.4f; // 0.4 to 0.8
    }
    
    /**
     * Calculates dynamic expansion rate based on current conditions.
     */
    public float calculateExpansionRate(int population, VillageResources resources, String biomeName) {
        float rate = baseExpansionRate;
        
        // Population influence
        if (population < 20) {
            rate *= (1.0f - populationInfluenceFactor * 0.5f); // Slower for small villages
        } else if (population > 100) {
            rate *= (1.0f + populationInfluenceFactor * 0.3f); // Faster for large villages
        }
        
        // Resource influence
        if (resources.getFood() < 50) {
            rate *= (1.0f - resourceInfluenceFactor * 0.3f); // Slower when food is scarce
        }
        if (resources.getWood() > 200) {
            rate *= (1.0f + resourceInfluenceFactor * 0.2f); // Faster with abundant wood
        }
        
        // Add dynamic variation for immersion
        float variation = 0.9f + RANDOM.nextFloat() * 0.2f; // 0.9 to 1.1
        return rate * variation;
    }
    
    /**
     * Calculates dynamic check interval based on village state.
     */
    public long calculateCheckInterval(int villageSize, float currentExpansionRate) {
        long interval = baseCheckInterval;
        
        // Adjust based on village size
        if (villageSize < 10) {
            interval *= 2; // Small villages check less frequently
        } else if (villageSize > 50) {
            interval /= 1.5f; // Large villages check more frequently
        }
        
        // Adjust based on expansion rate
        interval = (long)(interval / Math.max(0.5f, currentExpansionRate));
        
        // Add randomness for unpredictability
        float randomFactor = 0.7f + RANDOM.nextFloat() * 0.6f; // 0.7 to 1.3
        interval = (long)(interval * randomFactor);
        
        return Math.max(1000L, Math.min(30000L, interval)); // Clamp between 1-30 seconds
    }
    
    /**
     * Generates dynamic biome modifier with variation.
     */
    public float generateBiomeModifier(String biomeName) {
        float baseModifier = getBaseBiomeModifier(biomeName);
        
        // Add variation for immersion
        float variation = biomeVariationFactor * (RANDOM.nextFloat() - 0.5f) * 2; // ±variation factor
        float finalModifier = baseModifier + variation;
        
        return Math.max(0.2f, Math.min(2.5f, finalModifier)); // Clamp to reasonable range
    }
    
    /**
     * Gets base biome modifier (still variable, not hardcoded).
     */
    private float getBaseBiomeModifier(String biomeName) {
        if (biomeName == null) return 1.0f;
        
        return switch (biomeName.toLowerCase()) {
            case "plains", "meadow" -> 1.0f + RANDOM.nextFloat() * 0.4f; // 1.0-1.4
            case "forest", "birch_forest" -> 0.8f + RANDOM.nextFloat() * 0.4f; // 0.8-1.2
            case "desert", "badlands" -> 0.6f + RANDOM.nextFloat() * 0.4f; // 0.6-1.0
            case "mountain", "extreme_hills" -> 0.4f + RANDOM.nextFloat() * 0.4f; // 0.4-0.8
            case "swamp", "mangrove_swamp" -> 0.5f + RANDOM.nextFloat() * 0.5f; // 0.5-1.0
            case "taiga", "snowy_taiga" -> 0.7f + RANDOM.nextFloat() * 0.4f; // 0.7-1.1
            default -> 0.7f + RANDOM.nextFloat() * 0.6f; // 0.7-1.3
        };
    }
    
    // Getters for base values
    public float getBaseExpansionRate() { return baseExpansionRate; }
    public long getBaseCheckInterval() { return baseCheckInterval; }
    public float getBiomeVariationFactor() { return biomeVariationFactor; }
    public float getPopulationInfluenceFactor() { return populationInfluenceFactor; }
    public float getResourceInfluenceFactor() { return resourceInfluenceFactor; }
} 