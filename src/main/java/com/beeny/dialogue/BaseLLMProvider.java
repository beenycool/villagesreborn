package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseLLMProvider implements LLMDialogueProvider {
    protected final OkHttpClient client;
    protected final Gson gson;
    protected final String apiKey;
    protected final String endpoint;
    protected final String model;
    
    public BaseLLMProvider(@Nullable String apiKey, @Nullable String endpoint, @Nullable String model) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.gson = new Gson();
        
        this.client = new OkHttpClient.Builder()
            .connectTimeout(VillagersRebornConfig.LLM_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(VillagersRebornConfig.LLM_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(VillagersRebornConfig.LLM_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
            .build();
    }
    
    @Override
    public @NotNull CompletableFuture<String> generateDialogue(@NotNull DialogueRequest request) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(null);
        }
        
        long startTime = System.currentTimeMillis();
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            Request httpRequest = buildRequest(request);
            
            client.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    future.completeExceptionally(e);
                }
                
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            String dialogue = parseResponse(responseBody);
                            
                            if (dialogue != null && !dialogue.trim().isEmpty()) {
                                future.complete(cleanDialogue(dialogue));
                            } else {
                                future.complete(null);
                            }
                        } else {
                            future.completeExceptionally(new IOException("HTTP " + response.code() + ": " + response.message()));
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    } finally {
                        response.close();
                    }
                }
            });
            
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && 
               endpoint != null && !endpoint.trim().isEmpty();
    }
    
    @Override
    public void shutdown() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
    
    protected abstract @NotNull Request buildRequest(@NotNull DialogueRequest request) throws Exception;

    protected abstract @NotNull String parseResponse(@NotNull String responseBody) throws Exception;
    
    protected @Nullable String cleanDialogue(@Nullable String dialogue) {
        if (dialogue == null) return null;
        
        return dialogue.trim()
            .replaceAll("^\"|\"$", "") // Remove surrounding quotes
            .replaceAll("\\\\n", " ") // Replace escaped newlines with spaces
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();
    }
    
    protected @NotNull JsonObject createBasePrompt(@NotNull DialogueRequest request) {
        String contextualPrompt = DialoguePromptBuilder.buildContextualPrompt(
            request.context,
            request.category,
            request.conversationHistory
        );
        JsonObject prompt = new JsonObject();
        prompt.addProperty("text", contextualPrompt);
        return prompt;
    }
}