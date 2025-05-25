package com.beeny.villagesreborn.core.ai;

import java.util.UUID;

/**
 * Minimal ConversationInteraction implementation for TDD
 */
public class ConversationInteraction {
    private final long timestamp;
    private final UUID playerUUID;
    private final String playerMessage;
    private final String villagerResponse;
    private final MoodState moodAtTime;
    private final String location;

    public ConversationInteraction(long timestamp, UUID playerUUID, String playerMessage, 
                                 String villagerResponse, MoodState moodAtTime, String location) {
        this.timestamp = timestamp;
        this.playerUUID = playerUUID;
        this.playerMessage = playerMessage;
        this.villagerResponse = villagerResponse;
        this.moodAtTime = moodAtTime;
        this.location = location;
    }

    public long getTimestamp() { return timestamp; }
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerMessage() { return playerMessage; }
    public String getVillagerResponse() { return villagerResponse; }
    public MoodState getMoodAtTime() { return moodAtTime; }
    public String getLocation() { return location; }
}