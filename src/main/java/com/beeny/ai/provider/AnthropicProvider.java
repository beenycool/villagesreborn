package com.beeny.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.OkHttpClient;
import com.beeny.ai.LLMErrorHandler;
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

public class AnthropicProvider implements AIProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private String apiKey;
    private String model;
    private boolean initialized = false;
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private final LLMErrorHandler errorHandler = LLMErrorHandler.getInstance();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ANTHROPIC_API_VERSION = "2024-02-01";

    public AnthropicProvider() {
        client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("model", "claude-3-sonnet");
        if (apiKey == null || apiKey.isEmpty()) {
            String errorMsg = "Anthropic provider initialization failed: missing API key";
            LOGGER.error(errorMsg);
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, "Please provide a valid Anthropic API key in the configuration.");
            throw new IllegalArgumentException(errorMsg);
        }
        initialized = true;
        LOGGER.info("Anthropic provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<Boolean> validateAccess() {
        if (!initialized || apiKey == null || apiKey.isEmpty()) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject reqBody = new JsonObject();
                reqBody.addProperty("model", model);
                JsonArray messages = new JsonArray();
                JsonObject message = new JsonObject();
                message.addProperty("role", "user");
                message.addProperty("content", "Validation ping");
                messages.add(message);
                reqBody.add("messages", messages);
                reqBody.addProperty("max_tokens", 1);
                Request request = new Request.Builder().url(API_URL).post(RequestBody.create(reqBody.toString(), JSON)).header("Authorization", "Bearer " + apiKey).header("x-api-key", apiKey).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) return true;
                    int statusCode = response.code();
                    String body = response.body() != null ? response.body().string() : "";
                    if (statusCode == 401 || statusCode == 403) errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.INVALID_API_KEY, "Anthropic rejected your API key. Please check it is correct.");
                    else if (statusCode == 429) errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.API_RATE_LIMIT, "Anthropic API rate limit exceeded. Please try again later.");
                    else errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, "Anthropic API error: " + statusCode + " - " + body);
                    return false;
                }
            } catch (IOException e) {
                LOGGER.error("Error validating Anthropic access", e);
                errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.CONNECTION_ERROR, "Error connecting to Anthropic: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            String error = "Anthropic provider not initialized";
            errorHandler.reportErrorToClient(LLMErrorHandler.ErrorType.PROVIDER_ERROR, error);
            return CompletableFuture.failedFuture(new IllegalStateException(error));
        }
        String cacheKey = generateCacheKey(prompt, context);
        if (cache.containsKey(cacheKey)) return CompletableFuture.completedFuture(cache.get(cacheKey));
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = callAnthropicApi(prompt, context);
                cache.put(cacheKey, response);
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response from Anthropic", e);
                LLMErrorHandler.ErrorType errorType = errorHandler.determineErrorType(e);
                errorHandler.reportErrorToClient(errorType, e.getMessage());
                if (errorType == LLMErrorHandler.ErrorType.INVALID_API_KEY) throw new RuntimeException("Your Anthropic API key appears to be invalid. Please check your API key in the mod settings.", e);
                else if (errorType == LLMErrorHandler.ErrorType.CONNECTION_ERROR) throw new RuntimeException("Could not connect to Anthropic. Please check your internet connection.", e);
                else if (errorType == LLMErrorHandler.ErrorType.API_RATE_LIMIT) throw new RuntimeException("Anthropic API rate limit exceeded. Please try again later or switch to a different provider.", e);
                String mockResp = mockResponse(prompt);
                cache.put(cacheKey, mockResp);
                return mockResp;
            }
        }, executor);
    }

    private String callAnthropicApi(String prompt, Map<String, String> context) throws IOException {
        StringBuilder sysPrompt = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
        if (context != null && !context.isEmpty()) context.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) sysPrompt.append(" ").append(key).append(": ").append(value).append(".");
        });
        JsonObject reqBody = new JsonObject();
        reqBody.addProperty("model", model);
        int maxTokens = 1024;
        if (model.startsWith("claude-3-opus")) maxTokens = 4096;
        else if (model.startsWith("claude-sonnet-3.7") || model.startsWith("claude-sonnet-3.5")) maxTokens = 4096;
        else if (model.startsWith("claude-3")) maxTokens = 2048;
        reqBody.addProperty("max_tokens", maxTokens);
        reqBody.addProperty("system", sysPrompt.toString());
        reqBody.addProperty("temperature", 0.7);
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        reqBody.add("messages", gson.toJsonTree(new JsonObject[] { message }));
        if (model.startsWith("claude-3")) reqBody.addProperty("response_format", "text");
        Request request = new Request.Builder().url(API_URL).post(RequestBody.create(reqBody.toString(), JSON)).header("anthropic-api-key", apiKey).header("anthropic-version", ANTHROPIC_API_VERSION).header("Content-Type", "application/json").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                if (code == 401 || code == 403) throw new IOException("Authentication error: Invalid Anthropic API key or insufficient permissions");
                else if (code == 429) throw new IOException("Rate limit exceeded: Anthropic API quota has been reached");
                else if (code >= 500) throw new IOException("Anthropic service is currently unavailable: " + code);
                else throw new IOException("Anthropic API error: " + code + " - " + response.message() + (responseBody.isEmpty() ? "" : " - " + responseBody));
            }
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            if (jsonResponse.has("content") && jsonResponse.getAsJsonArray("content").size() > 0) return jsonResponse.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString().trim();
            else {
                LOGGER.error("Unexpected Anthropic API response structure: {}", responseBody);
                throw new IOException("Unexpected Anthropic API response structure");
            }
        } catch (IOException e) {
            LOGGER.error("Anthropic API communication error", e);
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
    public String getName() {
        return "Anthropic Claude";
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
