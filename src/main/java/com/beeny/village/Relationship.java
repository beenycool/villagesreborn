package com.beeny.village;

import com.beeny.village.VillagerAI.RelationshipType;

public class Relationship {
    private RelationshipType type;
    private long establishedTime;
    private int interactionCount;
    
    public Relationship(RelationshipType type) {
        this.type = type;
        establishedTime = System.currentTimeMillis();
        interactionCount = 0;
    }
    
    public RelationshipType getType() { return type; }
    
    public long getEstablishedTime() { return establishedTime; }
    
    public int getInteractionCount() { return interactionCount; }
    
    public void incrementInteractions() {
        interactionCount++;
    }
}