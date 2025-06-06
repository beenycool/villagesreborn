package com.beeny.villagesreborn.core.ai.memory;

import java.time.LocalDateTime;

/**
 * Basic memory structure for storing villager memories
 */
public class Memory {
    private final String type;
    private final String description;
    private final LocalDateTime timestamp;
    private final float emotionalIntensity;
    
    public Memory(String type, String description, LocalDateTime timestamp, float emotionalIntensity) {
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
        this.emotionalIntensity = emotionalIntensity;
    }
    
    public String getType() { return type; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public float getEmotionalIntensity() { return emotionalIntensity; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s (Intensity: %.2f)", 
            timestamp.toLocalDate(), type, description, emotionalIntensity);
    }
}