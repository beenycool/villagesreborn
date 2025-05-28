package com.beeny.villagesreborn.core.memory;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a global memory event that affects all villagers
 */
public class GlobalMemoryEvent {
    private final String eventType;
    private final long timestamp;
    private final Map<String, String> eventData;
    
    public GlobalMemoryEvent(String eventType, long timestamp, Map<String, String> eventData) {
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.eventData = eventData;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<String, String> getEventData() {
        return eventData;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GlobalMemoryEvent)) return false;
        GlobalMemoryEvent other = (GlobalMemoryEvent) obj;
        return Objects.equals(eventType, other.eventType) &&
               timestamp == other.timestamp &&
               Objects.equals(eventData, other.eventData);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventType, timestamp, eventData);
    }
}