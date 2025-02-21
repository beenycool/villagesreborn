package com.beeny.ai;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final LLMService INSTANCE = new LLMService();
    private static final int MAX_RETRIES = 3;
    private final Map<String, String> contextCache = new HashMap<>();
    
    private LLMService() {}
    
    public static LLMService getInstance() {
        return INSTANCE;
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
        
        // Add any contextual information
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
                // Actual LLM call implementation here
                // This would integrate with your chosen LLM provider
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