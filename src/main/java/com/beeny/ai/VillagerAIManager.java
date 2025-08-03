package com.beeny.ai;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.social.VillagerGossipNetwork;
import com.beeny.ai.planning.VillagerGOAP;
import com.beeny.ai.decision.VillagerUtilityAI;
import com.beeny.ai.learning.VillagerLearningSystem;
import com.beeny.ai.chat.VillagerChatSystem;
import com.beeny.ai.quests.VillagerQuestSystem;
import com.beeny.system.ServerVillagerManager;
import com.beeny.system.VillagerRelationshipManager;
import com.beeny.system.VillagerScheduleManager;
import com.beeny.system.VillagerProfessionManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central manager for all villager AI systems
 * Coordinates emotions, gossip, GOAP planning, utility AI, learning, and quests
 */
public class VillagerAIManager {

    private static final Logger logger = LoggerFactory.getLogger(VillagerAIManager.class);
    
    // AI system configuration
    public static class AIConfig {
        public boolean enableEmotions = true;
        public boolean enableGossip = true;
        public boolean enableGOAP = true;
        public boolean enableUtilityAI = true;
        public boolean enableLearning = true;
        public boolean enableChat = true;
        public boolean enableQuests = true;
        public boolean enablePersonalityEvolution = true;
        
        public int emotionUpdateInterval = 60; // seconds
        public int gossipSpreadInterval = 30; // seconds
        public int goapPlanningInterval = 10; // seconds
        public int learningUpdateInterval = 300; // seconds
        public int questGenerationInterval = 600; // seconds
        public int utilityAIUpdateInterval = 20; // seconds (default)
        
        public float emotionIntensityMultiplier = 1.0f;
        public float gossipSpreadMultiplier = 1.0f;
        public float learningRateMultiplier = 1.0f;
        public float questGenerationChance = 0.1f;
    }
    
    private static AIConfig config = new AIConfig();
    private static int getConfiguredThreadPoolSize() {
        String env = System.getenv("VILLAGERS_REBORN_AI_POOL_SIZE");
        if (env != null) {
            try {
                int size = Integer.parseInt(env);
                if (size > 0) return size;
            } catch (NumberFormatException ignored) {}
        }
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }
    private static volatile ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(getConfiguredThreadPoolSize());
    
    public static synchronized void reconfigureThreadPool() {
        int newSize = getConfiguredThreadPoolSize();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        scheduler = Executors.newScheduledThreadPool(newSize);
        logger.info("VillagerAIManager thread pool reconfigured to size: " + newSize);
    }
    private static final Map<String, Long> lastAIUpdate = new ConcurrentHashMap<>();
    private static final Map<String, VillagerAIState> villagerAIStates = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> playerLastInteractionTimestamps = new ConcurrentHashMap<>();
    private static final long INTERACTION_RATE_LIMIT_MS = 1000; // 1 second rate limit
    
    // Individual villager AI state
    public static class VillagerAIState {
        public long lastEmotionUpdate = 0;
        public long lastGOAPUpdate = 0;
        public long lastLearningUpdate = 0;
        public long lastQuestCheck = 0;
        public String currentGoal = "";
        public String currentAction = "";
        public boolean isAIActive = true;
        public long lastUtilityUpdate = 0;
        public Map<String, Object> contextData = new ConcurrentHashMap<>();
        
        public void setContext(String key, Object value) {
            contextData.put(key, value);
        }
        
        public Object getContext(String key) {
            return contextData.get(key);
        }
    }
    
