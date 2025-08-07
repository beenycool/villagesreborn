package com.beeny.config;

import com.beeny.Villagersreborn;
import com.google.gson.*;

import java.util.function.Consumer;
import java.util.function.Function;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "villagersreborn.json";
    
    private static <T> void loadIfPresent(JsonObject config, String key, 
            Function<JsonElement, T> extractor, 
            Consumer<T> setter) {
        if (config.has(key)) {
            setter.accept(extractor.apply(config.get(key)));
        }
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
            
            // AI Configuration using helper method
            loadIfPresent(config, "aiEnabled", JsonElement::getAsBoolean, VillagersRebornConfig::setAiEnabled);
            loadIfPresent(config, "aiProvider", JsonElement::getAsString, VillagersRebornConfig::setAiProvider);
            loadIfPresent(config, "aiApiKey", JsonElement::getAsString, VillagersRebornConfig::setAiApiKey);
            loadIfPresent(config, "aiRateLimitSeconds", JsonElement::getAsInt, VillagersRebornConfig::setAiRateLimitSeconds);
            loadIfPresent(config, "aiMaxTokens", JsonElement::getAsInt, VillagersRebornConfig::setAiMaxTokens);
            loadIfPresent(config, "toolCallingEnabled", JsonElement::getAsBoolean, VillagersRebornConfig::setToolCallingEnabled);
            loadIfPresent(config, "toolUseProbability", JsonElement::getAsDouble, VillagersRebornConfig::setToolUseProbability);
            
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