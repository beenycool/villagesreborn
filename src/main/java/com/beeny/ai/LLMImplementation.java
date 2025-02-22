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
    private final Cache<String, String> responseCache;

    public LLMImplementation() {
        VillagesConfig.LLMSettings settings = VillagesConfig.getInstance().getLLMSettings();
        
        this.client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(settings.apiKey))
            .endpoint(settings.endpoint)
            .buildClient();

        this.responseCache = new Cache<>(settings.maxCacheSize);
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        String cacheKey = generateCacheKey(prompt, context);
        VillagesConfig.LLMSettings settings = VillagesConfig.getInstance().getLLMSettings();

        if (responseCache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(responseCache.get(cacheKey));
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
                settings.modelType,
                new ChatCompletionsOptions(messages)
                    .setTemperature(settings.temperature)
                    .setMaxTokens(settings.contextLength)
            );

            return completions.getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            if (retryCount < MAX_RETRIES) {
                LOGGER.warn("LLM call failed, retrying ({}/{})", retryCount + 1, MAX_RETRIES);
                Thread.sleep(1000 * (retryCount + 1)); // Exponential backoff
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

    public static class Cache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;
        private final long timestamp;

        public Cache(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
            this.timestamp = System.currentTimeMillis();
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
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
