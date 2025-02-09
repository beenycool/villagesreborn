package com.villagesreborn.beeny.systems;

import java.util.Map;

public class ThreatEvent {
    private final String threatType;
    private final String location;
    private final int severity;
    private final Map<String, Object> metadata;

    public ThreatEvent(String threatType, String location, int severity, Map<String, Object> metadata) {
        this.threatType = threatType;
        this.location = location;
        this.severity = severity;
        this.metadata = metadata;
    }

    public String getThreatType() {
        return threatType;
    }

    public String getLocation() {
        return location;
    }

    public int getSeverity() {
        return severity;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        return "ThreatEvent[type=" + threatType + ", location=" + location 
             + ", severity=" + severity + "]";
    }
}
