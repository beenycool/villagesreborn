package com.villagesreborn.beeny.ai;

import com.villagesreborn.beeny.VillagerContext;

public class LocalLLMManager {
    private static final String MODEL_PATH = "models/llama3.2_1b.bin";
    private final LlamaModel model;

    public LocalLLMManager() {
        try {
            model = new LlamaModel(MODEL_PATH);
            model.setThreadCount(Runtime.getRuntime().availableProcessors() / 2);
        } catch (ModelLoadException e) {
            throw new RuntimeException("Failed to load AI model: " + MODEL_PATH, e);
        }
    }

    public String generateDialogue(VillagerContext context, boolean hasRecentTaskFailure) {
        // Update dialogue context with memory-based modifiers
        context.updateDialogueContext(
            context.memoryLog().getRecentPlayerInteractions
package com.villagesreborn.beeny.ai;

import com.villagesreborn.beeny.VillagerContext;

public class LocalLLMManager {
    private static final String MODEL_PATH = "models/llama3.2_1b.bin";
    private final LlamaModel model;

    public LocalLLMManager() {
        try {
            model = new LlamaModel(MODEL_PATH);
            model.setThreadCount(Runtime.getRuntime().availableProcessors() / 2);
        } catch (ModelLoadException e) {
            throw new RuntimeException("Failed to load AI model: " + MODEL_PATH, e);
        }
    }

    public String generateDialogue(VillagerContext context, boolean hasRecentTaskFailure) {
        context.updateDialogueContext(
            context.memoryLog().getRecentPlayerInteractions(),
            context.personality().memoryRetentionDuration()
        );
        return model.generate(createPrompt(context, hasRecentTaskFailure));
    }

    public String generatePersonality() {
        return model.generate(PERSONALITY_PROMPT);
    }

    private String createPrompt(VillagerContext context, boolean hasRecentTaskFailure) {
        return String.format(
            "Generate villager dialogue considering:%n" +
            "- Profession: %s%n" +
            "- Current mood: %s%n" +
            "- Recent task status: %s%n" +
            "- Recent memories: %s%n" +
            "Use natural speech patterns and include consequences where appropriate.%s",
            context.profession().getName(),
            context.moodState().name(),
            hasRecentTaskFailure ? "Failed important task" : "No recent issues",
            String.join(", ", context.memoryLog().getRecentMemories()),
            context.personality().moodSusceptibility() > 0.7 ? "%nNote: This character is highly mood-sensitive" : ""
        );
    }
    
    private static final String PERSONALITY_PROMPT = 
        "Generate a villager personality profile with:%n" +
        "- 3 core personality traits%n" +
        "- Mood susceptibility level (0.1-1.0)%n" + 
        "- Memory retention duration (short/medium/long)%n" +
        "- Primary motivation%n" +
        "- Distinct speech patterns";
}
