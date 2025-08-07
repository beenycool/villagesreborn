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
        config.addProperty("aiEnabled", VillagersRebornConfig.isAiEnabled());
        config.addProperty("aiProvider", VillagersRebornConfig.getAiProvider());
        config.addProperty("aiApiKey", VillagersRebornConfig.getAiApiKey());
        config.addProperty("aiRateLimitSeconds", VillagersRebornConfig.getAiRateLimitSeconds());
        config.addProperty("aiMaxTokens", VillagersRebornConfig.getAiMaxTokens());
        config.addProperty("toolCallingEnabled", VillagersRebornConfig.isToolCallingEnabled());
        config.addProperty("toolUseProbability", VillagersRebornConfig.getToolUseProbability());
        
        Files.writeString(configPath, GSON.toJson(config));
        Villagersreborn.LOGGER.info("Created default Villagers Reborn config");
    }
}