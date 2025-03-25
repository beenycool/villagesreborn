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
    
    // Add UISettings instance
    private UISettings uiSettings;

    private VillagesConfig() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("villagesreborn.json").toFile();
        load();
        // Initialize UISettings
        uiSettings = new UISettings();
    }

    public static VillagesConfig getInstance() {
        if (instance == null) {
            instance = new VillagesConfig();
        }
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
        if (data == null) {
            data = new ConfigData();
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
    
    // Get UISettings
    public UISettings getUISettings() {
        return uiSettings;
    }

    private static class ConfigData {
        String villageSpawnRate = "MEDIUM";
        String aiProvider = "OPENAI";
        List<String> enabledCultures = new ArrayList<>(List.of("ROMAN", "EGYPTIAN", "VICTORIAN", "NYC", "NETHER", "END"));
        boolean villagerPvPEnabled = false;
        boolean theftDetectionEnabled = true;
    }
    
    /**
     * Settings related to the UI display and customization
     */
    public class UISettings {
        private boolean showVillagerNameTags = true;
        private boolean showVillagerHealthBars = true;
        private boolean showVillageMarkers = true;
        private int villageMarkerRange = 64;
        private boolean compactVillagerInfo = false;
        private String colorScheme = "DEFAULT";
        
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
    }
}
