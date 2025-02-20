package com.beeny.ai;

import com.beeny.setup.LLMConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 3;
    private static final int THREAD_POOL_SIZE = 4;
    
    private static LLMService instance;
    private final LLMConfig config;
    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService executor;
    private final Map<String, String> responseCache;

    private LLMService(LLMConfig config) {
        this.config = config;
        this.gson = new Gson();
        this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "LLM-Worker");
            t.setDaemon(true);
            return t;
        });
        this.responseCache = new ConcurrentHashMap<>();
        
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    public static LLMService getInstance() {
        if (instance == null) {
            instance = new LLMService(new LLMConfig());
        }
        return instance;
    }

    public static void initialize(LLMConfig config) {
        synchronized (LLMService.class) {
            if (instance != null) {
                instance.shutdown();
            }
            instance = new LLMService(config);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public CompletableFuture<String> generateResponse(String prompt) {
        String cachedResponse = responseCache.get(prompt);
        if (cachedResponse != null) {
            return CompletableFuture.completedFuture(cachedResponse);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = switch (config.getProvider().toLowerCase()) {
                    case "openai" -> retryWithBackoff(() -> callOpenAI(prompt));
                    case "anthropic" -> retryWithBackoff(() -> callAnthropic(prompt));
                    case "local" -> retryWithBackoff(() -> callLocalModel(prompt));
                    default -> throw new IllegalStateException("Unsupported LLM provider: " + config.getProvider());
                };

                if (response != null) {
                    responseCache.put(prompt, response);
                }
                return response;
            } catch (Exception e) {
                LOGGER.error("Error generating response", e);
                return "I apologize, but I'm having trouble processing your request at the moment.";
            }
        }, executor);
    }

    private String retryWithBackoff(Callable<String> operation) throws Exception {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                return operation.call();
            } catch (IOException e) {
                lastException = e;
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    long backoffMs = (long) (Math.pow(2, retryCount) * 1000 + Math.random() * 1000);
                    LOGGER.warn("Request failed, retrying in {} ms (attempt {}/{}): {}", 
                        backoffMs, retryCount, MAX_RETRIES, e.getMessage());
                    Thread.sleep(backoffMs);
                }
            }
        }
        throw new IOException("Failed after " + MAX_RETRIES + " attempts", lastException);
    }

    private String callOpenAI(String prompt) throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ModelType.fromString(config.getModelType()).getId());
        requestBody.put("messages", List.of(message));
        requestBody.put("temperature", config.getTemperature());
        requestBody.put("max_tokens", config.getContextLength());

        String response = makeHttpRequest(config.getEndpoint() + "/chat/completions", 
            gson.toJson(requestBody), config.getApiKey());
        return extractResponseFromOpenAI(response);
    }

    private String callAnthropic(String prompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ModelType.fromString(config.getModelType()).getId());
        requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant:");
        requestBody.put("max_tokens_to_sample", config.getContextLength());
        requestBody.put("temperature", config.getTemperature());

        String response = makeHttpRequest(config.getEndpoint() + "/messages", 
            gson.toJson(requestBody), config.getApiKey());
        return extractResponseFromAnthropic(response);
    }

    private String callLocalModel(String prompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ModelType.fromString(config.getModelType()).getId());
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        String response = makeHttpRequest(config.getEndpoint() + "/api/generate", 
            gson.toJson(requestBody), null);
        return extractResponseFromLocal(response);
    }

    private String makeHttpRequest(String url, String jsonInput, String apiKey) throws IOException {
        RequestBody body = RequestBody.create(jsonInput, JSON);
        Request.Builder requestBuilder = new Request.Builder()
            .url(url)
            .post(body);

        if (apiKey != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null) {
                String errorBody = responseBody != null ? responseBody.string() : "No response body";
                throw new IOException(String.format("Request failed with code %d: %s", 
                    response.code(), errorBody));
            }
            return responseBody.string();
        }
    }

    private String extractResponseFromOpenAI(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            return message.get("content").getAsString();
        } catch (Exception e) {
            LOGGER.error("Error extracting OpenAI response: {}", response, e);
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }

    private String extractResponseFromAnthropic(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray content = jsonResponse.getAsJsonArray("content");
            JsonObject firstContent = content.get(0).getAsJsonObject();
            return firstContent.get("text").getAsString();
        } catch (Exception e) {
            LOGGER.error("Error extracting Anthropic response: {}", response, e);
            throw new RuntimeException("Failed to parse Anthropic response", e);
        }
    }

    private String extractResponseFromLocal(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.get("response").getAsString();
        } catch (Exception e) {
            LOGGER.error("Error extracting local model response: {}", response, e);
            throw new RuntimeException("Failed to parse local model response", e);
        }
    }
}