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
    
    public static void saveConfig() throws IOException {
        Path configPath = Paths.get("config", CONFIG_FILE_NAME);
        if (!Files.exists(configPath.getParent())) {
            Files.createDirectories(configPath.getParent());
        }
        JsonObject config = new JsonObject();
        // Core settings
        config.addProperty("villagerScanChunkRadius", VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS);
        config.addProperty("happinessNeutralThreshold", VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD);
        config.addProperty("happinessDecayRate", VillagersRebornConfig.HAPPINESS_DECAY_RATE);
        config.addProperty("happinessRecoveryRate", VillagersRebornConfig.HAPPINESS_RECOVERY_RATE);
        config.addProperty("aiThreadPoolSize", VillagersRebornConfig.AI_THREAD_POOL_SIZE);

        // LLM dialogue settings
        config.addProperty("enableDynamicDialogue", VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE);
        config.addProperty("llmProvider", VillagersRebornConfig.LLM_PROVIDER);
        // API key is no longer stored in config; read from environment variable
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

        Files.writeString(configPath, GSON.toJson(config));
    }

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
            
            // API key is no longer loaded from config; read from environment variable
            
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
            
            if (config.has("llmLocalUrl")) {
                VillagersRebornConfig.LLM_LOCAL_URL = config.get("llmLocalUrl").getAsString();
            }
            
            Villagersreborn.LOGGER.info("Loaded Villagers Reborn config");
        } catch (Exception e) {
            Villagersreborn.LOGGER.error("Failed to load Villagers Reborn config", e);
        }
    }
    
    private static void createDefaultConfig(Path configPath) throws IOException {
        saveConfig(
            VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS,
            VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD,
            VillagersRebornConfig.HAPPINESS_DECAY_RATE,
            VillagersRebornConfig.HAPPINESS_RECOVERY_RATE,
            VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE,
            VillagersRebornConfig.LLM_PROVIDER,
            VillagersRebornConfig.LLM_API_ENDPOINT,
            VillagersRebornConfig.LLM_MODEL,
            VillagersRebornConfig.LLM_TEMPERATURE,
            VillagersRebornConfig.LLM_MAX_TOKENS,
            VillagersRebornConfig.LLM_REQUEST_TIMEOUT,
            VillagersRebornConfig.FALLBACK_TO_STATIC,
            VillagersRebornConfig.ENABLE_DIALOGUE_CACHE,
            VillagersRebornConfig.DIALOGUE_CACHE_SIZE,
            VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT,
            VillagersRebornConfig.LLM_LOCAL_URL,
            configPath
        );
        Villagersreborn.LOGGER.info("Created default Villagers Reborn config");
    }

    /**
     * Save the current config to villagersreborn.json
     */
    public static void saveConfig(
        int villagerScanChunkRadius,
        int happinessNeutralThreshold,
        int happinessDecayRate,
        int happinessRecoveryRate,
        boolean enableDynamicDialogue,
        String llmProvider,
        String llmApiEndpoint,
        String llmModel,
        double llmTemperature,
        int llmMaxTokens,
        int llmRequestTimeout,
        boolean fallbackToStatic,
        boolean enableDialogueCache,
        int dialogueCacheSize,
        int conversationHistoryLimit,
        String llmLocalUrl
    ) throws IOException {
        Path configPath = Paths.get("config", CONFIG_FILE_NAME);
        saveConfig(
            villagerScanChunkRadius,
            happinessNeutralThreshold,
            happinessDecayRate,
            happinessRecoveryRate,
            enableDynamicDialogue,
            llmProvider,
            llmApiEndpoint,
            llmModel,
            llmTemperature,
            llmMaxTokens,
            llmRequestTimeout,
            fallbackToStatic,
            enableDialogueCache,
            dialogueCacheSize,
            conversationHistoryLimit,
            llmLocalUrl,
            configPath
        );
    }

    /**
     * Save config to a specific path
     */
    public static void saveConfig(
        int villagerScanChunkRadius,
        int happinessNeutralThreshold,
        int happinessDecayRate,
        int happinessRecoveryRate,
        boolean enableDynamicDialogue,
        String llmProvider,
        String llmApiEndpoint,
        String llmModel,
        double llmTemperature,
        int llmMaxTokens,
        int llmRequestTimeout,
        boolean fallbackToStatic,
        boolean enableDialogueCache,
        int dialogueCacheSize,
        int conversationHistoryLimit,
        String llmLocalUrl,
        Path configPath
    ) throws IOException {
        JsonObject config = new JsonObject();
        config.addProperty("//", "Set your API key via environment variable VILLAGERS_REBORN_API_KEY");
        config.addProperty("villagerScanChunkRadius", villagerScanChunkRadius);
        config.addProperty("happinessNeutralThreshold", happinessNeutralThreshold);
        config.addProperty("happinessDecayRate", happinessDecayRate);
        config.addProperty("happinessRecoveryRate", happinessRecoveryRate);
        config.addProperty("enableDynamicDialogue", enableDynamicDialogue);
        config.addProperty("llmProvider", llmProvider);
        config.addProperty("llmApiKey", ""); // Placeholder for user clarity, never used
        config.addProperty("llmApiEndpoint", llmApiEndpoint);
        config.addProperty("llmModel", llmModel);
        config.addProperty("llmTemperature", llmTemperature);
        config.addProperty("llmMaxTokens", llmMaxTokens);
        config.addProperty("llmRequestTimeout", llmRequestTimeout);
        config.addProperty("fallbackToStatic", fallbackToStatic);
        config.addProperty("enableDialogueCache", enableDialogueCache);
        config.addProperty("dialogueCacheSize", dialogueCacheSize);
        config.addProperty("conversationHistoryLimit", conversationHistoryLimit);
        config.addProperty("llmLocalUrl", llmLocalUrl);
        if (!Files.exists(configPath.getParent())) {
            Files.createDirectories(configPath.getParent());
        }
        Files.writeString(configPath, GSON.toJson(config));
    }
}