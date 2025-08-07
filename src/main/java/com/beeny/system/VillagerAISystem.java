package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.data.VillagerData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class VillagerAISystem {
    
    private static final Gson GSON = new Gson();
    private static final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private static final Pattern VILLAGER_NAME_PATTERN = Pattern.compile("\\b([A-Z][a-z]+)\\b");
    
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
        if (!VillagersRebornConfig.AI_ENABLED || VillagersRebornConfig.AI_API_KEY.isEmpty()) {
            return false;
        }
        
        // Check rate limiting
        String playerId = player.getUuidAsString();
        long currentTime = System.currentTimeMillis();
        long lastRequestTime = lastRequestTimes.getOrDefault(playerId, 0L);
        
        if (currentTime - lastRequestTime < VillagersRebornConfig.AI_RATE_LIMIT_SECONDS * 1000) {
            return false;
        }
        
        // Find nearby villagers and check if any are mentioned by name
        List<VillagerEntity> nearbyVillagers = getNearbyVillagers(player, 10.0);
        VillagerEntity targetVillager = findMentionedVillager(message, nearbyVillagers);
        
        if (targetVillager != null) {
            lastRequestTimes.put(playerId, currentTime);
            
            // Generate AI response asynchronously to avoid blocking
            Thread.ofVirtual().start(() -> {
                try {
                    generateAndSendAIResponse(targetVillager, player, message);
                } catch (Exception e) {
                    Villagersreborn.LOGGER.error("Error generating AI response", e);
                }
            });
            
            return true;
        }
        
        return false;
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
                if (lowerMessage.contains(data.getName().toLowerCase())) {
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
                if (VillagersRebornConfig.TOOL_CALLING_ENABLED && response.toolAction != null) {
                    executeToolAction(villager, response.toolAction, response.toolTarget);
                }
            }
            
        } catch (Exception e) {
            Villagersreborn.LOGGER.error("Error in AI response generation", e);
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
        prompt.append("Keep responses under ").append(VillagersRebornConfig.AI_MAX_TOKENS).append(" characters. ");
        prompt.append("Be conversational and mention your current activity if relevant. ");
        
        // Add tool-calling instructions if enabled
        if (VillagersRebornConfig.TOOL_CALLING_ENABLED) {
            prompt.append("\n\nIf appropriate for your profession and current activity, you may use tools. ");
            prompt.append("To use a tool, include [TOOL:action:target] in your response. ");
            prompt.append("Available tools: [TOOL:hoe:crops], [TOOL:fishing_rod:water], [TOOL:book:knowledge], [TOOL:anvil:item]. ");
            prompt.append("Only use tools when it makes sense for your profession and what you're doing.");
        }
        
        return prompt.toString();
    }
    
    private static AIResponse generateAIResponse(String prompt) {
        try {
            String response;
            
            if ("gemini".equals(VillagersRebornConfig.AI_PROVIDER)) {
                response = callGeminiAPI(prompt);
            } else if ("openrouter".equals(VillagersRebornConfig.AI_PROVIDER)) {
                response = callOpenRouterAPI(prompt);
            } else {
                Villagersreborn.LOGGER.warn("Unknown AI provider: " + VillagersRebornConfig.AI_PROVIDER);
                return null;
            }
            
            if (response != null) {
                return parseAIResponse(response);
            }
            
        } catch (Exception e) {
            Villagersreborn.LOGGER.error("Error calling AI API", e);
        }
        
        return null;
    }
    
    private static String callGeminiAPI(String prompt) throws IOException {
        URL url = new URL(GEMINI_API_URL + "?key=" + VillagersRebornConfig.AI_API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        JsonObject requestBody = new JsonObject();
        JsonObject contents = new JsonObject();
        JsonObject parts = new JsonObject();
        parts.addProperty("text", prompt);
        contents.add("parts", GSON.toJsonTree(new JsonObject[]{parts}));
        requestBody.add("contents", GSON.toJsonTree(new JsonObject[]{contents}));
        
        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(requestBody));
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                JsonObject responseJson = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (responseJson.has("candidates") && responseJson.getAsJsonArray("candidates").size() > 0) {
                    JsonObject candidate = responseJson.getAsJsonArray("candidates").get(0).getAsJsonObject();
                    if (candidate.has("content")) {
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                            JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                            if (part.has("text")) {
                                return part.get("text").getAsString();
                            }
                        }
                    }
                }
            }
        } else {
            Villagersreborn.LOGGER.warn("Gemini API returned status: " + responseCode);
        }
        
        return null;
    }
    
    private static String callOpenRouterAPI(String prompt) throws IOException {
        URL url = new URL(OPENROUTER_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + VillagersRebornConfig.AI_API_KEY);
        conn.setDoOutput(true);
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "anthropic/claude-3.5-haiku");
        requestBody.addProperty("max_tokens", VillagersRebornConfig.AI_MAX_TOKENS);
        
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        requestBody.add("messages", GSON.toJsonTree(new JsonObject[]{message}));
        
        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(requestBody));
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                JsonObject responseJson = JsonParser.parseString(response.toString()).getAsJsonObject();
                if (responseJson.has("choices") && responseJson.getAsJsonArray("choices").size() > 0) {
                    JsonObject choice = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("message")) {
                        JsonObject message2 = choice.getAsJsonObject("message");
                        if (message2.has("content")) {
                            return message2.get("content").getAsString();
                        }
                    }
                }
            }
        } else {
            Villagersreborn.LOGGER.warn("OpenRouter API returned status: " + responseCode);
        }
        
        return null;
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
        
        return new AIResponse(cleanMessage, toolAction, toolTarget);
    }
    
    private static void sendVillagerResponse(VillagerEntity villager, AIResponse response, PlayerEntity player) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        // Format the response message
        Text villagerMessage = Text.literal("<" + data.getName() + "> ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(response.message).formatted(Formatting.WHITE));
        
        // Send to nearby players
        villager.getWorld().getPlayers().forEach(nearbyPlayer -> {
            if (nearbyPlayer.getPos().distanceTo(villager.getPos()) < 20) {
                nearbyPlayer.sendMessage(villagerMessage, false);
            }
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
        
        villager.getWorld().getPlayers().forEach(player -> {
            if (player.getPos().distanceTo(villager.getPos()) < 15) {
                player.sendMessage(actionText, false);
            }
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
}