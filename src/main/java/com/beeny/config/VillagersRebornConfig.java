package com.beeny.config;

public class VillagersRebornConfig {
    
    public static int VILLAGER_SCAN_CHUNK_RADIUS = 8;
    
    
    public static int HAPPINESS_NEUTRAL_THRESHOLD = 50;
    
    
    public static int HAPPINESS_DECAY_RATE = 1;
    public static int HAPPINESS_RECOVERY_RATE = 1;
    
    
    public static int getBoundingBoxSize() {
        return VILLAGER_SCAN_CHUNK_RADIUS * 16;
    }
}