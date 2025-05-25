package com.beeny.villagesreborn.core.combat;

/**
 * Represents an assessment of threat level from a specific entity
 */
public class ThreatAssessment {
    
    private final ThreatLevel threatLevel;
    private final LivingEntity threatSource;
    private final float threatScore;
    
    public ThreatAssessment(ThreatLevel threatLevel, LivingEntity threatSource, float threatScore) {
        this.threatLevel = threatLevel;
        this.threatSource = threatSource;
        this.threatScore = Math.max(0.0f, Math.min(1.0f, threatScore));
    }
    
    public ThreatLevel getThreatLevel() {
        return threatLevel;
    }
    
    public LivingEntity getThreatSource() {
        return threatSource;
    }
    
    public float getThreatScore() {
        return threatScore;
    }
    
    @Override
    public String toString() {
        return "ThreatAssessment{" +
                "threatLevel=" + threatLevel +
                ", threatSource=" + threatSource +
                ", threatScore=" + threatScore +
                '}';
    }
}