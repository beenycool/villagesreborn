package com.beeny.setup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LLMConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String CONFIG_FILE = "villagesreborn_llm.json";
    
    private final Path configPath;
    private final transient Gson gson;
    
    @Expose private String modelType = "deepseek-coder";
    @Expose private String endpoint = "https://api.deepseek.ai/v1";
    @Expose private String apiKey = "";
    @Expose private String provider = "deepseek";
    @Expose private int contextLength = 2048;
    @Expose private double temperature = 0.7;
    @Expose private boolean useGPU = false;
    @Expose private boolean setupComplete = false;
    @Expose private Map<UUID, Boolean> welcomeSequenceShown = new HashMap<>();

    public LLMConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        this.gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();
    }

    public void initialize(SystemSpecs specs) {
        LOGGER.info("Initializing LLM configuration with system specs");
        useGPU = specs.hasGpuSupport();
        
        // Adjust context length based on available RAM
        if (specs.getAvailableRam() > 8192) { // More than 8GB
            contextLength = 4096;
        } else if (specs.getAvailableRam() > 4096) { // More than 4GB
            contextLength = 2048;
        } else {
            contextLength = 1024;
        }

        LOGGER.info("Initialized with: contextLength={}, useGPU={}", contextLength, useGPU);
        saveConfig();
    }

    public void load() {
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                LLMConfig loaded = gson.fromJson(json, LLMConfig.class);
                this.modelType = loaded.modelType;
                this.endpoint = loaded.endpoint;
                this.apiKey = loaded.apiKey;
                this.provider = loaded.provider;
                this.contextLength = loaded.contextLength;
                this.temperature = loaded.temperature;
                this.useGPU = loaded.useGPU;
                this.setupComplete = loaded.setupComplete;
                this.welcomeSequenceShown = loaded.welcomeSequenceShown;
                LOGGER.info("Loaded LLM configuration from {}", configPath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load LLM configuration", e);
        }
    }

    public void saveConfig() {
        try {
            String json = gson.toJson(this);
            Files.writeString(configPath, json);
            LOGGER.info("Saved LLM configuration to {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to save LLM configuration", e);
        }
    }

    public String getModelType() {
        return modelType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getProvider() {
        return provider;
    }

    public int getContextLength() {
        return contextLength;
    }

    public double getTemperature() {
        return temperature;
    }

    public boolean isUseGPU() {
        return useGPU;
    }

    public boolean isSetupComplete() {
        return setupComplete;
    }

    public boolean isConfigured() {
        return !apiKey.isEmpty() && setupComplete;
    }

    public boolean hasSeenWelcomeSequence(UUID playerId) {
        return welcomeSequenceShown.getOrDefault(playerId, false);
    }

    public void setWelcomeSequenceShown(UUID playerId) {
        welcomeSequenceShown.put(playerId, true);
        saveConfig();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        saveConfig();
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        saveConfig();
    }

    public void setProvider(String provider) {
        this.provider = provider;
        saveConfig();
    }

    public void setModelType(String modelType) {
        this.modelType = modelType;
        saveConfig();
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
        saveConfig();
    }

    public void setContextLength(int contextLength) {
        this.contextLength = contextLength;
        saveConfig();
    }

    public void setSetupComplete(boolean setupComplete) {
        this.setupComplete = setupComplete;
        saveConfig();
    }

    public void setUseGPU(boolean useGPU) {
        this.useGPU = useGPU;
        saveConfig();
    }
}
