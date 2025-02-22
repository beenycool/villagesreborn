package com.beeny.ai;

import com.beeny.setup.LLMConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final LLMService INSTANCE = new LLMService();
    private static final int MAX_RETRIES = 3;
private LLMImplementation llmImplementation;
private final Map<String, String> behaviorCache = new HashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> contextCache = new HashMap<>();
    private LLMConfig config;

    private LLMService() {
        this.llmImplementation = null;
    }

    public static LLMService getInstance() {
        return INSTANCE;
    }

    public void initialize(LLMConfig config) {
        this.config = config;
        this.llmImplementation = new LLMImplementation();
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

    public CompletableFuture<String> generateBehavior(String profession, String situation, Map<String, Object> context) {
        String cacheKey = String.format("%s_%s", profession, situation);
        if (behaviorCache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(behaviorCache.get(cacheKey));
        }

        String prompt = String.format(
            "You are a %s villager in Minecraft. Based on this situation:\n%s\n" +
            "What would be your next action? Consider:\n" +
            "1. Your profession's duties\n" +
            "2. Time of day\n" +
            "3. Current location\n" +
            "4. Other villagers nearby\n\n" +
            "Respond with a single action in the format:\n" +
            "ACTION: (walk/work/trade/rest/socialize)\n" +
            "TARGET: (specific location or villager)\n" +
            "DURATION: (time in ticks)\n" +
            "DETAIL: (specific activity description)",
            profession, situation
        );

        return callLLMWithRetry(prompt, context)
            .thenApply(response -> {
                behaviorCache.put(cacheKey, response);
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

    private CompletableFuture<String> callLLMWithRetry(String prompt, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
return llmImplementation.generateResponse(prompt, context.entrySet().stream()
    .collect(java.util.stream.Collectors.toMap(
        Map.Entry::getKey,
        e -> String.valueOf(e.getValue())
    ))).get();
                } catch (Exception e) {
                    if (attempt == 2) {
                        LOGGER.error("Failed to generate LLM response after 3 attempts", e);
return (String) getDefaultResponse(prompt);
                    }
                    try {
                        Thread.sleep(1000 * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
return (String) getDefaultResponse(prompt);
                    }
                }
            }
return (String) getDefaultResponse(prompt);
        }, executor);
    }

    private String getDefaultResponse(String prompt) {
        // Provide basic fallback behaviors
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
        llmImplementation.clearCache();
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
