package com.beeny.ai;

import com.beeny.ai.provider.*;
import com.beeny.setup.LLMConfig;
import com.beeny.setup.SystemSpecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final LLMService INSTANCE = new LLMService();
    private final Map<String, AIProvider> providers = new HashMap<>();
    private AIProvider currentProvider;
    private final Map<String, String> contextCache = new HashMap<>();
    private LLMConfig config;
    private SystemSpecs systemSpecs;

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
        currentProvider.initialize(providerConfig);
        LOGGER.info("LLMService initialized with provider: {} (Quick Start: {})",
            currentProvider.getName(), config.isQuickStartMode());
    }

    public CompletableFuture<String> generateResponse(String prompt) {
        return generateResponse(prompt, new HashMap<>());
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        systemSpecs.updatePerformanceMetrics();
        String cacheKey = generateCacheKey(prompt, context); int complexityLevel = systemSpecs.getAIComplexity();
        if (currentProvider == null || !currentProvider.isAvailable()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No available AI provider")
            );
        }
        if (contextCache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(contextCache.get(cacheKey));
        }
        context.put("complexity_level", String.valueOf(complexityLevel));
        if (complexityLevel == 0) {
            String response = getDefaultResponse(prompt);
            contextCache.put(cacheKey, response);
            return CompletableFuture.completedFuture(response);
        }
        final String finalPrompt = complexityLevel == 1 ? simplifyPrompt(prompt) : prompt;
        return currentProvider.generateResponse(finalPrompt, context)
            .thenApply(response -> {
                contextCache.put(cacheKey, response);
                return response;
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating response", e);
                return getDefaultResponse(finalPrompt);
            });
    }

    private String simplifyPrompt(String prompt) {
        String simplified = prompt.replaceAll("\\{.*?\\}", "")
                                .replaceAll("\\[.*?\\]", "")
                                .trim();
        return simplified + "\nPlease provide a simple response using basic actions and descriptions.";
    }

    private String getDefaultResponse(String prompt) {
        systemSpecs.updatePerformanceMetrics();
        prompt = prompt.toLowerCase();
        if (prompt.contains("farmer")) {
            return "ACTION: work\nTARGET: nearest_farm\nDURATION: 6000\nDETAIL: tending crops and harvesting";
        }
        if (prompt.contains("fisherman")) {
            return "ACTION: work\nTARGET: nearest_water\nDURATION: 4800\nDETAIL: fishing at the pond";
        }
        if (prompt.contains("trader") || prompt.contains("merchant")) {
            return "ACTION: trade\nTARGET: nearest_player\nDURATION: 3000\nDETAIL: offering special deals";
        }
        if (prompt.contains("night") || prompt.contains("sleeping")) {
            return "ACTION: sleep\nTARGET: nearest_bed\nDURATION: 8000\nDETAIL: resting until morning";
        }
        if (prompt.contains("eating") || prompt.contains("hungry")) {
            return "ACTION: eat\nTARGET: nearest_food\nDURATION: 1200\nDETAIL: having a meal";
        }
        if (prompt.contains("roman")) {
            return "ACTION: gather\nTARGET: forum\nDURATION: 3600\nDETAIL: discussing politics at the forum";
        }
        if (prompt.contains("egyptian")) {
            return "ACTION: pray\nTARGET: temple\nDURATION: 3600\nDETAIL: offering prayers to the gods";
        }
        if (prompt.contains("victorian")) {
            return "ACTION: socialize\nTARGET: tea_room\nDURATION: 3000\nDETAIL: enjoying afternoon tea";
        }
        return "ACTION: walk\nTARGET: village_center\nDURATION: 2400\nDETAIL: exploring the village";
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
