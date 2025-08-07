package com.beeny.config;

public class VillagersRebornConfig {
    
    public static int VILLAGER_SCAN_CHUNK_RADIUS = 8;
    
    
    public static int HAPPINESS_NEUTRAL_THRESHOLD = 50;
    
    
    public static int HAPPINESS_DECAY_RATE = 1;
    public static int HAPPINESS_RECOVERY_RATE = 1;
    
    // AI Configuration
    // AI Configuration
    private static volatile boolean AI_ENABLED = false;
    private static volatile String AI_PROVIDER = "gemini"; // "gemini" or "openrouter"
    private static volatile String AI_API_KEY = "";
    private static volatile int AI_RATE_LIMIT_SECONDS = 30;
    private static volatile int AI_MAX_TOKENS = 150;
    
    // Tool-calling Configuration
    private static volatile boolean TOOL_CALLING_ENABLED = true;
    private static volatile double TOOL_USE_PROBABILITY = 0.3;
    
    
    public static int getBoundingBoxSize() {
        return VILLAGER_SCAN_CHUNK_RADIUS * 16;
    }
}