package com.beeny.ai;

import com.beeny.ai.provider.*;
import com.beeny.setup.LLMConfig;
import com.beeny.setup.SystemSpecs;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final LLMService INSTANCE = new LLMService();
    private final Map<String, AIProvider> providers = new HashMap<>();
    private AIProvider currentProvider;
    private final Map<String, String> context = new HashMap<>();
    private LLMConfig config;
    private SystemSpecs systemSpecs;
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();

    private LLMService() {
        this.systemSpecs = new SystemSpecs();
        providers.put("deepseek", new DeepSeekProvider());
        providers.put("openrouter", new OpenRouterProvider());
        providers.put("mistral", new MistralProvider());
        providers.put("gemini", new GeminiProvider());
        providers.put("anthropic", new AnthropicProvider());
        providers.put("openai", new OpenAIProvider());
        providers.put("azure", new AzureOpenAIProvider());
        providers.put("cohere", new CohereProvider());
    }

    public static LLMService getInstance() {
        return INSTANCE;
    }

    public void initialize(LLMConfig config) {
        this.config = config;
        String providerName = config.getProvider();
        currentProvider = providers.get(providerName);
        if (currentProvider == null || config.isQuickStartMode()) {
            LOGGER.info("Using DeepSeek provider for {}",
                config.isQuickStartMode() ? "Quick Start mode" : "fallback");
            currentProvider = providers.get("deepseek");
        }
        Map<String, String> providerConfig = new HashMap<>();
        if (config.isQuickStartMode()) {
            providerConfig.put("apiKey", "");
            providerConfig.put("endpoint", "https://api.deepseek.ai/v1");
            providerConfig.put("modelName", "deepseek-coder");
        } else {
            providerConfig.put("apiKey", config.getApiKey());
            providerConfig.put("endpoint", config.getEndpoint());
            providerConfig.put("modelName", config.getModelType());
        }
        try {
            currentProvider.initialize(providerConfig);
            LOGGER.info("LLMService initialized with provider: {} (Quick Start: {})",
                currentProvider.getName(), config.isQuickStartMode());
        } catch (Exception e) {
            LOGGER.error("Failed to initialize LLM provider", e);
            LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
            errorHandler.reportErrorToClient(errorType, "Failed to initialize LLM provider: " + e.getMessage());
        }
    }

    public CompletableFuture<String> generateResponse(String prompt) {
        return generateResponse(prompt, new HashMap<>());
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (currentProvider == null || !currentProvider.isAvailable()) {
            String errorMessage = "No LLM provider available. Please check your configuration and API keys.";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, errorMessage);
            return CompletableFuture.failedFuture(new IllegalStateException(errorMessage));
        }

        String finalPrompt = simplifyPrompt(prompt);
        Map<String, String> fullContext = new HashMap<>(this.context);
        fullContext.putAll(context);

        return currentProvider.generateResponse(finalPrompt, fullContext)
            .orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(e -> {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                LOGGER.error("Error generating response", cause);
                
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(cause);
                errorHandler.reportErrorToClient(errorType, cause.getMessage());
                
                return getDefaultResponse(finalPrompt);
            });
    }

    public CompletableFuture<Boolean> testConnection() {
        if (currentProvider == null || !currentProvider.isAvailable()) {
            String errorMessage = "No LLM provider available. Please check your configuration and API keys.";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, errorMessage);
            return CompletableFuture.completedFuture(false);
        }

        return currentProvider.generateResponse("Test connection.", new HashMap<>())
            .orTimeout(10, TimeUnit.SECONDS)
            .thenApply(response -> true)
            .exceptionally(e -> {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                LOGGER.error("Connection test failed", cause);
                
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(cause);
                errorHandler.reportErrorToClient(errorType, 
                    "Connection test failed: " + cause.getMessage());
                
                return false;
            });
    }

    private String simplifyPrompt(String prompt) {
        if (prompt.length() > 500) {
            LOGGER.debug("Truncating long prompt of length {}", prompt.length());
            return prompt.substring(0, 500) + "...";
        }
        return prompt;
    }

    private String getDefaultResponse(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("name")) {
            return "Olivia the Village Helper";
        } else if (lowerPrompt.contains("personality")) {
            return "Friendly, helpful, and knowledgeable";
        } else {
            return "I'm sorry, I couldn't process that request. Please try again later.";
        }
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        for (Map.Entry<String, String> entry : context.entrySet()) {
            key.append("|").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return key.toString();
    }

    public void pruneCache() {
    }

    public CompletableFuture<List<String>> getRecipeSuggestions(
            ServerPlayerEntity player, VillagerEntity villager, String culture) {
        if (currentProvider == null || !currentProvider.isAvailable()) {
            LOGGER.warn("No AI provider available for recipe suggestions");
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        String prompt = String.format(
            "Generate 3-5 crafting recipe suggestions for a %s culture villager interacting with player %s. " +
            "The villager's profession is %s. Recipes should use common Minecraft items but reflect cultural themes. " +
            "Return each recipe on a new line in the format: 'RecipeName: Item1 xCount, Item2 xCount'",
            culture,
            player.getName().getString(),
            villager.getVillagerData().getProfession().toString()
        );

        return currentProvider.generateResponse(prompt, new HashMap<>())
            .thenApply(response -> {
                List<String> suggestions = new ArrayList<>();
                if (response != null && !response.isEmpty()) {
                    suggestions.addAll(Arrays.asList(response.split("\n")));
                    LOGGER.info("Generated {} recipe suggestions for culture {}", suggestions.size(), culture);
                }
                return suggestions;
            })
            .exceptionally(ex -> {
                LOGGER.error("Failed to generate recipe suggestions", ex);
                return new ArrayList<>();
            });
    }

    public void shutdown() {
        if (currentProvider != null) {
            currentProvider.shutdown();
        }
    }

    public void clearCache() {
        context.clear();
    }

    public void addToContext(String key, String value) {
        context.put(key, value);
    }

    public Map<String, String> getContextCache() {
        return new HashMap<>(context);
    }

    public boolean setProvider(String providerName) {
        AIProvider newProvider = providers.get(providerName);
        if (newProvider != null) {
            if (currentProvider != null) {
                currentProvider.shutdown();
            }
            currentProvider = newProvider;
            Map<String, String> providerConfig = new HashMap<>();
            providerConfig.put("apiKey", config.getApiKey());
            providerConfig.put("endpoint", config.getEndpoint());
            providerConfig.put("modelName", config.getModelType());
            
            try {
                currentProvider.initialize(providerConfig);
                LOGGER.info("Switched to provider: {}", currentProvider.getName());
                testConnection();
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to initialize provider: {}", providerName, e);
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, 
                    "Failed to initialize provider " + providerName + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public String getCurrentProviderName() {
        return currentProvider != null ? currentProvider.getName() : "None";
    }
}
