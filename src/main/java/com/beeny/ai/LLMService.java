package com.beeny.ai;

import com.beeny.ai.provider.AIProvider;
import com.beeny.ai.provider.AzureOpenAIProvider;
import com.beeny.ai.provider.OpenAIProvider;
import com.beeny.ai.provider.AnthropicProvider;
import com.beeny.ai.provider.GeminiProvider;
import com.beeny.ai.provider.CohereProvider;
import com.beeny.ai.provider.MistralProvider;
import com.beeny.setup.LLMConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final LLMService INSTANCE = new LLMService();
    private static final int MAX_RETRIES = 3;

    private final Map<String, AIProvider> providers = new HashMap<>();
    private AIProvider currentProvider;
    private final Map<String, String> contextCache = new HashMap<>();
    private LLMConfig config;

    private LLMService() {
        // Register available providers
        providers.put("azure", new AzureOpenAIProvider());
        providers.put("openai", new OpenAIProvider());
        providers.put("anthropic", new AnthropicProvider());
        providers.put("gemini", new GeminiProvider());
        providers.put("cohere", new CohereProvider());
        providers.put("mistral", new MistralProvider());
    }

    public static LLMService getInstance() {
        return INSTANCE;
    }

    public void initialize(LLMConfig config) {
        this.config = config;
        String providerName = config.getProvider();
        currentProvider = providers.get(providerName);
        
        if (currentProvider == null) {
            LOGGER.error("Provider {} not found, falling back to Azure", providerName);
            currentProvider = providers.get("azure");
        }

        Map<String, String> providerConfig = new HashMap<>();
        providerConfig.put("apiKey", config.getApiKey());
        providerConfig.put("endpoint", config.getEndpoint());
        providerConfig.put("modelName", config.getModelType());
        
        currentProvider.initialize(providerConfig);
        LOGGER.info("LLMService initialized with provider: {}", currentProvider.getName());
    }

    public CompletableFuture<String> generateResponse(String prompt) {
        return generateResponse(prompt, new HashMap<>());
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (currentProvider == null || !currentProvider.isAvailable()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No available AI provider")
            );
        }

        String cacheKey = generateCacheKey(prompt, context);
        if (contextCache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(contextCache.get(cacheKey));
        }

        return currentProvider.generateResponse(prompt, context)
            .thenApply(response -> {
                contextCache.put(cacheKey, response);
                return response;
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating response", e);
                return getDefaultResponse(prompt);
            });
    }

    private String getDefaultResponse(String prompt) {
        if (prompt.contains("farmer")) {
            return "ACTION: work\nTARGET: nearest_farm\nDURATION: 6000\nDETAIL: tending crops";
        }
        return "ACTION: walk\nTARGET: village_center\nDURATION: 2400\nDETAIL: patrolling";
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
    }

    public void pruneCache() {
        contextCache.clear();
    }

    public void shutdown() {
        LOGGER.info("Shutting down LLMService");
        if (currentProvider != null) {
            currentProvider.shutdown();
        }
        contextCache.clear();
    }

    public void clearCache() {
        contextCache.clear();
    }

    public void addToContext(String key, String value) {
        contextCache.put("context_" + key, value);
    }

    public Map<String, String> getContextCache() {
        return new HashMap<>(contextCache);
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
            currentProvider.initialize(providerConfig);
            LOGGER.info("Switched to provider: {}", currentProvider.getName());
            return true;
        }
        return false;
    }

    public String getCurrentProviderName() {
        return currentProvider != null ? currentProvider.getName() : "None";
    }
}
