package com.beeny.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.beeny.ai.LLMErrorHandler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OpenAIProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private String apiKey, modelName;
    private boolean initialized = false;
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();

    public OpenAIProvider() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.modelName = config.getOrDefault("modelName", "gpt-4-turbo-preview");
        
        if (apiKey == null || apiKey.isEmpty()) {
            String errorMsg = "OpenAI provider initialization failed: missing API key";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, 
                "Please provide a valid OpenAI API key in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        
        initialized = true;
        LOGGER.info("OpenAI provider initialized with model: {}", modelName);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "OpenAI provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
        }

        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(cache.get(cacheKey));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callOpenAIApi(prompt, context);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from OpenAI", e);
                
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) {
                    throw new RuntimeException("Your OpenAI API key appears to be invalid. Please check your API key in the mod settings.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) {
                    throw new RuntimeException("Could not connect to OpenAI. Please check your internet connection.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) {
                    throw new RuntimeException("OpenAI API rate limit exceeded. Please try again later or switch to a different provider.", e);
                } else {
                    throw new RuntimeException("Failed to generate response from OpenAI: " + e.getMessage(), e);
                }
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Boolean> validateAccess() {
        if (!initialized || apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", modelName);
                
                JsonArray messagesArray = new JsonArray();
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", "Validate API key (respond with OK)");
                messagesArray.add(userMessage);
                
                requestBody.add("messages", messagesArray);
                requestBody.addProperty("max_tokens", 1);
                
                Request request = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return true;
                    } else {
                        int statusCode = response.code();
                        String body = response.body() != null ? response.body().string() : "";
                        
                        if (statusCode == 401 || statusCode == 403) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, 
                                "OpenAI rejected your API key. Please check it is correct.");
                        } else if (statusCode == 429) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT,
                                "OpenAI rate limit exceeded. Please try again later.");
                        } else {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR,
                                "OpenAI API error: " + statusCode + " - " + body);
                        }
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error validating OpenAI access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR,
                    "Error connecting to OpenAI: " + e.getMessage());
                return false;
            }
        });
    }

    private String callOpenAIApi(String prompt, Map<String, String> context) throws IOException {
        JsonArray messagesArray = new JsonArray();
        if (context != null && !context.isEmpty()) {
            StringBuilder systemContent = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
            context.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) systemContent.append(" ").append(key).append(": ").append(value).append(".");
            });
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemContent.toString());
            messagesArray.add(systemMessage);
        }
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messagesArray.add(userMessage);
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);
        requestBody.add("messages", messagesArray);
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", modelName.contains("gpt-4-turbo") ? 4096 : modelName.contains("gpt-4") ? 8192 : 4096);
        if (modelName.startsWith("gpt-4-turbo") || modelName.startsWith("gpt-4")) {
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "text");
            requestBody.add("response_format", responseFormat);
        }
        Request request = new Request.Builder()
            .url(API_URL)
            .post(RequestBody.create(requestBody.toString(), JSON))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (code == 401 || code == 403) {
                    throw new IOException("Authentication error: Invalid API key or insufficient permissions");
                } else if (code == 429) {
                    throw new IOException("Rate limit exceeded: OpenAI API quota has been reached");
                } else if (code == 500 || code == 502 || code == 503) {
                    throw new IOException("OpenAI service is currently unavailable: " + code);
                } else {
                    throw new IOException("OpenAI API error: " + code + " - " + response.message() + 
                                         (responseBody.isEmpty() ? "" : " - " + responseBody));
                }
            }
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                return jsonResponse.getAsJsonArray("choices").get(0)
                    .getAsJsonObject().getAsJsonObject("message")
                    .get("content").getAsString().trim();
            } else {
                LOGGER.error("Unexpected OpenAI API response structure: {}", responseBody);
                throw new IOException("Unexpected OpenAI API response structure");
            }
        } catch (IOException e) {
            LOGGER.error("OpenAI API communication error", e);
            throw e;
        }
    }

    private String mockResponse(String prompt) {
        if (prompt.toLowerCase().contains("name")) return "Maria the Skilled Artisan";
        else if (prompt.toLowerCase().contains("personality")) return "Creative, dedicated, and meticulous crafter";
        else return "How can I assist you today?";
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
    }

    @Override
    public String getName() {
        return "OpenAI GPT";
    }

    @Override
    public boolean isAvailable() {
        return initialized && apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        cache.clear();
        initialized = false;
    }
}
