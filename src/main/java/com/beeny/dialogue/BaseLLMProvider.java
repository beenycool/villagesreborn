package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class BaseLLMProvider implements LLMDialogueProvider {
    protected final OkHttpClient client;
    protected final Gson gson;
    protected final String apiKey;
    protected final String endpoint;
    protected final String model;
    
    public BaseLLMProvider(String apiKey, String endpoint, String model) {
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
    public CompletableFuture<String> generateDialogue(DialogueRequest request) {
        if (!isConfigured()) {
            return CompletableFuture.completedFuture(null);
        }
        
        long startTime = System.currentTimeMillis();
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            Request httpRequest = buildRequest(request);
            
            client.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
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
    
    protected abstract Request buildRequest(DialogueRequest request) throws Exception;
    
    protected abstract String parseResponse(String responseBody) throws Exception;
    
    protected String cleanDialogue(String dialogue) {
        if (dialogue == null) return null;
        
        return dialogue.trim()
            .replaceAll("^\"|\"$", "") // Remove surrounding quotes
            .replaceAll("\\\\n", " ") // Replace escaped newlines with spaces
            .replaceAll("\\s+", " ") // Normalize whitespace
            .trim();
    }
    
    protected JsonObject createBasePrompt(DialogueRequest request) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("You are ").append(request.context.villagerData.getName())
                    .append(", a ").append(request.context.villagerData.getPersonality().toLowerCase())
                    .append(" villager in a Minecraft village. ");
        
        promptBuilder.append("Your profession is ")
                    .append(request.context.villager.getVillagerData().profession().toString().replace("minecraft:", ""))
                    .append(". ");
        
        promptBuilder.append("Your hobby is ").append(request.context.villagerData.getHobby().toLowerCase()).append(". ");
        
        promptBuilder.append("You are ").append(request.context.villagerData.getHappinessDescription().toLowerCase()).append(". ");
        
        promptBuilder.append("The time is ").append(request.context.timeOfDay.name.toLowerCase())
                    .append(" and the weather is ").append(request.context.weather).append(". ");
        
        if (!request.context.villagerData.getSpouseName().isEmpty()) {
            promptBuilder.append("You are married to ").append(request.context.villagerData.getSpouseName()).append(". ");
        }
        
        if (!request.context.villagerData.getChildrenNames().isEmpty()) {
            promptBuilder.append("You have children: ").append(String.join(", ", request.context.villagerData.getChildrenNames())).append(". ");
        }
        
        promptBuilder.append("The player ").append(request.context.player.getName().getString())
                    .append(" is talking to you. ");
        
        if (request.context.playerReputation > 20) {
            promptBuilder.append("You know this player well and like them. ");
        } else if (request.context.playerReputation < -10) {
            promptBuilder.append("You don't trust this player much. ");
        }
        
        if (request.conversationHistory != null && !request.conversationHistory.trim().isEmpty()) {
            promptBuilder.append("Recent conversation: ").append(request.conversationHistory).append(" ");
        }
        
        promptBuilder.append("Respond as this villager would, staying in character. ");
        promptBuilder.append("Keep the response under 50 words and make it conversational. ");
        promptBuilder.append("Don't use asterisks or action descriptions, just speak naturally. ");
        
        switch (request.category) {
            case GREETING -> promptBuilder.append("Give a greeting appropriate to your personality and relationship with the player.");
            case WEATHER -> promptBuilder.append("Comment on the current weather in a way that fits your personality.");
            case WORK -> promptBuilder.append("Talk about your work or profession in your own style.");
            case FAMILY -> promptBuilder.append("Mention something about your family or personal life.");
            case GOSSIP -> promptBuilder.append("Share some village gossip or news, but keep it light and friendly.");
            case HOBBY -> promptBuilder.append("Talk about your hobby or something you enjoy doing.");
            case ADVICE -> promptBuilder.append("Give some friendly advice or wisdom.");
            case STORY -> promptBuilder.append("Tell a brief, interesting story from your experience.");
            case FAREWELL -> promptBuilder.append("Say goodbye in a way that matches your personality.");
            default -> promptBuilder.append("Have a casual conversation with the player.");
        }
        
        JsonObject prompt = new JsonObject();
        prompt.addProperty("text", promptBuilder.toString());
        return prompt;
    }
}