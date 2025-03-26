package com.beeny.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AzureOpenAIProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private String apiKey;
    private String endpoint;
    private String modelName;
    private boolean initialized = false;
    private OpenAIClient client;

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.endpoint = config.get("endpoint");
        this.modelName = config.get("modelName");
        
        if (apiKey == null || endpoint == null) {
            LOGGER.error("Azure OpenAI provider initialization failed: missing required configuration");
            return;
        }
        
        try {
            this.client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(apiKey))
                .buildClient();
            
            initialized = true;
            LOGGER.info("Azure OpenAI provider initialized with model: {}", modelName);
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Azure OpenAI client", e);
        }
    }

    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) return CompletableFuture.completedFuture(cache.get(cacheKey));
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callAzureOpenAI(prompt, context);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from Azure OpenAI", e);
                String mockResp = mockResponse(prompt);
                cache.put(cacheKey, mockResp);
                return mockResp;
            }
        }, executor);
    }

    private String callAzureOpenAI(String prompt, Map<String, String> context) {
        try {
            List<ChatMessage> chatMessages = new ArrayList<>();
            if (context != null && !context.isEmpty()) {
                StringBuilder systemContent = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
                context.forEach((key, value) -> {
                    if (value != null && !value.isEmpty()) systemContent.append(" ").append(key).append(": ").append(value).append(".");
                });
                chatMessages.add(new ChatMessage(ChatRole.SYSTEM, systemContent.toString()));
            }
            chatMessages.add(new ChatMessage(ChatRole.USER, prompt));
            ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
            options.setMaxTokens(300);
            options.setTemperature(0.7);
            options.setModel(modelName);
            ChatCompletions completions = client.getChatCompletions(modelName, options);
            if (completions != null && !completions.getChoices().isEmpty()) {
                ChatChoice choice = completions.getChoices().get(0);
                return choice.getMessage().getContent();
            } else {
                LOGGER.warn("Empty response received from Azure OpenAI");
                return mockResponse(prompt);
            }
        } catch (Exception e) {
            LOGGER.error("Exception during Azure OpenAI API call", e);
            throw e;
        }
    }

    private String mockResponse(String prompt) {
        if (prompt.toLowerCase().contains("name")) return "John the Wise Trader";
        else if (prompt.toLowerCase().contains("personality")) return "Friendly, wise, and generous merchant";
        else return "I understand, let me help you with that.";
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
    }

    @Override
    public String getName() {
        return "Azure OpenAI";
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
