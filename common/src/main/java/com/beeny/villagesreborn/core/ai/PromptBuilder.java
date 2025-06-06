package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.*;
import com.beeny.villagesreborn.core.conversation.ConversationContext;
import com.beeny.villagesreborn.core.combat.*;
import com.beeny.villagesreborn.core.expansion.VillageResources;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * Enhanced PromptBuilder for LLM-powered villager conversations
 * Assembles system prompts, conversation history, rich memories, and dynamic context layers
 */
public class PromptBuilder {
    
    public static final int CONTEXT_WINDOW_SIZE = 10;
    public static final int MAX_PROMPT_LENGTH = 32000; // Conservative estimate for character limits
    private static final int DEFAULT_CONTEXT_WINDOW_SIZE = 10;
    private static final int MAX_TOKEN_ESTIMATE = 8000; // Conservative estimate for token limits
    private static final int CHARS_PER_TOKEN_ESTIMATE = 4; // Rough estimate
    
    // Enhanced system prompt template with richer context
    private static final String ENHANCED_SYSTEM_PROMPT_TEMPLATE = 
        "You are a villager in a Minecraft village with a rich personal history and complex relationships.\n\n" +
        "Your personality: %s\n" +
        "Your current mood: %s\n" +
        "Your background: %s\n\n" +
        "Guidelines:\n" +
        "- Respond naturally as this character would, drawing on your memories and experiences\n" +
        "- Keep responses conversational (1-3 sentences unless discussing memories)\n" +
        "- Reference your personal history, relationships, and village events when relevant\n" +
        "- Show personality through word choice, tone, and emotional responses\n" +
        "- Remember and reference previous conversations with players\n" +
        "- Express emotions based on your memories and current relationships\n" +
        "- Use minecraft-appropriate language and no modern references\n" +
        "- Share stories and memories when appropriate to build connection\n" +
        "- Show grudges, affections, and complex feelings based on your history\n";
    
