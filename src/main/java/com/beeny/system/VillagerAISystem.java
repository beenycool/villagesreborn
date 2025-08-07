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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VillagerAISystem {
    
    private static final Gson GSON = new Gson();
    private static final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    
    // API endpoints
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    
    // POJO classes for API responses
    private static class GeminiResponse {
        private List<Candidate> candidates;
        public List<Candidate> getCandidates() { return candidates; }
    }
    
    private static class Candidate {
        private Content content;
        public Content getContent() { return content; }
    }
    
    private static class Content {
        private List<Part> parts;
        public List<Part> getParts() { return parts; }
    }
    
    private static class Part {
        private String text;
        public String getText() { return text; }
    }
    
    private static class OpenRouterResponse {
        private List<Choice> choices;
        public List<Choice> getChoices() { return choices; }
    }
    
    private static class Choice {
        private Message message;
        public Message getMessage() { return message; }
    }
    
    private static class Message {
        private String content;
        public String getContent() { return content; }
    }
    
    // Tool definition structure
    private static class Tool {
        final String action;
        final String defaultTarget;
        final String description;
        
        Tool(String action, String defaultTarget, String description) {
            this.action = action;
            this.defaultTarget = defaultTarget;
            this.description = description;
        }
        
        String toPromptFormat() {
            return String.format("[TOOL:%s:%s]", action, defaultTarget);
        }
    }
    
    private static final List<Tool> AVAILABLE_TOOLS = List.of(
        new Tool("hoe", "crops", "Tend to crops"),
        new Tool("fishing_rod", "water", "Fish in water"),
        new Tool("book", "knowledge", "Share knowledge"),
        new Tool("anvil", "item", "Work on items")
    );
    
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
        
        // Sanitize input message
        if (message.length() > 500) {
            message = message.substring(0, 500);
        }
        message = sanitizeInput(message);
        
        // Check rate limiting with atomic operation
        String playerId = player.getUuidAsString();
        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = lastRequestTimes.get(playerId);
        
        if (lastRequestTime != null && currentTime - lastRequestTime < VillagersRebornConfig.getAiRateLimitSeconds() * 1000) {
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
    
    /**
     * Sanitize input to prevent injection attacks
     */
    private static String sanitizeInput(String input) {
        if (input == null) return "";
        // Basic sanitization - remove potential injection characters
        return input.replaceAll("[<>\"'&]", "").trim();
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
        prompt.append("Keep responses under ").append(VillagersRebornConfig.getAiMaxTokens()).append(" characters. ");
        prompt.append("Be conversational and mention your current activity if relevant. ");
        
        // Add tool-calling instructions if enabled
        if (VillagersRebornConfig.isToolCallingEnabled()) {
            prompt.append("\n\nIf appropriate for your profession and current activity, you may use tools. ");
            prompt.append("To use a tool, include [TOOL:action:target] in your response. ");
            String toolsList = AVAILABLE_TOOLS.stream()
                .map(Tool::toPromptFormat)
                .collect(Collectors.joining(", "));
            prompt.append("Available tools: ").append(toolsList).append(". ");
            prompt.append("Only use tools when it makes sense for your profession and what you're doing.");
        }
        
        return prompt.toString();
    }
    
    private static AIResponse generateAIResponse(String prompt) {
        try {
            String response;
            
            if (VillagersRebornConfig.AI_PROVIDER_GEMINI.equals(VillagersRebornConfig.getAiProvider())) {
                response = callGeminiAPI(prompt);
            } else if (VillagersRebornConfig.AI_PROVIDER_OPENROUTER.equals(VillagersRebornConfig.getAiProvider())) {
                response = callOpenRouterAPI(prompt);
            } else {
                Villagersreborn.LOGGER.warn("Unknown AI provider: " + VillagersRebornConfig.getAiProvider());
                return null;
            }
            
            if (response != null) {
                return parseAIResponse(response);
            }
            
        } catch (IOException e) {
            Villagersreborn.LOGGER.error("Network error calling AI API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Villagersreborn.LOGGER.error("AI API call was interrupted", e);
        } catch (Exception e) {
            Villagersreborn.LOGGER.error("Unexpected error calling AI API", e);
        }
        
        return null;
    }
    
    private static String callGeminiAPI(String prompt) throws IOException, InterruptedException {
        JsonObject requestBody = buildGeminiRequest(prompt);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GEMINI_API_URL))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", VillagersRebornConfig.getAiApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return parseGeminiResponse(response.body());
        } else {
            // Read error body for debugging
            String errorBody = response.body();
            Villagersreborn.LOGGER.warn("Gemini API returned status: {} - Error: {}", 
                response.statusCode(), errorBody);
        }
        
        return null;
    }
    
    private static JsonObject buildGeminiRequest(String prompt) {
        JsonObject requestBody = new JsonObject();
        JsonObject contents = new JsonObject();
        JsonObject parts = new JsonObject();
        parts.addProperty("text", prompt);
        contents.add("parts", GSON.toJsonTree(new JsonObject[]{parts}));
        requestBody.add("contents", GSON.toJsonTree(new JsonObject[]{contents}));
        
        // Add generation config with token limit
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", VillagersRebornConfig.getAiMaxTokens());
        requestBody.add("generationConfig", generationConfig);
        
        return requestBody;
    }
    
    private static String parseGeminiResponse(String responseString) {
        GeminiResponse response = GSON.fromJson(responseString, GeminiResponse.class);
        if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            Candidate candidate = response.getCandidates().get(0);
            if (candidate.getContent() != null && 
                candidate.getContent().getParts() != null && 
                !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }
    
    private static String callOpenRouterAPI(String prompt) throws IOException, InterruptedException {
        JsonObject requestBody = buildOpenRouterRequest(prompt);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OPENROUTER_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + VillagersRebornConfig.getAiApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return parseOpenRouterResponse(response.body());
        } else {
            // Read error body for debugging
            String errorBody = response.body();
            Villagersreborn.LOGGER.warn("OpenRouter API returned status: {} - Error: {}", 
                response.statusCode(), errorBody);
        }
        
        return null;
    }
    
    private static JsonObject buildOpenRouterRequest(String prompt) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "anthropic/claude-3.5-haiku");
        requestBody.addProperty("max_tokens", VillagersRebornConfig.getAiMaxTokens());
        
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        requestBody.add("messages", GSON.toJsonTree(new JsonObject[]{message}));
        
        return requestBody;
    }
    
    private static String parseOpenRouterResponse(String responseString) {
        OpenRouterResponse response = GSON.fromJson(responseString, OpenRouterResponse.class);
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            Choice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
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
        
        // Send to nearby players (optimized)
        villager.getWorld().getEntitiesByClass(PlayerEntity.class, villager.getBoundingBox().expand(20), p -> true).forEach(nearbyPlayer -> {
            nearbyPlayer.sendMessage(villagerMessage, false);
        });
    }
    
    private static void executeToolAction(VillagerEntity villager, String action, String target) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        // Validate tool action against available tools
        boolean validTool = AVAILABLE_TOOLS.stream()
            .anyMatch(tool -> tool.action.equalsIgnoreCase(action));
        
        if (!validTool) {
            Villagersreborn.LOGGER.warn("Invalid tool action attempted: {}", action);
            return;
        }
        
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
        
        villager.getWorld().getEntitiesByClass(PlayerEntity.class, villager.getBoundingBox().expand(15), p -> true).forEach(player -> {
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
}