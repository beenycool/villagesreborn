package com.beeny.villagesreborn.core.llm;

import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Manages LLM providers, API key validation, and model selection
 */
public class LLMProviderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LLMProviderManager.class);
    
    private final LLMApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final Map<LLMProvider, List<String>> modelCache;
    
    // API key validation patterns
    private static final Pattern OPENAI_KEY_PATTERN = Pattern.compile("^sk-[A-Za-z0-9]{40,60}$");
    private static final Pattern ANTHROPIC_KEY_PATTERN = Pattern.compile("^sk-ant-api03-[A-Za-z0-9\\-]{60,100}$");
    private static final Pattern OPENROUTER_KEY_PATTERN = Pattern.compile("^sk-or-v1-[A-Za-z0-9]{60,80}$");
    private static final Pattern GROQ_KEY_PATTERN = Pattern.compile("^gsk_[A-Za-z0-9]{40,70}$");

    public LLMProviderManager(LLMApiClient apiClient) {
        this.apiClient = apiClient;
        this.objectMapper = new ObjectMapper();
        this.modelCache = new ConcurrentHashMap<>();
        initializeStaticModels();
    }

    /**
     * Validate API key format for the specified provider
     */
    public boolean validateApiKey(LLMProvider provider, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }

        return switch (provider) {
            case OPENAI -> OPENAI_KEY_PATTERN.matcher(apiKey).matches();
            case ANTHROPIC -> ANTHROPIC_KEY_PATTERN.matcher(apiKey).matches();
            case OPENROUTER -> OPENROUTER_KEY_PATTERN.matcher(apiKey).matches();
            case GROQ -> GROQ_KEY_PATTERN.matcher(apiKey).matches();
            case LOCAL -> true; // Local doesn't require API key validation
        };
    }

    /**
     * Validate API key by making an actual API call
     */
    public CompletableFuture<Boolean> validateApiKeyWithApi(LLMProvider provider, String apiKey) {
        if (!validateApiKey(provider, apiKey)) {
            return CompletableFuture.completedFuture(false);
        }
        
        return apiClient.validateKey(provider, apiKey);
    }

    /**
     * Get available models for a provider (cached)
     */
    public List<String> getAvailableModels(LLMProvider provider) {
        return modelCache.computeIfAbsent(provider, this::getStaticModels);
    }

    /**
     * Fetch dynamic models from providers that support it
     */
    public CompletableFuture<List<String>> fetchDynamicModels(LLMProvider provider) {
        if (!provider.supportsDynamicModels()) {
            return CompletableFuture.completedFuture(getAvailableModels(provider));
        }

        return apiClient.fetchModels(provider)
            .thenApply(this::parseModelsFromJson)
            .thenApply(models -> {
                // Update cache with fetched models
                modelCache.put(provider, models);
                return models;
            });
    }

    /**
     * Get recommended models based on hardware tier
     */
    public List<String> getRecommendedModels(LLMProvider provider, HardwareTier hardwareTier) {
        List<String> allModels = getAvailableModels(provider);
        
        return switch (hardwareTier) {
            case HIGH -> allModels; // All models available for high-tier
            case MEDIUM -> filterModelsForMediumTier(allModels);
            case LOW -> filterModelsForLowTier(allModels);
            case UNKNOWN -> filterModelsForLowTier(allModels); // Conservative approach
        };
    }

    /**
     * Clear the model cache
     */
    public void clearModelCache() {
        modelCache.clear();
        initializeStaticModels();
    }

    private void initializeStaticModels() {
        // Pre-populate cache with known static models
        for (LLMProvider provider : LLMProvider.values()) {
            if (!provider.supportsDynamicModels()) {
                modelCache.put(provider, getStaticModels(provider));
            }
        }
    }

    private List<String> getStaticModels(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> Arrays.asList(
                "gpt-4",
                "gpt-4-turbo",
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k"
            );
            case ANTHROPIC -> Arrays.asList(
                "claude-3-opus-20240229",
                "claude-3-sonnet-20240229",
                "claude-3-haiku-20240307",
                "claude-2.1",
                "claude-2.0"
            );
            case GROQ -> Arrays.asList(
                "llama2-70b-4096",
                "mixtral-8x7b-32768",
                "gemma-7b-it"
            );
            case LOCAL -> Arrays.asList(
                "llama2:7b",
                "llama2:13b",
                "codellama:7b",
                "mistral:7b"
            );
            case OPENROUTER -> Arrays.asList(
                "openai/gpt-4",
                "anthropic/claude-3-opus",
                "meta-llama/llama-2-70b-chat",
                // Add medium-tier friendly models for OPENROUTER
                "openai/gpt-3.5-turbo",
                "anthropic/claude-3-haiku",
                "meta-llama/llama-2-7b-chat"
            ); // Default models, will be replaced by dynamic fetch
        };
    }

    private List<String> parseModelsFromJson(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode dataNode = root.get("data");
            
            List<String> models = new ArrayList<>();
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode modelNode : dataNode) {
                    JsonNode idNode = modelNode.get("id");
                    if (idNode != null) {
                        models.add(idNode.asText());
                    }
                }
            }
            
            return models;
        } catch (Exception e) {
            LOGGER.error("Failed to parse models from JSON response", e);
            return Collections.emptyList();
        }
    }

    private List<String> filterModelsForMediumTier(List<String> allModels) {
        // Filter out the most resource-intensive models
        return allModels.stream()
            .filter(model -> !isHighEndModel(model))
            .toList();
    }

    private List<String> filterModelsForLowTier(List<String> allModels) {
        // Only include lightweight models
        return allModels.stream()
            .filter(this::isLightweightModel)
            .toList();
    }

    private boolean isHighEndModel(String model) {
        String modelLower = model.toLowerCase();
        return modelLower.contains("gpt-4") ||
               modelLower.contains("claude-3-opus") ||
               modelLower.contains("70b") ||
               modelLower.contains("175b");
    }

    private boolean isLightweightModel(String model) {
        String modelLower = model.toLowerCase();
        return modelLower.contains("3.5-turbo") ||
               modelLower.contains("haiku") ||
               modelLower.contains("7b") ||
               modelLower.contains("gemma") ||
               modelLower.contains("mistral");
    }
}