package com.beeny.villagesreborn.core.ai.memory;

/**
 * Represents the emotional and social impact of village events
 */
public enum EventImpact {
    DEVASTATING(-1.0f),
    VERY_NEGATIVE(-0.7f),
    NEGATIVE(-0.4f),
    NEUTRAL(0.0f),
    POSITIVE(0.4f),
    VERY_POSITIVE(0.7f),
    LIFE_CHANGING(1.0f);
    
    private final float intensity;
    
    EventImpact(float intensity) {
        this.intensity = intensity;
    }
    
    public float getIntensity() {
        return intensity;
    }
}