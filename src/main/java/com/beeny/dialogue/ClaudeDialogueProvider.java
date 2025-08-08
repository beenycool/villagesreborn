package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.constants.StringConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClaudeDialogueProvider extends BaseLLMProvider {
    private static final String DEFAULT_ENDPOINT = StringConstants.DEFAULT_CLAUDE_ENDPOINT;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    public ClaudeDialogueProvider() {
        super(
            System.getenv(StringConstants.ENV_API_KEY),
            VillagersRebornConfig.LLM_API_ENDPOINT.isEmpty() ? DEFAULT_ENDPOINT : VillagersRebornConfig.LLM_API_ENDPOINT,
            VillagersRebornConfig.LLM_MODEL.isEmpty() ? StringConstants.DEFAULT_CLAUDE_MODEL : VillagersRebornConfig.LLM_MODEL
        );
    }

    public ClaudeDialogueProvider(String apiKey, String endpoint, String model) {
        super(
            apiKey != null ? apiKey : System.getenv(StringConstants.ENV_API_KEY),
            (endpoint == null || endpoint.isEmpty()) ? DEFAULT_ENDPOINT : endpoint,
            (model == null || model.isEmpty()) ? StringConstants.DEFAULT_CLAUDE_MODEL : model
        );
    }
    
    @Override
    public String getProviderName() {
        return StringConstants.PROVIDER_NAME_CLAUDE;
    }
    
    @Override
    protected @NotNull Request buildRequest(@NotNull DialogueRequest request) throws Exception {
        JsonObject requestBody = new JsonObject();
        
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        
        // Build messages array for Claude
        JsonArray messages = new JsonArray();
        
        // System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("type", "text");
        systemMessage.addProperty("text", "You are a helpful AI that generates dialogue for Minecraft villagers. Respond only with the dialogue text, nothing else.");
        
        // Add system message to system field
        requestBody.add("system", systemMessage);
        
        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", createBasePrompt(request).get("text").getAsString());
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        
        return new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(gson.toJson(requestBody), JSON))
            .addHeader("x-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("anthropic-version", "2023-06-01")
            .build();
    }
    
    @Override
    protected @NotNull String parseResponse(@NotNull String responseBody) throws Exception {
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        
        // Check for errors
        if (response.has("error")) {
            JsonObject error = response.getAsJsonObject("error");
            throw new Exception("Claude API Error: " + error.get("message").getAsString());
        }
        
        // Parse the response
        if (response.has("content")) {
            JsonArray content = response.getAsJsonArray("content");
            if (content.size() > 0) {
                JsonObject contentItem = content.get(0).getAsJsonObject();
                if (contentItem.has("text")) {
                    return contentItem.get("text").getAsString();
                }
            }
        }
        
        throw new Exception("No valid response from Claude API");
    }
}