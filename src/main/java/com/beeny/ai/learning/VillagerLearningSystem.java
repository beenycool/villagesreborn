package com.beeny.ai.learning;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Learning and adaptation system that allows villagers to learn from experiences
 * and adapt their behavior patterns over time
 */
public class VillagerLearningSystem {
    
    // Experience types that villagers can learn from
    public enum ExperienceType {
        PLAYER_INTERACTION,
        TRADING,
        SOCIALIZING,
        WORKING,
        EMOTIONAL_EVENT,
        RELATIONSHIP_OUTCOME,
        ENVIRONMENTAL_CHANGE,
        DECISION_OUTCOME
    }
    
    // Learning patterns and behavioral adaptations
    public static class Experience {
        public final ExperienceType type;
        public final String context;
        public final Map<String, Object> parameters;
        // Thread-safe mutable outcome
        private final java.util.concurrent.atomic.AtomicReference<Float> outcome; // -1.0 (very negative) to 1.0 (very positive)
        public final long timestamp;
        private final java.util.concurrent.atomic.AtomicInteger reinforcements; // Thread-safe reinforcement count
        
        public Experience(ExperienceType type, String context, Map<String, Object> parameters, float outcome) {
            this.type = type;
            this.context = context;
            this.parameters = new HashMap<>(parameters);
            this.outcome = new java.util.concurrent.atomic.AtomicReference<>(outcome);
            this.timestamp = System.currentTimeMillis();
            this.reinforcements = new java.util.concurrent.atomic.AtomicInteger(1);
        }
        
        public void reinforce(float newOutcome) {
            // Update outcome using weighted average (recent experiences matter more)
            int currentReinforcements = reinforcements.get();
            float weight = 1.0f / (currentReinforcements + 1);
            float currentOutcome = outcome.get();
            float updatedOutcome = (currentOutcome * (1 - weight)) + (newOutcome * weight);
            outcome.set(updatedOutcome);
            reinforcements.incrementAndGet();
        }
        
        public boolean isRecent() {
            return (System.currentTimeMillis() - timestamp) < 3600000; // 1 hour
        }
        
        public float getRelevanceScore() {
            // Newer experiences and those with more reinforcements are more relevant
            float timeRelevance = Math.max(0.1f, 1.0f - ((System.currentTimeMillis() - timestamp) / 86400000.0f)); // 1 day decay
            float reinforcementRelevance = Math.min(1.0f, reinforcements.get() / 10.0f);
            return timeRelevance * reinforcementRelevance;
        }

        // Thread-safe getter for outcome
        public float getOutcome() {
            return outcome.get();
        }
    }
    
    // Learned patterns that influence future behavior
    public static class LearnedPattern {
        public final String patternId;
        public final String description;
        public final Map<String, Float> conditions; // Condition name -> threshold
        public final Map<String, Float> outcomes; // Expected outcome probabilities
        public float confidence; // 0.0 to 1.0
        public int timesObserved;
        
        public LearnedPattern(String patternId, String description) {
            this.patternId = patternId;
            this.description = description;
            this.conditions = new ConcurrentHashMap<>();
            this.outcomes = new ConcurrentHashMap<>();
            this.confidence = 0.1f;
            this.timesObserved = 0;
        }
        
        public void updatePattern(Map<String, Float> observedConditions, float outcome) {
            timesObserved++;
            
            // Update confidence based on consistency
            confidence = Math.min(1.0f, confidence + (1.0f / timesObserved));
            
            // Update condition thresholds
            for (Map.Entry<String, Float> entry : observedConditions.entrySet()) {
                String condition = entry.getKey();
                float value = entry.getValue();
                
                if (conditions.containsKey(condition)) {
                    // Weighted average with existing threshold
                    float weight = 1.0f / timesObserved;
                    conditions.put(condition, conditions.get(condition) * (1 - weight) + value * weight);
                } else {
                    conditions.put(condition, value);
                }
            }
            
            // Update outcome expectations
            String outcomeKey = outcome > 0 ? "positive" : "negative";
            outcomes.put(outcomeKey, outcomes.getOrDefault(outcomeKey, 0.0f) + 1.0f);
        }
        
        public boolean matches(Map<String, Float> currentConditions) {
            for (Map.Entry<String, Float> entry : conditions.entrySet()) {
                String condition = entry.getKey();
                float threshold = entry.getValue();
                
                if (!currentConditions.containsKey(condition) ||
                    Math.abs(currentConditions.get(condition) - threshold) > 0.3f) {
                    return false;
                }
            }
            return true;
        }
        
