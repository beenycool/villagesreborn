package com.beeny.ai.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.ai.LLMErrorHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OpenRouterProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private OkHttpClient client;
    private String apiKey;
    private String model;
    private boolean initialized = false;
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();

    public OpenRouterProvider() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("modelName", "openrouter/command-r");
        
        if (apiKey == null || apiKey.isEmpty()) {
            String errorMsg = "OpenRouter provider initialization failed: missing API key";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, 
                "Please provide a valid OpenRouter API key in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        
        initialized = true;
        LOGGER.info("OpenRouter provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "OpenRouter provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
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
                requestBody.addProperty("max_tokens", getMaxTokensForModel(model));
                
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("HTTP-Referer", "https://github.com/beeny/villagesreborn")
                    .addHeader("X-Title", "Villagesreborn Minecraft Mod")
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        int code = response.code();
                        String responseBody = response.body() != null ? response.body().string() : "";
                        
                        // Generate more helpful error messages based on status code
                        if (code == 401 || code == 403) {
                            throw new IOException("Authentication error: Invalid OpenRouter API key or insufficient permissions");
                        } else if (code == 429) {
                            throw new IOException("Rate limit exceeded: OpenRouter API quota has been reached");
                        } else if (code >= 500) {
                            throw new IOException("OpenRouter service is currently unavailable: " + code);
                        } else {
                            throw new IOException("OpenRouter API error: " + code + " - " + response.message() + 
                                                (responseBody.isEmpty() ? "" : " - " + responseBody));
                        }
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
                LOGGER.error("Error generating response from OpenRouter", e);
                
                // Determine error type and report to user
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                
                // For some error types, we can try to provide helpful guidance
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) {
                    throw new RuntimeException("Your OpenRouter API key appears to be invalid. Please check your API key in the mod settings.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) {
                    throw new RuntimeException("Could not connect to OpenRouter. Please check your internet connection.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) {
                    throw new RuntimeException("OpenRouter API rate limit exceeded. Please try again later or switch to a different provider.", e);
                } else {
                    throw new RuntimeException("Failed to generate response from OpenRouter: " + e.getMessage(), e);
                }
            }
        });
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
                    .addHeader("Authorization", "Bearer " + apiKey)
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
                                "OpenRouter rejected your API key. Please check it is correct.");
                        } else if (statusCode == 429) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT,
                                "OpenRouter API rate limit exceeded. Please try again later.");
                        } else {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR,
                                "OpenRouter API error: " + statusCode + " - " + body);
                        }
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error validating OpenRouter access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR,
                    "Error connecting to OpenRouter: " + e.getMessage());
                return false;
            }
        });
    }

    private int getMaxTokensForModel(String model) {
        return switch (model) {
            case "openrouter/command-r" -> 4096;
            case "openrouter/solar" -> 8192;
            case "openrouter/neural-chat" -> 4096;
            default -> 4096;
        };
    }

    @Override
    public boolean isAvailable() {
        return initialized && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getName() {
        return "openrouter";
    }

    @Override
    public void shutdown() {
        // Nothing to do for OkHttp client
    }
}
