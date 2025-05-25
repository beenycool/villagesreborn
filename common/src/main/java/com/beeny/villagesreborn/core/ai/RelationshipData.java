package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.combat.RelationshipLevel;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Extended RelationshipData implementation for combat and AI systems
 */
public class RelationshipData {
    private final UUID playerUUID;
    private float trustLevel = 0.0f;
    private float friendshipLevel = 0.0f;
    private int interactionCount = 0;
    
    // Combat-related relationship tracking
    private final Map<UUID, RelationshipLevel> relationships = new HashMap<>();

    public RelationshipData(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }
    
    // Constructor for tests that need multiple relationship tracking
    public RelationshipData() {
        this.playerUUID = UUID.randomUUID(); // Default UUID for tests
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public float getTrustLevel() { return trustLevel; }
    public float getFriendshipLevel() { return friendshipLevel; }
    public int getInteractionCount() { return interactionCount; }

    public void adjustTrust(float amount) {
        this.trustLevel = Math.max(-1.0f, Math.min(1.0f, trustLevel + amount));
    }

    public void adjustFriendship(float amount) {
        this.friendshipLevel = Math.max(-1.0f, Math.min(1.0f, friendshipLevel + amount));
    }

    public void incrementInteractionCount() {
        this.interactionCount++;
    }
    
    // Combat relationship methods
    public void setRelationshipLevel(UUID entityUUID, RelationshipLevel level) {
        relationships.put(entityUUID, level);
    }
    
    public RelationshipLevel getRelationshipLevel(UUID entityUUID) {
        return relationships.getOrDefault(entityUUID, RelationshipLevel.STRANGER);
    }
    
    public boolean isEnemy(UUID entityUUID) {
        return getRelationshipLevel(entityUUID) == RelationshipLevel.ENEMY;
    }
    
    public boolean isFriend(UUID entityUUID) {
        RelationshipLevel level = getRelationshipLevel(entityUUID);
        return level == RelationshipLevel.FRIEND || level == RelationshipLevel.CLOSE_FRIEND || level == RelationshipLevel.FAMILY;
    }
}