package com.beeny.config;

public class VillagersRebornConfig {
    
    public static int VILLAGER_SCAN_CHUNK_RADIUS = 8;
    
    
    public static int HAPPINESS_NEUTRAL_THRESHOLD = 50;
    
    
    public static int HAPPINESS_DECAY_RATE = 1;
    public static int HAPPINESS_RECOVERY_RATE = 1;
    
    
    public static boolean ENABLE_DYNAMIC_DIALOGUE = true;
    public static String LLM_PROVIDER = "gemini"; // "gemini", "openrouter", or "local"
    public static String LLM_API_KEY = "";
    public static String LLM_API_ENDPOINT = "";
    public static String LLM_MODEL = "";
    public static double LLM_TEMPERATURE = 0.7;
    public static int LLM_MAX_TOKENS = 150;
    public static int LLM_REQUEST_TIMEOUT = 10000; // 10 seconds
    public static boolean FALLBACK_TO_STATIC = true;
    public static boolean ENABLE_DIALOGUE_CACHE = true;
    public static int DIALOGUE_CACHE_SIZE = 1000;
    public static int CONVERSATION_HISTORY_LIMIT = 10;
    
    // Local LLM settings (when LLM_PROVIDER = "local")
    // Renamed for naming consistency with other llm* keys
    public static String LLM_LOCAL_URL = "http://localhost:8080";
    
    
    public static int getBoundingBoxSize() {
        return VILLAGER_SCAN_CHUNK_RADIUS * 16;
    }
    // AI thread pool size for VillagerAIManager
    public static int AI_THREAD_POOL_SIZE = 4;
}