    /**
     * Initialize AI systems for a villager
     */
    public static void initializeVillagerAI(VillagerEntity villager) {
        if (villager == null) return;
        
        String uuid = villager.getUuidAsString();
        VillagerAIState state = new VillagerAIState();
        villagerAIStates.put(uuid, state);
        
        // Initialize individual AI systems
        if (config.enableGOAP) {
            VillagerGOAP.VillagerGOAPManager.initializeVillager(villager);
        }
        
        // Initialize profession system
        VillagerProfessionManager.initializeVillagerProfession(villager);
        
        // Set initial emotional state based on personality
        if (config.enableEmotions) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                initializeEmotionalState(villager, data);
            }
        }
        
        state.setContext("last_initialization", System.currentTimeMillis());
    }
    
    private static void initializeEmotionalState(VillagerEntity villager, VillagerData data) {
        VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
        
        // Set initial emotions based on personality
        switch (data.getPersonality()) {
            case "Cheerful" -> {
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS, 30.0f, "personality_initialization");
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.EXCITEMENT, 20.0f, "personality_initialization");
            }
            case "Grumpy" -> {
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.ANGER, 20.0f, "personality_initialization");
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS, -10.0f, "personality_initialization");
            }
            case "Shy" -> {
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.FEAR, 15.0f, "personality_initialization");
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LONELINESS, 25.0f, "personality_initialization");
            }
            case "Energetic" -> {
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.EXCITEMENT, 40.0f, "personality_initialization");
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.BOREDOM, -20.0f, "personality_initialization");
            }
            case "Curious" -> {
                emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.CURIOSITY, 35.0f, "personality_initialization");
            }
        }
    }
    
    /**
     * Main AI update tick for a villager
     */
    public static void updateVillagerAI(VillagerEntity villager) {
        if (villager == null || villager.getWorld().isClient) return;
        
        String uuid = villager.getUuidAsString();
        VillagerAIState state = villagerAIStates.get(uuid);
        
        if (state == null) {
            initializeVillagerAI(villager);
            state = villagerAIStates.get(uuid);
        }
        
        if (!state.isAIActive) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Update emotions
        if (config.enableEmotions && shouldUpdateEmotion(state, currentTime)) {
            try {
                updateEmotionalState(villager, state);
            } catch (Exception e) {
                logger.error("Error updating emotional state for villager {}", uuid, e);
            }
        }
        
        // Update GOAP planning
        if (config.enableGOAP && shouldUpdateGOAP(state, currentTime)) {
            try {
                updateGOAPPlanning(villager, state);
            } catch (Exception e) {
                logger.error("Error updating GOAP planning for villager {}", uuid, e);
            }
        }
        
        // Update utility AI decisions
        if (config.enableUtilityAI && shouldUpdateUtilityAI(state, currentTime)) {
            try {
                updateUtilityDecisions(villager, state);
                state.lastUtilityUpdate = System.currentTimeMillis();
            } catch (Exception e) {
                logger.error("Error updating utility decisions for villager {}", uuid, e);
            }
        }
        
        // Update learning
        if (config.enableLearning && shouldUpdateLearning(state, currentTime)) {
            try {
                updateLearning(villager, state);
            } catch (Exception e) {
                logger.error("Error updating learning for villager {}", uuid, e);
            }
        }
        
        // Check for quest opportunities
        if (config.enableQuests && shouldCheckQuests(state, currentTime)) {
            try {
                checkQuestOpportunities(villager, state);
            } catch (Exception e) {
                logger.error("Error checking quest opportunities for villager {}", uuid, e);
            }
        }
        
        // Update profession satisfaction and evaluate career changes
        VillagerProfessionManager.updateProfessionSatisfaction(villager);
        VillagerProfessionManager.evaluateCareerChange(villager);
        
        // Process work experience
        VillagerScheduleManager.Activity currentActivity = VillagerScheduleManager.getCurrentActivity(villager);
        if (currentActivity == VillagerScheduleManager.Activity.WORK) {
            VillagerProfessionManager.onVillagerWork(villager);
        }
        
        lastAIUpdate.put(uuid, currentTime);
    }
    
    private static boolean shouldUpdateEmotion(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastEmotionUpdate) >= (config.emotionUpdateInterval * 1000);
    }
    
    private static boolean shouldUpdateGOAP(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastGOAPUpdate) >= (config.goapPlanningInterval * 1000);
    }
    
    private static boolean shouldUpdateLearning(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastLearningUpdate) >= (config.learningUpdateInterval * 1000);
    }
    
    private static boolean shouldCheckQuests(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastQuestCheck) >= (config.questGenerationInterval * 1000);
    }
    
    private static boolean shouldUpdateUtilityAI(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastUtilityUpdate) >= (config.utilityAIUpdateInterval * 1000);
    }
    
    private static void updateEmotionalState(VillagerEntity villager, VillagerAIState state) {
        // Process emotional decay and environmental influences
        VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        if (data == null) return;
        
        // Environmental emotional influences
        processEnvironmentalEmotions(villager, emotions, data);
        
        // Social emotional influences
        processSocialEmotions(villager, emotions, data);
        
        // Update context
        state.setContext("dominant_emotion", emotions.getDominantEmotion().name());
        state.setContext("emotional_description", emotions.getEmotionalDescription());
        state.lastEmotionUpdate = System.currentTimeMillis();
    }
    
    private static void processEnvironmentalEmotions(VillagerEntity villager, VillagerEmotionSystem.EmotionalState emotions, VillagerData data) {
        // Time of day influences
        VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(villager.getWorld().getTimeOfDay());
        
        switch (timeOfDay) {
            case DAWN -> emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.CONTENTMENT, 5.0f, "dawn_peace");
            case NIGHT -> {
                if (data.getPersonality().equals("Nervous")) {
                    emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.FEAR, 10.0f, "night_anxiety");
                }
            }
        }
        
        // Weather influences
        if (villager.getWorld().isRaining()) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.CONTENTMENT, -5.0f, "rainy_weather");
        }
        
        // Isolation effects
        List<VillagerEntity> nearby = villager.getWorld().getEntitiesByClass(
            VillagerEntity.class, villager.getBoundingBox().expand(20), v -> v != villager);
        
        if (nearby.isEmpty()) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LONELINESS, 3.0f, "isolation");
        } else if (nearby.size() > 5) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LONELINESS, -2.0f, "social_environment");
        }
    }
    
    private static void processSocialEmotions(VillagerEntity villager, VillagerEmotionSystem.EmotionalState emotions, VillagerData data) {
        // Spouse and family influences
        if (!data.getSpouseId().isEmpty()) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LOVE, 5.0f, "married_life");
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LONELINESS, -10.0f, "married_life");
        }
        
        if (!data.getChildrenIds().isEmpty()) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS, 8.0f, "family_joy");
        }
        
        // Work satisfaction
        VillagerScheduleManager.Activity currentActivity = VillagerScheduleManager.getCurrentActivity(villager);
        if (currentActivity == VillagerScheduleManager.Activity.WORK) {
            float workSatisfaction = VillagerLearningSystem.getLearnedPreference(villager, "work_satisfaction");
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.CONTENTMENT, workSatisfaction * 5.0f, "work_experience");
        }
    }
    
    private static void updateGOAPPlanning(VillagerEntity villager, VillagerAIState state) {
        try {
            VillagerGOAP.VillagerGOAPManager.updateVillager(villager);
            state.lastGOAPUpdate = System.currentTimeMillis();
        } catch (Exception e) {
            // Fallback to schedule system if GOAP fails
            VillagerScheduleManager.Activity currentActivity = VillagerScheduleManager.getCurrentActivity(villager);
            state.currentAction = currentActivity.description;
        }
    }
    
    private static void updateUtilityDecisions(VillagerEntity villager, VillagerAIState state) {
        // Run utility AI decisions periodically
        VillagerUtilityAI.UtilityAIManager.updateVillagerDecisions(villager);
        
        // Update context with current decision priorities
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            // Check for marriage opportunities
            if (data.getSpouseId().isEmpty() && data.getAge() > 100) {
                List<VillagerEntity> potentialPartners = VillagerRelationshipManager.findPotentialPartners(villager);
                if (!potentialPartners.isEmpty()) {
                    state.setContext("marriage_candidates", potentialPartners.size());
                }
            }
        }
    }
    
    private static void updateLearning(VillagerEntity villager, VillagerAIState state) {
        // Periodic learning updates
        VillagerLearningSystem.evolvePersonality(villager);
        
        state.lastLearningUpdate = System.currentTimeMillis();
        
        // Update learning analytics in context
        Map<String, Object> analytics = VillagerLearningSystem.getLearningAnalytics(villager);
        state.setContext("learning_analytics", analytics);
    }
    
    private static void checkQuestOpportunities(VillagerEntity villager, VillagerAIState state) {
        // Look for nearby players to offer quests to
        List<PlayerEntity> nearbyPlayers = villager.getWorld().getEntitiesByClass(
            PlayerEntity.class, villager.getBoundingBox().expand(10), p -> true);
        
        for (PlayerEntity player : nearbyPlayers) {
            if (Math.random() < config.questGenerationChance) {
                VillagerQuestSystem.QuestManager.generateQuestForVillager(villager, player);
            }
        }
        
        state.lastQuestCheck = System.currentTimeMillis();
    }
    
    /**
     * Handle player interaction with villager
     */
    public static Text handlePlayerInteraction(VillagerEntity villager, PlayerEntity player, String message) {
        // Rate limiting: ignore if player interacts too frequently
        UUID playerId = player.getUuid();
        long now = System.currentTimeMillis();
        Long lastInteraction = playerLastInteractionTimestamps.get(playerId);
        if (lastInteraction != null && (now - lastInteraction) < INTERACTION_RATE_LIMIT_MS) {
            // Optionally, return a message indicating rate limit
            return Text.literal("You're interacting too quickly. Please wait a moment.").formatted(Formatting.RED);
            // Or: return null; // to silently ignore
        }
        playerLastInteractionTimestamps.put(playerId, now);

        if (!config.enableChat) {
            return null; // Fall back to default dialogue system
        }
        
        // Update AI state for interaction
        VillagerAIState state = villagerAIStates.get(villager.getUuidAsString());
        if (state != null) {
            state.setContext("last_player_interaction", now);
            state.setContext("last_interacting_player", player.getUuidAsString());
        }
        
        // Process chat if message provided
        if (message != null && !message.trim().isEmpty()) {
            return VillagerChatSystem.processPlayerMessage(villager, player, message);
        }
        
        // Check for quest opportunities
        VillagerQuestSystem.onPlayerInteractWithVillager(villager, player);
        
        // Apply global reputation influence
        if (config.enableGossip) {
            VillagerGossipNetwork.applyGlobalReputation(villager, player);
        }
        
        // Generate enhanced dialogue that considers AI state
        return VillagerChatSystem.generateEnhancedDialogue(villager, player,
            com.beeny.system.VillagerDialogueSystem.DialogueCategory.GREETING);
    }
    
    /**
     * Handle villager trade events
     */
    public static void onVillagerTrade(VillagerEntity villager, PlayerEntity player, boolean fairTrade, int emeraldValue) {
        // Record learning experience
        if (config.enableLearning) {
            VillagerLearningSystem.processTradeOutcome(villager, player, fairTrade, emeraldValue);
        }
        
        // Create gossip about the trade
        if (config.enableGossip) {
            VillagerGossipNetwork.CommonGossipEvents.playerTraded(villager, player, fairTrade);
        }
        
        // Emotional response to trade
        if (config.enableEmotions) {
            VillagerEmotionSystem.EmotionalEvent tradeEvent = fairTrade ? 
                VillagerEmotionSystem.EmotionalEvent.SUCCESSFUL_TRADE :
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.ANGER, 20.0f, "unfair_trade", false);
            
            VillagerEmotionSystem.processEmotionalEvent(villager, tradeEvent);
        }
        
        // Update profession skills and satisfaction
        // Note: In actual implementation, you'd pass the traded ItemStack
        VillagerProfessionManager.onVillagerTrade(villager, new net.minecraft.item.ItemStack(net.minecraft.item.Items.EMERALD), fairTrade);
        
        // Update quest progress
        if (config.enableQuests) {
            // This would be called when specific quest items are traded
            // VillagerQuestSystem.onItemFetchedToVillager(player, villager, itemType, quantity);
        }
    }
    
    /**
     * Handle villager marriage events
     */
    public static void onVillagerMarriage(VillagerEntity villager1, VillagerEntity villager2) {
        if (config.enableEmotions) {
            VillagerEmotionSystem.processEmotionalEvent(villager1, VillagerEmotionSystem.EmotionalEvent.MARRIAGE);
            VillagerEmotionSystem.processEmotionalEvent(villager2, VillagerEmotionSystem.EmotionalEvent.MARRIAGE);
        }
        
        if (config.enableGossip) {
            VillagerGossipNetwork.CommonGossipEvents.villagerMarried(villager1, villager2);
        }
        
        if (config.enableLearning) {
            VillagerLearningSystem.processSocialOutcome(villager1, villager2, "marriage", 1.0f);
            VillagerLearningSystem.processSocialOutcome(villager2, villager1, "marriage", 1.0f);
        }
    }
    
    /**
     * Periodic cleanup and maintenance
     */
    public static void performMaintenance() {
        // Clean up old gossip
        if (config.enableGossip) {
            VillagerGossipNetwork.cleanupOldGossip();
        }
        
        // Clean up expired quests
        if (config.enableQuests) {
            VillagerQuestSystem.QuestManager.cleanupExpiredQuests();
        }
        
        // Update global learning patterns
        if (config.enableLearning) {
            VillagerLearningSystem.updateGlobalLearning();
        }
        
        // Clean up inactive AI states
        villagerAIStates.entrySet().removeIf(entry -> {
            long lastUpdate = lastAIUpdate.getOrDefault(entry.getKey(), 0L);
            return (System.currentTimeMillis() - lastUpdate) > 3600000; // 1 hour
        });
    }
    
    /**
     * Save AI data to NBT
     */
    public static void saveAIDataToNbt(VillagerEntity villager, NbtCompound nbt) {
        if (config.enableEmotions) {
            VillagerEmotionSystem.saveEmotionsToNbt(villager, nbt);
        }
        
        if (config.enableLearning) {
            VillagerLearningSystem.saveLearningDataToNbt(villager, nbt);
        }
        
        // Save AI state
        VillagerAIState state = villagerAIStates.get(villager.getUuidAsString());
        if (state != null) {
            NbtCompound aiStateNbt = new NbtCompound();
            aiStateNbt.putString("current_goal", state.currentGoal);
            aiStateNbt.putString("current_action", state.currentAction);
            aiStateNbt.putBoolean("is_ai_active", state.isAIActive);
            nbt.put("ai_state", aiStateNbt);
        }
    }
    
    /**
     * Load AI data from NBT
     */
    public static void loadAIDataFromNbt(VillagerEntity villager, NbtCompound nbt) {
        if (config.enableEmotions) {
            VillagerEmotionSystem.loadEmotionsFromNbt(villager, nbt);
        }
        
        if (config.enableLearning) {
            VillagerLearningSystem.loadLearningDataFromNbt(villager, nbt);
        }
        
        // Load AI state
        if (nbt.contains("ai_state")) {
            NbtCompound aiStateNbt = nbt.getCompound("ai_state");
            if (aiStateNbt != null) {
                VillagerAIState state = new VillagerAIState();
                state.currentGoal = aiStateNbt.getString("current_goal");
                state.currentAction = aiStateNbt.getString("current_action");
                state.isAIActive = aiStateNbt.getBoolean("is_ai_active");
                villagerAIStates.put(villager.getUuidAsString(), state);
            }
            }
        }
    }
    
    /**
     * Cleanup AI data for a villager
     */
    public static void cleanupVillagerAI(String villagerUuid) {
        villagerAIStates.remove(villagerUuid);
        lastAIUpdate.remove(villagerUuid);
        
        if (config.enableEmotions) {
            VillagerEmotionSystem.clearEmotions(villagerUuid);
        }
        
        if (config.enableGOAP) {
            VillagerGOAP.VillagerGOAPManager.cleanup(villagerUuid);
        }
        
        if (config.enableLearning) {
            VillagerLearningSystem.clearLearningData(villagerUuid);
        }
        
        // Cleanup profession data
        VillagerProfessionManager.cleanupProfessionData(villagerUuid);
    }
    
    /**
     * Global AI system management
     */
    public static void initializeGlobalAI() {
        // Start maintenance scheduler
        scheduler.scheduleAtFixedRate(() -> {
            try {
                VillagerAIManager.performMaintenance();
            } catch (Exception e) {
                logger.error("Exception in performMaintenance scheduled task", e);
            }
        }, 5, 5, TimeUnit.MINUTES);

        // Start AI update scheduler for all villagers
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (VillagerEntity villager : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
                    if (villager.getWorld() instanceof ServerWorld) {
                        updateVillagerAI(villager);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception in AI update scheduled task", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    public static void shutdownGlobalAI() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    /**
     * Configuration management
     */
    public static AIConfig getConfig() {
        return config;
    }
    
    public static void updateConfig(AIConfig newConfig) {
        config = newConfig;
    }
    
    /**
     * Analytics and debugging
     */
    public static Map<String, Object> getGlobalAIAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("active_villager_ais", villagerAIStates.size());
        analytics.put("total_tracked_villagers", StreamSupport.stream(ServerVillagerManager.getInstance().getAllTrackedVillagers().spliterator(), false).count());
        analytics.put("gossip_size", VillagerGossipNetwork.class.getSimpleName()); // Would get actual size
        analytics.put("config", config);
        
        // Emotion statistics
        Map<String, Integer> emotionStats = new HashMap<>();
        for (VillagerAIState state : villagerAIStates.values()) {
            Object dominantEmotion = state.getContext("dominant_emotion");
            if (dominantEmotion != null) {
                String emotion = dominantEmotion.toString();
                emotionStats.put(emotion, emotionStats.getOrDefault(emotion, 0) + 1);
            }
        }
        analytics.put("emotion_distribution", emotionStats);
        
        return analytics;
    }
    
    public static VillagerAIState getVillagerAIState(VillagerEntity villager) {
        return villagerAIStates.get(villager.getUuidAsString());
    }
    
    public static List<Text> getVillagerAIStatus(VillagerEntity villager) {
        List<Text> status = new ArrayList<>();
        VillagerAIState state = getVillagerAIState(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        if (state == null || data == null) {
            status.add(Text.literal("AI not initialized").formatted(Formatting.GRAY));
            return status;
        }
        
        status.add(Text.literal("=== AI Status for " + data.getName() + " ===").formatted(Formatting.GOLD));
        status.add(Text.literal("AI Active: " + state.isAIActive).formatted(state.isAIActive ? Formatting.GREEN : Formatting.RED));
        
        if (config.enableEmotions) {
            String emotionalState = (String) state.getContext("emotional_description");
            status.add(Text.literal("Emotional State: " + (emotionalState != null ? emotionalState : "unknown")).formatted(Formatting.AQUA));
        }
        
        if (!state.currentGoal.isEmpty()) {
            status.add(Text.literal("Current Goal: " + state.currentGoal).formatted(Formatting.YELLOW));
        }
        
        if (!state.currentAction.isEmpty()) {
            status.add(Text.literal("Current Action: " + state.currentAction).formatted(Formatting.WHITE));
        }
        
        if (config.enableLearning) {
            Object analytics = state.getContext("learning_analytics");
            if (analytics instanceof Map) {
                Map<?, ?> learningData = (Map<?, ?>) analytics;
                status.add(Text.literal("Learning Rate: " + learningData.get("learning_rate")).formatted(Formatting.LIGHT_PURPLE));
                status.add(Text.literal("Experiences: " + learningData.get("experience_count")).formatted(Formatting.LIGHT_PURPLE));
            }
        }
        
        // Add profession information
        VillagerProfessionManager.ProfessionData profData = VillagerProfessionManager.getProfessionData(villager);
        if (profData != null) {
            status.add(Text.literal("Profession: " + profData.getProfessionId()).formatted(Formatting.YELLOW));
            status.add(Text.literal("Skill Level: " + String.format("%.1f", profData.getSkillLevel())).formatted(Formatting.GREEN));
            status.add(Text.literal("Job Satisfaction: " + String.format("%.1f", profData.getSatisfaction())).formatted(Formatting.AQUA));
            status.add(Text.literal("Professional Competency: " + String.format("%.1f", profData.getOverallCompetency())).formatted(Formatting.GOLD));
        }
        
        return status;
    }
}