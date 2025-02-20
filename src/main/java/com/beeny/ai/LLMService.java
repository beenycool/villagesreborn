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
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LLMService {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();
    
    private static LLMService instance;
    private final LLMConfig config;

    private LLMService(LLMConfig config) {
        this.config = config;
    }

    public static LLMService getInstance() {
        if (instance == null) {
            instance = new LLMService(new LLMConfig());
        }
        return instance;
    }

    public static void initialize(LLMConfig config) {
        instance = new LLMService(config);
    }

    public CompletableFuture<String> generateResponse(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                switch (config.getProvider().toLowerCase()) {
                    case "openai" -> {
                        String response = callOpenAI(prompt);
                        return extractResponseFromOpenAI(response);
                    }
                    case "anthropic" -> {
                        String response = callAnthropic(prompt);
                        return extractResponseFromAnthropic(response);
                    }
                    case "local" -> {
                        String response = callLocalModel(prompt);
                        return extractResponseFromLocal(response);
                    }
                    default -> throw new IllegalStateException("Unsupported LLM provider: " + config.getProvider());
                }
            } catch (Exception e) {
                LOGGER.error("Error generating response", e);
                return "I apologize, but I'm having trouble processing your request at the moment.";
            }
        });
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

        return makeHttpRequest(config.getEndpoint() + "/chat/completions", gson.toJson(requestBody), config.getApiKey());
    }

    private String callAnthropic(String prompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ModelType.fromString(config.getModelType()).getId());
        requestBody.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant:");
        requestBody.put("max_tokens_to_sample", config.getContextLength());
        requestBody.put("temperature", config.getTemperature());

        return makeHttpRequest(config.getEndpoint() + "/messages", gson.toJson(requestBody), config.getApiKey());
    }

    private String callLocalModel(String prompt) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ModelType.fromString(config.getModelType()).getId());
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        return makeHttpRequest(config.getEndpoint() + "/api/generate", gson.toJson(requestBody), null);
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
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code());
            }
            ResponseBody responseBody = response.body();
            return responseBody != null ? responseBody.string() : "";
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
            LOGGER.error("Error extracting OpenAI response", e);
            return "Error processing response";
        }
    }

    private String extractResponseFromAnthropic(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray content = jsonResponse.getAsJsonArray("content");
            JsonObject firstContent = content.get(0).getAsJsonObject();
            return firstContent.get("text").getAsString();
        } catch (Exception e) {
            LOGGER.error("Error extracting Anthropic response", e);
            return "Error processing response";
        }
    }

    private String extractResponseFromLocal(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.get("response").getAsString();
        } catch (Exception e) {
            LOGGER.error("Error extracting local model response", e);
            return "Error processing response";
        }
    }
}