        public float getPredictedOutcome() {
            float positive = outcomes.getOrDefault("positive", 0.0f);
            float negative = outcomes.getOrDefault("negative", 0.0f);
            
            if (positive + negative == 0) return 0.0f;
            return (positive - negative) / (positive + negative);
        }
    }
    
    // Individual villager's learning profile
    public static class VillagerLearningProfile {
        private final List<Experience> experiences;
        private final Map<String, LearnedPattern> patterns;
        private final Map<String, Float> preferences; // Learned preferences
        private final Map<String, Integer> behaviorCounts; // Track behavior frequency
        private float learningRate; // How quickly they adapt (0.0 to 1.0)
        private long lastLearningUpdate;
        
        public VillagerLearningProfile() {
            this.experiences = Collections.synchronizedList(new ArrayList<>());
            this.patterns = new ConcurrentHashMap<>();
            this.preferences = new ConcurrentHashMap<>();
            this.behaviorCounts = new ConcurrentHashMap<>();
            this.learningRate = 0.5f; // Default learning rate
            this.lastLearningUpdate = System.currentTimeMillis();
        }
        
        public void addExperience(Experience experience) {
            experiences.add(experience);
            
            // Limit experience history to prevent memory issues
            if (experiences.size() > 200) {
                experiences.removeIf(exp -> !exp.isRecent() && exp.reinforcements.get() < 3);
            }
            
            // Trigger pattern learning
            updatePatterns(experience);
            updatePreferences(experience);
        }
        
        private void updatePatterns(Experience experience) {
            String patternId = experience.type + "_" + experience.context;
            LearnedPattern pattern = patterns.computeIfAbsent(patternId, 
                k -> new LearnedPattern(k, "Pattern for " + experience.type + " in " + experience.context));
            
            // Convert experience parameters to conditions
            Map<String, Float> conditions = new HashMap<>();
            for (Map.Entry<String, Object> entry : experience.parameters.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    conditions.put(entry.getKey(), ((Number) entry.getValue()).floatValue());
                }
            }
            
            pattern.updatePattern(conditions, experience.getOutcome());
        }
        
        private void updatePreferences(Experience experience) {
            // Learn preferences based on positive/negative outcomes
            String preferenceKey = experience.type + "_preference";
            float currentPreference = preferences.getOrDefault(preferenceKey, 0.0f);
            
            // Update preference using learning rate
            float adjustment = experience.getOutcome() * learningRate * 0.1f;
            float newPreference = Math.max(-1.0f, Math.min(1.0f, currentPreference + adjustment));
            preferences.put(preferenceKey, newPreference);
        }
        
        public float predictOutcome(ExperienceType type, String context, Map<String, Float> conditions) {
            String patternId = type + "_" + context;
            LearnedPattern pattern = patterns.get(patternId);
            
            if (pattern != null && pattern.matches(conditions) && pattern.confidence > 0.3f) {
                return pattern.getPredictedOutcome() * pattern.confidence;
            }
            
            return 0.0f; // No prediction available
        }
        
        public List<Experience> getRelevantExperiences(ExperienceType type, String context) {
            return experiences.stream()
                .filter(exp -> exp.type == type && exp.context.equals(context))
                .sorted((a, b) -> Float.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(10)
                .toList();
        }
        
        public void adjustLearningRate(VillagerEntity villager) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return;
            
            // Personality affects learning rate
            String personality = data.getPersonality();
            if (personality == null) personality = "Unknown";
            float personalityModifier = switch (personality) {
                case "Curious" -> 1.5f; // Learns faster
                case "Serious" -> 1.2f; // Methodical learning
                case "Lazy" -> 0.7f; // Learns slower
                case "Nervous" -> 0.8f; // Cautious learning
                default -> 1.0f;
            };
            
            // Age affects learning rate (young learn faster, old learn slower)
            float ageModifier = Math.max(0.3f, 1.5f - (data.getAge() / 400.0f));
            
            learningRate = Math.max(0.1f, Math.min(1.0f, 0.5f * personalityModifier * ageModifier));
        }
        
        public void incrementBehaviorCount(String behavior) {
            behaviorCounts.put(behavior, behaviorCounts.getOrDefault(behavior, 0) + 1);
        }
        
        public int getBehaviorCount(String behavior) {
            return behaviorCounts.getOrDefault(behavior, 0);
        }
        
        public float getPreference(String preferenceType) {
            return preferences.getOrDefault(preferenceType, 0.0f);
        }
        
