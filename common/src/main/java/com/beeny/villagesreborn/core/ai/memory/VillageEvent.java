package com.beeny.villagesreborn.core.ai.memory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a significant village-wide event that affects the community
 */
public class VillageEvent {
    private final String eventType;
    private final String description;
    private final LocalDateTime timestamp;
    private final List<UUID> participantsInvolved;
    private final EventImpact impact;
    
    public VillageEvent(String eventType, String description, LocalDateTime timestamp, 
                       List<UUID> participantsInvolved, EventImpact impact) {
        this.eventType = eventType;
        this.description = description;
        this.timestamp = timestamp;
        this.participantsInvolved = participantsInvolved;
        this.impact = impact;
    }
    
    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<UUID> getParticipantsInvolved() { return participantsInvolved; }
    public EventImpact getImpact() { return impact; }
}