package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.conversation.ConversationContext;

import java.util.List;

/**
 * Builds comprehensive prompts for LLM-powered villager conversations
 * Assembles system prompts, conversation history, and dynamic context layers
 */
public class PromptBuilder {
    
    private static final int DEFAULT_CONTEXT_WINDOW_SIZE = 10;
    private static final int MAX_TOKEN_ESTIMATE = 8000; // Conservative estimate for token limits
    private static final int CHARS_PER_TOKEN_ESTIMATE = 4; // Rough estimate
    
    // System prompt template
    private static final String SYSTEM_PROMPT_TEMPLATE = 
        "You are a villager in a Minecraft village. You have your own personality, mood, and memories.\n\n" +
        "Your personality: %s\n" +
        "Your current mood: %s\n\n" +
        "Guidelines:\n" +
        "- Respond naturally as this character would\n" +
        "- Keep responses concise (1-3 sentences)\n" +
        "- Reference village life when relevant\n" +
        "- Show personality through word choice and tone\n" +
        "- Remember previous conversations with players\n" +
        "- Use minecraft-appropriate language\n" +
        "- No modern references\n" +
        "- Show emotion appropriate to mood and relationships\n";
    
    /**
     * Builds a complete prompt for villager conversation
     * @param villagerBrain the villager's brain containing personality and memories
     * @param context the current conversation context
     * @return assembled prompt string
     */
    public static String buildPrompt(VillagerBrain villagerBrain, ConversationContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // System prompt with villager identity
        prompt.append(buildSystemPrompt(villagerBrain));
        prompt.append("\n\n");
        
        // Current context
        prompt.append(buildCurrentContext(context));
        prompt.append("\n\n");
        
        // Conversation history (with token limiting)
        String historySection = buildConversationHistory(villagerBrain.getShortTermMemory());
        if (!historySection.trim().isEmpty()) {
            prompt.append(historySection);
            prompt.append("\n\n");
        }
        
        // Response instruction
        prompt.append("Current Player Message: [Waiting for input]\n");
        prompt.append("Your Response: ");
        
        // Apply token length limits by truncating if necessary
        return enforceTokenLimits(prompt.toString(), villagerBrain.getShortTermMemory());
    }
    
    /**
     * Builds the system prompt including villager personality and world context
     * @param villagerBrain the villager's brain
     * @return system prompt string
     */
    public static String buildSystemPrompt(VillagerBrain villagerBrain) {
        String personalityDesc = villagerBrain.getPersonalityTraits().generateDescription();
        String moodDesc = villagerBrain.getCurrentMood().getOverallMood().toString();
        
        return String.format(SYSTEM_PROMPT_TEMPLATE, personalityDesc, moodDesc);
    }
    
    /**
     * Builds conversation history section with proper formatting
     * @param shortTermMemory the conversation history
     * @return formatted conversation history
     */
    public static String buildConversationHistory(ConversationHistory shortTermMemory) {
        List<ConversationInteraction> recentInteractions = 
            shortTermMemory.getRecent(DEFAULT_CONTEXT_WINDOW_SIZE);
        
        if (recentInteractions.isEmpty()) {
            return "";
        }
        
        StringBuilder history = new StringBuilder();
        history.append("Recent Conversation History:\n");
        
        for (ConversationInteraction interaction : recentInteractions) {
            history.append("Player: ").append(interaction.getPlayerMessage()).append("\n");
            history.append("You: ").append(interaction.getVillagerResponse()).append("\n");
        }
        
        return history.toString();
    }
    
    /**
     * Builds current context section with dynamic context layers
     * @param context the conversation context
     * @return formatted context section
     */
    public static String buildCurrentContext(ConversationContext context) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Current Situation:\n");
        
        // Basic context
        contextBuilder.append("- Time: ").append(context.getTimeOfDay()).append("\n");
        contextBuilder.append("- Weather: ").append(context.getWeather()).append("\n");
        contextBuilder.append("- Location: ").append(context.getLocation()).append("\n");
        contextBuilder.append("- Player Relationship: ").append(context.getRelationship()).append("\n");
        
        // Dynamic context layers
        if (context.hasNearbyVillagers()) {
            contextBuilder.append("- Other villagers nearby: ").append(context.getNearbyVillagers()).append("\n");
        }
        
        return contextBuilder.toString();
    }
    
    /**
     * Enforces token-length limits by truncating oldest entries if necessary
     * @param prompt the current prompt
     * @param shortTermMemory the conversation history for truncation
     * @return prompt within token limits
     */
    private static String enforceTokenLimits(String prompt, ConversationHistory shortTermMemory) {
        int estimatedTokens = estimateTokenCount(prompt);
        
        if (estimatedTokens <= MAX_TOKEN_ESTIMATE) {
            return prompt;
        }
        
        // If prompt is too long, rebuild with fewer history entries
        int maxHistoryEntries = DEFAULT_CONTEXT_WINDOW_SIZE;
        while (estimatedTokens > MAX_TOKEN_ESTIMATE && maxHistoryEntries > 0) {
            maxHistoryEntries--;
            
            // Rebuild prompt with fewer history entries
            StringBuilder truncatedPrompt = new StringBuilder();
            
            // Keep system prompt and context (essential)
            String[] sections = prompt.split("Recent Conversation History:");
            truncatedPrompt.append(sections[0]);
            
            // Add truncated history
            if (maxHistoryEntries > 0) {
                List<ConversationInteraction> limitedHistory = 
                    shortTermMemory.getRecent(maxHistoryEntries);
                
                if (!limitedHistory.isEmpty()) {
                    truncatedPrompt.append("Recent Conversation History:\n");
                    for (ConversationInteraction interaction : limitedHistory) {
                        truncatedPrompt.append("Player: ").append(interaction.getPlayerMessage()).append("\n");
                        truncatedPrompt.append("You: ").append(interaction.getVillagerResponse()).append("\n");
                    }
                    truncatedPrompt.append("\n\n");
                }
            }
            
            // Add response instruction
            truncatedPrompt.append("Current Player Message: [Waiting for input]\n");
            truncatedPrompt.append("Your Response: ");
            
            prompt = truncatedPrompt.toString();
            estimatedTokens = estimateTokenCount(prompt);
        }
        
        return prompt;
    }
    
    /**
     * Estimates token count for a given text
     * @param text the text to estimate
     * @return estimated token count
     */
    private static int estimateTokenCount(String text) {
        // Simple heuristic: roughly 4 characters per token
        return text.length() / CHARS_PER_TOKEN_ESTIMATE;
    }
}