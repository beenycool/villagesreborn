package com.beeny.ai.core;

import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-dimensional emotion system that extends beyond simple happiness
 * Emotions influence behavior, dialogue, and decision-making
 */
public class VillagerEmotionSystem {

    // Prevents stack overflow from recursive emotional contagion
    private static final ThreadLocal<Integer> contagionDepth = ThreadLocal.withInitial(() -> 0);
    
    public enum EmotionType {
        HAPPINESS(0.0f, 100.0f, 50.0f, 0.95f),
        FEAR(-50.0f, 50.0f, 0.0f, 0.90f),
        ANGER(-50.0f, 50.0f, 0.0f, 0.85f),
        LOVE(0.0f, 100.0f, 0.0f, 0.98f),
        CURIOSITY(0.0f, 100.0f, 30.0f, 0.92f),
        BOREDOM(-20.0f, 80.0f, 0.0f, 0.88f),
        EXCITEMENT(0.0f, 100.0f, 0.0f, 0.80f),
        STRESS(-100.0f, 0.0f, -10.0f, 0.85f),
        LONELINESS(-50.0f, 20.0f, 0.0f, 0.93f),
        CONTENTMENT(0.0f, 80.0f, 40.0f, 0.96f);
        
        public final float minValue;
        public final float maxValue;
        public final float defaultValue;
        public final float decayRate; // How quickly the emotion returns to baseline
        
