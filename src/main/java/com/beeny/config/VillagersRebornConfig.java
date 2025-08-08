package com.beeny.config;

public class VillagersRebornConfig {

    /**
     * Radius in chunks used when scanning for nearby entities or areas relevant to villagers.
     * Units: chunks (1 chunk = 16 blocks). Effective scan box size is {@link #getBoundingBoxSize()} blocks.
     */
    // AI Provider Constants
    public static final String AI_PROVIDER_GEMINI = "gemini";
    public static final String AI_PROVIDER_OPENROUTER = "openrouter";

    public static int VILLAGER_SCAN_CHUNK_RADIUS = 8;


    /**
     * Baseline happiness value at which villagers are considered neutral.
     * Range: 0-100 (unitless scale where higher is happier).
     */
    public static int HAPPINESS_NEUTRAL_THRESHOLD = 50;


    /**
     * Amount happiness decreases per tick interval when conditions are unfavorable.
     * Units: points per game tick or configured update step (unitless points).
     */
    public static int HAPPINESS_DECAY_RATE = 1;
    /**
     * Amount happiness increases per tick interval when recovering.
     * Units: points per game tick or configured update step (unitless points).
     */
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
    public static String getAiApiKey() {
        // First check environment variable
        String envKey = System.getenv("VILLAGERS_REBORN_AI_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        return AI_API_KEY;
    }
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


    /**
     * Enables dynamic, LLM-powered dialogue for villagers.
     * If false, the system uses static, predefined lines.
     */
    public static boolean ENABLE_DYNAMIC_DIALOGUE = true;
    /**
     * Selected LLM provider used for dynamic dialogue generation.
     * Supported values: "gemini", "openrouter", "local", "claude".
     */
    public static String LLM_PROVIDER = "gemini"; // "gemini", "openrouter", "local", or "claude"
    /**
     * API key for the chosen LLM provider. Prefer using env var VILLAGERS_REBORN_API_KEY.
     * Leave empty to rely on environment variable injection.
     */
    public static String LLM_API_KEY = "";
    
    public static String getLlmApiKey() {
        // First check environment variable
        String envKey = System.getenv("VILLAGERS_REBORN_LLM_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        return LLM_API_KEY;
    }
    /**
     * Override for the LLM API base endpoint.
     * Example: https://api.openrouter.ai/v1
     */
    public static String LLM_API_ENDPOINT = "";
    /**
     * Model identifier for the selected provider.
     * Examples: "gemini-1.5-flash", "openrouter/some-model", "local-gguf-name".
     */
    public static String LLM_MODEL = "";
    /**
     * Sampling temperature controlling creativity vs determinism in responses.
     * Range: typically 0.0 - 1.0 (unitless).
     */
    public static double LLM_TEMPERATURE = 0.7;
    /**
     * Maximum number of tokens to request from the LLM per response.
     * Units: tokens.
     */
    public static int LLM_MAX_TOKENS = 150;
    /**
     * Timeout for LLM requests before they are aborted.
     * Units: milliseconds.
     */
    public static int LLM_REQUEST_TIMEOUT = 10000; // 10 seconds
    /**
     * If true, falls back to static dialogue when the LLM fails or is disabled.
     */
    public static boolean FALLBACK_TO_STATIC = true;
    /**
     * Enables caching of dialogue responses to reduce repeated LLM calls.
     */
    public static boolean ENABLE_DIALOGUE_CACHE = true;
    /**
     * Maximum number of cached dialogue entries retained.
     * Units: entries.
     */
    public static int DIALOGUE_CACHE_SIZE = 1000;
    /**
     * Maximum number of recent conversation exchanges preserved per villager.
     * Units: messages/turns.
     */
    public static int CONVERSATION_HISTORY_LIMIT = 10;
    /**
     * Maximum number of past experiences stored in memory for learning/recall.
     * Units: experiences.
     */
    public static int MAX_EXPERIENCE_HISTORY = 200;

    /**
     * Base URL for a locally hosted LLM service when {@link #LLM_PROVIDER} is "local".
     * Example: http://localhost:8080
     */
    public static String LLM_LOCAL_URL = "http://localhost:8080";
    
    // AI Subsystem Update Intervals (in milliseconds)
    /**
     * Update interval for the emotion system.
     * Units: milliseconds.
     */
    public static int AI_EMOTION_UPDATE_INTERVAL = 60000; // 60 seconds

    /**
     * Update interval for the AI manager state system.
     * Units: milliseconds.
     */
    public static int AI_MANAGER_UPDATE_INTERVAL = 5000; // 5 seconds

    /**
     * Update interval for the GOAP planning system.
     * Units: milliseconds.
     */
    public static int AI_GOAP_UPDATE_INTERVAL = 3000; // 3 seconds

    /**
     * Cooldown period for villager marriage proposals.
     * Units: milliseconds.
     */
    public static int MARRIAGE_PACKET_COOLDOWN = 5000; // 5 seconds
    /**
     * Full endpoint for a locally hosted LLM service when {@link #LLM_PROVIDER} is "local".
     * Example: http://localhost:8080
     */
    public static String LLM_LOCAL_ENDPOINT = "http://localhost:8080";
    /**
     * Utility deriving the scan bounding box in blocks from {@link #VILLAGER_SCAN_CHUNK_RADIUS}.
     * Units: blocks.
     * 1 chunk = 16 blocks, so size = radius * 16.
     */
    public static int getBoundingBoxSize() {
        return VILLAGER_SCAN_CHUNK_RADIUS * 16;
    }
    /**
     * Number of threads allocated to the AI task executor in VillagerAIManager.
     * Units: threads.
     */
    public static int AI_THREAD_POOL_SIZE = 4;
}