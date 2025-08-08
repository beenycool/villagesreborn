package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.config.AIConfig;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.data.VillagerData;
import com.beeny.config.AIConfig;
import com.beeny.dialogue.DialogueProviderFactory;
import com.beeny.dialogue.DialogueRequest;
import com.beeny.dialogue.LLMDialogueProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;

public class VillagerAISystem {
    
    private static final Gson GSON = new Gson();
    private static final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    
    // Thread pool for AI processing with bounded queue to prevent memory issues
    private static final ExecutorService AI_THREAD_POOL = new ThreadPoolExecutor(
        4, 4, // Core and max pool size
        0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(64), // Bounded queue to prevent memory issues
        r -> {
            Thread t = new Thread(r, "VillagerAI-Worker");
            t.setDaemon(true);
            return t;
        },
        new ThreadPoolExecutor.DiscardPolicy() // Discard excess tasks to prevent blocking
    );
    
    // API endpoints
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    
    public static class AIResponse {
        public final String message;
        public final String toolAction;
        public final String toolTarget;
        
        public AIResponse(String message, String toolAction, String toolTarget) {
            this.message = message;
            this.toolAction = toolAction;
            this.toolTarget = toolTarget;
        }
    }
    
    /**
     * Processes a chat message to see if a villager should respond with AI
     */
    public static boolean processPlayerChat(PlayerEntity player, String message) {
        if (!VillagersRebornConfig.isAiEnabled() || VillagersRebornConfig.getAiApiKey().isEmpty()) {
            return false;
        }
        
        // Validate input
        if (message == null || message.trim().isEmpty()) {
            player.sendMessage(Text.literal("Message cannot be empty").formatted(Formatting.RED), false);
            return false;
        }
        if (message.length() > AIConfig.MAX_PROMPT_LENGTH) {
            player.sendMessage(Text.literal("Message too long (max " + AIConfig.MAX_PROMPT_LENGTH + " characters)").formatted(Formatting.RED), false);
            return false;
        }
        if (!isValidMessage(message)) {
            player.sendMessage(Text.literal("Invalid characters in message").formatted(Formatting.RED), false);
            return false;
        }
        
        // Check rate limiting
        String playerId = player.getUuidAsString();
        long currentTime = System.currentTimeMillis();
        long lastRequestTime = lastRequestTimes.getOrDefault(playerId, 0L);
        
        if (currentTime - lastRequestTime < VillagersRebornConfig.getAiRateLimitSeconds() * 1000) {
            return false;
        }
        
        // Find nearby villagers and check if any are mentioned by name
        List<VillagerEntity> nearbyVillagers = getNearbyVillagers(player, AIConfig.CHAT_DETECTION_RADIUS);
        VillagerEntity targetVillager = findMentionedVillager(message, nearbyVillagers);
        
        if (targetVillager != null) {
            lastRequestTimes.put(playerId, currentTime);
            
            // Generate AI response asynchronously using managed thread pool
            try {
                AI_THREAD_POOL.submit(() -> {
                    try {
                        generateAndSendAIResponse(targetVillager, player, message);
                    } catch (Exception e) {
                        Villagersreborn.LOGGER.error("Error generating AI response", e);
                    }
                });
            } catch (RejectedExecutionException e) {
                Villagersreborn.LOGGER.warn("AI thread pool at capacity, rejecting request for player {}", playerId);
                player.sendMessage(Text.literal("AI system is busy, please try again later").formatted(Formatting.YELLOW), false);
            }
            
            return true;
        }
        
        return false;
    }
    
    private static boolean isValidMessage(String message) {
        // Allow letters, numbers, spaces, and common punctuation
        return message.matches("^[a-zA-Z0-9 ,.!?'\"-]+$");
    }
    
