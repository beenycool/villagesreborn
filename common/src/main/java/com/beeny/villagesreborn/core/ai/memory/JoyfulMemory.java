package com.beeny.villagesreborn.core.ai.memory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a positive, joyful memory that can improve villager mood
 */
public class JoyfulMemory {
    private final String description;
    private final LocalDateTime timestamp;
    private final List<UUID> sharedWith;
    private final float happinessLevel;
    
    public JoyfulMemory(String description, LocalDateTime timestamp,
                       List<UUID> sharedWith, float happinessLevel) {
        this.description = description;
        this.timestamp = timestamp;
        this.sharedWith = sharedWith;
        this.happinessLevel = happinessLevel;
    }
    
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<UUID> getSharedWith() { return sharedWith; }
    public float getHappinessLevel() { return happinessLevel; }
}