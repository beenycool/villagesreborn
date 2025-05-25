package com.beeny.villagesreborn.core.conversation;

/**
 * Minimal ConversationContext implementation for TDD
 */
public class ConversationContext {
    private String timeOfDay = "Day";
    private String weather = "Clear";
    private String location = "Village";
    private String relationship = "Neutral";
    private boolean hasNearbyVillagers = false;
    private String nearbyVillagers = "";

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