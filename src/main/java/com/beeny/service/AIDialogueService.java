package com.beeny.service;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.dialogue.BaseLLMProvider;
import com.beeny.dialogue.LLMDialogueProvider.DialogueRequest;
import com.beeny.dialogue.DialogueResponseHandler;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import java.util.concurrent.CompletableFuture;

public class AIDialogueService extends BaseLLMProvider {

    public AIDialogueService() {
        super(
            VillagersRebornConfig.getAiApiKey(),
            getEndpointForProvider(VillagersRebornConfig.getAiProvider()),
            getModelForProvider(VillagersRebornConfig.getAiProvider())
        );
    }

    private static String getEndpointForProvider(String provider) {
        return switch (provider) {
            case VillagersRebornConfig.AI_PROVIDER_GEMINI -> VillagersRebornConfig.GEMINI_ENDPOINT;
            case VillagersRebornConfig.AI_PROVIDER_OPENROUTER -> VillagersRebornConfig.OPENROUTER_ENDPOINT;
            default -> throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        };
    }

    private static String getModelForProvider(String provider) {
        return switch (provider) {
            case VillagersRebornConfig.AI_PROVIDER_GEMINI -> VillagersRebornConfig.GEMINI_MODEL;
            case VillagersRebornConfig.AI_PROVIDER_OPENROUTER -> VillagersRebornConfig.OPENROUTER_MODEL;
            default -> throw new IllegalArgumentException("Unsupported AI provider: " + provider);
        };
    }

    @Override
    protected @NotNull Request buildRequest(@NotNull DialogueRequest request) throws Exception {
        JsonObject payload = createDialoguePayload(request);
        RequestBody body = RequestBody.create(
            MediaType.get("application/json; charset=utf-8"),
            gson.toJson(payload)
        );
        return new Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();
    }

    @Override
    protected @NotNull String parseResponse(@NotNull String responseBody) {
        return DialogueResponseHandler.extractMessage(responseBody);
    }

    public CompletableFuture<String> generateVillagerResponse(DialogueRequest request) {
        return generateDialogue(request);
    }
    
    private JsonObject createDialoguePayload(DialogueRequest request) {
        JsonObject payload = new JsonObject();
        payload.addProperty("prompt", request.prompt);
        payload.addProperty("model", model);
        payload.addProperty("max_tokens", VillagersRebornConfig.getAiMaxTokens());
        return payload;
    }
}