package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.common.Player;

/**
 * Enhanced ConversationContext with comprehensive context data for LLM conversations
 */
public class ConversationContext {
    private Player sender;
    private String message;
    private long timestamp;
    private String world;
    private String timeOfDay = "Day";
    private String weather = "Clear";
    private String location = "Village";
    private String relationship = "Neutral";
    private boolean hasNearbyVillagers = false;
    private String nearbyVillagers = "";

    public ConversationContext() {
        this.timestamp = System.currentTimeMillis();
    }

    public ConversationContext(Player sender, String message, long timestamp, String world) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.world = world;
    }

    // Getters and setters
    public Player getSender() { return sender; }
    public void setSender(Player sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    public String getTimeOfDay() { return timeOfDay; }
    public String getWeather() { return weather; }
    public String getLocation() { return location; }
    public String getRelationship() { return relationship; }
    public boolean hasNearbyVillagers() { return hasNearbyVillagers; }
    public String getNearbyVillagers() { return nearbyVillagers; }

    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    public void setWeather(String weather) { this.weather = weather; }
    public void setLocation(String location) { this.location = location; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public void setHasNearbyVillagers(boolean hasNearbyVillagers) { this.hasNearbyVillagers = hasNearbyVillagers; }
    public void setNearbyVillagers(String nearbyVillagers) { this.nearbyVillagers = nearbyVillagers; }
}