        EmotionType(float minValue, float maxValue, float defaultValue, float decayRate) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.defaultValue = defaultValue;
            this.decayRate = decayRate;
        }
    }
    
    public static class EmotionalState {
        private final Map<EmotionType, Float> emotions = new ConcurrentHashMap<>();
        private final Map<EmotionType, Long> lastUpdate = new ConcurrentHashMap<>();
        private float emotionalStability = 0.5f; // 0 = very volatile, 1 = very stable
        
        public EmotionalState() {
            // Initialize with default values
            for (EmotionType type : EmotionType.values()) {
                emotions.put(type, type.defaultValue);
                lastUpdate.put(type, System.currentTimeMillis());
            }
        }
        
        public float getEmotion(EmotionType type) {
            updateEmotionDecay(type);
            return emotions.getOrDefault(type, type.defaultValue);
        }
        
        public void adjustEmotion(EmotionType type, float change, String reason) {
            float currentValue = getEmotion(type);
            float stabilityFactor = 0.5f + (emotionalStability * 0.5f);
            float adjustedChange = change * stabilityFactor;
            
            float newValue = Math.max(type.minValue, 
                            Math.min(type.maxValue, currentValue + adjustedChange));
            
            emotions.put(type, newValue);
            lastUpdate.put(type, System.currentTimeMillis());
            
            // Emotional contagion - some emotions affect others
            applyEmotionalContagion(type, adjustedChange);
        }
        
        private void updateEmotionDecay(EmotionType type) {
            long lastUpdateTime = lastUpdate.getOrDefault(type, System.currentTimeMillis());
            long timeDiff = System.currentTimeMillis() - lastUpdateTime;
            
            if (timeDiff > 60000) { // Decay every minute
                float currentValue = emotions.getOrDefault(type, type.defaultValue);
                float targetValue = type.defaultValue;
                float decayAmount = (currentValue - targetValue) * (1.0f - type.decayRate);
                
                emotions.put(type, currentValue - decayAmount);
                lastUpdate.put(type, System.currentTimeMillis());
            }
        }
        
        private void applyEmotionalContagion(EmotionType changedEmotion, float change) {
            int depth = contagionDepth.get() + 1;
            contagionDepth.set(depth);
            if (depth > 2) {
                contagionDepth.set(depth - 1);
                return;
            }
            try {
                switch (changedEmotion) {
                    case HAPPINESS -> {
                        adjustEmotion(EmotionType.CONTENTMENT, change * 0.3f, "happiness_contagion");
                        adjustEmotion(EmotionType.EXCITEMENT, change * 0.2f, "happiness_contagion");
                        adjustEmotion(EmotionType.STRESS, -change * 0.4f, "happiness_reduces_stress");
                    }
                    case FEAR -> {
                        adjustEmotion(EmotionType.STRESS, change * 0.5f, "fear_stress_contagion");
                        adjustEmotion(EmotionType.ANGER, change * 0.3f, "fear_anger_contagion");
                        adjustEmotion(EmotionType.HAPPINESS, -change * 0.6f, "fear_reduces_happiness");
                    }
                    case ANGER -> {
                        adjustEmotion(EmotionType.STRESS, change * 0.4f, "anger_stress_contagion");
                        adjustEmotion(EmotionType.HAPPINESS, -change * 0.5f, "anger_reduces_happiness");
                        adjustEmotion(EmotionType.LOVE, -change * 0.3f, "anger_reduces_love");
                    }
                    case LOVE -> {
                        adjustEmotion(EmotionType.HAPPINESS, change * 0.6f, "love_happiness_contagion");
                        adjustEmotion(EmotionType.CONTENTMENT, change * 0.4f, "love_contentment_contagion");
                        adjustEmotion(EmotionType.LONELINESS, -change * 0.8f, "love_reduces_loneliness");
                    }
                    case LONELINESS -> {
                        adjustEmotion(EmotionType.BOREDOM, change * 0.5f, "loneliness_boredom_contagion");
                        adjustEmotion(EmotionType.STRESS, change * 0.3f, "loneliness_stress_contagion");
                        adjustEmotion(EmotionType.HAPPINESS, -change * 0.4f, "loneliness_reduces_happiness");
                    }
                }
            } finally {
                contagionDepth.set(contagionDepth.get() - 1);
            }
        }
        
        public Map<EmotionType, Float> getAllEmotions() {
            Map<EmotionType, Float> result = new HashMap<>();
            for (EmotionType type : EmotionType.values()) {
                result.put(type, getEmotion(type));
            }
            return result;
        }
        
        public EmotionType getDominantEmotion() {
            EmotionType dominant = EmotionType.CONTENTMENT;
            float highestIntensity = 0;
            
            for (EmotionType type : EmotionType.values()) {
                float intensity = Math.abs(getEmotion(type) - type.defaultValue);
                if (intensity > highestIntensity) {
                    highestIntensity = intensity;
                    dominant = type;
                }
            }
            return dominant;
        }
        
        public String getEmotionalDescription() {
            EmotionType dominant = getDominantEmotion();
            float intensity = Math.abs(getEmotion(dominant) - dominant.defaultValue);
            
            if (intensity < 10) return "calm";
            if (intensity < 25) return "slightly " + dominant.name().toLowerCase();
            if (intensity < 50) return dominant.name().toLowerCase();
            return "very " + dominant.name().toLowerCase();
        }
        
        public float getEmotionalStability() {
            return emotionalStability;
        }
        
        public void setEmotionalStability(float stability) {
            this.emotionalStability = Math.max(0.0f, Math.min(1.0f, stability));
        }
    }
    
    // Global emotion states for all villagers
    private static final Map<String, EmotionalState> villagerEmotions = new ConcurrentHashMap<>();
    
    public static EmotionalState getEmotionalState(VillagerEntity villager) {
        String uuid = villager.getUuidAsString();
        return villagerEmotions.computeIfAbsent(uuid, k -> new EmotionalState());
    }
    
    public static void processEmotionalEvent(VillagerEntity villager, EmotionalEvent event) {
        EmotionalState state = getEmotionalState(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        if (data == null) return;
        
        // Apply personality modifiers to emotional reactions
        float personalityMultiplier = getPersonalityEmotionalMultiplier(data.getPersonality(), event.emotionType);
        float adjustedImpact = event.impact * personalityMultiplier;
        
        state.adjustEmotion(event.emotionType, adjustedImpact, event.reason);
        
        // Update legacy happiness based on emotional state
        updateLegacyHappiness(data, state);
        
        // Apply emotional contagion to nearby villagers
        if (event.contagious) {
            applyEmotionalContagionToNearby(villager, event.emotionType, adjustedImpact * 0.3f);
        }
    }
    
    private static float getPersonalityEmotionalMultiplier(String personality, EmotionType emotionType) {
        return switch (personality) {
            case "Energetic" -> switch (emotionType) {
                case EXCITEMENT, HAPPINESS -> 1.5f;
                case BOREDOM -> 0.5f;
                default -> 1.0f;
            };
            case "Shy" -> switch (emotionType) {
                case FEAR, STRESS -> 1.4f;
                case EXCITEMENT -> 0.6f;
                default -> 1.0f;
            };
            case "Grumpy" -> switch (emotionType) {
                case ANGER -> 1.6f;
                case HAPPINESS -> 0.7f;
                default -> 1.0f;
            };
            case "Cheerful" -> switch (emotionType) {
                case HAPPINESS, EXCITEMENT -> 1.4f;
                case ANGER, STRESS -> 0.6f;
                default -> 1.0f;
            };
            case "Nervous" -> switch (emotionType) {
                case FEAR, STRESS, BOREDOM -> 1.3f;
                case CONTENTMENT -> 0.8f;
                default -> 1.0f;
            };
            case "Confident" -> switch (emotionType) {
                case FEAR, STRESS -> 0.5f;
                case EXCITEMENT -> 1.3f;
                default -> 1.0f;
            };
            default -> 1.0f;
        };
    }
    
    private static void updateLegacyHappiness(VillagerData data, EmotionalState state) {
        // Convert multi-dimensional emotions back to single happiness value for compatibility
        float happiness = state.getEmotion(EmotionType.HAPPINESS);
        float contentment = state.getEmotion(EmotionType.CONTENTMENT);
        float stress = state.getEmotion(EmotionType.STRESS);
        float love = state.getEmotion(EmotionType.LOVE);
        
        float legacyHappiness = (happiness + contentment + love + (-stress * 0.5f)) / 3.0f;
        legacyHappiness = Math.max(0, Math.min(100, legacyHappiness));
        
        data.setHappiness((int) legacyHappiness);
    }
    
    private static void applyEmotionalContagionToNearby(VillagerEntity source, EmotionType emotionType, float impact) {
        List<VillagerEntity> nearbyVillagers = source.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            source.getBoundingBox().expand(10.0),
            v -> v != source && v.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA) != null
        );
        
        for (VillagerEntity nearby : nearbyVillagers) {
            VillagerData nearbyData = nearby.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerData sourceData = source.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            // Relationship affects emotional contagion strength
            float relationshipFactor = 1.0f;
            if (nearbyData != null && sourceData != null) {
                int reputation = nearbyData.getPlayerReputation(source.getUuidAsString());
                relationshipFactor = 0.5f + (reputation / 100.0f); // 0.5 to 1.5 range
            }
            
            EmotionalState nearbyState = getEmotionalState(nearby);
            nearbyState.adjustEmotion(emotionType, impact * relationshipFactor, "emotional_contagion");
        }
    }
    
    public static class EmotionalEvent {
        public final EmotionType emotionType;
        public final float impact;
        public final String reason;
        public final boolean contagious;
        
        public EmotionalEvent(EmotionType emotionType, float impact, String reason, boolean contagious) {
            this.emotionType = emotionType;
            this.impact = impact;
            this.reason = reason;
            this.contagious = contagious;
        }
        
        // Common emotional events
        public static final EmotionalEvent SUCCESSFUL_TRADE = new EmotionalEvent(EmotionType.HAPPINESS, 15.0f, "successful_trade", false);
        public static final EmotionalEvent PLAYER_INTERACTION = new EmotionalEvent(EmotionType.HAPPINESS, 8.0f, "player_interaction", false);
        public static final EmotionalEvent VILLAGER_DEATH = new EmotionalEvent(EmotionType.FEAR, 30.0f, "villager_death", true);
        public static final EmotionalEvent MARRIAGE = new EmotionalEvent(EmotionType.LOVE, 50.0f, "marriage", true);
        public static final EmotionalEvent BIRTH = new EmotionalEvent(EmotionType.HAPPINESS, 40.0f, "birth", true);
        public static final EmotionalEvent RAID = new EmotionalEvent(EmotionType.FEAR, 60.0f, "raid", true);
        public static final EmotionalEvent LONELY = new EmotionalEvent(EmotionType.LONELINESS, 20.0f, "isolation", false);
        public static final EmotionalEvent REPETITIVE_TASK = new EmotionalEvent(EmotionType.BOREDOM, 10.0f, "repetitive_task", false);
        public static final EmotionalEvent DISCOVERY = new EmotionalEvent(EmotionType.CURIOSITY, 25.0f, "discovery", false);
    }
    
    // NBT serialization for persistence
    public static void saveEmotionsToNbt(VillagerEntity villager, NbtCompound nbt) {
        EmotionalState state = villagerEmotions.get(villager.getUuidAsString());
        if (state == null) return;
        
        NbtCompound emotionsNbt = new NbtCompound();
        for (EmotionType type : EmotionType.values()) {
            emotionsNbt.putFloat(type.name(), state.getEmotion(type));
        }
        emotionsNbt.putFloat("emotional_stability", state.getEmotionalStability());
        
        nbt.put("emotions", emotionsNbt);
    }
    
    public static void loadEmotionsFromNbt(VillagerEntity villager, NbtCompound nbt) {
        if (!nbt.contains("emotions")) return;
        
        NbtCompound emotionsNbt = nbt.getCompound("emotions").orElse(null);
        EmotionalState state = new EmotionalState();
        
        for (EmotionType type : EmotionType.values()) {
            if (emotionsNbt.contains(type.name())) {
                float value = emotionsNbt.getFloat(type.name()).orElse(0f);
                state.emotions.put(type, value);
            }
        }
        
        if (emotionsNbt.contains("emotional_stability")) {
            state.setEmotionalStability(emotionsNbt.getFloat("emotional_stability").orElse(0f));
        }
        
        villagerEmotions.put(villager.getUuidAsString(), state);
    }
    
    public static void clearEmotions(String villagerUuid) {
        villagerEmotions.remove(villagerUuid);
    }
    
    // Get emotional context for dialogue generation
    public static String getEmotionalContext(VillagerEntity villager) {
        EmotionalState state = getEmotionalState(villager);
        StringBuilder context = new StringBuilder();
        
        context.append("Emotional state: ").append(state.getEmotionalDescription()).append(". ");
        
        // Add specific high emotions
        for (EmotionType type : EmotionType.values()) {
            float value = state.getEmotion(type);
            float intensity = Math.abs(value - type.defaultValue);
            if (intensity > 30) {
                context.append("Feeling ").append(type.name().toLowerCase()).append(" (").append((int)value).append("). ");
            }
        }
        
        return context.toString();
    }
}