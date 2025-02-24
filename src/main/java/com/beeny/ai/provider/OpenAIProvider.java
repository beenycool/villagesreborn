package com.beeny.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAIProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private String apiKey;
    private String modelName;
    private boolean initialized = false;

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.modelName = config.get("modelName");
        
        if (apiKey == null) {
            LOGGER.error("OpenAI provider initialization failed: missing API key");
            return;
        }
        
        initialized = true;
        LOGGER.info("OpenAI provider initialized with model: {}", modelName);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("OpenAI provider not initialized")
            );
        }

        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(cache.get(cacheKey));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Implement actual OpenAI API call
                // For now, return a mock response
                String response = mockResponse(prompt);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from OpenAI", e);
                throw new RuntimeException("Failed to generate response", e);
            }
        }, executor);
    }

    private String mockResponse(String prompt) {
        // This is a temporary mock implementation
        if (prompt.toLowerCase().contains("name")) {
            return "Maria the Skilled Artisan";
        } else if (prompt.toLowerCase().contains("personality")) {
            return "Creative, dedicated, and meticulous crafter";
        } else {
            return "How can I assist you today?";
        }
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
    }

    @Override
    public String getName() {
        return "OpenAI GPT";
    }

    @Override
    public boolean isAvailable() {
        return initialized;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        cache.clear();
        initialized = false;
    }
}