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
            
            // AI Configuration
            if (config.has("aiEnabled")) {
                VillagersRebornConfig.AI_ENABLED = config.get("aiEnabled").getAsBoolean();
            }
            
            if (config.has("aiProvider")) {
                VillagersRebornConfig.AI_PROVIDER = config.get("aiProvider").getAsString();
            }
            
            if (config.has("aiApiKey")) {
                VillagersRebornConfig.AI_API_KEY = config.get("aiApiKey").getAsString();
            }
            
            if (config.has("aiRateLimitSeconds")) {
                VillagersRebornConfig.AI_RATE_LIMIT_SECONDS = config.get("aiRateLimitSeconds").getAsInt();
            }
            
            if (config.has("aiMaxTokens")) {
                VillagersRebornConfig.AI_MAX_TOKENS = config.get("aiMaxTokens").getAsInt();
            }
            
            if (config.has("toolCallingEnabled")) {
                VillagersRebornConfig.TOOL_CALLING_ENABLED = config.get("toolCallingEnabled").getAsBoolean();
            }
            
            if (config.has("toolUseProbability")) {
                VillagersRebornConfig.TOOL_USE_PROBABILITY = config.get("toolUseProbability").getAsDouble();
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
        
        // AI Configuration
        config.addProperty("aiEnabled", VillagersRebornConfig.AI_ENABLED);
        config.addProperty("aiProvider", VillagersRebornConfig.AI_PROVIDER);
        config.addProperty("aiApiKey", VillagersRebornConfig.AI_API_KEY);
        config.addProperty("aiRateLimitSeconds", VillagersRebornConfig.AI_RATE_LIMIT_SECONDS);
        config.addProperty("aiMaxTokens", VillagersRebornConfig.AI_MAX_TOKENS);
        config.addProperty("toolCallingEnabled", VillagersRebornConfig.TOOL_CALLING_ENABLED);
        config.addProperty("toolUseProbability", VillagersRebornConfig.TOOL_USE_PROBABILITY);
        
        Files.writeString(configPath, GSON.toJson(config));
        Villagersreborn.LOGGER.info("Created default Villagers Reborn config");
    }
}