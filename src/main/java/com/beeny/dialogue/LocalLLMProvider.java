package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.system.VillagerDialogueSystem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class LocalLLMProvider extends BaseLLMProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagersReborn/LocalLLM");
    private final HttpClient httpClient;

    public LocalLLMProvider() {
        super(
            "", // No API key for local
            VillagersRebornConfig.LLM_LOCAL_URL,
            VillagersRebornConfig.LLM_MODEL
        );
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(VillagersRebornConfig.LLM_REQUEST_TIMEOUT))
            .build();
    }

    @Override
    public CompletableFuture<String> generateDialogue(DialogueRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use DialoguePromptBuilder for contextual prompt
                String prompt = DialoguePromptBuilder.buildContextualPrompt(
                    request.context,
                    request.category,
                    request.conversationHistory
                );
                return generateLocalResponse(prompt);
            } catch (Exception e) {
                LOGGER.error("Failed to generate dialogue with local LLM", e);
                throw new RuntimeException("Local LLM generation failed", e);
            }
        });
    }

    // Required by BaseLLMProvider, but not used for local provider
    @Override
    public okhttp3.Request buildRequest(LLMDialogueProvider.DialogueRequest request) {
        throw new UnsupportedOperationException("LocalLLMProvider does not use OkHttp requests.");
    }

    @Override
    public String parseResponse(String responseBody) {
        throw new UnsupportedOperationException("LocalLLMProvider does not use OkHttp responses.");
    }
    
    // Overloaded to accept prompt string directly
    private String generateLocalResponse(String prompt) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        payload.addProperty("prompt", prompt);
        payload.addProperty("temperature", VillagersRebornConfig.LLM_TEMPERATURE);
        payload.addProperty("max_tokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        payload.addProperty("repeat_penalty", 1.1);
        payload.addProperty("stream", false);

        // Add stop sequences to prevent runaway generation
        JsonArray stopSequences = new JsonArray();
        stopSequences.add("\n\n");
        stopSequences.add("Player:");
        stopSequences.add("Villager:");
        payload.add("stop", stopSequences);

        String body = new Gson().toJson(payload);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(VillagersRebornConfig.LLM_REQUEST_TIMEOUT))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonObject responseJson = new Gson().fromJson(response.body(), JsonObject.class);

            // Handle different response formats
            if (responseJson.has("choices") && responseJson.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (choice.has("text")) {
                    return cleanResponse(choice.get("text").getAsString());
                }
            } else if (responseJson.has("content")) {
                return cleanResponse(responseJson.get("content").getAsString());
            } else if (responseJson.has("data") && responseJson.getAsJsonArray("data").size() > 0) {
                JsonObject data = responseJson.getAsJsonArray("data").get(0).getAsJsonObject();
                if (data.has("text")) {
                    return cleanResponse(data.get("text").getAsString());
                }
            }

            throw new RuntimeException("Unexpected response format from llama.cpp");
        } else {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }
    
    private String cleanResponse(String response) {
        if (response == null) return "";
        
        // Remove leading/trailing whitespace and quotes
        response = response.trim();
        if (response.startsWith("\"") && response.endsWith("\"")) {
            response = response.substring(1, response.length() - 1);
        }
        
        // Remove common prefixes
        response = response.replaceFirst("(?i)^(villager says:|villager:|response:|answer:)", "").trim();
        
        // Ensure the response ends with proper punctuation
        if (!response.isEmpty() && !response.matches(".*[.!?]$")) {
            response += ".";
        }
        
        return response;
    }
    
    @Override
    public boolean isConfigured() {
        return "local".equals(VillagersRebornConfig.LLM_PROVIDER) &&
               VillagersRebornConfig.LLM_LOCAL_URL != null &&
               !VillagersRebornConfig.LLM_LOCAL_URL.isEmpty();
    }
    
    @Override
    public String getProviderName() {
        return "local";
    }
    
    @Override
    public void shutdown() {
        // No special shutdown needed for HTTP client
    }
}