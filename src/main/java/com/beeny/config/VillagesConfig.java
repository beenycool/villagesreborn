package com.beeny.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VillagesConfig {
    private static VillagesConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;
    private ConfigData data;
    
    private UISettings uiSettings;
    private LLMSettings llmSettings;

    private VillagesConfig() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("villagesreborn.json").toFile();
        load();
        uiSettings = new UISettings();
    }

    public static VillagesConfig getInstance() {
        if (instance == null) instance = new VillagesConfig();
        return instance;
    }

    private void load() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                data = gson.fromJson(reader, ConfigData.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (data == null) data = new ConfigData();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getVillageSpawnRate() {
        return data.villageSpawnRate;
    }

    public void cycleVillageSpawnRate() {
        data.villageSpawnRate = switch (data.villageSpawnRate) {
            case "LOW" -> "MEDIUM";
            case "MEDIUM" -> "HIGH";
            default -> "LOW";
        };
    }

    public String getAIProvider() {
        return data.aiProvider;
    }

    public void cycleAIProvider() {
        List<String> providers = new ArrayList<>(List.of(
            "OPENAI", "ANTHROPIC", "DEEPSEEK", "GEMINI", "MISTRAL", 
            "AZURE", "COHERE", "OPENROUTER"
        ));
        int currentIndex = providers.indexOf(data.aiProvider);
        data.aiProvider = providers.get((currentIndex + 1) % providers.size());
    }

    public List<String> getEnabledCultures() {
        return data.enabledCultures;
    }

    public boolean isVillagerPvPEnabled() {
        return data.villagerPvPEnabled;
    }

    public void toggleVillagerPvP() {
        data.villagerPvPEnabled = !data.villagerPvPEnabled;
    }

    public boolean isTheftDetectionEnabled() {
        return data.theftDetectionEnabled;
    }

    public void toggleTheftDetection() {
        data.theftDetectionEnabled = !data.theftDetectionEnabled;
    }
    
    public UISettings getUISettings() {
        return uiSettings;
    }
    
    public LLMSettings getLLMSettings() {
        return llmSettings;
    }

    private static class ConfigData {
        String villageSpawnRate = "MEDIUM";
        String aiProvider = "OPENAI";
        List<String> enabledCultures = new ArrayList<>(List.of("ROMAN", "EGYPTIAN", "VICTORIAN", "NYC", "NETHER", "END"));
        boolean villagerPvPEnabled = false;
        boolean theftDetectionEnabled = true;
    }
    
    public class UISettings {
        private boolean showVillagerNameTags = true;
        private boolean showVillagerHealthBars = true;
        private boolean showVillageMarkers = true;
        private int villageMarkerRange = 64;
        private boolean compactVillagerInfo = false;
        private String colorScheme = "DEFAULT";
        
        private String conversationLabelFormat = "Speaking to: {name}";
        private String conversationHudPosition = "BOTTOM_RIGHT";
        private boolean showCulture = true;
        private boolean showProfession = true;
        private int backgroundColor = 0x80000000;
        private int borderColor = 0x80FFFFFF;
        private int labelColor = 0xFFFFFFFF;
        private int nameColor = 0xFFFFFF00;
        
        public boolean isShowVillagerNameTags() {
            return showVillagerNameTags;
        }
        
        public void setShowVillagerNameTags(boolean showVillagerNameTags) {
            this.showVillagerNameTags = showVillagerNameTags;
        }
        
        public boolean isShowVillagerHealthBars() {
            return showVillagerHealthBars;
        }
        
        public void setShowVillagerHealthBars(boolean showVillagerHealthBars) {
            this.showVillagerHealthBars = showVillagerHealthBars;
        }
        
        public boolean isShowVillageMarkers() {
            return showVillageMarkers;
        }
        
        public void setShowVillageMarkers(boolean showVillageMarkers) {
            this.showVillageMarkers = showVillageMarkers;
        }
        
        public int getVillageMarkerRange() {
            return villageMarkerRange;
        }
        
        public void setVillageMarkerRange(int villageMarkerRange) {
            this.villageMarkerRange = Math.max(16, Math.min(256, villageMarkerRange));
        }
        
        public boolean isCompactVillagerInfo() {
            return compactVillagerInfo;
        }
        
        public void setCompactVillagerInfo(boolean compactVillagerInfo) {
            this.compactVillagerInfo = compactVillagerInfo;
        }
        
        public String getColorScheme() {
            return colorScheme;
        }
        
        public void setColorScheme(String colorScheme) {
            this.colorScheme = colorScheme;
        }
        
        public void toggleNameTags() {
            this.showVillagerNameTags = !this.showVillagerNameTags;
        }
        
        public void toggleHealthBars() {
            this.showVillagerHealthBars = !this.showVillagerHealthBars;
        }
        
        public void toggleVillageMarkers() {
            this.showVillageMarkers = !this.showVillageMarkers;
        }
        
        public void toggleCompactInfo() {
            this.compactVillagerInfo = !this.compactVillagerInfo;
        }
        
        public String getConversationLabelFormat() {
            return conversationLabelFormat;
        }
        
        public void setConversationLabelFormat(String conversationLabelFormat) {
            this.conversationLabelFormat = conversationLabelFormat;
        }
        
        public String getConversationHudPosition() {
            return conversationHudPosition;
        }
        
        public void setConversationHudPosition(String conversationHudPosition) {
            this.conversationHudPosition = conversationHudPosition;
        }
        
        public boolean isShowCulture() {
            return showCulture;
        }
        
        public void setShowCulture(boolean showCulture) {
            this.showCulture = showCulture;
        }
        
        public boolean isShowProfession() {
            return showProfession;
        }
        
        public void setShowProfession(boolean showProfession) {
            this.showProfession = showProfession;
        }
        
        public int getBackgroundColor() {
            return backgroundColor;
        }
        
        public void setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
        }
        
        public int getBorderColor() {
            return borderColor;
        }
        
        public void setBorderColor(int borderColor) {
            this.borderColor = borderColor;
        }
        
        public int getLabelColor() {
            return labelColor;
        }
        
        public void setLabelColor(int labelColor) {
            this.labelColor = labelColor;
        }
        
        public int getNameColor() {
            return nameColor;
        }
        
        public void setNameColor(int nameColor) {
            this.nameColor = nameColor;
        }
    }
    
    public class LLMSettings {
        private String apiKey = "";
        private String model = "gpt-3.5-turbo";
        private String endpoint = "https://api.openai.com/v1/chat/completions";
        private int maxTokens = 1000;
        private boolean localModel = false;
        private String localModelPath = "";
        private int contextLength = 4;
        private boolean useMemory = true;
        private String modelType = "gpt-3.5-turbo";
        private float temperature = 0.7f;
        private int maxCacheSize = 100;
        private int cacheTTLSeconds = 300;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public boolean isLocalModel() {
            return localModel;
        }
        
        public void setLocalModel(boolean localModel) {
            this.localModel = localModel;
        }
        
        public String getLocalModelPath() {
            return localModelPath;
        }
        
        public void setLocalModelPath(String localModelPath) {
            this.localModelPath = localModelPath;
        }
        
        public int getContextLength() {
            return contextLength;
        }
        
        public void setContextLength(int contextLength) {
            this.contextLength = contextLength;
        }
        
        public boolean isUseMemory() {
            return useMemory;
        }
        
        public void setUseMemory(boolean useMemory) {
            this.useMemory = useMemory;
        }
        
        public String getModelType() {
            return modelType;
        }
        
        public void setModelType(String modelType) {
            this.modelType = modelType;
        }
        
        public float getTemperature() {
            return temperature;
        }
        
        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }
        
        public int getMaxCacheSize() {
            return maxCacheSize;
        }
        
        public void setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }
        
        public int getCacheTTLSeconds() {
            return cacheTTLSeconds;
        }
        
        public void setCacheTTLSeconds(int cacheTTLSeconds) {
            this.cacheTTLSeconds = cacheTTLSeconds;
        }
    }
}
