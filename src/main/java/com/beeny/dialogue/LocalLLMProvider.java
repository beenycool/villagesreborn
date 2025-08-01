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

public class LocalLLMProvider implements LLMDialogueProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagersReborn/LocalLLM");
    private final HttpClient httpClient;
    private final Gson gson;
    
    public LocalLLMProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(VillagersRebornConfig.LLM_REQUEST_TIMEOUT))
            .build();
        this.gson = new Gson();
    }
    
    @Override
    public CompletableFuture<String> generateDialogue(DialogueRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateLocalResponse(request);
            } catch (Exception e) {
                LOGGER.error("Failed to generate dialogue with local LLM", e);
                throw new RuntimeException("Local LLM generation failed", e);
            }
        });
    }
    
    private String generateLocalResponse(DialogueRequest request) throws IOException, InterruptedException {
        String apiUrl = VillagersRebornConfig.LLM_LOCAL_URL + "/completion";
        
        // Build the prompt with context
        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append("You are a villager in Minecraft. ");
        fullPrompt.append("Your profession is ").append(request.context.villager.getVillagerData().profession().toString().replace("minecraft:", "")).append(". ");
        fullPrompt.append("Your personality is ").append(request.context.villagerData.getPersonality()).append(". ");
        fullPrompt.append("The current weather is ").append(request.context.weather).append(". ");
        fullPrompt.append("The time is ").append(request.context.timeOfDay.name().toLowerCase()).append(". ");
        
        if (request.context.playerReputation != 0) {
            fullPrompt.append("Your relationship with the player is ");
            if (request.context.playerReputation > 50) {
                fullPrompt.append("very positive");
            } else if (request.context.playerReputation > 0) {
                fullPrompt.append("positive");
            } else if (request.context.playerReputation < -20) {
                fullPrompt.append("negative");
            } else {
                fullPrompt.append("neutral");
            }
            fullPrompt.append(". ");
        }
        
        if (request.conversationHistory != null && !request.conversationHistory.isEmpty()) {
            fullPrompt.append("Recent conversation: ").append(request.conversationHistory).append(". ");
        }
        
        fullPrompt.append("Respond naturally as a villager would. Keep responses brief (1-2 sentences). ");
        fullPrompt.append("Category: ").append(request.category.name().toLowerCase()).append(". ");
        fullPrompt.append("Prompt: ").append(request.prompt);
        
        // Build the request payload
        JsonObject payload = new JsonObject();
        payload.addProperty("prompt", fullPrompt.toString());
        payload.addProperty("temperature", VillagersRebornConfig.LLM_TEMPERATURE);
        payload.addProperty("max_tokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        payload.addProperty("top_p", 0.9);
        payload.addProperty("top_k", 40);
        payload.addProperty("repeat_penalty", 1.1);
        payload.addProperty("stream", false);
        
        // Add stop sequences to prevent runaway generation
        JsonArray stopSequences = new JsonArray();
        stopSequences.add("\n\n");
        stopSequences.add("Player:");
        stopSequences.add("Villager:");
        payload.add("stop", stopSequences);
        
        String requestBody = gson.toJson(payload);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(VillagersRebornConfig.LLM_REQUEST_TIMEOUT))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            
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