    // Legacy system prompt template (for backward compatibility)
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
     * Builds a complete prompt for villager conversation with enhanced memory integration
     * @param villagerBrain the villager's brain containing personality and memories
     * @param context the current conversation context
     * @return assembled prompt string
     */
    public String buildPrompt(VillagerBrain villagerBrain, ConversationContext context) {
        StringBuilder prompt = new StringBuilder();
        
        // Enhanced system prompt with villager identity and background
        prompt.append(buildEnhancedSystemPrompt(villagerBrain));
        prompt.append("\n\n");
        
        // Rich memory context
        String memoryContext = buildMemoryContext(villagerBrain.getMemoryBank(), context);
        if (!memoryContext.trim().isEmpty()) {
            prompt.append(memoryContext);
            prompt.append("\n\n");
        }
        
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
     * Builds enhanced system prompt including personality, mood, and background narrative
     * @param villagerBrain the villager's brain
     * @return enhanced system prompt string
     */
    public String buildEnhancedSystemPrompt(VillagerBrain villagerBrain) {
        String personalityDesc = "Unknown personality";
        String moodDesc = "NEUTRAL";
        String backgroundStory = "";
        if (villagerBrain.getMemoryBank() != null) {
            backgroundStory = generateBackgroundNarrative(villagerBrain.getMemoryBank());
        }
        
        if (villagerBrain.getPersonalityTraits() != null) {
            personalityDesc = villagerBrain.getPersonalityTraits().generateDescription();
        }
        
        if (villagerBrain.getCurrentMood() != null && villagerBrain.getCurrentMood().getOverallMood() != null) {
            moodDesc = villagerBrain.getCurrentMood().getOverallMood().toString();
        }
        
        return String.format(ENHANCED_SYSTEM_PROMPT_TEMPLATE, personalityDesc, moodDesc, backgroundStory);
    }
    
    /**
     * Builds the legacy system prompt for backward compatibility
     */
    public static String buildSystemPrompt(VillagerBrain villagerBrain) {
        String personalityDesc = "Unknown personality";
        String moodDesc = "NEUTRAL";
        
        if (villagerBrain.getPersonalityTraits() != null) {
            personalityDesc = villagerBrain.getPersonalityTraits().generateDescription();
        }
        
        if (villagerBrain.getCurrentMood() != null && villagerBrain.getCurrentMood().getOverallMood() != null) {
            moodDesc = villagerBrain.getCurrentMood().getOverallMood().toString();
        }
        
        return String.format(SYSTEM_PROMPT_TEMPLATE, personalityDesc, moodDesc);
    }
    
    /**
     * Builds a rich memory context section drawing from the enhanced memory bank
     */
    public String buildMemoryContext(MemoryBank memoryBank, ConversationContext context) {
        StringBuilder memoryContext = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        // Recent significant memories (last 7 days)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Memory> recentMemories = memoryBank.getMemoriesFromPeriod(weekAgo, LocalDateTime.now());
        
        if (!recentMemories.isEmpty()) {
            memoryContext.append("Recent Events on Your Mind:\n");
            recentMemories.stream()
                .sorted((m1, m2) -> Float.compare(Math.abs(m2.getEmotionalIntensity()), Math.abs(m1.getEmotionalIntensity())))
                .limit(3)
                .forEach(memory -> memoryContext.append(String.format("- [%s] %s\n", 
                    memory.getTimestamp().format(formatter), memory.getDescription())));
            memoryContext.append("\n");
        }
        
        // Personal stories that define this villager
        List<PersonalStory> stories = memoryBank.getPersonalStories();
        if (!stories.isEmpty()) {
            memoryContext.append("Your Personal History:\n");
            stories.stream()
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(2)
                .forEach(story -> memoryContext.append(String.format("- %s: %s\n", 
                    story.getTitle(), truncateForContext(story.getNarrative(), 100))));
            memoryContext.append("\n");
        }
        
        // Important relationships
        if (!memoryBank.getVillagerRelationships().isEmpty()) {
            memoryContext.append("Important Relationships:\n");
            memoryBank.getVillagerRelationships().values().stream()
                .filter(rel -> Math.abs(rel.getRelationshipStrength()) > 0.3f)
                .sorted((r1, r2) -> Float.compare(Math.abs(r2.getRelationshipStrength()), Math.abs(r1.getRelationshipStrength())))
                .limit(3)
                .forEach(rel -> {
                    String feeling = rel.getRelationshipStrength() > 0 ? "fond of" : "troubled by";
                    memoryContext.append(String.format("- You are %s %s (%s)\n", 
                        feeling, rel.getVillagerName(), rel.getRelationshipType().name().toLowerCase()));
                });
            memoryContext.append("\n");
        }
        
        // Traumatic events that still affect behavior
        List<TraumaticEvent> traumas = memoryBank.getTraumaticEvents();
        if (!traumas.isEmpty()) {
            memoryContext.append("Difficult Experiences:\n");
            traumas.stream()
                .filter(trauma -> trauma.getSeverity() > 0.5f)
                .sorted((t1, t2) -> Float.compare(t2.getSeverity(), t1.getSeverity()))
                .limit(2)
                .forEach(trauma -> memoryContext.append(String.format("- %s (triggers: %s)\n", 
                    truncateForContext(trauma.getDescription(), 80), trauma.getTrigger())));
        }
        
        return memoryContext.toString();
    }
    
    /**
     * Generates a background narrative from the villager's personal stories and memories
     */
    private String generateBackgroundNarrative(MemoryBank memoryBank) {
        StringBuilder background = new StringBuilder();

        if (memoryBank != null) {
            List<PersonalStory> stories = memoryBank.getPersonalStories();
            if (stories != null && !stories.isEmpty()) {
                PersonalStory primaryStory = stories.get(stories.size() - 1); // Most recent
                if (primaryStory != null && primaryStory.getNarrative() != null) {
                    background.append(truncateForContext(primaryStory.getNarrative(), 150));
                } else {
                    background.append("A villager with an unrecorded story.");
                }
            } else {
                background.append("A villager with their own story yet to be fully discovered.");
            }

            Map<UUID, VillagerRelationshipMemory> relationships = memoryBank.getVillagerRelationships();
            if (relationships != null) {
                long positiveRelationships = relationships.values().stream()
                    .filter(Objects::nonNull)
                    .filter(rel -> rel.getRelationshipStrength() > 0.3f)
                    .count();

                if (positiveRelationships > 0) {
                    background.append(" Well-connected in the village community.");
                }
            }
        } else {
            background.append("A villager with no recorded memories.");
        }

        return background.toString();
    }
    
    /**
     * Builds conversation history section with proper formatting
     * @param shortTermMemory the conversation history
     * @return formatted conversation history
     */
    public String buildConversationHistory(ConversationHistory shortTermMemory) {
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
    public String buildCurrentContext(ConversationContext context) {
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
     * Builds a quest generation prompt using the enhanced memory system
     */
    public static String buildQuestPrompt(MemoryBank memoryBank, VillageResources resources, String villagerName) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Generate a personal quest for villager ").append(villagerName).append(":\n\n");
        
        // Village context
        prompt.append("Village Status:\n");
        prompt.append("- Population: ").append(resources.getPopulation()).append("\n");
        prompt.append("- Resources: Wood(").append(resources.getWood())
              .append("), Stone(").append(resources.getStone())
              .append("), Food(").append(resources.getFood()).append(")\n\n");
        
        // Personal context from memories
        List<PersonalStory> stories = memoryBank.getPersonalStories();
        if (!stories.isEmpty()) {
            prompt.append("Personal Background:\n");
            stories.stream().limit(2).forEach(story -> 
                prompt.append("- ").append(story.getTitle()).append(": ").append(story.getNarrative()).append("\n"));
            prompt.append("\n");
        }
        
        // Current relationships
        if (!memoryBank.getVillagerRelationships().isEmpty()) {
            prompt.append("Key Relationships:\n");
            memoryBank.getVillagerRelationships().values().stream()
                .filter(rel -> Math.abs(rel.getRelationshipStrength()) > 0.4f)
                .limit(3)
                .forEach(rel -> prompt.append("- ").append(rel.getVillagerName())
                    .append(" (").append(rel.getRelationshipType()).append(")\n"));
            prompt.append("\n");
        }
        
        prompt.append("Create a quest that:\n");
        prompt.append("1. Reflects the villager's personal story and relationships\n");
        prompt.append("2. Addresses a village need or personal goal\n");
        prompt.append("3. Has clear objectives and meaningful rewards\n");
        prompt.append("4. Fits the Minecraft village setting\n\n");
        
        prompt.append("Format: JSON with title, description, objectives, rewards, questType");
        
        return prompt.toString();
    }
    
    /**
     * Enforces token-length limits by truncating oldest entries if necessary
     * @param prompt the current prompt
     * @param shortTermMemory the conversation history for truncation
     * @return prompt within token limits
     */
    private String enforceTokenLimits(String prompt, ConversationHistory shortTermMemory) {
        if (prompt.length() <= MAX_PROMPT_LENGTH) {
            return prompt;
        }
        
        // If prompt is too long, rebuild with fewer history entries
        int maxHistoryEntries = DEFAULT_CONTEXT_WINDOW_SIZE;
        while (prompt.length() > MAX_PROMPT_LENGTH && maxHistoryEntries > 0) {
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
        }
        
        return prompt;
    }
    
    /**
     * Truncates text for context while preserving meaning
     */
    private String truncateForContext(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    // Legacy prompt building methods (maintained for backward compatibility)
    
    /**
     * Builds a combat decision prompt for AI analysis
     */
    public static String buildCombatPrompt(
        CombatSituation situation,
        CombatPersonalityTraits traits,
        ThreatAssessment threat,
        RelationshipData relationships
    ) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a villager facing a combat situation. Analyze and decide:\n\n");
        
        // Personality
        prompt.append("Personality:\n");
        prompt.append("- Courage: ").append(String.format("%.1f", traits.getCourage())).append("\n");
        prompt.append("- Aggression: ").append(String.format("%.1f", traits.getAggression())).append("\n");
        prompt.append("- Self-Preservation: ").append(String.format("%.1f", traits.getSelfPreservation())).append("\n");
        prompt.append("- Loyalty: ").append(String.format("%.1f", traits.getLoyalty())).append("\n\n");
        
        // Threat assessment
        prompt.append("Current Threat: ").append(threat.getThreatLevel())
                .append(" (Score: ").append(String.format("%.2f", threat.getThreatScore())).append(")\n");
        
        // Enemies
        prompt.append("Enemies: ");
        for (LivingEntity enemy : situation.getEnemies()) {
            prompt.append("Enemy(").append(enemy.getUUID().toString().substring(0, 8)).append(") ");
        }
        prompt.append("\n");
        
        // Allies
        prompt.append("Allies Present: ");
        if (situation.getAllies().size() > 0) {
            prompt.append("Yes (").append(situation.getAllies().size()).append(")");
        } else {
            prompt.append("None");
        }
        prompt.append("\n");
        
        // Decision options
        prompt.append("\nDecide: ATTACK/DEFEND/FLEE/NEGOTIATE\n");
        prompt.append("Target Priority: [enemy IDs in order]\n");
        prompt.append("Tactical Notes: [brief reasoning]\n\n");
        prompt.append("Keep response under 100 tokens.\n");
        prompt.append("Format: ACTION|target1,target2|weaponType|reasoning");
        
        return prompt.toString();
    }
    
    /**
     * Builds an expansion planning prompt for AI analysis
     */
    public static String buildExpansionPrompt(
        VillageResources resources,
        Object terrainData,
        Object biomeData,
        Object populationTrends
    ) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Village Expansion Analysis:\n\n");
        
        prompt.append("Current State:\n");
        prompt.append("- Population: ").append(resources.getPopulation()).append("\n");
        prompt.append("- Wood: ").append(resources.getWood()).append("\n");
        prompt.append("- Stone: ").append(resources.getStone()).append("\n");
        prompt.append("- Food: ").append(resources.getFood()).append("\n\n");
        
        prompt.append("Recommend:\n");
        prompt.append("1. Building Types: [house/workshop/farm/defense] with quantities\n");
        prompt.append("2. Placement Strategy: [compact/distributed/linear/defensive]\n");
        prompt.append("3. Resource Priority: [which resources to focus on]\n");
        prompt.append("4. Timeline: [expansion phases]\n\n");
        
        prompt.append("Response format: JSON with buildingPlan, placementStrategy, resourceFocus");
        
        return prompt.toString();
    }
    
    /**
     * Builds a governance prompt for AI candidate generation
     */
    public static String buildGovernancePrompt(
        Object electionContext,
        Object needs,
        Object candidateRequirements
    ) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Generate a mayoral candidate for a Minecraft village:\n\n");
        
        prompt.append("Village Context:\n");
        prompt.append("- Population: [dynamic]\n");
        prompt.append("- Primary Issues: [economic/defense/expansion]\n");
        prompt.append("- Economic Status: [stable/growing/declining]\n\n");
        
        prompt.append("Create a candidate with:\n");
        prompt.append("1. Name and Background\n");
        prompt.append("2. Key Personality Traits (3-4 traits)\n");
        prompt.append("3. Campaign Platform (3 main policy points)\n");
        prompt.append("4. Speaking Style (formal/casual/passionate/practical)\n");
        prompt.append("5. Stance on Major Issues\n\n");
        
        prompt.append("Format as JSON with name, background, traits, platform, speakingStyle, stances");
        
        return prompt.toString();
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