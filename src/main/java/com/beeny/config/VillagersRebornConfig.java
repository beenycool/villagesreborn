package com.beeny.config;

public class VillagersRebornConfig {
    
    // AI Provider Constants
    public static final String AI_PROVIDER_GEMINI = "gemini";
    public static final String AI_PROVIDER_OPENROUTER = "openrouter";
    
    public static int VILLAGER_SCAN_CHUNK_RADIUS = 8;
    
    
    public static int HAPPINESS_NEUTRAL_THRESHOLD = 50;
    
    
    public static int HAPPINESS_DECAY_RATE = 1;
    public static int HAPPINESS_RECOVERY_RATE = 1;
    
    // AI Configuration
    private static volatile boolean AI_ENABLED = false;
    private static volatile String AI_PROVIDER = AI_PROVIDER_GEMINI;
    private static volatile String AI_API_KEY = "";
    private static volatile int AI_RATE_LIMIT_SECONDS = 30;
    private static volatile int AI_MAX_TOKENS = 150;
    
    // Tool-calling Configuration
    private static volatile boolean TOOL_CALLING_ENABLED = true;
    private static volatile double TOOL_USE_PROBABILITY = 0.3;
    
    // AI Configuration getters
    public static boolean isAiEnabled() { return AI_ENABLED; }
    public static String getAiProvider() { return AI_PROVIDER; }
    public static String getAiApiKey() { return AI_API_KEY; }
    public static int getAiRateLimitSeconds() { return AI_RATE_LIMIT_SECONDS; }
    public static int getAiMaxTokens() { return AI_MAX_TOKENS; }
    
    // Tool-calling Configuration getters
    public static boolean isToolCallingEnabled() { return TOOL_CALLING_ENABLED; }
    public static double getToolUseProbability() { return TOOL_USE_PROBABILITY; }
    
    // AI Configuration setters (for commands)
    public static void setAiEnabled(boolean enabled) { AI_ENABLED = enabled; }
    public static void setAiProvider(String provider) { AI_PROVIDER = provider; }
    public static void setAiApiKey(String apiKey) { AI_API_KEY = apiKey; }
    public static void setAiRateLimitSeconds(int seconds) { AI_RATE_LIMIT_SECONDS = seconds; }
    public static void setAiMaxTokens(int tokens) { AI_MAX_TOKENS = tokens; }
    
    // Tool-calling Configuration setters (for commands)
    public static void setToolCallingEnabled(boolean enabled) { TOOL_CALLING_ENABLED = enabled; }
    public static void setToolUseProbability(double probability) { TOOL_USE_PROBABILITY = probability; }
    
    
    public static int getBoundingBoxSize() {
        return VILLAGER_SCAN_CHUNK_RADIUS * 16;
    }
}