package com.beeny.ai;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.ai.openai.models.ChatRole;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.beeny.config.VillagesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;

import java.util.*;
import java.util.concurrent.*;

public class LLMImplementation {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final int MAX_RETRIES = 3;
    private final OpenAIClient client;
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final TTLCache responseCache;

    public LLMImplementation() {
        VillagesConfig.LLMSettings settings = VillagesConfig.getInstance().getLLMSettings();
        
        this.client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(settings.getApiKey()))
            .endpoint(settings.getEndpoint())
            .buildClient();

        this.responseCache = new TTLCache(settings.getMaxCacheSize());
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        String cacheKey = generateCacheKey(prompt, context);
        VillagesConfig.LLMSettings settings = VillagesConfig.getInstance().getLLMSettings();

        String cachedResponse = responseCache.get(cacheKey);
        if (cachedResponse != null) {
            return CompletableFuture.completedFuture(cachedResponse);
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(cacheKey, future);

        executor.submit(() -> {
            try {
                String response = callLLMWithRetry(prompt, context, 0);
                responseCache.put(cacheKey, response);
                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                pendingRequests.remove(cacheKey);
            }
        });

        return future;
    }

    private String callLLMWithRetry(String prompt, Map<String, String> context, int retryCount) throws Exception {
        VillagesConfig.LLMSettings settings = VillagesConfig.getInstance().getLLMSettings();

        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatRole.SYSTEM, getSystemPrompt(context)));
            messages.add(new ChatMessage(ChatRole.USER, prompt));

            ChatCompletions completions = client.getChatCompletions(
                settings.getModelType(),
                new ChatCompletionsOptions(messages)
                    .setTemperature(Double.valueOf(settings.getTemperature()))
                    .setMaxTokens(settings.getContextLength())
            );

            return completions.getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            if (retryCount < MAX_RETRIES) {
                LOGGER.warn("LLM call failed, retrying ({}/{})", retryCount + 1, MAX_RETRIES);
                Thread.sleep(1000 * (retryCount + 1));
                return callLLMWithRetry(prompt, context, retryCount + 1);
            }
            throw e;
        }
    }

    private String getSystemPrompt(Map<String, String> context) {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an AI assistant specialized in generating content for a Minecraft village mod.\n");
        systemPrompt.append("Generate specific, practical responses that can be implemented in Minecraft.\n");
        systemPrompt.append("Consider available blocks, game mechanics, and maintain cultural authenticity.\n");
        systemPrompt.append("Keep responses concise and focused on the requested format.\n\n");

        if (!context.isEmpty()) {
            systemPrompt.append("Current context:\n");
            context.forEach((key, value) -> 
                systemPrompt.append("- ").append(key).append(": ").append(value).append("\n")
            );
        }

        return systemPrompt.toString();
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
    }

    private static class CacheEntry {
        private final String value;
        private final long timestamp;

        public CacheEntry(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }

        public String getValue() {
            return value;
        }
    }

    private class TTLCache {
        private final Map<String, CacheEntry> cache = new LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                VillagesConfig.LLMSettings settings = VillagesConfig.getInstance().getLLMSettings();
                return size() > maxSize || eldest.getValue().isExpired(settings.getCacheTTLSeconds() * 1000);
            }
        };
        
        private final int maxSize;

        public TTLCache(int maxSize) {
            this.maxSize = maxSize;
        }

        public synchronized String get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry != null) {
                VillagesConfig.LLMSettings settings = VillagesConfig.getInstance().getLLMSettings();
                if (!entry.isExpired(settings.getCacheTTLSeconds() * 1000)) {
                    return entry.getValue();
                } else {
                    cache.remove(key);
                }
            }
            return null;
        }

        public synchronized void put(String key, String value) {
            cache.put(key, new CacheEntry(value));
        }

        public synchronized int size() {
            return cache.size();
        }

        public synchronized void clear() {
            cache.clear();
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public Map<String, CompletableFuture<String>> getPendingRequests() {
        return new HashMap<>(pendingRequests);
    }

    public int getCacheSize() {
        return responseCache.size();
    }

    public void clearCache() {
        responseCache.clear();
    }
}
