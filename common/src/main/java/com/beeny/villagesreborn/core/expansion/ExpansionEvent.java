package com.beeny.villagesreborn.core.expansion;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents an AI-generated expansion event that can trigger dynamic village changes.
 * These events create emergent gameplay and reduce predictability.
 */
public class ExpansionEvent {
    
    public enum EventType {
        POPULATION_BOOM,        // Sudden population growth requiring expansion
        RESOURCE_DISCOVERY,     // New resources found, enabling specialized buildings
        TRADE_ROUTE_OPENED,     // New trade opportunities requiring infrastructure
        THREAT_DETECTED,        // External threat requiring defensive expansion
        CULTURAL_SHIFT,         // Changes in villager preferences for buildings
        SEASONAL_MIGRATION,     // Temporary population changes
        TECHNOLOGICAL_ADVANCE,  // New building types become available
        ENVIRONMENTAL_CHANGE,   // Biome characteristics shift
        DIPLOMATIC_EVENT,       // Relations with other villages change expansion needs
        ECONOMIC_BOOM          // Increased wealth enabling luxury expansions
    }
    
    public enum Priority {
        LOW(0.2f),
        MEDIUM(0.5f),
        HIGH(0.8f),
        URGENT(1.0f);
        
        private final float multiplier;
        
        Priority(float multiplier) {
            this.multiplier = multiplier;
        }
        
        public float getMultiplier() {
            return multiplier;
        }
    }
    
    private final EventType type;
    private final Priority priority;
    private final String description;
    private final Map<String, Object> parameters;
    private final List<String> triggeredBuildings;
    private final float expansionRateModifier;
    private final long duration; // How long the event lasts
    private final long createdTime;
    
    public ExpansionEvent(EventType type, Priority priority, String description,
                         Map<String, Object> parameters, List<String> triggeredBuildings,
                         float expansionRateModifier, long duration) {
        this.type = type;
        this.priority = priority;
        this.description = description;
        this.parameters = new HashMap<>(parameters);
        this.triggeredBuildings = new ArrayList<>(triggeredBuildings);
        this.expansionRateModifier = expansionRateModifier;
        this.duration = duration;
        this.createdTime = System.currentTimeMillis();
    }
    
    /**
     * Checks if this event is still active.
     */
    public boolean isActive() {
        return (System.currentTimeMillis() - createdTime) < duration;
    }
    
    /**
     * Gets the remaining duration of this event.
     */
    public long getRemainingDuration() {
        long elapsed = System.currentTimeMillis() - createdTime;
        return Math.max(0, duration - elapsed);
    }
    
    /**
     * Calculates the current influence of this event on expansion rate.
     */
    public float getCurrentInfluence() {
        if (!isActive()) {
            return 1.0f; // No influence if expired
        }
        
        float remainingRatio = (float) getRemainingDuration() / duration;
        return 1.0f + (expansionRateModifier - 1.0f) * remainingRatio * priority.getMultiplier();
    }
    
    /**
     * Gets buildings that should be prioritized due to this event.
     */
    public List<String> getTriggeredBuildings() {
        return isActive() ? new ArrayList<>(triggeredBuildings) : new ArrayList<>();
    }
    
    // Getters
    public EventType getType() { return type; }
    public Priority getPriority() { return priority; }
    public String getDescription() { return description; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
    public float getExpansionRateModifier() { return expansionRateModifier; }
    public long getDuration() { return duration; }
    public long getCreatedTime() { return createdTime; }
    
    @Override
    public String toString() {
        return String.format("ExpansionEvent{type=%s, priority=%s, description='%s', active=%s}", 
                           type, priority, description, isActive());
    }
} 