    private static List<VillagerEntity> getNearbyVillagers(PlayerEntity player, double radius) {
        return player.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            player.getBoundingBox().expand(radius),
            villager -> villager.getAttached(Villagersreborn.VILLAGER_DATA) != null
        );
    }
    
    private static VillagerEntity findMentionedVillager(String message, List<VillagerEntity> villagers) {
        String lowerMessage = message.toLowerCase();
        
        for (VillagerEntity villager : villagers) {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null && !data.getName().isEmpty()) {
                // Use word boundaries to avoid partial matches
                Pattern namePattern = Pattern.compile("\\b" + Pattern.quote(data.getName().toLowerCase()) + "\\b");
                if (namePattern.matcher(lowerMessage).find()) {
                    return villager;
                }
            }
        }
        
        return null;
    }
    
    private static void generateAndSendAIResponse(VillagerEntity villager, PlayerEntity player, String playerMessage) {
        try {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data == null) return;
            
            // Build context for AI
            String context = buildVillagerContext(villager, player, data);
            String prompt = buildAIPrompt(context, playerMessage, data);
            
            // Generate AI response
            AIResponse response = generateAIResponse(prompt);
            
            if (response != null && !response.message.isEmpty()) {
                // Send response to nearby players
                sendVillagerResponse(villager, response, player);
                
                // Execute tool action if specified
                if (VillagersRebornConfig.isToolCallingEnabled() && response.toolAction != null) {
                    executeToolAction(villager, response.toolAction, response.toolTarget);
                }
            }
            
        } catch (JsonSyntaxException e) {
            player.sendMessage(Text.literal("Error: Invalid response from AI service").formatted(Formatting.RED), false);
            Villagersreborn.LOGGER.error("JSON parsing error in AI response", e);
        } catch (Exception e) {
            player.sendMessage(Text.literal("Error: Unexpected error processing AI response").formatted(Formatting.RED), false);
            Villagersreborn.LOGGER.error("Unexpected error in AI response generation", e);
        }
    }
    
    private static String buildVillagerContext(VillagerEntity villager, PlayerEntity player, VillagerData data) {
        StringBuilder context = new StringBuilder();
        
        // Current activity
        VillagerScheduleManager.Activity currentActivity = VillagerScheduleManager.getCurrentActivity(villager);
        context.append("Currently ").append(currentActivity.description.toLowerCase()).append(". ");
        
        // Basic info
        context.append("Name: ").append(data.getName()).append(". ");
        context.append("Personality: ").append(data.getPersonality()).append(". ");
        context.append("Profession: ").append(villager.getVillagerData().profession().toString().replace("minecraft:", "")).append(". ");
        context.append("Happiness: ").append(data.getHappiness()).append("%. ");
        
        // Family info
        if (!data.getSpouseName().isEmpty()) {
            context.append("Married to ").append(data.getSpouseName()).append(". ");
        }
        if (!data.getChildrenNames().isEmpty()) {
            context.append("Parent of ").append(String.join(", ", data.getChildrenNames())).append(". ");
        }
        
        // Time and weather
        long worldTime = villager.getWorld().getTimeOfDay();
        VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(worldTime);
        context.append("Time: ").append(timeOfDay.name.toLowerCase()).append(". ");
        
        if (villager.getWorld().isRaining()) {
            context.append("Weather: raining. ");
        } else if (villager.getWorld().isThundering()) {
            context.append("Weather: stormy. ");
        }
        
        return context.toString();
    }
    
    private static String buildAIPrompt(String context, String playerMessage, VillagerData data) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are ").append(data.getName()).append(", a villager in Minecraft. ");
        prompt.append("Context: ").append(context);
        prompt.append("\n\nPlayer said: \"").append(playerMessage).append("\"");
        prompt.append("\n\nRespond as ").append(data.getName()).append(" in character. ");
        prompt.append("Keep responses under ").append(VillagersRebornConfig.getAiMaxTokens()).append(" characters. ");
        prompt.append("Be conversational and mention your current activity if relevant. ");
        
        // Add tool-calling instructions if enabled
        if (VillagersRebornConfig.isToolCallingEnabled()) {
            prompt.append("\n\nIf appropriate for your profession and current activity, you may use tools. ");
            prompt.append("To use a tool, include [TOOL:action:target] in your response. ");
            prompt.append("Available tools: [TOOL:hoe:crops], [TOOL:fishing_rod:water], [TOOL:book:knowledge], [TOOL:anvil:item]. ");
            prompt.append("Only use tools when it makes sense for your profession and what you're doing.");
        }
        
        return prompt.toString();
    }
    
    private static AIResponse generateAIResponse(String prompt) {
        try {
            LLMDialogueProvider provider = DialogueProviderFactory.createProvider(
                VillagersRebornConfig.getAiProvider(),
                VillagersRebornConfig.getAiApiKey(),
                getEndpointForProvider(VillagersRebornConfig.getAiProvider()),
                getModelForProvider(VillagersRebornConfig.getAiProvider())
            );
            
            DialogueRequest request = new DialogueRequest(
                "",  // Context is already included in prompt
                "player_chat",
                prompt,
                Collections.emptyList()
            );
            
            CompletableFuture<String> future = provider.generateDialogue(request);
            return future.thenApply(VillagerAISystem::parseAIResponse).get();
        } catch (Exception e) {
            Villagersreborn.LOGGER.error("Error generating AI response", e);
            return null;
        }
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
    
    private static AIResponse parseAIResponse(String response) {
        // Extract tool commands if present
        Pattern toolPattern = Pattern.compile("\\[TOOL:([^:]+):([^\\]]+)\\]");
        Matcher matcher = toolPattern.matcher(response);
        
        String toolAction = null;
        String toolTarget = null;
        String cleanMessage = response;
        
        if (matcher.find()) {
            toolAction = matcher.group(1);
            toolTarget = matcher.group(2);
            cleanMessage = response.replaceAll("\\[TOOL:[^\\]]+\\]", "").trim();
        }
        
        // Sanitize AI response to prevent harmful content
        cleanMessage = sanitizeAIResponse(cleanMessage);
        
        return new AIResponse(cleanMessage, toolAction, toolTarget);
    }
    
    /**
     * Sanitizes AI response content to prevent harmful or unwanted content
     */
    private static String sanitizeAIResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "..."; // Fallback for empty responses
        }
        
        // Limit response length to prevent spam
        if (response.length() > 500) {
            response = response.substring(0, 497) + "...";
        }
        
        // Remove potentially harmful patterns
        response = response.replaceAll("(?i)\\b(server|admin|op|whitelist|ban|kick)\\b", "[REDACTED]");
        
        // Remove excessive whitespace and normalize
        response = response.replaceAll("\\s+", " ").trim();
        
        // Remove any remaining formatting codes or special characters that could be problematic
        response = response.replaceAll("[§&][0-9a-fk-or]", "");
        
        return response;
    }
    
    private static void sendVillagerResponse(VillagerEntity villager, AIResponse response, PlayerEntity player) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        // Format the response message
        Text villagerMessage = Text.literal("<" + data.getName() + "> ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(response.message).formatted(Formatting.WHITE));
        
        // Send to nearby players (optimized)
        villager.getWorld().getEntitiesByClass(PlayerEntity.class, villager.getBoundingBox().expand(AIConfig.BROADCAST_RADIUS), p -> true).forEach(nearbyPlayer -> {
            nearbyPlayer.sendMessage(villagerMessage, false);
        });
    }
    
    private static void executeToolAction(VillagerEntity villager, String action, String target) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        // Simple tool action simulation - just send a message about the action
        String actionMessage = switch (action.toLowerCase()) {
            case "hoe" -> data.getName() + " starts tending to the " + target + " with a hoe.";
            case "fishing_rod" -> data.getName() + " casts a fishing line into the " + target + ".";
            case "book" -> data.getName() + " opens a book to share knowledge about " + target + ".";
            case "anvil" -> data.getName() + " begins working on " + target + " at the anvil.";
            default -> data.getName() + " uses a tool for " + action + " on " + target + ".";
        };
        
        // Send action message to nearby players
        Text actionText = Text.literal(actionMessage).formatted(Formatting.ITALIC, Formatting.GRAY);
        
        villager.getWorld().getEntitiesByClass(PlayerEntity.class, villager.getBoundingBox().expand(AIConfig.TOOL_ACTION_RADIUS), p -> true).forEach(player -> {
            player.sendMessage(actionText, false);
        });
        
        Villagersreborn.LOGGER.info("Villager {} performed tool action: {} on {}", data.getName(), action, target);
    }
    
    /**
     * Mask API key for logging/display purposes
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
    
    /**
     * Shutdown the AI thread pool gracefully
     */
    public static void shutdown() {
        if (AI_THREAD_POOL != null && !AI_THREAD_POOL.isShutdown()) {
            AI_THREAD_POOL.shutdown();
            try {
                if (!AI_THREAD_POOL.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    AI_THREAD_POOL.shutdownNow();
                }
            } catch (InterruptedException e) {
                AI_THREAD_POOL.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}