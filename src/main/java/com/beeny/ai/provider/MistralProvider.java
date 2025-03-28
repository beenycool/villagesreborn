package com.beeny.ai.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.ai.LLMErrorHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MistralProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private String apiKey;
    private String model;
    private boolean initialized = false;
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

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
        
        if (apiKey == null || apiKey.isEmpty()) {
            String errorMsg = "Mistral provider initialization failed: missing API key";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, 
                "Please provide a valid Mistral API key in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        
        initialized = true;
        LOGGER.info("Mistral provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "Mistral provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
        }

        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(cache.get(cacheKey));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callMistralApi(prompt, context);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from Mistral", e);
                
                // Determine error type and report to user
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                
                // Provide helpful guidance based on error type
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) {
                    throw new RuntimeException("Your Mistral API key appears to be invalid. Please check your API key in the mod settings.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) {
                    throw new RuntimeException("Could not connect to Mistral. Please check your internet connection.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) {
                    throw new RuntimeException("Mistral API rate limit exceeded. Please try again later or switch to a different provider.", e);
                } else {
                    throw new RuntimeException("Failed to generate response from Mistral: " + e.getMessage(), e);
                }
            }
        });
    }

    private String callMistralApi(String prompt, Map<String, String> context) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        
        JsonArray messages = new JsonArray();
        
        if (context != null && !context.isEmpty()) {
            // Use system prompt if available, otherwise build from context
            if (context.containsKey("system_prompt")) {
                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", context.get("system_prompt"));
                messages.add(systemMessage);
            } else {
                // Build system message from context
                StringBuilder systemContent = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
                context.forEach((key, value) -> {
                    if (value != null && !value.isEmpty()) 
                        systemContent.append(" ").append(key).append(": ").append(value).append(".");
                });
                
                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", systemContent.toString());
                messages.add(systemMessage);
            }
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
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                
                // Generate more helpful error messages based on status code
                if (code == 401 || code == 403) {
                    throw new IOException("Authentication error: Invalid Mistral API key or insufficient permissions");
                } else if (code == 429) {
                    throw new IOException("Rate limit exceeded: Mistral API quota has been reached");
                } else if (code >= 500) {
                    throw new IOException("Mistral service is currently unavailable: " + code);
                } else {
                    throw new IOException("Mistral API error: " + code + " - " + response.message() + 
                                         (responseBody.isEmpty() ? "" : " - " + responseBody));
                }
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            String generatedText = jsonResponse.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
            
            return generatedText;
        } 
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        if (context != null) {
            context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        }
        return key.toString();
    }

    private String mockResponse(String prompt) {
        if (prompt.toLowerCase().contains("name")) return "Thomas the Diplomatic Scholar";
        else if (prompt.toLowerCase().contains("personality")) return "Thoughtful, balanced, and insightful advisor";
        else return "Let me assist you with your question.";
    }

    @Override
    public boolean isAvailable() {
        return initialized && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public CompletableFuture<Boolean> validateAccess() {
        if (!initialized || apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Minimal validation request
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                JsonArray messages = new JsonArray();
                JsonObject message = new JsonObject();
                message.addProperty("role", "user");
                message.addProperty("content", "Validation ping");
                messages.add(message);
                requestBody.add("messages", messages);
                requestBody.addProperty("max_tokens", 1);

                Request request = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .header("Authorization", "Bearer " + apiKey)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return true;
                    } else {
                        int statusCode = response.code();
                        String body = response.body() != null ? response.body().string() : "";
                        
                        // Handle specific error codes
                        if (statusCode == 401 || statusCode == 403) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY,
                                "Mistral rejected your API key. Please check it is correct.");
                        } else if (statusCode == 429) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT,
                                "Mistral API rate limit exceeded. Please try again later.");
                        } else {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR,
                                "Mistral API error: " + statusCode + " - " + body);
                        }
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error validating Mistral access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR,
                    "Error connecting to Mistral: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public String getName() {
        return "mistral";
    }

    @Override
    public void shutdown() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        cache.clear();
    }
}