        public Set<String> getLearnedPatternIds() {
            return new HashSet<>(patterns.keySet());
        }
        
        public LearnedPattern getPattern(String patternId) {
            return patterns.get(patternId);
        }
    }
    
    // Global learning profiles for all villagers
    private static final Map<String, VillagerLearningProfile> learningProfiles = new ConcurrentHashMap<>();
    
    public static VillagerLearningProfile getLearningProfile(VillagerEntity villager) {
        String uuid = villager.getUuidAsString();
        VillagerLearningProfile profile = learningProfiles.computeIfAbsent(uuid, k -> new VillagerLearningProfile());
        profile.adjustLearningRate(villager);
        return profile;
    }
    
    // Learning event processors
    public static void processPlayerInteraction(VillagerEntity villager, PlayerEntity player, String interactionType, float outcome) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("player_reputation", data != null ? data.getPlayerReputation(player.getUuidAsString()) : 0);
        parameters.put("villager_happiness", data != null ? data.getHappiness() : 50);
        parameters.put("time_of_day", villager.getWorld().getTimeOfDay() % 24000);
        
        Experience experience = new Experience(ExperienceType.PLAYER_INTERACTION, interactionType, parameters, outcome);
        profile.addExperience(experience);
        
        // Update emotional response based on learned patterns
        if (outcome > 0.5f) {
            VillagerEmotionSystem.processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, outcome * 15.0f, "positive_interaction", false));
        } else if (outcome < -0.5f) {
            VillagerEmotionSystem.processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.ANGER, Math.abs(outcome) * 10.0f, "negative_interaction", false));
        }
    }
    
    public static void processTradeOutcome(VillagerEntity villager, PlayerEntity player, boolean fair, int emeraldValue) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("trade_value", emeraldValue);
        parameters.put("player_reputation", data != null ? data.getPlayerReputation(player.getUuidAsString()) : 0);
        parameters.put("fair_trade", fair ? 1.0f : 0.0f);
        
        float outcome = fair ? 0.7f : -0.8f;
        Experience experience = new Experience(ExperienceType.TRADING, "emerald_trade", parameters, outcome);
        profile.addExperience(experience);
        
        // Learn player trading patterns
        String playerPattern = "player_" + player.getUuidAsString() + "_trading";
        profile.incrementBehaviorCount(playerPattern);
        
        // Adjust future trade expectations
        if (!fair) {
            profile.preferences.compute(
                "player_" + player.getUuidAsString() + "_trade_caution",
                (k, v) -> v == null ? 0.2f : v + 0.2f
            );
        }
    }
    
    public static void processSocialOutcome(VillagerEntity villager, VillagerEntity other, String interactionType, float outcome) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        VillagerData villagerData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        VillagerData otherData = other.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("other_personality",
            otherData != null && otherData.getPersonality() != null
                ? otherData.getPersonality()
                : "");
        parameters.put("relationship_level", villagerData != null ? villagerData.getPlayerReputation(other.getUuidAsString()) : 0);
        parameters.put("villager_happiness", villagerData != null ? villagerData.getHappiness() : 50);
        
        Experience experience = new Experience(ExperienceType.SOCIALIZING, interactionType, parameters, outcome);
        profile.addExperience(experience);
        
        // Learn social preferences
        if (otherData != null) {
            String socialPreference = "personality_" + otherData.getPersonality() + "_compatibility";
            float currentPref = profile.preferences.getOrDefault(socialPreference, 0.0f);
            profile.preferences.put(socialPreference, currentPref + (outcome * 0.1f));
        }
    }
    
    public static void processWorkOutcome(VillagerEntity villager, String workType, float satisfaction) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("work_count", profile.getBehaviorCount("work_" + workType));
        parameters.put("villager_happiness", data != null ? data.getHappiness() : 50);
        parameters.put("time_of_day", villager.getWorld().getTimeOfDay() % 24000);
        
        Experience experience = new Experience(ExperienceType.WORKING, workType, parameters, satisfaction);
        profile.addExperience(experience);
        
        profile.incrementBehaviorCount("work_" + workType);
        
        // Learn work preferences
        String workPreference = "work_" + workType + "_satisfaction";
        float currentSatisfaction = profile.preferences.getOrDefault(workPreference, 0.0f);
        profile.preferences.put(workPreference, currentSatisfaction + (satisfaction * 0.05f));
    }
    
    // Behavioral adaptation methods
    public static boolean shouldAdaptBehavior(VillagerEntity villager, String behaviorType) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        
        List<Experience> relevantExperiences;
        try {
            relevantExperiences = profile.getRelevantExperiences(
                ExperienceType.valueOf(behaviorType.toUpperCase()), "general");
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid behaviorType for ExperienceType: " + behaviorType);
            return false;
        }
        
        if (relevantExperiences.size() < 3) return false; // Need enough data
        
        // Calculate average outcome
        double avgOutcome = relevantExperiences.stream()
            .mapToDouble(exp -> exp.getOutcome() * exp.getRelevanceScore())
            .average()
            .orElse(0.0);
        
        // Adapt if consistently negative outcomes
        return avgOutcome < -0.3;
    }
    
    public static float getLearnedPreference(VillagerEntity villager, String preferenceType) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        return profile.getPreference(preferenceType);
    }
    
    public static float predictInteractionOutcome(VillagerEntity villager, PlayerEntity player, String interactionType) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        Map<String, Float> conditions = new HashMap<>();
        conditions.put("player_reputation", data != null ? (float)data.getPlayerReputation(player.getUuidAsString()) : 0.0f);
        conditions.put("villager_happiness", data != null ? (float)data.getHappiness() : 50.0f);
        conditions.put("time_of_day", (float)(villager.getWorld().getTimeOfDay() % 24000));
        
        return profile.predictOutcome(ExperienceType.PLAYER_INTERACTION, interactionType, conditions);
    }
    
    // Personality evolution based on experiences
    public static void evolvePersonality(VillagerEntity villager) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        if (data == null || profile.experiences.size() < 50) return; // Need enough experiences
        
        // Analyze patterns in recent experiences
        Map<String, Integer> emotionalTrends = new HashMap<>();
        for (Experience exp : profile.experiences) {
            if (exp.isRecent()) {
                if (exp.getOutcome() > 0.5f) {
                    emotionalTrends.put("positive", emotionalTrends.getOrDefault("positive", 0) + 1);
                } else if (exp.getOutcome() < -0.5f) {
                    emotionalTrends.put("negative", emotionalTrends.getOrDefault("negative", 0) + 1);
                }
            }
        }
        
        int positiveCount = emotionalTrends.getOrDefault("positive", 0);
        int negativeCount = emotionalTrends.getOrDefault("negative", 0);
        
        // Personality shifts based on predominant experiences
        String currentPersonality = data.getPersonality();
        String[] personalities = VillagerData.PERSONALITIES;
        
        // Very gradual personality evolution (1% chance with strong emotional trends)
        if (positiveCount > negativeCount * 3 && !currentPersonality.equals("Cheerful")) {
            if (ThreadLocalRandom.current().nextFloat() < 0.01f) {
                if (Arrays.asList(personalities).contains("Cheerful")) {
                    data.setPersonality("Cheerful");
                }
            }
        } else if (negativeCount > positiveCount * 3 && !currentPersonality.equals("Grumpy")) {
            if (ThreadLocalRandom.current().nextFloat() < 0.01f) {
                if (Arrays.asList(personalities).contains("Grumpy")) {
                    data.setPersonality("Grumpy");
                }
            }
        }
    }
    
    // NBT serialization for persistence
    public static void saveLearningDataToNbt(VillagerEntity villager, NbtCompound nbt) {
        VillagerLearningProfile profile = learningProfiles.get(villager.getUuidAsString());
        if (profile == null) return;
        
        NbtCompound learningNbt = new NbtCompound();

        // Save preferences
        NbtCompound preferencesNbt = new NbtCompound();
        for (Map.Entry<String, Float> entry : profile.preferences.entrySet()) {
            preferencesNbt.putFloat(entry.getKey(), entry.getValue());
        }
        learningNbt.put("preferences", preferencesNbt);

        // Save behavior counts
        NbtCompound behaviorsNbt = new NbtCompound();
        for (Map.Entry<String, Integer> entry : profile.behaviorCounts.entrySet()) {
            behaviorsNbt.putInt(entry.getKey(), entry.getValue());
        }
        learningNbt.put("behaviors", behaviorsNbt);

        // Save recent experiences (limit to 50 most recent)
        NbtList experiencesNbt = new NbtList();
        profile.experiences.stream()
            .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
            .limit(50)
            .forEach(exp -> {
                NbtCompound expNbt = new NbtCompound();
                expNbt.putString("type", exp.type.name());
                expNbt.putString("context", exp.context);
                expNbt.putFloat("outcome", exp.getOutcome());
                expNbt.putLong("timestamp", exp.timestamp);
                expNbt.putInt("reinforcements", exp.reinforcements.get());

                // Serialize parameters (limit to 5 keys)
                NbtCompound paramsNbt = new NbtCompound();
                int paramCount = 0;
                for (Map.Entry<String, Object> param : exp.parameters.entrySet()) {
                    if (paramCount++ >= 5) break;
                    Object value = param.getValue();
                    if (value instanceof String) {
                        paramsNbt.putString(param.getKey(), (String) value);
                    } else if (value instanceof Float) {
                        paramsNbt.putFloat(param.getKey(), (Float) value);
                    } else if (value instanceof Integer) {
                        paramsNbt.putInt(param.getKey(), (Integer) value);
                    } // Add more types as needed
                }
                expNbt.put("parameters", paramsNbt);

                experiencesNbt.add(expNbt);
            });
        learningNbt.put("experiences", experiencesNbt);

        // Save learned patterns (limit to 30, summarize if more)
        NbtList patternsNbt = new NbtList();
        profile.patterns.entrySet().stream()
            .limit(30)
            .forEach(entry -> {
                LearnedPattern pattern = entry.getValue();
                NbtCompound patNbt = new NbtCompound();
                patNbt.putString("pattern_id", pattern.patternId);
                patNbt.putString("description", pattern.description);
                patNbt.putFloat("confidence", pattern.confidence);
                patNbt.putInt("times_observed", pattern.timesObserved);

                // Serialize conditions (limit to 5 keys)
                NbtCompound condNbt = new NbtCompound();
                int condCount = 0;
                for (Map.Entry<String, Float> cond : pattern.conditions.entrySet()) {
                    if (condCount++ >= 5) break;
                    condNbt.putFloat(cond.getKey(), cond.getValue());
                }
                patNbt.put("conditions", condNbt);

                // Serialize outcomes (limit to 3 keys)
                NbtCompound outNbt = new NbtCompound();
                int outCount = 0;
                for (Map.Entry<String, Float> out : pattern.outcomes.entrySet()) {
                    if (outCount++ >= 3) break;
                    outNbt.putFloat(out.getKey(), out.getValue());
                }
                patNbt.put("outcomes", outNbt);

                patternsNbt.add(patNbt);
            });
        learningNbt.put("patterns", patternsNbt);

        learningNbt.putFloat("learning_rate", profile.learningRate);

        nbt.put("learning_data", learningNbt);
    }
    
    public static void loadLearningDataFromNbt(VillagerEntity villager, NbtCompound nbt) {
        if (!nbt.contains("learning_data")) return;
        
        NbtCompound learningNbt = nbt.getCompound("learning_data").orElse(null);
        VillagerLearningProfile profile = new VillagerLearningProfile();
        
        // Load preferences
        if (learningNbt.contains("preferences")) {
            NbtCompound preferencesNbt = learningNbt.getCompound("preferences").orElse(null);
            if (preferencesNbt != null) {
                for (String key : preferencesNbt.getKeys()) {
                    profile.preferences.put(key, preferencesNbt.getFloat(key).orElse(0f));
                }
            }
        }
        
        // Load behavior counts
        if (learningNbt.contains("behaviors")) {
            NbtCompound behaviorsNbt = learningNbt.getCompound("behaviors").orElse(null);
            if (behaviorsNbt != null) {
                for (String key : behaviorsNbt.getKeys()) {
                    profile.behaviorCounts.put(key, behaviorsNbt.getInt(key).orElse(0));
                }
            }
        }
        
        if (learningNbt != null && learningNbt.contains("learning_rate")) {
            profile.learningRate = learningNbt.getFloat("learning_rate").orElse(0f);
        }
        
        learningProfiles.put(villager.getUuidAsString(), profile);
    }
    
    public static void clearLearningData(String villagerUuid) {
        learningProfiles.remove(villagerUuid);
    }
    
    // Global learning system management
    public static void updateGlobalLearning() {
        // Cleanup old learning profiles
        learningProfiles.entrySet().removeIf(entry -> {
            VillagerLearningProfile profile = entry.getValue();
            return (System.currentTimeMillis() - profile.lastLearningUpdate) > 86400000; // 24 hours
        });
    }
    
    // Analytics and debugging
    public static Map<String, Object> getLearningAnalytics(VillagerEntity villager) {
        VillagerLearningProfile profile = getLearningProfile(villager);
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("experience_count", profile.experiences.size());
        analytics.put("pattern_count", profile.patterns.size());
        analytics.put("learning_rate", profile.learningRate);
        analytics.put("top_preferences", profile.preferences.entrySet().stream()
            .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
            .limit(5)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll));
        
        return analytics;
    }
}