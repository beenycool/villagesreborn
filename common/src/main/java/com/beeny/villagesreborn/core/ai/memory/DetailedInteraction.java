package com.beeny.villagesreborn.core.ai.memory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed record of an interaction between a villager and a player
 */
public class DetailedInteraction {
    private final UUID playerUUID;
    private final String playerName;
    private final String interactionType;
    private final String context;
    private final String villagerResponse;
    private final LocalDateTime timestamp;
    private final float sentimentChange;
    
    public DetailedInteraction(UUID playerUUID, String playerName, String interactionType,
                              String context, String villagerResponse, LocalDateTime timestamp,
                              float sentimentChange) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.interactionType = interactionType;
        this.context = context;
        this.villagerResponse = villagerResponse;
        this.timestamp = timestamp;
        this.sentimentChange = sentimentChange;
    }
    
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public String getInteractionType() { return interactionType; }
    public String getContext() { return context; }
    public String getVillagerResponse() { return villagerResponse; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public float getSentimentChange() { return sentimentChange; }
}