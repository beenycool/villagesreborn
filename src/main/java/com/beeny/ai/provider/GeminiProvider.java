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

public class GeminiProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private String apiKey;
    private String model;
    private boolean initialized = false;

    public GeminiProvider() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("modelName", "gemini-1.5-pro");
        
        if (apiKey == null) {
            LOGGER.error("Gemini provider initialization failed: missing API key");
            return;
        }
        
        initialized = true;
        LOGGER.info("Gemini provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Gemini provider not initialized"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = API_URL + model + ":generateContent?key=" + apiKey;
                
                JsonObject requestBody = new JsonObject();
                JsonArray contents = new JsonArray();
                
                // Add system prompt if available
                if (context.containsKey("system_prompt")) {
                    JsonObject systemMessage = new JsonObject();
                    JsonObject systemRole = new JsonObject();
                    systemRole.addProperty("role", "system");
                    systemMessage.add("role", systemRole);
                    systemMessage.addProperty("parts", context.get("system_prompt"));
                    contents.add(systemMessage);
                }
                
                // Add user prompt
                JsonObject userMessage = new JsonObject();
                JsonObject parts = new JsonObject();
                parts.addProperty("text", prompt);
                JsonArray partsArray = new JsonArray();
                partsArray.add(parts);
                userMessage.addProperty("role", "user");
                userMessage.add("parts", partsArray);
                contents.add(userMessage);
                
                requestBody.add("contents", contents);
                
                // Add generation config
                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("temperature", 0.7);
                generationConfig.addProperty("maxOutputTokens", 1024);
                requestBody.add("generationConfig", generationConfig);
                
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response);
                    }
                    
                    String responseBody = response.body().string();
                    JsonObject jsonResponse = new com.google.gson.JsonParser().parse(responseBody).getAsJsonObject();
                    String generatedText = jsonResponse.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonArray("content")
                        .get(0).getAsJsonObject()
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
                    
                    return generatedText;
                }
            } catch (Exception e) {
                LOGGER.error("Error generating response from Gemini", e);
                throw new RuntimeException("Failed to generate response from Gemini", e);
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return initialized && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public void shutdown() {
        // Nothing to do for OkHttp client
    }
}