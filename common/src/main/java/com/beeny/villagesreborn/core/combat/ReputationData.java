package com.beeny.villagesreborn.core.combat;

/**
 * Minimal ReputationData implementation for haggling system
 */
public class ReputationData {
    private float reputationLevel = 0.0f;
    
    public ReputationData() {}
    
    public float getReputationLevel() {
        return reputationLevel;
    }
    
    public void adjustReputation(float amount) {
        this.reputationLevel = Math.max(-1.0f, Math.min(1.0f, reputationLevel + amount));
    }
}