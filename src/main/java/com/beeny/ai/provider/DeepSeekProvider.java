package com.beeny.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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

    public DeepSeekProvider() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void initialize(Map<String, String> config) {
        this.apiKey = config.get("apiKey");
        this.model = config.getOrDefault("model", "deepseek-1.0");
        
        if (apiKey == null) {
            LOGGER.error("DeepSeek provider initialization failed: missing API key");
            return;
        }
        
        initialized = true;
        LOGGER.info("DeepSeek provider initialized with model: {}", model);
    }

    @Override
    public CompletableFuture<String> generateResponse(String prompt, Map<String, String> context) {
        if (!initialized) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("DeepSeek provider not initialized")
            );
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
                // Fall back to mock response
                String mockResp = mockResponse(prompt);
                cache.put(cacheKey, mockResp);
                return mockResp;
            }
        }, executor);
    }

    private String callDeepSeekApi(String prompt, Map<String, String> context) throws IOException {
        // Build system prompt from context
        StringBuilder systemPrompt = new StringBuilder("You are a helpful assistant for a Minecraft villager AI.");
        if (context != null && !context.isEmpty()) {
            context.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    systemPrompt.append(" ").append(key).append(": ").append(value).append(".");
                }
            });
        }

        // Create request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", 300);
        requestBody.addProperty("system", systemPrompt.toString());
        
        // Add messages
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        requestBody.add("messages", gson.toJsonTree(new JsonObject[] { message }));
        
        // Create request
        Request request = new Request.Builder()
            .url(API_URL)
            .post(RequestBody.create(requestBody.toString(), JSON))
            .header("X-API-Key", apiKey)
            .header("Content-Type", "application/json")
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.error("DeepSeek API error: {} - {}", response.code(), response.message());
                throw new IOException("DeepSeek API error: " + response.code() + " - " + response.message());
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            if (jsonResponse.has("content") && jsonResponse.getAsJsonArray("content").size() > 0) {
                return jsonResponse.getAsJsonArray("content").get(0)
                    .getAsJsonObject().get("text").getAsString().trim();
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
        // This is a temporary mock implementation
        if (prompt.toLowerCase().contains("name")) {
            return "Elena the Wise Scholar";
        } else if (prompt.toLowerCase().contains("personality")) {
            return "Intellectual, curious, and knowledgeable librarian";
        } else {
            return "I'd be happy to assist you with that inquiry.";
        }
    }

    private String generateCacheKey(String prompt, Map<String, String> context) {
        StringBuilder key = new StringBuilder(prompt);
        context.forEach((k, v) -> key.append("|").append(k).append("=").append(v));
        return key.toString();
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