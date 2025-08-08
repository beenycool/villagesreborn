package com.beeny.service;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.dialogue.BaseLLMProvider;
import com.beeny.dialogue.DialogueRequest;
import com.beeny.dialogue.DialogueResponseHandler;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    protected @NotNull JsonObject buildRequest(@NotNull DialogueRequest request) {
        return createDialoguePayload(request);
    }

    @Override
    protected @NotNull String parseResponse(@NotNull String responseBody) {
        return DialogueResponseHandler.extractMessage(responseBody);
    }

    public CompletableFuture<String> generateVillagerResponse(DialogueRequest request) {
        return generateDialogue(request);
    }
}