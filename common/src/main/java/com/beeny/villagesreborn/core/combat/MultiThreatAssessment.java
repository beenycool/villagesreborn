package com.beeny.villagesreborn.core.combat;

import java.util.List;

/**
 * Assessment of threats from multiple sources in a combat situation
 */
public class MultiThreatAssessment {
    
    private final ThreatLevel overallThreatLevel;
    private final float averageThreatScore;
    private final List<LivingEntity> threatSources;
    private final List<ThreatAssessment> individualThreats;
    
    public MultiThreatAssessment(ThreatLevel overallThreatLevel, 
                               float averageThreatScore,
                               List<LivingEntity> threatSources,
                               List<ThreatAssessment> individualThreats) {
        this.overallThreatLevel = overallThreatLevel;
        this.averageThreatScore = Math.max(0.0f, Math.min(1.0f, averageThreatScore));
        this.threatSources = threatSources;
        this.individualThreats = individualThreats;
    }
    
    public ThreatLevel getThreatLevel() {
        return overallThreatLevel;
    }
    
    public float getThreatScore() {
        return averageThreatScore;
    }
    
    public List<LivingEntity> getThreatSources() {
        return threatSources;
    }
    
    public List<ThreatAssessment> getIndividualThreats() {
        return individualThreats;
    }
    
    public boolean hasThreats() {
        return !threatSources.isEmpty();
    }
    
    public int getThreatCount() {
        return threatSources.size();
    }
    
    @Override
    public String toString() {
        return "MultiThreatAssessment{" +
                "overallThreatLevel=" + overallThreatLevel +
                ", averageThreatScore=" + averageThreatScore +
                ", threatCount=" + getThreatCount() +
                '}';
    }
}