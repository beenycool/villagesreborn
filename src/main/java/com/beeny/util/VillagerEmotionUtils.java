package com.beeny.util;

import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.AIWorldManagerRefactored;
import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for villager emotion processing operations.
 * Centralizes emotion event processing to eliminate code duplication.
 */
public class VillagerEmotionUtils {
    
    /**
     * Process a simple emotional event for a villager.
     * 
     * @param villager The villager entity
     * @param emotionType The type of emotion to process
     * @param intensity The intensity of the emotion (-100 to 100)
     * @param description Description of what caused the emotion
     */
    public static void processEmotionalEvent(@NotNull VillagerEntity villager, 
                                           @NotNull VillagerEmotionSystem.EmotionType emotionType,
                                           float intensity, 
                                           @NotNull String description) {
        AIWorldManagerRefactored.getInstance().getEmotionSystem()
            .processEmotionalEvent(villager, 
                new VillagerEmotionSystem.EmotionalEvent(emotionType, intensity, description, false));
    }
    
    /**
     * Process a contagious emotional event for a villager.
     * 
     * @param villager The villager entity
     * @param emotionType The type of emotion to process
     * @param intensity The intensity of the emotion (-100 to 100)
     * @param description Description of what caused the emotion
     */
    public static void processContagiousEmotionalEvent(@NotNull VillagerEntity villager,
                                                     @NotNull VillagerEmotionSystem.EmotionType emotionType,
                                                     float intensity,
                                                     @NotNull String description) {
        AIWorldManagerRefactored.getInstance().getEmotionSystem()
            .processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(emotionType, intensity, description, true));
    }
    
    /**
     * Process a predefined emotional event for a villager.
     * 
     * @param villager The villager entity
     * @param event The predefined emotional event
     */
    public static void processEmotionalEvent(@NotNull VillagerEntity villager,
                                           @NotNull VillagerEmotionSystem.EmotionalEvent event) {
        AIWorldManagerRefactored.getInstance().getEmotionSystem()
            .processEmotionalEvent(villager, event);
    }
    
    /**
     * Process multiple emotional effects from socializing.
     * 
     * @param villager The villager entity
     */
    public static void processSocializingEffects(@NotNull VillagerEntity villager) {
        processEmotionalEvent(villager, VillagerEmotionSystem.EmotionType.HAPPINESS, 10.0f, "socializing");
        processEmotionalEvent(villager, VillagerEmotionSystem.EmotionType.LONELINESS, -15.0f, "socializing");
    }
    
    /**
     * Process emotional effects from working.
     * 
     * @param villager The villager entity
     * @param world The world instance for random events
     */
    public static void processWorkingEffects(@NotNull VillagerEntity villager) {
        processEmotionalEvent(villager, VillagerEmotionSystem.EmotionType.CONTENTMENT, 8.0f, "working");
        
        // Small chance of boredom from repetitive work
        if (villager.getWorld().getRandom().nextFloat() < 0.2f) {
            processEmotionalEvent(villager, VillagerEmotionSystem.EmotionalEvent.REPETITIVE_TASK);
        }
    }
    
    /**
     * Process emotional effects from learning gossip.
     * 
     * @param villager The villager entity
     */
    public static void processGossipLearningEffects(@NotNull VillagerEntity villager) {
        processEmotionalEvent(villager, VillagerEmotionSystem.EmotionType.CURIOSITY, 10.0f, "learning_gossip");
    }
}