package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OpenRouterDialogueProvider extends BaseLLMProvider {
    private static final String DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    public OpenRouterDialogueProvider() {
        super(
            System.getenv("VILLAGERS_REBORN_API_KEY"),
            VillagersRebornConfig.LLM_API_ENDPOINT.isEmpty() ? DEFAULT_ENDPOINT : VillagersRebornConfig.LLM_API_ENDPOINT,
            VillagersRebornConfig.LLM_MODEL.isEmpty() ? "openai/gpt-3.5-turbo" : VillagersRebornConfig.LLM_MODEL
        );
    }

    public OpenRouterDialogueProvider(String apiKey, String endpoint, String model) {
        super(
            apiKey != null ? apiKey : System.getenv("VILLAGERS_REBORN_API_KEY"),
            (endpoint == null || endpoint.isEmpty()) ? DEFAULT_ENDPOINT : endpoint,
            (model == null || model.isEmpty()) ? "openai/gpt-3.5-turbo" : model
        );
    }
    
    @Override
    public String getProviderName() {
        return "OpenRouter";
    }
    
    @Override
    protected Request buildRequest(DialogueRequest request) throws Exception {
        JsonObject requestBody = new JsonObject();
        
        requestBody.addProperty("model", model);
        requestBody.addProperty("temperature", VillagersRebornConfig.LLM_TEMPERATURE);
        requestBody.addProperty("max_tokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        requestBody.addProperty("top_p", 0.95);
        requestBody.addProperty("frequency_penalty", 0.0);
        requestBody.addProperty("presence_penalty", 0.0);
        
        // Build messages array
        JsonArray messages = new JsonArray();
        
        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a helpful AI that generates dialogue for Minecraft villagers. Respond only with the dialogue text, nothing else.");
        messages.add(systemMessage);
        
        // User message with the prompt
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", createBasePrompt(request).get("text").getAsString());
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        
        return new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(gson.toJson(requestBody), JSON))
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/villagesreborn/mod") // Required by OpenRouter
            .addHeader("X-Title", "VillagersReborn Mod") // Required by OpenRouter
            .build();
    }
    
    @Override
    protected String parseResponse(String responseBody) throws Exception {
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        
        // Check for errors
        if (response.has("error")) {
            JsonObject error = response.getAsJsonObject("error");
            throw new Exception("OpenRouter API Error: " + error.get("message").getAsString());
        }
        
        // Parse the response
        if (response.has("choices")) {
            JsonArray choices = response.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content")) {
                        return message.get("content").getAsString();
                    }
                }
            }
        }
        
        throw new Exception("No valid response from OpenRouter API");
    }
}