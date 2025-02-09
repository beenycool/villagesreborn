package com.villagesreborn.beeny.systems;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class CrimeEvent {
    private final UUID perpetrator;
    private final String crimeType;
    private final Instant timestamp;
    private final Map<String, Object> evidence;

    public CrimeEvent(UUID perpetrator, String crimeType, Map<String, Object> evidence) {
        this.perpetrator = perpetrator;
        this.crimeType = crimeType;
        this.timestamp = Instant.now();
        this.evidence = evidence;
    }

    public UUID getPerpetrator() {
        return perpetrator;
    }

    public String getCrimeType() {
        return crimeType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }
    
    @Override
    public String toString() {
        return "CrimeEvent[perpetrator=" + perpetrator + ", crimeType=" + crimeType 
             + ", timestamp=" + timestamp + "]";
    }
}
