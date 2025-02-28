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
    private final UISettings uiSettings;
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
    
    public static class UISettings {
        public String conversationHudPosition = "BOTTOM_RIGHT";
        public String conversationLabelFormat = "Speaking to: {name}";
        public boolean showCulture = true;
        public boolean showProfession = true;
        public int backgroundColor = 0x80000000; // Semi-transparent black
        public int borderColor = 0xFFCCCCCC;     // Light gray
        public int labelColor = 0xFFFFFFFF;      // White
        public int nameColor = 0xFFFFAA00;       // Gold
        
        public UISettings() {
            // Default constructor with values already set
        }
    }

    private VillagesConfig() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("villagesreborn.json");
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        generalSettings = loadGeneralConfig();
        llmSettings = loadLLMConfig();
        uiSettings = loadUIConfig();
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
    
    public UISettings getUISettings() {
        return uiSettings;
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
    
    private UISettings loadUIConfig() {
        Path uiPath = configPath.resolveSibling("villagesreborn_ui.json");
        if (Files.exists(uiPath)) {
            try {
                String content = Files.readString(uiPath);
                return gson.fromJson(content, UISettings.class);
            } catch (IOException e) {
                return new UISettings();
            }
        }
        return new UISettings();
    }

    public void save() {
        try {
            Path generalPath = configPath.resolveSibling("villagesreborn_general.json");
            Path llmPath = configPath.resolveSibling("villagesreborn_llm.json");
            Path uiPath = configPath.resolveSibling("villagesreborn_ui.json");
            
            String generalJson = gson.toJson(generalSettings);
            String llmJson = gson.toJson(llmSettings);
            String uiJson = gson.toJson(uiSettings);
            
            Files.writeString(generalPath, generalJson);
            Files.writeString(llmPath, llmJson);
            Files.writeString(uiPath, uiJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
