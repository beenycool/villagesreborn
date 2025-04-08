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
    private VillagesConfig() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("villagesreborn.json").toFile();
        load();
        // Nested settings objects are now part of ConfigData and initialized there
    }

    public static VillagesConfig getInstance() {
        if (instance == null) instance = new VillagesConfig();
        return instance;
    }

    public void load() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                data = gson.fromJson(reader, ConfigData.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (data == null) {
            data = new ConfigData();
            // Ensure nested objects are initialized if creating new data
            if (data.uiSettings == null) data.uiSettings = new UISettings();
            if (data.llmSettings == null) data.llmSettings = new LLMSettings();
            if (data.gameplaySettings == null) data.gameplaySettings = new GameplaySettings();
        }
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

    // AI Provider is now managed within LLMSettings

    public List<String> getEnabledCultures() {
        return data.enabledCultures;
    }

    // Gameplay settings (PvP, Theft, Memory, Trading, Crafting, Gifts) are now managed within GameplaySettings
    
    public UISettings getUISettings() {
        // Ensure data and nested object are not null
        if (data == null) load(); // Try loading if data is null
        if (data == null) data = new ConfigData(); // Create if still null
        if (data.uiSettings == null) data.uiSettings = new UISettings(); // Initialize if null
        return data.uiSettings;
    }

    public LLMSettings getLLMSettings() {
        if (data == null) load();
        if (data == null) data = new ConfigData();
        if (data.llmSettings == null) data.llmSettings = new LLMSettings();
        return data.llmSettings;
    }

    public GameplaySettings getGameplaySettings() {
        if (data == null) load();
        if (data == null) data = new ConfigData();
        if (data.gameplaySettings == null) data.gameplaySettings = new GameplaySettings();
        return data.gameplaySettings;
    }

    private static class ConfigData {
        String villageSpawnRate = "MEDIUM";
        // aiProvider removed, now in LLMSettings
        List<String> enabledCultures = new ArrayList<>(List.of("ROMAN", "EGYPTIAN", "VICTORIAN", "NYC", "NETHER", "END"));
        // Gameplay settings moved to GameplaySettings class
        
        // Hold instances of the settings classes
        UISettings uiSettings = new UISettings();
        LLMSettings llmSettings = new LLMSettings();
        GameplaySettings gameplaySettings = new GameplaySettings();
    }
    
    public static class UISettings { // Made static
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
    
    public static class LLMSettings { // Made static
        private String apiKey = "";
        // private String model = ""; // Removed redundant field
        private String endpoint = ""; // Default endpoint (provider-specific, often optional)
        private int maxTokens = 1000;
        private boolean localModel = false;
        private String localModelPath = "";
        private int contextLength = 4;
        private boolean useMemory = true;
        private String model = ""; // Renamed from modelType back to model for clarity
        private float temperature = 0.7f;
        private int maxCacheSize = 100;
        private int cacheTTLSeconds = 300;
        private boolean advancedConversationsEnabled = false;
        private String aiDetailLevel = "BALANCED";
        private int aiResponseDelay = 0;
        private String provider = "deepseek"; // Add provider field with default value
        private boolean developerModeEnabled = false; // Added for simplified UI toggle

        public String getProvider() {
            return provider;
        }
        
        public void setProvider(String provider) {
            this.provider = provider;
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        // Removed getter/setter for the redundant 'model' field
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
        
        public String getModel() { // Renamed from getModelType
            return model;
        }

        public void setModel(String model) { // Renamed from setModelType
            this.model = model;
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
        
        public boolean isAdvancedConversationsEnabled() {
            return advancedConversationsEnabled;
        }
        
        public void setAdvancedConversationsEnabled(boolean advancedConversationsEnabled) {
            this.advancedConversationsEnabled = advancedConversationsEnabled;
        }
        
        public String getAiDetailLevel() {
            return aiDetailLevel;
        }
        
        public void setAiDetailLevel(String aiDetailLevel) {
            this.aiDetailLevel = aiDetailLevel;
        }
        
        public void cycleAiDetailLevel() {
            this.aiDetailLevel = switch (this.aiDetailLevel) {
                case "MINIMAL" -> "BALANCED";
                case "BALANCED" -> "DETAILED";
                case "DETAILED" -> "CUSTOM";
                default -> "MINIMAL";
            };
        }
        
        public int getAiResponseDelay() {
            return aiResponseDelay;
        }
        
        public void setAiResponseDelay(int aiResponseDelay) {
            this.aiResponseDelay = Math.max(0, Math.min(5000, aiResponseDelay));
        }
        
        // Moved from VillagesConfig main class
        public boolean isDeveloperModeEnabled() {
            return developerModeEnabled;
        }
        
        // Moved from VillagesConfig main class
        public void setDeveloperModeEnabled(boolean developerModeEnabled) {
            this.developerModeEnabled = developerModeEnabled;
        }
    }
    
    public static class GameplaySettings { // Made static
        private float eventFrequencyMultiplier = 1.0f;
        private float villagerActivityFrequencyMultiplier = 1.0f;
        private float tradingFrequencyMultiplier = 1.0f;
        private float relationshipIntensityMultiplier = 1.0f;
        private float culturalEventMultiplier = 1.0f;
        private boolean progressiveVillageDevelopment = true;
        private boolean dynamicEconomyEnabled = true;
        private boolean weatherAffectsEvents = true;
        private boolean villageDefenseEnabled = true;
        private boolean resourceGatheringEnabled = true;
        private boolean dynamicPopulationGrowth = true;
        private boolean culturalBiasEnabled = true;
        private int minDaysBetweenEvents = 2;
        private boolean showEventNotifications = true;
        private int maxConcurrentEvents = 3;
        private boolean playerReputationAffectsEvents = true;

        // Added fields moved from ConfigData
        private boolean villagerPvPEnabled = false;
        private boolean theftDetectionEnabled = true;
        private int villagerMemoryDuration = 3; // days
        private boolean villagerTradingBoostEnabled = true;
        private boolean uniqueCraftingRecipesEnabled = true;
        private int culturalGiftModifier = 150; // percentage
        
        public float getEventFrequencyMultiplier() {
            return eventFrequencyMultiplier;
        }
        
        public void setEventFrequencyMultiplier(float multiplier) {
            this.eventFrequencyMultiplier = Math.max(0.25f, Math.min(5.0f, multiplier));
        }
        
        public float getVillagerActivityFrequencyMultiplier() {
            return villagerActivityFrequencyMultiplier;
        }
        
        public void setVillagerActivityFrequencyMultiplier(float multiplier) {
            this.villagerActivityFrequencyMultiplier = Math.max(0.25f, Math.min(5.0f, multiplier));
        }
        
        public float getTradingFrequencyMultiplier() {
            return tradingFrequencyMultiplier;
        }
        
        public void setTradingFrequencyMultiplier(float multiplier) {
            this.tradingFrequencyMultiplier = Math.max(0.25f, Math.min(5.0f, multiplier));
        }
        
        public float getRelationshipIntensityMultiplier() {
            return relationshipIntensityMultiplier;
        }
        
        public void setRelationshipIntensityMultiplier(float multiplier) {
            this.relationshipIntensityMultiplier = Math.max(0.25f, Math.min(5.0f, multiplier));
        }
        
        public float getCulturalEventMultiplier() {
            return culturalEventMultiplier;
        }
        
        public void setCulturalEventMultiplier(float multiplier) {
            this.culturalEventMultiplier = Math.max(0.25f, Math.min(5.0f, multiplier));
        }
        
        public boolean isProgressiveVillageDevelopment() {
            return progressiveVillageDevelopment;
        }
        
        public void setProgressiveVillageDevelopment(boolean enabled) {
            this.progressiveVillageDevelopment = enabled;
        }
        
        public void toggleProgressiveVillageDevelopment() {
            this.progressiveVillageDevelopment = !this.progressiveVillageDevelopment;
        }
        
        public boolean isDynamicEconomyEnabled() {
            return dynamicEconomyEnabled;
        }
        
        public void setDynamicEconomyEnabled(boolean enabled) {
            this.dynamicEconomyEnabled = enabled;
        }
        
        public void toggleDynamicEconomy() {
            this.dynamicEconomyEnabled = !this.dynamicEconomyEnabled;
        }
        
        public boolean isWeatherAffectsEvents() {
            return weatherAffectsEvents;
        }
        
        public void setWeatherAffectsEvents(boolean enabled) {
            this.weatherAffectsEvents = enabled;
        }
        
        public void toggleWeatherAffectsEvents() {
            this.weatherAffectsEvents = !this.weatherAffectsEvents;
        }
        
        public boolean isVillageDefenseEnabled() {
            return villageDefenseEnabled;
        }
        
        public void setVillageDefenseEnabled(boolean enabled) {
            this.villageDefenseEnabled = enabled;
        }
        
        public void toggleVillageDefense() {
            this.villageDefenseEnabled = !this.villageDefenseEnabled;
        }
        
        public boolean isResourceGatheringEnabled() {
            return resourceGatheringEnabled;
        }
        
        public void setResourceGatheringEnabled(boolean enabled) {
            this.resourceGatheringEnabled = enabled;
        }
        
        public void toggleResourceGathering() {
            this.resourceGatheringEnabled = !this.resourceGatheringEnabled;
        }
        
        public boolean isDynamicPopulationGrowth() {
            return dynamicPopulationGrowth;
        }
        
        public void setDynamicPopulationGrowth(boolean enabled) {
            this.dynamicPopulationGrowth = enabled;
        }
        
        public void toggleDynamicPopulationGrowth() {
            this.dynamicPopulationGrowth = !this.dynamicPopulationGrowth;
        }
        
        public boolean isCulturalBiasEnabled() {
            return culturalBiasEnabled;
        }
        
        public void setCulturalBiasEnabled(boolean enabled) {
            this.culturalBiasEnabled = enabled;
        }
        
        public void toggleCulturalBias() {
            this.culturalBiasEnabled = !this.culturalBiasEnabled;
        }
        
        public int getMinDaysBetweenEvents() {
            return minDaysBetweenEvents;
        }
        
        public void setMinDaysBetweenEvents(int days) {
            this.minDaysBetweenEvents = Math.max(0, Math.min(14, days));
        }
        
        public boolean isShowEventNotifications() {
            return showEventNotifications;
        }
        
        public void setShowEventNotifications(boolean enabled) {
            this.showEventNotifications = enabled;
        }
        
        public void toggleShowEventNotifications() {
            this.showEventNotifications = !this.showEventNotifications;
        }
        
        public int getMaxConcurrentEvents() {
            return maxConcurrentEvents;
        }
        
        public void setMaxConcurrentEvents(int count) {
            this.maxConcurrentEvents = Math.max(1, Math.min(10, count));
        }
        
        public boolean isPlayerReputationAffectsEvents() {
            return playerReputationAffectsEvents;
        }
        
        public void setPlayerReputationAffectsEvents(boolean enabled) {
            this.playerReputationAffectsEvents = enabled;
        }
        
        public void togglePlayerReputationAffectsEvents() {
            this.playerReputationAffectsEvents = !this.playerReputationAffectsEvents;
        }
        
        // --- Gameplay Settings Getters/Setters/Togglers ---
        
        public boolean isVillagerPvPEnabled() {
            return villagerPvPEnabled;
        }
    
        public void toggleVillagerPvP() {
            this.villagerPvPEnabled = !this.villagerPvPEnabled;
        }
        
        public void setVillagerPvPEnabled(boolean enabled) { // Added explicit setter
             this.villagerPvPEnabled = enabled;
        }
    
        public boolean isTheftDetectionEnabled() {
            return theftDetectionEnabled;
        }
    
        public void toggleTheftDetection() {
            this.theftDetectionEnabled = !this.theftDetectionEnabled;
        }
        
        public void setTheftDetectionEnabled(boolean enabled) { // Added explicit setter
             this.theftDetectionEnabled = enabled;
        }
    
        public int getVillagerMemoryDuration() {
            return villagerMemoryDuration;
        }
    
        public void setVillagerMemoryDuration(int days) {
            this.villagerMemoryDuration = Math.max(1, Math.min(7, days));
        }
    
        public boolean isVillagerTradingBoostEnabled() {
            return villagerTradingBoostEnabled;
        }
    
        public void toggleVillagerTradingBoost() {
            this.villagerTradingBoostEnabled = !this.villagerTradingBoostEnabled;
        }
        
        public void setVillagerTradingBoostEnabled(boolean enabled) { // Added explicit setter
             this.villagerTradingBoostEnabled = enabled;
        }
    
        public boolean isUniqueCraftingRecipesEnabled() {
            return uniqueCraftingRecipesEnabled;
        }
    
        public void toggleUniqueCraftingRecipes() {
            this.uniqueCraftingRecipesEnabled = !this.uniqueCraftingRecipesEnabled;
        }
        
        public void setUniqueCraftingRecipesEnabled(boolean enabled) { // Added explicit setter
             this.uniqueCraftingRecipesEnabled = enabled;
        }
    
        public int getCulturalGiftModifier() {
            return culturalGiftModifier;
        }
    
        public void setCulturalGiftModifier(int percentage) {
            this.culturalGiftModifier = Math.max(50, Math.min(200, percentage));
        }
    }
}
