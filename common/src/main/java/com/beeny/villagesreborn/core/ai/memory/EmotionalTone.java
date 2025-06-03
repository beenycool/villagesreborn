package com.beeny.villagesreborn.core.ai.memory;

/**
 * Emotional tone associated with memories and stories
 */
public enum EmotionalTone {
    MELANCHOLIC(-0.8f),
    SAD(-0.6f),
    DISAPPOINTED(-0.4f),
    NEUTRAL(0.0f),
    HOPEFUL(0.4f),
    HAPPY(0.6f),
    JOYFUL(0.8f),
    EUPHORIC(1.0f),
    NOSTALGIC(0.2f),
    PROUD(0.7f),
    GRATEFUL(0.5f),
    ANGRY(-0.7f),
    FEARFUL(-0.5f),
    EXCITED(0.9f);
    
    private final float intensity;
    
    EmotionalTone(float intensity) {
        this.intensity = intensity;
    }
    
    public float getIntensity() {
        return intensity;
    }
}