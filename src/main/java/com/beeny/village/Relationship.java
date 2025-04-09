package com.beeny.village;

import com.beeny.village.VillagerAI.RelationshipType;

public class Relationship {
    private RelationshipType type;
    private long establishedTime;
    private int interactionCount;
    private int score; // Numerical score representing relationship strength
    
    public Relationship(RelationshipType type) {
        this.type = type;
        establishedTime = System.currentTimeMillis();
        interactionCount = 0;
        this.score = 0; // Initialize score to 0
    }
    
    public RelationshipType getType() { return type; }
    
    public long getEstablishedTime() { return establishedTime; }
    
    public int getInteractionCount() { return interactionCount; }
    
    public void incrementInteractions() {
        interactionCount++;
    }
    
    public int getScore() { return score; }
    
    public void modifyScore(int amount) {
        this.score += amount;
        // TODO: Add clamping or bounds checking if necessary (e.g., min/max relationship score)
    }
}