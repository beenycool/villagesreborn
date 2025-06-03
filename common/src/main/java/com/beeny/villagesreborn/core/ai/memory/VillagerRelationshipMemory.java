package com.beeny.villagesreborn.core.ai.memory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores detailed relationship information between villagers
 */
public class VillagerRelationshipMemory {
    private final UUID villagerUUID;
    private final String villagerName;
    private final RelationshipType relationshipType;
    private final String lastInteraction;
    private final LocalDateTime lastUpdate;
    private final float relationshipStrength;
    
    public VillagerRelationshipMemory(UUID villagerUUID, String villagerName, 
                                     RelationshipType relationshipType, String lastInteraction,
                                     LocalDateTime lastUpdate, float relationshipStrength) {
        this.villagerUUID = villagerUUID;
        this.villagerName = villagerName;
        this.relationshipType = relationshipType;
        this.lastInteraction = lastInteraction;
        this.lastUpdate = lastUpdate;
        this.relationshipStrength = relationshipStrength;
    }
    
    public UUID getVillagerUUID() { return villagerUUID; }
    public String getVillagerName() { return villagerName; }
    public RelationshipType getRelationshipType() { return relationshipType; }
    public String getLastInteraction() { return lastInteraction; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public float getRelationshipStrength() { return relationshipStrength; }
}