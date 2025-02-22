package com.beeny.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VillagesConfig {
    private static VillagesConfig instance;
    private final Path configPath;
    private final GeneralSettings generalSettings;
    private final LLMSettings llmSettings;
    private final Gson gson;

    public static class LLMSettings {
        public String modelType = "gpt-3.5-turbo";
        public String endpoint = "https://api.openai.com/v1";
        public String apiKey = "";
        public String provider = "openai";
        public int maxCacheSize = 100;
        public int contextLength = 2048;
        public double temperature = 0.7;
        public boolean useGPU = false;
        public long cacheTTLSeconds = 3600;
    }

    public static class GeneralSettings {
        public Map<String, Boolean> features;
        public boolean showWelcomeSequence;
        public boolean showWelcomeForReturningPlayers;

        public GeneralSettings() {
            features = new HashMap<>();
            features.put("custom_ai", true);
            features.put("llm_behaviors", true);
            features.put("cultural_events", true);
            features.put("detailed_animations", true);
            showWelcomeSequence = true;
            showWelcomeForReturningPlayers = false;
        }
    }

    private VillagesConfig() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("villagesreborn.json");
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        generalSettings = loadGeneralConfig();
        llmSettings = loadLLMConfig();
    }

    public static VillagesConfig getInstance() {
        if (instance == null) {
            instance = new VillagesConfig();
        }
        return instance;
    }

    public GeneralSettings getGeneralSettings() {
        return generalSettings;
    }

    public LLMSettings getLLMSettings() {
        return llmSettings;
    }

    private GeneralSettings loadGeneralConfig() {
        Path generalPath = configPath.resolveSibling("villagesreborn_general.json");
        if (Files.exists(generalPath)) {
            try {
                String content = Files.readString(generalPath);
                return gson.fromJson(content, GeneralSettings.class);
            } catch (IOException e) {
                return new GeneralSettings();
            }
        }
        return new GeneralSettings();
    }

    private LLMSettings loadLLMConfig() {
        Path llmPath = configPath.resolveSibling("villagesreborn_llm.json");
        if (Files.exists(llmPath)) {
            try {
                String content = Files.readString(llmPath);
                return gson.fromJson(content, LLMSettings.class);
            } catch (IOException e) {
                return new LLMSettings();
            }
        }
        return new LLMSettings();
    }

    public void save() {
        try {
            Path generalPath = configPath.resolveSibling("villagesreborn_general.json");
            Path llmPath = configPath.resolveSibling("villagesreborn_llm.json");
            
            String generalJson = gson.toJson(generalSettings);
            String llmJson = gson.toJson(llmSettings);
            
            Files.writeString(generalPath, generalJson);
            Files.writeString(llmPath, llmJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
