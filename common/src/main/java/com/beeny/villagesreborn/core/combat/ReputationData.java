package com.beeny.villagesreborn.core.combat;

import java.util.UUID;

/**
 * Tracks reputation data for trade and social interactions
 */
public class ReputationData {
    private final UUID entityUUID;
    private float reputation = 0.0f;
    private int tradeCount = 0;
    private int successfulTradesCount = 0;
    
    public ReputationData(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }
    
    // Constructor for tests
    public ReputationData() {
        this.entityUUID = UUID.randomUUID();
    }
    
    public UUID getEntityUUID() {
        return entityUUID;
    }
    
    public float getReputation() {
        return reputation;
    }
    
    public float getReputationLevel() {
        return reputation;
    }
    
    public int getTradeCount() {
        return tradeCount;
    }
    
    public int getSuccessfulTradesCount() {
        return successfulTradesCount;
    }
    
    public double getSuccessRate() {
        return tradeCount > 0 ? (double) successfulTradesCount / tradeCount : 0.0;
    }
    
    public void adjustReputation(float amount) {
        this.reputation = Math.max(-1.0f, Math.min(1.0f, reputation + amount));
    }
    
    public void recordTrade(boolean successful) {
        tradeCount++;
        if (successful) {
            successfulTradesCount++;
            adjustReputation(0.05f); // Small reputation boost for successful trades
        } else {
            adjustReputation(-0.02f); // Small reputation penalty for failed trades
        }
    }
    
    public void reset() {
        reputation = 0.0f;
        tradeCount = 0;
        successfulTradesCount = 0;
    }
}