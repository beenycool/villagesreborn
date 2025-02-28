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

public class CohereProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String API_URL = "https://api.cohere.ai/v1/generate";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private String apiKey;
    private String model;
    private boolean initialized = false;

    public CohereProvider() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("modelName", "command");
        
        if (apiKey == null) {
            LOGGER.error("Cohere provider initialization failed: missing API key");
            return;
        }
        
        initialized = true;
        LOGGER.info("Cohere provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Cohere provider not initialized"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                
                // Add system prompt if available
                String systemPrompt = context.getOrDefault("system_prompt", "");
                String fullPrompt = systemPrompt.isEmpty() ? prompt : systemPrompt + "\n\n" + prompt;
                
                requestBody.addProperty("model", model);
                requestBody.addProperty("prompt", fullPrompt);
                requestBody.addProperty("max_tokens", 1024);
                requestBody.addProperty("temperature", 0.7);
                requestBody.addProperty("k", 0);
                requestBody.addProperty("stop_sequences", "");
                requestBody.addProperty("return_likelihoods", "NONE");
                
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
                    String generatedText = jsonResponse.get("generations")
                        .getAsJsonArray()
                        .get(0)
                        .getAsJsonObject()
                        .get("text")
                        .getAsString();
                    
                    return generatedText.trim();
                }
            } catch (Exception e) {
                LOGGER.error("Error generating response from Cohere", e);
                throw new RuntimeException("Failed to generate response from Cohere", e);
            }
        });
    }

    @Override
    public String getName() {
        return "cohere";
    }

    @Override
    public boolean isAvailable() {
        return initialized && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public void shutdown() {
        // Nothing to do for OkHttp client
    }
}