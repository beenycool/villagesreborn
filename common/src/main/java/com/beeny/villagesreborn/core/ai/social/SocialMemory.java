package com.beeny.villagesreborn.core.ai.social;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a shared social experience between villagers
 */
public class SocialMemory {
    private final String experienceType;
    private final String description;
    private final LocalDateTime timestamp;
    private final List<UUID> participantUUIDs;
    private final float emotionalImpact;
    private int significanceLevel = 1; // 1-5 scale
    private boolean isOngoing = false;
    
    public SocialMemory(String experienceType, String description, LocalDateTime timestamp,
                       List<UUID> participantUUIDs, float emotionalImpact) {
        this.experienceType = experienceType;
        this.description = description;
        this.timestamp = timestamp;
        this.participantUUIDs = participantUUIDs;
        this.emotionalImpact = emotionalImpact;
        this.significanceLevel = calculateSignificanceLevel(emotionalImpact, participantUUIDs.size());
    }
    
    private int calculateSignificanceLevel(float impact, int participantCount) {
        float baseSignificance = Math.abs(impact) * 3;
        float participantBonus = Math.min(2.0f, participantCount * 0.3f);
        return Math.max(1, Math.min(5, Math.round(baseSignificance + participantBonus)));
    }
    
    /**
     * Checks if this memory involves a specific villager
     */
    public boolean involvesVillager(UUID villagerUUID) {
        return participantUUIDs.contains(villagerUUID);
    }
    
    /**
     * Determines if this is a positive or negative memory
     */
    public boolean isPositiveMemory() {
        return emotionalImpact > 0.0f;
    }
    
    /**
     * Determines if this is a highly significant memory
     */
    public boolean isHighlySignificant() {
        return significanceLevel >= 4;
    }
    
    /**
     * Gets the age of this memory in days
     */
    public long getAgeDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(timestamp, LocalDateTime.now());
    }
    
    /**
     * Calculates how much this memory should influence current behavior
     */
    public float getCurrentInfluence() {
        long ageDays = getAgeDays();
        float ageDecay = Math.max(0.1f, 1.0f - (ageDays * 0.02f)); // 2% decay per day
        return Math.abs(emotionalImpact) * ageDecay * (significanceLevel / 5.0f);
    }
    
    // Getters
    public String getExperienceType() { return experienceType; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<UUID> getParticipantUUIDs() { return participantUUIDs; }
    public float getEmotionalImpact() { return emotionalImpact; }
    public int getSignificanceLevel() { return significanceLevel; }
    public boolean isOngoing() { return isOngoing; }
    
    // Setters
    public void setSignificanceLevel(int level) {
        this.significanceLevel = Math.max(1, Math.min(5, level));
    }
    
    public void setOngoing(boolean ongoing) {
        this.isOngoing = ongoing;
    }
    
    @Override
    public String toString() {
        return String.format("SocialMemory[%s: %s (Impact: %.2f, Significance: %d)]", 
            experienceType, description, emotionalImpact, significanceLevel);
    }
}