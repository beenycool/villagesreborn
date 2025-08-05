package com.beeny.ai.core;

import net.minecraft.entity.passive.VillagerEntity;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class VillagerEmotionSystem {
    
    public enum EmotionType {
        HAPPINESS,
        SADNESS,
        ANGER,
        FEAR,
        LOVE,
        LONELINESS,
        STRESS,
        BOREDOM,
        CONTENTMENT,
        EXCITEMENT,
        ANXIETY,
        CONFIDENCE,
        CURIOSITY
    }
    
    public static class EmotionalState {
        private final Map<EmotionType, Float> emotions = new HashMap<>();
        
        public EmotionalState() {
            // Initialize all emotions to neutral (50.0f)
            for (EmotionType type : EmotionType.values()) {
                emotions.put(type, 50.0f);
            }
        }
        
        public float getEmotion(@NotNull EmotionType type) {
            return emotions.getOrDefault(type, 50.0f);
        }
        
        public void setEmotion(@NotNull EmotionType type, float value) {
            emotions.put(type, Math.max(0.0f, Math.min(100.0f, value)));
        }
        
        public void adjustEmotion(@NotNull EmotionType type, float delta) {
            float current = getEmotion(type);
            setEmotion(type, current + delta);
        }
        
        public EmotionType getDominantEmotion() {
            EmotionType dominant = null;
            float maxValue = 0.0f;
            
            for (Map.Entry<EmotionType, Float> entry : emotions.entrySet()) {
                if (entry.getValue() > maxValue) {
                    maxValue = entry.getValue();
                    dominant = entry.getKey();
                }
            }
            
            return dominant != null ? dominant : EmotionType.CONTENTMENT;
        }
        
        public String getEmotionalDescription() {
            EmotionType dominant = getDominantEmotion();
            float intensity = getEmotion(dominant);
            
            String intensityDesc;
            if (intensity > 80) {
                intensityDesc = "very ";
            } else if (intensity > 60) {
                intensityDesc = "quite ";
            } else if (intensity > 40) {
                intensityDesc = "somewhat ";
            } else {
                intensityDesc = "mildly ";
            }
            
            return intensityDesc + dominant.name().toLowerCase().replace("_", " ");
        }
    }
    
    public static class EmotionalEvent {
        public final EmotionType primaryEmotion;
        public final float intensity;
        public final String description;
        public final boolean isPositive;
        
        public EmotionalEvent(EmotionType primaryEmotion, float intensity, String description) {
            this(primaryEmotion, intensity, description, isPositiveEmotion(primaryEmotion));
        }
        
        public EmotionalEvent(EmotionType primaryEmotion, float intensity, String description, boolean isPositive) {
            this.primaryEmotion = primaryEmotion;
            this.intensity = intensity;
            this.description = description;
            this.isPositive = isPositive;
        }
        
        private static boolean isPositiveEmotion(EmotionType emotion) {
            return switch (emotion) {
                case HAPPINESS, LOVE, CONTENTMENT, EXCITEMENT, CONFIDENCE, CURIOSITY -> true;
                case SADNESS, ANGER, FEAR, LONELINESS, STRESS, BOREDOM, ANXIETY -> false;
            };
        }
        
        // Predefined common events
        public static final EmotionalEvent SUCCESSFUL_TRADE = new EmotionalEvent(EmotionType.HAPPINESS, 10.0f, "successful trade", true);
        public static final EmotionalEvent FAILED_TRADE = new EmotionalEvent(EmotionType.SADNESS, 5.0f, "failed trade", false);
        public static final EmotionalEvent PLAYER_INTERACTION = new EmotionalEvent(EmotionType.HAPPINESS, 8.0f, "player interaction", true);
        public static final EmotionalEvent VILLAGER_DEATH = new EmotionalEvent(EmotionType.SADNESS, 20.0f, "villager death", false);
        public static final EmotionalEvent REPETITIVE_TASK = new EmotionalEvent(EmotionType.BOREDOM, 5.0f, "repetitive task", false);
    }
    
    private static final Map<String, EmotionalState> villagerEmotions = new HashMap<>();
    
    public static EmotionalState getEmotionalState(@NotNull VillagerEntity villager) {
        return villagerEmotions.computeIfAbsent(villager.getUuidAsString(), k -> new EmotionalState());
    }
    
    public static void processEmotionalEvent(@NotNull VillagerEntity villager, @NotNull EmotionalEvent event) {
        EmotionalState state = getEmotionalState(villager);
        state.adjustEmotion(event.primaryEmotion, event.intensity);
        
        // Secondary emotion effects
        switch (event.primaryEmotion) {
            case HAPPINESS -> {
                state.adjustEmotion(EmotionType.SADNESS, -event.intensity * 0.5f);
                state.adjustEmotion(EmotionType.CONTENTMENT, event.intensity * 0.3f);
            }
            case SADNESS -> {
                state.adjustEmotion(EmotionType.HAPPINESS, -event.intensity * 0.5f);
                state.adjustEmotion(EmotionType.LONELINESS, event.intensity * 0.4f);
            }
            case LOVE -> {
                state.adjustEmotion(EmotionType.LONELINESS, -event.intensity * 0.7f);
                state.adjustEmotion(EmotionType.HAPPINESS, event.intensity * 0.5f);
            }
            case STRESS -> {
                state.adjustEmotion(EmotionType.ANXIETY, event.intensity * 0.6f);
                state.adjustEmotion(EmotionType.CONTENTMENT, -event.intensity * 0.4f);
            }
        }
    }
    
    public static void updateEmotionalDecay(@NotNull VillagerEntity villager) {
        EmotionalState state = getEmotionalState(villager);
        
        // Gradual decay towards neutral (50.0f)
        for (EmotionType type : EmotionType.values()) {
            float current = state.getEmotion(type);
            float target = 50.0f;
            float decay = (target - current) * 0.01f; // 1% decay per update
            state.adjustEmotion(type, decay);
        }
    }
    
    public static void clearEmotionalState(@NotNull String villagerUuid) {
        villagerEmotions.remove(villagerUuid);
    }
}