package com.beeny.ai;

import com.beeny.setup.LLMConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static LLMService instance;
    private static final int MAX_RETRIES = 3;
    private final Map<String, String> contextCache = new HashMap<>();
    private LLMConfig config;

    private LLMService() {}

    public static LLMService getInstance() {
        if (instance == null) {
            instance = new LLMService();
        }
        return instance;
    }

    public void initialize(LLMConfig config) {
        this.config = config;
        Path modelPath = FabricLoader.getInstance().getConfigDir()
                .resolve("villagesreborn/models/" + config.getModelType());
        LOGGER.info("Loading AI model from: {}", modelPath.toAbsolutePath());
        // Add logic to load the model file based on the model path
        // e.g., initialize model parameters, allocate resources etc.
        LOGGER.info("LLMService initialized with config: modelType={}", config.getModelType());
    }

    public CompletableFuture<String> generateResponse(String prompt) {
        return generateResponse(prompt, new HashMap<>());
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        String cacheKey = generateCacheKey(prompt, context);
        if (contextCache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(contextCache.get(cacheKey));
        }

        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append("You are a Minecraft village generation AI assistant.\n");
        enhancedPrompt.append("Provide specific, Minecraft-appropriate responses.\n");
        enhancedPrompt.append("Consider available blocks, game mechanics, and cultural authenticity.\n\n");
        
        context.forEach((key, value) -> 
            enhancedPrompt.append(key).append(": ").append(value).append("\n")
        );
        
        enhancedPrompt.append("\nPrompt: ").append(prompt);

        return callLLMWithRetry(enhancedPrompt.toString(), 0)
            .thenApply(response -> {
                contextCache.put(cacheKey, response);
                return response;
            });
    }

    private CompletableFuture<String> callLLMWithRetry(String prompt, int retryCount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (config != null) {
                    LOGGER.debug("Using model: {}", config.getModelType());
                }
                return "Generated response"; // Placeholder
            } catch (Exception e) {
                if (retryCount < MAX_RETRIES) {
                    LOGGER.warn("LLM call failed, retrying ({}/{})", retryCount + 1, MAX_RETRIES);
                    return callLLMWithRetry(prompt, retryCount + 1).join();
                }
                LOGGER.error("LLM call failed after {} retries", MAX_RETRIES);
                throw new RuntimeException("Failed to generate response", e);
            }
        });
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
    }

    public void shutdown() {
        LOGGER.info("Shutting down LLMService");
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
}