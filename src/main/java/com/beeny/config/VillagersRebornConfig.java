package com.beeny.config;

public class VillagersRebornConfig {
    // Bounding box size for entity scanning (in chunks)
    public static int VILLAGER_SCAN_CHUNK_RADIUS = 8;
    
    // Happiness threshold for natural decay/recovery
    public static int HAPPINESS_NEUTRAL_THRESHOLD = 50;
    
    // Happiness adjustment values for natural decay/recovery
    public static int HAPPINESS_DECAY_RATE = 1;
    public static int HAPPINESS_RECOVERY_RATE = 1;
    
    // Get the bounding box size in blocks
    public static int getBoundingBoxSize() {
        return VILLAGER_SCAN_CHUNK_RADIUS * 16;
    }
}