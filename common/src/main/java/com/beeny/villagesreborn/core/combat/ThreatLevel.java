package com.beeny.villagesreborn.core.combat;

/**
 * Enumeration of threat levels for combat assessment
 */
public enum ThreatLevel {
    NONE(0.0f),
    LOW(0.25f),
    MODERATE(0.5f),
    HIGH(0.75f),
    EXTREME(1.0f);
    
    private final float value;
    
    ThreatLevel(float value) {
        this.value = value;
    }
    
    public float getValue() {
        return value;
    }
}