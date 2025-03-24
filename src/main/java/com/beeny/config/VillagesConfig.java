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

    private static class ConfigData {
        String villageSpawnRate = "MEDIUM";
        String aiProvider = "OPENAI";
        List<String> enabledCultures = new ArrayList<>(List.of("ROMAN", "EGYPTIAN", "VICTORIAN", "NYC"));
        boolean villagerPvPEnabled = false;
        boolean theftDetectionEnabled = true;
    }
}
