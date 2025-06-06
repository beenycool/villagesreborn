package com.beeny.villagesreborn.core.ai.memory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a traumatic event that affects villager behavior
 */
public class TraumaticEvent {
    private final String description;
    private final String trigger;
    private final LocalDateTime timestamp;
    private final float severity;
    private final List<UUID> involvedEntities;
    
    public TraumaticEvent(String description, String trigger, LocalDateTime timestamp,
                         float severity, List<UUID> involvedEntities) {
        this.description = description;
        this.trigger = trigger;
        this.timestamp = timestamp;
        this.severity = severity;
        this.involvedEntities = involvedEntities;
    }
    
    public String getDescription() { return description; }
    public String getTrigger() { return trigger; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public float getSeverity() { return severity; }
    public List<UUID> getInvolvedEntities() { return involvedEntities; }
}