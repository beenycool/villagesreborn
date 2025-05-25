package com.beeny.villagesreborn.core.expansion;

public class ExpansionConfig {
    private final int populationThreshold;
    private final int woodThreshold;
    private final int stoneThreshold;
    
    public ExpansionConfig(int populationThreshold, int woodThreshold, int stoneThreshold) {
        this.populationThreshold = populationThreshold;
        this.woodThreshold = woodThreshold;
        this.stoneThreshold = stoneThreshold;
    }
    
    public int getPopulationThreshold() {
        return populationThreshold;
    }
    
    public int getWoodThreshold() {
        return woodThreshold;
    }
    
    public int getStoneThreshold() {
        return stoneThreshold;
    }
}