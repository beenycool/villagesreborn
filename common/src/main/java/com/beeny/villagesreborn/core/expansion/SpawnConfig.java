package com.beeny.villagesreborn.core.expansion;

public class SpawnConfig {
    private final int minGroupSize;
    private final int maxGroupSize;
    private final double spawnChance;
    private final String[] requiredBiomes;
    
    private SpawnConfig(Builder builder) {
        this.minGroupSize = builder.minGroupSize;
        this.maxGroupSize = builder.maxGroupSize;
        this.spawnChance = builder.spawnChance;
        this.requiredBiomes = builder.requiredBiomes;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public int getMinGroupSize() {
        return minGroupSize;
    }
    
    public int getMaxGroupSize() {
        return maxGroupSize;
    }
    
    public double getSpawnChance() {
        return spawnChance;
    }
    
    public String[] getRequiredBiomes() {
        return requiredBiomes;
    }
    
    public static class Builder {
        private int minGroupSize = 1;
        private int maxGroupSize = 1;
        private double spawnChance = 0.1;
        private String[] requiredBiomes = new String[0];
        
        public Builder minGroupSize(int minGroupSize) {
            this.minGroupSize = minGroupSize;
            return this;
        }
        
        public Builder maxGroupSize(int maxGroupSize) {
            this.maxGroupSize = maxGroupSize;
            return this;
        }
        
        public Builder spawnChance(double spawnChance) {
            this.spawnChance = spawnChance;
            return this;
        }
        
        public Builder requiredBiomes(String... biomes) {
            this.requiredBiomes = biomes;
            return this;
        }
        
        public SpawnConfig build() {
            return new SpawnConfig(this);
        }
    }
}