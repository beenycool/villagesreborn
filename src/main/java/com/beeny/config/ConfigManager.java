package com.beeny.config;

import com.beeny.Villagersreborn;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "villagersreborn.json";
    
    public static void loadConfig() {
        try {
            Path configPath = Paths.get("config", CONFIG_FILE_NAME);
            
            
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            
            
            if (!Files.exists(configPath)) {
                createDefaultConfig(configPath);
                return;
            }
            
            
            String content = Files.readString(configPath);
            JsonObject config = JsonParser.parseString(content).getAsJsonObject();
            
            
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
            
            // AI thread pool size
            if (config.has("aiThreadPoolSize")) {
                VillagersRebornConfig.AI_THREAD_POOL_SIZE = config.get("aiThreadPoolSize").getAsInt();
            }

            // Load LLM dialogue settings
            if (config.has("enableDynamicDialogue")) {
                VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = config.get("enableDynamicDialogue").getAsBoolean();
            }
            
            if (config.has("llmProvider")) {
                VillagersRebornConfig.LLM_PROVIDER = config.get("llmProvider").getAsString();
            }
            
            if (config.has("llmApiKey")) {
                VillagersRebornConfig.LLM_API_KEY = config.get("llmApiKey").getAsString();
            }
            
            if (config.has("llmApiEndpoint")) {
                VillagersRebornConfig.LLM_API_ENDPOINT = config.get("llmApiEndpoint").getAsString();
            }
            
            if (config.has("llmModel")) {
                VillagersRebornConfig.LLM_MODEL = config.get("llmModel").getAsString();
            }
            
            if (config.has("llmTemperature")) {
                VillagersRebornConfig.LLM_TEMPERATURE = config.get("llmTemperature").getAsDouble();
            }
            
            if (config.has("llmMaxTokens")) {
                VillagersRebornConfig.LLM_MAX_TOKENS = config.get("llmMaxTokens").getAsInt();
            }
            
            if (config.has("llmRequestTimeout")) {
                VillagersRebornConfig.LLM_REQUEST_TIMEOUT = config.get("llmRequestTimeout").getAsInt();
            }
            
            if (config.has("fallbackToStatic")) {
                VillagersRebornConfig.FALLBACK_TO_STATIC = config.get("fallbackToStatic").getAsBoolean();
            }
            
            if (config.has("enableDialogueCache")) {
                VillagersRebornConfig.ENABLE_DIALOGUE_CACHE = config.get("enableDialogueCache").getAsBoolean();
            }
            
            if (config.has("dialogueCacheSize")) {
                VillagersRebornConfig.DIALOGUE_CACHE_SIZE = config.get("dialogueCacheSize").getAsInt();
            }
            
            if (config.has("conversationHistoryLimit")) {
                VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT = config.get("conversationHistoryLimit").getAsInt();
            }
            
            if (config.has("localLlmUrl")) {
                VillagersRebornConfig.LLM_LOCAL_URL = config.get("localLlmUrl").getAsString();
            }
            
            Villagersreborn.LOGGER.info("Loaded Villagers Reborn config");
        } catch (Exception e) {
            Villagersreborn.LOGGER.error("Failed to load Villagers Reborn config", e);
        }
    }
    
    private static void createDefaultConfig(Path configPath) throws IOException {
        JsonObject config = new JsonObject();
        config.addProperty("villagerScanChunkRadius", VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS);
        config.addProperty("happinessNeutralThreshold", VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD);
        config.addProperty("happinessDecayRate", VillagersRebornConfig.HAPPINESS_DECAY_RATE);
        config.addProperty("happinessRecoveryRate", VillagersRebornConfig.HAPPINESS_RECOVERY_RATE);
        
        // Add LLM dialogue settings
        config.addProperty("enableDynamicDialogue", VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE);
        config.addProperty("llmProvider", VillagersRebornConfig.LLM_PROVIDER);
        config.addProperty("llmApiKey", VillagersRebornConfig.LLM_API_KEY);
        config.addProperty("llmApiEndpoint", VillagersRebornConfig.LLM_API_ENDPOINT);
        config.addProperty("llmModel", VillagersRebornConfig.LLM_MODEL);
        config.addProperty("llmTemperature", VillagersRebornConfig.LLM_TEMPERATURE);
        config.addProperty("llmMaxTokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        config.addProperty("llmRequestTimeout", VillagersRebornConfig.LLM_REQUEST_TIMEOUT);
        config.addProperty("fallbackToStatic", VillagersRebornConfig.FALLBACK_TO_STATIC);
        config.addProperty("enableDialogueCache", VillagersRebornConfig.ENABLE_DIALOGUE_CACHE);
        config.addProperty("dialogueCacheSize", VillagersRebornConfig.DIALOGUE_CACHE_SIZE);
        config.addProperty("conversationHistoryLimit", VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT);
        config.addProperty("localLlmUrl", VillagersRebornConfig.LLM_LOCAL_URL);
        
        Files.writeString(configPath, GSON.toJson(config));
        Villagersreborn.LOGGER.info("Created default Villagers Reborn config");
    }
}