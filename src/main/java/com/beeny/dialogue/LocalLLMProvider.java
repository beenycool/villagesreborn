package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LocalLLMProvider now conforms to BaseLLMProvider's template method:
 * - buildRequest constructs the OkHttp Request for the local server
 * - parseResponse extracts the text from the local server response
 * Transport execution and timeouts are handled by BaseLLMProvider.
 */
public class LocalLLMProvider extends BaseLLMProvider {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public LocalLLMProvider() {
        super(
            "", // no API key for local usage
            VillagersRebornConfig.LLM_LOCAL_ENDPOINT,
            VillagersRebornConfig.LLM_MODEL
        );
    }

    @Override
    public String getProviderName() {
        return "local";
    }

    @Override
    protected @NotNull Request buildRequest(@NotNull DialogueRequest request) {
        // Build contextual prompt via shared builder
        String prompt = DialoguePromptBuilder.buildContextualPrompt(
            request.context,
            request.category,
            request.conversationHistory
        );

        JsonObject payload = new JsonObject();
        payload.addProperty("prompt", prompt);
        payload.addProperty("temperature", VillagersRebornConfig.LLM_TEMPERATURE);
        payload.addProperty("max_tokens", VillagersRebornConfig.LLM_MAX_TOKENS);
        payload.addProperty("repeat_penalty", 1.1);
        payload.addProperty("stream", false);

        JsonArray stop = new JsonArray();
        stop.add("\n\n");
        stop.add("Player:");
        stop.add("Villager:");
        payload.add("stop", stop);

        return new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(new Gson().toJson(payload), JSON))
            .addHeader("Content-Type", "application/json")
            .build();
    }

    @Override
    protected @NotNull String parseResponse(@NotNull String responseBody) {
        JsonObject responseJson = new Gson().fromJson(responseBody, JsonObject.class);

        // Try common llama.cpp compatible shapes
        if (responseJson.has("choices") && responseJson.getAsJsonArray("choices").size() > 0) {
            JsonObject choice = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject();
            if (choice.has("text")) return cleanResponse(choice.get("text").getAsString());
        }
        if (responseJson.has("content")) {
            return cleanResponse(responseJson.get("content").getAsString());
        }
        if (responseJson.has("data") && responseJson.getAsJsonArray("data").size() > 0) {
            JsonObject data = responseJson.getAsJsonArray("data").get(0).getAsJsonObject();
            if (data.has("text")) return cleanResponse(data.get("text").getAsString());
        }

        throw new IllegalStateException("Unexpected response format from local LLM");
    }

    private @NotNull String cleanResponse(@Nullable String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) s = s.substring(1, s.length() - 1);
        s = s.replaceFirst("(?i)^(villager says:|villager:|response:|answer:)", "").trim();
        if (!s.isEmpty() && !s.matches(".*[.!?]$")) s += ".";
        return s;
    }

    @Override
    public boolean isConfigured() {
        // For local provider, only endpoint is required
        return endpoint != null && !endpoint.isEmpty();
    }

    @Override
    public void shutdown() {
        // OkHttp shutdown handled by BaseLLMProvider
    }
}