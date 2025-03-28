package com.beeny.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

public class DeepSeekProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private String apiKey;
    private String model;
    private boolean initialized = false;
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private static final String API_URL = "https://api.deepseek.com/v1/messages";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();

    public DeepSeekProvider() {
        client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("model", "deepseek-1.0");
        if (apiKey == null || apiKey.isEmpty()) {
            String errorMsg = "DeepSeek provider initialization failed: missing API key";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, 
                "Please provide a valid DeepSeek API key in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        initialized = true;
        LOGGER.info("DeepSeek provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "DeepSeek provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
        }
        
        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(cache.get(cacheKey));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callDeepSeekApi(prompt, context);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from DeepSeek", e);
                
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) {
                    throw new RuntimeException("Your DeepSeek API key appears to be invalid. Please check your API key in the mod settings.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) {
                    throw new RuntimeException("Could not connect to DeepSeek. Please check your internet connection.", e);
                } else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) {
                    throw new RuntimeException("DeepSeek API rate limit exceeded. Please try again later or switch to a different provider.", e);
                } else {
                    String mockResp = mockResponse(prompt);
                    cache.put(cacheKey, mockResp);
                    return mockResp;
                }
            }
        }, executor);
    }

    private String callDeepSeekApi(String prompt, Map<String, String> context) throws IOException {
        StringBuilder sysPrompt = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
        if (context != null && !context.isEmpty()) {
            context.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) sysPrompt.append(" ").append(key).append(": ").append(value).append(".");
            });
        }
        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("model", model);
        reqBody.addProperty("max_tokens", 300);
        reqBody.addProperty("system", sysPrompt.toString());
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        reqBody.add("messages", gson.toJsonTree(new JsonObject[] { message }));
        Request request = new Request.Builder().url(API_URL).post(RequestBody.create(reqBody.toString(), JSON)).header("X-API-Key", apiKey).header("Content-Type", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (code == 401 || code == 403) {
                    throw new IOException("Authentication error: Invalid DeepSeek API key or insufficient permissions");
                } else if (code == 429) {
                    throw new IOException("Rate limit exceeded: DeepSeek API quota has been reached");
                } else if (code >= 500) {
                    throw new IOException("DeepSeek service is currently unavailable: " + code);
                } else {
                    throw new IOException("DeepSeek API error: " + code + " - " + response.message() + 
                                         (responseBody.isEmpty() ? "" : " - " + responseBody));
                }
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            if (jsonResponse.has("content") && jsonResponse.getAsJsonArray("content").size() > 0) {
                return jsonResponse.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().trim();
            } else {
                LOGGER.error("Unexpected DeepSeek API response structure: {}", responseBody);
                throw new IOException("Unexpected DeepSeek API response structure");
            }
        } catch (IOException e) {
            LOGGER.error("DeepSeek API communication error", e);
            throw e;
        }
    }

    private String mockResponse(String prompt) {
        if (prompt.toLowerCase().contains("name")) return "Elena the Wise Scholar";
        else if (prompt.toLowerCase().contains("personality")) return "Intellectual, curious, and knowledgeable librarian";
        else return "I'd be happy to assist you with that inquiry.";
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
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
                JsonObject message = new JsonObject();
                message.addProperty("role", "user");
                message.addProperty("content", "Validation ping");
                reqBody.add("messages", gson.toJsonTree(new JsonObject[] { message }));
                reqBody.addProperty("max_tokens", 1);

                Request request = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(reqBody.toString(), JSON))
                    .header("X-API-Key", apiKey)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        return true;
                    } else {
                        int statusCode = response.code();
                        String body = response.body() != null ? response.body().string() : "";
                        
                        if (statusCode == 401 || statusCode == 403) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY,
                                "DeepSeek rejected your API key. Please check it is correct.");
                        } else if (statusCode == 429) {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT,
                                "DeepSeek API rate limit exceeded. Please try again later.");
                        } else {
                            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR,
                                "DeepSeek API error: " + statusCode + " - " + body);
                        }
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error validating DeepSeek access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR,
                    "Error connecting to DeepSeek: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public String getName() {
        return "DeepSeek";
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
