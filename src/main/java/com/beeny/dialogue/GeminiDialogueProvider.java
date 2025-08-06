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

public class GeminiDialogueProvider extends BaseLLMProvider {
    private static final String DEFAULT_ENDPOINT = StringConstants.DEFAULT_GEMINI_ENDPOINT;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    public GeminiDialogueProvider() {
        super(
            System.getenv(StringConstants.ENV_API_KEY),
            (VillagersRebornConfig.LLM_API_ENDPOINT == null || VillagersRebornConfig.LLM_API_ENDPOINT.isEmpty()) ? DEFAULT_ENDPOINT : VillagersRebornConfig.LLM_API_ENDPOINT,
            (VillagersRebornConfig.LLM_MODEL == null || VillagersRebornConfig.LLM_MODEL.isEmpty()) ? StringConstants.DEFAULT_GEMINI_MODEL : VillagersRebornConfig.LLM_MODEL
        );
    }

    public GeminiDialogueProvider(String apiKey, String endpoint, String model) {
        super(
            apiKey != null ? apiKey : System.getenv(StringConstants.ENV_API_KEY),
            (endpoint == null || endpoint.isEmpty()) ? DEFAULT_ENDPOINT : endpoint,
            (model == null || model.isEmpty()) ? StringConstants.DEFAULT_GEMINI_MODEL : model
        );
    }
    
    @Override
    public String getProviderName() {
        return StringConstants.PROVIDER_NAME_GEMINI;
    }
    
    @Override
    protected @NotNull Request buildRequest(@NotNull DialogueRequest request) throws Exception {
        JsonObject requestBody = new JsonObject();
        
        // Build the contents array
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        
        JsonObject basePrompt = createBasePrompt(request);
        if (basePrompt != null && basePrompt.has("text") && basePrompt.get("text") != null && !basePrompt.get("text").isJsonNull()) {
            part.addProperty("text", basePrompt.get("text").getAsString());
            parts.add(part);
        }
        // If "text" is missing or null, skip adding the part or handle as needed.
        content.add("parts", parts);
        contents.add(content);
        
        requestBody.add("contents", contents);
        
        // Add generation config
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", VillagersRebornConfig.LLM_TEMPERATURE);
        generationConfig.addProperty("maxOutputTokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        generationConfig.addProperty("topP", 0.95);
        generationConfig.addProperty("topK", 40);
        requestBody.add("generationConfig", generationConfig);
        
        // Add safety settings to be more permissive for creative dialogue
        JsonArray safetySettings = new JsonArray();
        String[] categories = {
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH", 
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT"
        };
        
        for (String category : categories) {
            JsonObject setting = new JsonObject();
            setting.addProperty("category", category);
            setting.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.add(setting);
        }
        requestBody.add("safetySettings", safetySettings);
        
        String url = endpoint + "/" + model + ":generateContent?key=" + apiKey;
        
        return new Request.Builder()
            .url(url)
            .post(RequestBody.create(gson.toJson(requestBody), JSON))
            .addHeader("Content-Type", "application/json")
            .build();
    }
    
    @Override
    protected @NotNull String parseResponse(@NotNull String responseBody) throws Exception {
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        
        // Check for errors
        if (response.has("error")) {
            JsonObject error = response.getAsJsonObject("error");
            throw new Exception("Gemini API Error: " + error.get("message").getAsString());
        }
        
        // Parse the response
        if (response.has("candidates")) {
            JsonArray candidates = response.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                
                // Check if the response was blocked
                if (candidate.has("finishReason")) {
                    String finishReason = candidate.get("finishReason").getAsString();
                    if (finishReason.equals("SAFETY")) {
                        throw new Exception("Response blocked by safety filters");
                    }
                }
                
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts.size() > 0) {
                            JsonObject part = parts.get(0).getAsJsonObject();
                            if (part.has("text")) {
                                return part.get("text").getAsString();
                            }
                        }
                    }
                }
            }
        }
        
        throw new Exception("No valid response from Gemini API");
    }
}