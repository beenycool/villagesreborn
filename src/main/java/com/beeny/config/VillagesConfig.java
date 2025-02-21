package com.beeny.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VillagesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String CONFIG_FILE = "config/villagesreborn_llm.json";
    private static VillagesConfig INSTANCE;
    
    private LLMSettings llmSettings;
    private Map<String, CultureSettings> cultureSettings;
    private GeneralSettings generalSettings;

    public static class LLMSettings {
        public String provider = "openai";
        public String model = "gpt-4";
        public float temperature = 0.7f;
        public int maxTokens = 256;
        public int timeout = 10000;
        public boolean cacheResponses = true;
        public int maxCacheSize = 1000;
        public Map<String, Float> promptWeights = new HashMap<>();
    }

    public static class CultureSettings {
        public float evolutionRate = 0.1f;
        public float interactionChance = 0.05f;
        public int minVillagersForEvent = 3;
        public int maxActiveEvents = 3;
        public boolean allowCulturalBlending = true;
        public Map<String, Float> buildingWeights = new HashMap<>();
        public Map<String, String> architecturalRules = new HashMap<>();
    }

    public static class GeneralSettings {
        public boolean dynamicEvolution = true;
        public int evolutionCheckInterval = 24000;
        public float eventFrequency = 0.05f;
        public int maxVillageRadius = 64;
        public boolean debugLogging = false;
        public boolean showWelcomeSequence = true;
        public boolean showWelcomeForReturningPlayers = false;
        public Map<String, Boolean> features = new HashMap<>();
    }

    private VillagesConfig() {
        this.llmSettings = new LLMSettings();
        this.cultureSettings = new HashMap<>();
        this.generalSettings = new GeneralSettings();
        initializeDefaultSettings();
    }

    public static VillagesConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VillagesConfig();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    private void initializeDefaultSettings() {
        // LLM prompt weights
        llmSettings.promptWeights.put("cultural_event", 1.0f);
        llmSettings.promptWeights.put("villager_dialogue", 0.8f);
        llmSettings.promptWeights.put("building_generation", 1.0f);
        llmSettings.promptWeights.put("social_interaction", 0.9f);

        // Default culture settings
        String[] defaultCultures = {"roman", "egyptian", "victorian", "nyc"};
        for (String culture : defaultCultures) {
            CultureSettings settings = new CultureSettings();
            settings.buildingWeights.put("residential", 0.4f);
            settings.buildingWeights.put("cultural", 0.3f);
            settings.buildingWeights.put("commercial", 0.2f);
            settings.buildingWeights.put("special", 0.1f);
            cultureSettings.put(culture, settings);
        }

        // General features
        generalSettings.features.put("dynamic_schedules", true);
        generalSettings.features.put("cultural_evolution", true);
        generalSettings.features.put("inter_village_trade", true);
        generalSettings.features.put("village_events", true);
    }

    public void load() {
        try {
            Path configPath = Path.of(CONFIG_FILE);
            if (Files.exists(configPath)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (Reader reader = Files.newBufferedReader(configPath)) {
                    VillagesConfig loaded = gson.fromJson(reader, VillagesConfig.class);
                    if (loaded != null) {
                        this.llmSettings = loaded.llmSettings;
                        this.cultureSettings = loaded.cultureSettings;
                        this.generalSettings = loaded.generalSettings;
                    }
                }
            } else {
                save(); // Create default config
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public void save() {
        try {
            Path configDir = Path.of("config");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(Path.of(CONFIG_FILE))) {
                gson.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public LLMSettings getLLMSettings() {
        return llmSettings;
    }

    public CultureSettings getCultureSettings(String culture) {
        return cultureSettings.getOrDefault(culture, new CultureSettings());
    }

    public GeneralSettings getGeneralSettings() {
        return generalSettings;
    }

    public void updateLLMSettings(LLMSettings settings) {
        this.llmSettings = settings;
        save();
    }

    public void updateCultureSettings(String culture, CultureSettings settings) {
        this.cultureSettings.put(culture, settings);
        save();
    }

    public void updateGeneralSettings(GeneralSettings settings) {
        this.generalSettings = settings;
        save();
    }
}