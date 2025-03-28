package com.beeny.ai.provider;

import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.ai.LLMErrorHandler;

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
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();

    public CohereProvider() {
        client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("modelName", "command");
        if (apiKey == null || apiKey.isEmpty()) {
            String errorMsg = "Cohere provider initialization failed: missing API key";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, "Please provide a valid Cohere API key in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        initialized = true;
        LOGGER.info("Cohere provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "Cohere provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callCohereApi(prompt, context);
            } catch (Exception e) {
                LOGGER.error("Error generating response from Cohere", e);
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) {
                    throw new RuntimeException("Your Cohere API key appears to be invalid. Please check your API key in the mod settings.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) {
                    throw new RuntimeException("Could not connect to Cohere. Please check your internet connection.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) {
                    throw new RuntimeException("Cohere API rate limit exceeded. Please try again later or switch to a different provider.", e);
                } else {
                    throw new RuntimeException("Failed to generate response from Cohere: " + e.getMessage(), e);
                }
            }
        });
    }

    private String callCohereApi(String prompt, Map<String, String> context) throws IOException {
        JsonObject reqBody = new JsonObject();
        String sysPrompt = context.getOrDefault("system_prompt", "");
        String fullPrompt = sysPrompt.isEmpty() ? prompt : sysPrompt + "\n\n" + prompt;
        reqBody.addProperty("model", model);
        reqBody.addProperty("prompt", fullPrompt);
        reqBody.addProperty("max_tokens", 1024);
        reqBody.addProperty("temperature", 0.7);
        reqBody.addProperty("k", 0);
        reqBody.addProperty("stop_sequences", "");
        reqBody.addProperty("return_likelihoods", "NONE");
        RequestBody body = RequestBody.create(reqBody.toString(), JSON);
        Request request = new Request.Builder().url(API_URL).post(body).addHeader("Authorization", "Bearer " + apiKey).addHeader("Content-Type", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                if (code == 401 || code == 403) {
                    throw new IOException("Authentication error: Invalid Cohere API key or insufficient permissions");
                } else if (code == 429) {
                    throw new IOException("Rate limit exceeded: Cohere API quota has been reached");
                } else if (code >= 500) {
                    throw new IOException("Cohere service is currently unavailable: " + code);
                } else {
                    throw new IOException("Cohere API error: " + code + " - " + response.message() + (responseBody.isEmpty() ? "" : " - " + responseBody));
                }
            }
            String responseBody = response.body().string();
            JsonObject jsonResponse = new com.google.gson.JsonParser().parse(responseBody).getAsJsonObject();
            return jsonResponse.get("generations").getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString().trim();
        }
    }

    @Override
    public CompletableFuture<Boolean> validateAccess() {
        if (!initialized || apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject reqBody = new JsonObject();
                reqBody.addProperty("model", model);
                reqBody.addProperty("prompt", "Validation ping");
                reqBody.addProperty("max_tokens", 1);
                reqBody.addProperty("temperature", 0.0);
                Request request = new Request.Builder().url(API_URL).post(RequestBody.create(reqBody.toString(), JSON)).addHeader("Authorization", "Bearer " + apiKey).addHeader("Content-Type", "application/json").build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return true;
                    } else {
                        int statusCode = response.code();
                        String body = response.body() != null ? response.body().string() : "";
                        if (statusCode == 401 || statusCode == 403) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, "Cohere rejected your API key. Please check it is correct.");
                        } else if (statusCode == 429) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT, "Cohere API rate limit exceeded. Please try again later.");
                        } else {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, "Cohere API error: " + statusCode + " - " + body);
                        }
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error validating Cohere access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR, "Error connecting to Cohere: " + e.getMessage());
                return false;
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
