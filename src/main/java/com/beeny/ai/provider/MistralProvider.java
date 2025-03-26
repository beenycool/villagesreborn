package com.beeny.ai.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MistralProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private String apiKey;
    private String model;
    private boolean initialized = false;

    public MistralProvider() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("modelName", "mistral-large");
        
        if (apiKey == null) {
            LOGGER.error("Mistral provider initialization failed: missing API key");
            return;
        }
        
        initialized = true;
        LOGGER.info("Mistral provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Mistral provider not initialized"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                
                JsonArray messages = new JsonArray();
                
                if (context.containsKey("system_prompt")) {
                    JsonObject systemMessage = new JsonObject();
                    systemMessage.addProperty("role", "system");
                    systemMessage.addProperty("content", context.get("system_prompt"));
                    messages.add(systemMessage);
                }
                
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", prompt);
                messages.add(userMessage);
                
                requestBody.add("messages", messages);
                requestBody.addProperty("temperature", 0.7);
                requestBody.addProperty("max_tokens", model.contains("large") ? 4096 : 2048);
                requestBody.addProperty("top_p", 0.9);
                requestBody.addProperty("response_format", "text");
                
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response);
                    }
                    
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = new com.google.gson.JsonParser().parse(responseBody).getAsJsonObject();
                    String generatedText = jsonResponse.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                    
                    return generatedText;
                }
            } catch (Exception e) {
                LOGGER.error("Error generating response from Mistral", e);
                throw new RuntimeException("Failed to generate response from Mistral", e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return initialized && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getName() {
        return "mistral";
    }

    @Override
    public void shutdown() {
        // Nothing to do for OkHttp client
    }
}
