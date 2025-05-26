package com.beeny.villagesreborn.core.expansion;

import java.util.List;
import java.util.Map;

/**
 * Represents population and economic growth trends for expansion planning
 */
public class GrowthTrends {
    
    public enum TrendDirection {
        DECLINING,
        STABLE,
        GROWING,
        RAPID_GROWTH
    }
    
    private final TrendDirection populationTrend;
    private final TrendDirection economicTrend;
    private final Map<String, Float> resourceConsumptionRates;
    private final List<String> emergingNeeds;
    private final float projectedGrowthRate;
    private final int timeframeMonths;
    
    public GrowthTrends(TrendDirection populationTrend,
                       TrendDirection economicTrend,
                       Map<String, Float> resourceConsumptionRates,
                       List<String> emergingNeeds,
                       float projectedGrowthRate,
                       int timeframeMonths) {
        this.populationTrend = populationTrend;
        this.economicTrend = economicTrend;
        this.resourceConsumptionRates = resourceConsumptionRates;
        this.emergingNeeds = emergingNeeds;
        this.projectedGrowthRate = projectedGrowthRate;
        this.timeframeMonths = timeframeMonths;
    }
    
    public TrendDirection getPopulationTrend() {
        return populationTrend;
    }
    
    public TrendDirection getEconomicTrend() {
        return economicTrend;
    }
    
    public Map<String, Float> getResourceConsumptionRates() {
        return resourceConsumptionRates;
    }
    
    public List<String> getEmergingNeeds() {
        return emergingNeeds;
    }
    
    public float getProjectedGrowthRate() {
        return projectedGrowthRate;
    }
    
    /**
     * Gets the population growth rate as a percentage
     */
    public float getPopulationGrowthRate() {
        return projectedGrowthRate * 100.0f;
    }
    
    public int getTimeframeMonths() {
        return timeframeMonths;
    }
    
    public float getResourceConsumptionRate(String resource) {
        return resourceConsumptionRates.getOrDefault(resource, 0.0f);
    }
    
    public boolean hasEmergingNeed(String need) {
        return emergingNeeds.contains(need);
    }
    
    public boolean isGrowthPositive() {
        return projectedGrowthRate > 0.0f;
    }
    
    public boolean requiresExpansion() {
        return populationTrend == TrendDirection.GROWING || 
               populationTrend == TrendDirection.RAPID_GROWTH ||
               projectedGrowthRate > 0.1f;
    }
    
    @Override
    public String toString() {
        return String.format("GrowthTrends{population=%s, economic=%s, growth=%.2f%%, timeframe=%d months}",
                           populationTrend, economicTrend, projectedGrowthRate * 100, timeframeMonths);
    }
}