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
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeminiProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private OkHttpClient client;
    private String apiKey, model;
    private boolean initialized = false;
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();
    private final Gson gson = new Gson();

    public GeminiProvider() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        if (context != null) {
            context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        }
        return key.toString();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("modelName", "gemini-2.0-flash-lite");
        
        if (apiKey == null || apiKey.isEmpty()) {
            String errorMsg = "Gemini provider initialization failed: missing API key";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, 
                "Please provide a valid Gemini API key in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        
        initialized = true;
        LOGGER.info("Gemini provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "Gemini provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
        }

        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(cache.get(cacheKey));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callGeminiApi(prompt, context);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from Gemini", e);
                
                // Determine error type and report to user
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                
                // For some error types, we can try to provide helpful guidance
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) {
                    throw new RuntimeException("Your Gemini API key appears to be invalid. Please check your API key in the mod settings.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) {
                    throw new RuntimeException("Could not connect to Gemini. Please check your internet connection.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) {
                    throw new RuntimeException("Gemini API rate limit exceeded. Please try again later or switch to a different provider.", e);
                } else {
                    throw new RuntimeException("Failed to generate response from Gemini: " + e.getMessage(), e);
                }
            }
        }, executor);
    }
    
    private String callGeminiApi(String prompt, Map<String, String> context) throws IOException {
        String url = API_URL + model + ":generateContent?key=" + apiKey;
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        
        // Add context as system message if available
        if (context != null && !context.isEmpty()) {
            StringBuilder systemContent = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
            context.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    systemContent.append(" ").append(key).append(": ").append(value).append(".");
                }
            });
            
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "user");
            JsonObject systemParts = new JsonObject();
            systemParts.addProperty("text", systemContent.toString());
            JsonArray systemPartsArray = new JsonArray();
            systemPartsArray.add(systemParts);
            systemMessage.add("parts", systemPartsArray);
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
        
        // Configure generation parameters
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        
        int maxTokens = switch (model) {
            case "gemini-2.0-pro", "gemini-2.0-flash" -> 128000;
            case "gemini-2.0-flash-lite" -> 32000;
            default -> 32000;
        };
        
        generationConfig.addProperty("maxOutputTokens", maxTokens);
        generationConfig.addProperty("candidateCount", 1);
        generationConfig.add("stopSequences", new JsonArray());
        requestBody.add("generationConfig", generationConfig);
        
        // Make API request
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (code == 401 || code == 403) {
                    throw new IOException("Authentication error: Invalid API key or insufficient permissions");
                } else if (code == 429) {
                    throw new IOException("Rate limit exceeded: Gemini API quota has been reached");
                } else if (code >= 500) {
                    throw new IOException("Gemini service is currently unavailable: " + code);
                } else {
                    throw new IOException("Gemini API error: " + code + " - " + response.message());
                }
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
                return jsonResponse.getAsJsonArray("candidates").get(0)
                    .getAsJsonObject().getAsJsonArray("content").get(0)
                    .getAsJsonObject().getAsJsonArray("parts").get(0)
                    .getAsJsonObject().get("text").getAsString().trim();
            } else {
                LOGGER.error("Unexpected Gemini API response structure: {}", responseBody);
                throw new IOException("Unexpected Gemini API response structure");
            }
        } catch (IOException e) {
            LOGGER.error("Gemini API communication error", e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<Boolean> validateAccess() {
        if (!initialized || apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Minimal validation request
                String url = API_URL + model + ":generateContent?key=" + apiKey;
                JsonObject requestBody = new JsonObject();
                JsonArray contents = new JsonArray();
                
                JsonObject userMessage = new JsonObject();
                JsonObject parts = new JsonObject();
                parts.addProperty("text", "Validation ping");
                JsonArray partsArray = new JsonArray();
                partsArray.add(parts);
                userMessage.addProperty("role", "user");
                userMessage.add("parts", partsArray);
                contents.add(userMessage);
                
                requestBody.add("contents", contents);
                
                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("maxOutputTokens", 1);
                requestBody.add("generationConfig", generationConfig);
                
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return true;
                    } else {
                        int statusCode = response.code();
                        String responseBody = response.body() != null ? response.body().string() : "";
                        
                        // Handle specific error codes
                        if (statusCode == 401 || statusCode == 403) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY,
                                "Gemini rejected your API key. Please check it is correct.");
                        } else if (statusCode == 429) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT,
                                "Gemini API rate limit exceeded. Please try again later.");
                        } else {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR,
                                "Gemini API error: " + statusCode + " - " + responseBody);
                        }
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error validating Gemini access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR,
                    "Error connecting to Gemini: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public boolean isAvailable() {
        return initialized && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getName() {
        return "Gemini";
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        cache.clear();
        initialized = false;
    }
}
