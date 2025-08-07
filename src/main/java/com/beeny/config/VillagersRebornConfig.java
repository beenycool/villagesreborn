package com.beeny.config;

public class VillagersRebornConfig {
    
    public static int VILLAGER_SCAN_CHUNK_RADIUS = 8;
    
    
    public static int HAPPINESS_NEUTRAL_THRESHOLD = 50;
    
    
    public static int HAPPINESS_DECAY_RATE = 1;
    public static int HAPPINESS_RECOVERY_RATE = 1;
    
    // AI Configuration
    public static boolean AI_ENABLED = false;
    public static String AI_PROVIDER = "gemini"; // "gemini" or "openrouter"
    public static String AI_API_KEY = "";
    public static int AI_RATE_LIMIT_SECONDS = 30;
    public static int AI_MAX_TOKENS = 150;
    
    // Tool-calling Configuration
    public static boolean TOOL_CALLING_ENABLED = true;
    public static double TOOL_USE_PROBABILITY = 0.3;
    
    
    public static int getBoundingBoxSize() {
        return VILLAGER_SCAN_CHUNK_RADIUS * 16;
    }
}