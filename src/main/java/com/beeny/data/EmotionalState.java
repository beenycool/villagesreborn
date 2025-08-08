package com.beeny.data;

import java.util.function.Supplier;

public class EmotionalState {
    private final Supplier<Long> lastUpdateSupplier;
    private long lastUpdate;
    
    public EmotionalState() {
        this.lastUpdateSupplier = System::currentTimeMillis;
        this.lastUpdate = lastUpdateSupplier.get();
    }
    
    // Getters and setters
    public long getLastUpdate() {
        return lastUpdate;
    }
    
    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    
    // Method to refresh timestamp using supplier
    public void refreshTimestamp() {
        this.lastUpdate = lastUpdateSupplier.get();
    }
}