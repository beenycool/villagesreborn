package com.beeny.config;

import com.beeny.Villagersreborn;
import com.beeny.constants.StringConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "villagersreborn.json";
    private static final String FABRIC_CONFIG_RESOURCE = "villagersreborn:config/villagersreborn.json";

    /**
     * Register Fabric resource reload listener for config.
     * Call this during mod initialization.
     */
    public static void registerConfigReloadListener() {
        // For modern Fabric API, SimpleResourceReloadListener was removed; avoid compile-time dependency here.
        // We simply load config during init and rely on manual reload through file changes or commands.
        loadConfig();
    }

    /**
     * Load config from Fabric resource system if present, else fallback to file IO.
     */
    public static void loadConfig() {
        // Attempt resource load if available is removed due to API changes; fallback to file load
        loadConfigFromFile();
    }

    /**
     * Try to load config from Fabric resource system.
     * @return true if loaded, false if not found.
     */
    private static boolean loadConfigFromResource(net.minecraft.resource.ResourceManager manager) {
        // Deprecated path not supported in current mappings; always return false to use file config.
        return false;
    }

    /**
     * Load config from file system.
     */
    private static void loadConfigFromFile() {
        try {
            Path configPath = Paths.get("config", CONFIG_FILE_NAME);
            
            if (!Files.exists(configPath)) {
                createDefaultConfig(configPath);
                return;
            }
            
            String content = Files.readString(configPath);
            JsonObject config = JsonParser.parseString(content).getAsJsonObject();
            applyConfig(config);
            
            // Use accessor-based AI config (copilot/fix)
            if (config.has("villagerScanChunkRadius")) {
                VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS = config.get("villagerScanChunkRadius").getAsInt();
            }
            
            if (config.has("happinessNeutralThreshold")) {
                VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD = config.get("happinessNeutralThreshold").getAsInt();
            }
            
            if (config.has("happinessDecayRate")) {
                VillagersRebornConfig.HAPPINESS_DECAY_RATE = config.get("happinessDecayRate").getAsInt();
            }
            
            if (config.has("happinessRecoveryRate")) {
                VillagersRebornConfig.HAPPINESS_RECOVERY_RATE = config.get("happinessRecoveryRate").getAsInt();
            }
            
            // AI Configuration (use accessors)
            if (config.has("aiEnabled")) {
                VillagersRebornConfig.setAiEnabled(config.get("aiEnabled").getAsBoolean());
            }
            
            if (config.has("aiProvider")) {
                VillagersRebornConfig.setAiProvider(config.get("aiProvider").getAsString());
            }
            
            if (config.has("aiApiKey")) {
                VillagersRebornConfig.setAiApiKey(config.get("aiApiKey").getAsString());
            }
            
            if (config.has("aiRateLimitSeconds")) {
                VillagersRebornConfig.setAiRateLimitSeconds(config.get("aiRateLimitSeconds").getAsInt());
            }
            
            if (config.has("aiMaxTokens")) {
                VillagersRebornConfig.setAiMaxTokens(config.get("aiMaxTokens").getAsInt());
            }
            
            if (config.has("toolCallingEnabled")) {
                VillagersRebornConfig.setToolCallingEnabled(config.get("toolCallingEnabled").getAsBoolean());
            }
            
            if (config.has("toolUseProbability")) {
                VillagersRebornConfig.setToolUseProbability(config.get("toolUseProbability").getAsDouble());
            }
            
            Villagersreborn.LOGGER.info("Loaded Villagers Reborn config");
        } catch (Exception e) {
            Villagersreborn.LOGGER.error(StringConstants.ERROR_CONFIG_LOAD_FAILED, e);
        }
    }

    /**
     * Apply config values from JsonObject with validation.
     * Missing or invalid entries fall back to defaults and are logged.
     */
    private static void applyConfig(JsonObject config) {
        // Int validators with ranges
        VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS = validateInt(
                config, "villagerScanChunkRadius",
                VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS,
                0, 64);

        VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD = validateInt(
                config, "happinessNeutralThreshold",
                VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD,
                0, 100);

        VillagersRebornConfig.HAPPINESS_DECAY_RATE = validateInt(
                config, "happinessDecayRate",
                VillagersRebornConfig.HAPPINESS_DECAY_RATE,
                0, Integer.MAX_VALUE);

        VillagersRebornConfig.HAPPINESS_RECOVERY_RATE = validateInt(
                config, "happinessRecoveryRate",
                VillagersRebornConfig.HAPPINESS_RECOVERY_RATE,
                0, Integer.MAX_VALUE);

        // Booleans
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = validateBoolean(
                config, "enableDynamicDialogue",
                VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE);

        // Strings
        VillagersRebornConfig.LLM_PROVIDER = validateString(
                config, "llmProvider",
                VillagersRebornConfig.LLM_PROVIDER,
                new String[]{"gemini", "openrouter", "local"});

        VillagersRebornConfig.LLM_API_ENDPOINT = validateString(
                config, "llmApiEndpoint",
                VillagersRebornConfig.LLM_API_ENDPOINT,
                null);

        VillagersRebornConfig.LLM_MODEL = validateString(
                config, "llmModel",
                VillagersRebornConfig.LLM_MODEL,
                null);

        // Double (temperature)
        VillagersRebornConfig.LLM_TEMPERATURE = validateDouble(
                config, "llmTemperature",
                VillagersRebornConfig.LLM_TEMPERATURE,
                0.0, 2.0);

        // More ints
        VillagersRebornConfig.LLM_MAX_TOKENS = validateInt(
                config, "llmMaxTokens",
                VillagersRebornConfig.LLM_MAX_TOKENS,
                1, 1_000_000);

        VillagersRebornConfig.LLM_REQUEST_TIMEOUT = validateInt(
                config, "llmRequestTimeout",
                VillagersRebornConfig.LLM_REQUEST_TIMEOUT,
                100, 3_600_000);

        VillagersRebornConfig.FALLBACK_TO_STATIC = validateBoolean(
                config, "fallbackToStatic",
                VillagersRebornConfig.FALLBACK_TO_STATIC);

        VillagersRebornConfig.ENABLE_DIALOGUE_CACHE = validateBoolean(
                config, "enableDialogueCache",
                VillagersRebornConfig.ENABLE_DIALOGUE_CACHE);

        VillagersRebornConfig.DIALOGUE_CACHE_SIZE = validateInt(
                config, "dialogueCacheSize",
                VillagersRebornConfig.DIALOGUE_CACHE_SIZE,
                0, Integer.MAX_VALUE);

        VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT = validateInt(
                config, "conversationHistoryLimit",
                VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT,
                0, 10_000);

        VillagersRebornConfig.LLM_LOCAL_URL = validateString(
                config, "llmLocalUrl",
                VillagersRebornConfig.LLM_LOCAL_URL,
                null);

        // AI Subsystem Update Intervals
        VillagersRebornConfig.AI_EMOTION_UPDATE_INTERVAL = validateInt(
                config, "aiEmotionUpdateInterval",
                VillagersRebornConfig.AI_EMOTION_UPDATE_INTERVAL,
                50, Integer.MAX_VALUE);

        VillagersRebornConfig.AI_MANAGER_UPDATE_INTERVAL = validateInt(
                config, "aiManagerUpdateInterval",
                VillagersRebornConfig.AI_MANAGER_UPDATE_INTERVAL,
                1, Integer.MAX_VALUE);

        VillagersRebornConfig.AI_GOAP_UPDATE_INTERVAL = validateInt(
                config, "aiGoapUpdateInterval",
                VillagersRebornConfig.AI_GOAP_UPDATE_INTERVAL,
                1, Integer.MAX_VALUE);

        VillagersRebornConfig.MARRIAGE_PACKET_COOLDOWN = validateInt(
                config, "marriagePacketCooldown",
                VillagersRebornConfig.MARRIAGE_PACKET_COOLDOWN,
                0, Integer.MAX_VALUE);
    }

    public static int getInt(String key, int defaultValue) {
        switch (key) {
            case "villagerScanChunkRadius":
                return VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS;
            case "happinessNeutralThreshold":
                return VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD;
            case "happinessDecayRate":
                return VillagersRebornConfig.HAPPINESS_DECAY_RATE;
            case "happinessRecoveryRate":
                return VillagersRebornConfig.HAPPINESS_RECOVERY_RATE;
            case "llmMaxTokens":
                return VillagersRebornConfig.LLM_MAX_TOKENS;
            case "llmRequestTimeout":
                return VillagersRebornConfig.LLM_REQUEST_TIMEOUT;
            case "dialogueCacheSize":
                return VillagersRebornConfig.DIALOGUE_CACHE_SIZE;
            case "conversationHistoryLimit":
                return VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT;
            case "aiEmotionUpdateInterval":
                return VillagersRebornConfig.AI_EMOTION_UPDATE_INTERVAL;
            case "aiManagerUpdateInterval":
                return VillagersRebornConfig.AI_MANAGER_UPDATE_INTERVAL;
            case "aiGoapUpdateInterval":
                return VillagersRebornConfig.AI_GOAP_UPDATE_INTERVAL;
            case "marriagePacketCooldown":
                return VillagersRebornConfig.MARRIAGE_PACKET_COOLDOWN;
            default:
                return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        switch (key) {
            case "enableDynamicDialogue":
                return VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE;
            case "fallbackToStatic":
                return VillagersRebornConfig.FALLBACK_TO_STATIC;
            case "enableDialogueCache":
                return VillagersRebornConfig.ENABLE_DIALOGUE_CACHE;
            default:
                return defaultValue;
        }
    }

    public static String getString(String key, String defaultValue) {
        switch (key) {
            case "llmProvider":
                return VillagersRebornConfig.LLM_PROVIDER;
            case "llmApiEndpoint":
                return VillagersRebornConfig.LLM_API_ENDPOINT;
            case "llmModel":
                return VillagersRebornConfig.LLM_MODEL;
            case "llmLocalUrl":
                return VillagersRebornConfig.LLM_LOCAL_URL;
            default:
                return defaultValue;
        }
    }

    public static double getDouble(String key, double defaultValue) {
        switch (key) {
            case "llmTemperature":
                return VillagersRebornConfig.LLM_TEMPERATURE;
            default:
                return defaultValue;
        }
    }

    private static void createDefaultConfig(Path configPath) throws IOException {
        // Ensure parent directory exists before saving
        Path parentDir = configPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        // Use the no-arg saveConfig which reads values from VillagersRebornConfig
        saveConfig();
        Villagersreborn.LOGGER.info("Created default Villagers Reborn config");
    }

    /**
     * Convenience overload to save using current in-memory config values.
     */
    public static void saveConfig() throws IOException {
        Path configPath = Paths.get("config", CONFIG_FILE_NAME);
        saveConfig(configPath);
    }
    
    // Removed redundant multi-argument saveConfig overload that proxied to the Path variant.

    /**
     * Save config to a specific path using current in-memory values.
     */
    // Validation helpers

    private static int validateInt(JsonObject obj, String key, int fallback, int min, int max) {
        if (!obj.has(key)) {
            Villagersreborn.LOGGER.warn("Config key '{}' missing; using default {}", key, fallback);
            return fallback;
        }
        JsonElement el = obj.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            Villagersreborn.LOGGER.warn("Config key '{}' has invalid type (expected integer); using default {}", key, fallback);
            return fallback;
        }
        try {
            int val = el.getAsInt();
            if (val < min || val > max) {
                Villagersreborn.LOGGER.warn("Config key '{}' out of range [{}..{}] (was {}); using default {}", key, min, max, val, fallback);
                return fallback;
            }
            return val;
        } catch (Exception ex) {
            Villagersreborn.LOGGER.warn("Config key '{}' could not be parsed as integer; using default {} (error: {})", key, fallback, ex.toString());
            return fallback;
        }
    }

    private static double validateDouble(JsonObject obj, String key, double fallback, double min, double max) {
        if (!obj.has(key)) {
            Villagersreborn.LOGGER.warn("Config key '{}' missing; using default {}", key, fallback);
            return fallback;
        }
        JsonElement el = obj.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            Villagersreborn.LOGGER.warn("Config key '{}' has invalid type (expected number); using default {}", key, fallback);
            return fallback;
        }
        try {
            double val = el.getAsDouble();
            if (Double.isNaN(val) || val < min || val > max) {
                Villagersreborn.LOGGER.warn("Config key '{}' out of range [{}..{}] (was {}); using default {}", key, min, max, val, fallback);
                return fallback;
            }
            return val;
        } catch (Exception ex) {
            Villagersreborn.LOGGER.warn("Config key '{}' could not be parsed as number; using default {} (error: {})", key, fallback, ex.toString());
            return fallback;
        }
    }

    private static boolean validateBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key)) {
            Villagersreborn.LOGGER.warn("Config key '{}' missing; using default {}", key, fallback);
            return fallback;
        }
        JsonElement el = obj.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            Villagersreborn.LOGGER.warn("Config key '{}' has invalid type (expected boolean); using default {}", key, fallback);
            return fallback;
        }
        try {
            return el.getAsBoolean();
        } catch (Exception ex) {
            Villagersreborn.LOGGER.warn("Config key '{}' could not be parsed as boolean; using default {} (error: {})", key, fallback, ex.toString());
            return fallback;
        }
    }

    private static String validateString(JsonObject obj, String key, String fallback, String[] allowed) {
        if (!obj.has(key)) {
            Villagersreborn.LOGGER.warn("Config key '{}' missing; using default '{}'", key, fallback);
            return fallback;
        }
        JsonElement el = obj.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            Villagersreborn.LOGGER.warn("Config key '{}' has invalid type (expected string); using default '{}'", key, fallback);
            return fallback;
        }
        String val = el.getAsString();
        if (allowed != null) {
            boolean ok = false;
            for (String a : allowed) {
                if (a.equalsIgnoreCase(val)) { ok = true; break; }
            }
            if (!ok) {
                Villagersreborn.LOGGER.warn("Config key '{}' has unsupported value '{}'; allowed: {}; using default '{}'", key, val, String.join(", ", allowed), fallback);
                return fallback;
            }
            // normalize to canonical case from allowed list
            for (String a : allowed) {
                if (a.equalsIgnoreCase(val)) return a;
            }
        }
        return val;
    }

    private static void saveConfig(Path configPath) throws IOException {
        JsonObject config = new JsonObject();
        config.addProperty("//", "Set your API key via environment variable VILLAGERS_REBORN_API_KEY");
        config.addProperty("villagerScanChunkRadius", VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS);
        config.addProperty("happinessNeutralThreshold", VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD);
        config.addProperty("happinessDecayRate", VillagersRebornConfig.HAPPINESS_DECAY_RATE);
        config.addProperty("happinessRecoveryRate", VillagersRebornConfig.HAPPINESS_RECOVERY_RATE);

        // LLM and dialogue config (from HEAD)
        config.addProperty("enableDynamicDialogue", VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE);
        config.addProperty("llmProvider", VillagersRebornConfig.LLM_PROVIDER);
        config.addProperty("llmApiKey", ""); // Placeholder for user clarity, never used
        config.addProperty("llmApiEndpoint", VillagersRebornConfig.LLM_API_ENDPOINT);
        config.addProperty("llmModel", VillagersRebornConfig.LLM_MODEL);
        config.addProperty("llmTemperature", VillagersRebornConfig.LLM_TEMPERATURE);
        config.addProperty("llmMaxTokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        config.addProperty("llmRequestTimeout", VillagersRebornConfig.LLM_REQUEST_TIMEOUT);
        config.addProperty("fallbackToStatic", VillagersRebornConfig.FALLBACK_TO_STATIC);
        config.addProperty("enableDialogueCache", VillagersRebornConfig.ENABLE_DIALOGUE_CACHE);
        config.addProperty("dialogueCacheSize", VillagersRebornConfig.DIALOGUE_CACHE_SIZE);
        config.addProperty("conversationHistoryLimit", VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT);
        config.addProperty("llmLocalUrl", VillagersRebornConfig.LLM_LOCAL_URL);

        // AI config (from copilot/fix)
        config.addProperty("aiEnabled", VillagersRebornConfig.isAiEnabled());
        config.addProperty("aiProvider", VillagersRebornConfig.getAiProvider());
        config.addProperty("aiApiKey", VillagersRebornConfig.getAiApiKey());
        config.addProperty("aiRateLimitSeconds", VillagersRebornConfig.getAiRateLimitSeconds());
        config.addProperty("aiMaxTokens", VillagersRebornConfig.getAiMaxTokens());
        config.addProperty("toolCallingEnabled", VillagersRebornConfig.isToolCallingEnabled());
        config.addProperty("toolUseProbability", VillagersRebornConfig.getToolUseProbability());

        Path parentDir = configPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        Files.writeString(configPath, GSON.toJson(config));
    }
}