package com.beeny.ai;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.social.VillagerGossipManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.text.MutableText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Central AI World Manager - replaces all static AI systems
 * 
 * This class manages all AI-related functionality for a specific server instance.
 * Unlike the old static approach, this manager is instantiated per server and
 * properly handles lifecycle management and state cleanup.
 */
public class AIWorldManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIWorldManager.class);
    
    private final MinecraftServer server;
    private final ScheduledExecutorService scheduler;
    
    // AI System Components (instance-based, not static)
    private final VillagerEmotionSystem emotionManager;
    private final VillagerLearningManager learningManager;
    private final VillagerGossipManager gossipManager;
    private final VillagerPlanningManager planningManager;
    private final VillagerQuestManager questManager;
    
    // Configuration
    private final AIConfig config;
    
    // Villager tracking for this server instance
    private final Map<String, VillagerAIState> villagerStates = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAIUpdate = new ConcurrentHashMap<>();
    
    // Per-server AI state
    public static class VillagerAIState {
        public long lastEmotionUpdate = 0;
        public long lastGOAPUpdate = 0;
        public long lastLearningUpdate = 0;
        public long lastQuestCheck = 0;
        public String currentGoal = "";
        public String currentAction = "";
        public boolean isAIActive = true;
        public long lastUtilityUpdate = 0;
        public final Map<String, Object> contextData = new ConcurrentHashMap<>();
        
        public void setContext(@NotNull String key, @Nullable Object value) {
            contextData.put(key, value);
        }
        
        @Nullable
        public Object getContext(@NotNull String key) {
            return contextData.get(key);
        }
    }
    
    // AI Configuration class
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
        public int utilityAIUpdateInterval = 20; // seconds
        
        public float emotionIntensityMultiplier = 1.0f;
        public float gossipSpreadMultiplier = 1.0f;
        public float learningRateMultiplier = 1.0f;
        public float questGenerationChance = 0.1f;
    }
    
    public AIWorldManager(@NotNull MinecraftServer server) {
        this.server = server;
        this.config = new AIConfig();
        
        // Create thread pool for AI processing
        int threadPoolSize = getConfiguredThreadPoolSize();
        this.scheduler = Executors.newScheduledThreadPool(threadPoolSize);
        
        // Initialize AI system managers
        this.emotionManager = new VillagerEmotionSystem();
        this.learningManager = new VillagerLearningManager(this);
        this.gossipManager = new VillagerGossipManager(this);
        this.planningManager = new VillagerPlanningManager(this);
        this.questManager = new VillagerQuestManager(this);
        
        LOGGER.info("AIWorldManager initialized for server with {} threads", threadPoolSize);
    }
    
    private int getConfiguredThreadPoolSize() {
        String env = System.getenv("VILLAGERS_REBORN_AI_POOL_SIZE");
        if (env != null) {
            try {
                int size = Integer.parseInt(env);
                if (size > 0) return size;
            } catch (NumberFormatException ignored) {}
        }
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Start the AI system - called when server starts
     */
    public void start() {
        LOGGER.info("Starting AI World Manager...");
        
        // Start maintenance scheduler
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performMaintenance();
            } catch (Exception e) {
                LOGGER.error("Exception in AI maintenance task", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        
        // Start staggered AI update scheduler
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performStaggeredAIUpdates();
            } catch (Exception e) {
                LOGGER.error("Exception in AI update task", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
        
        LOGGER.info("AI World Manager started successfully");
    }
    
    /**
     * Shutdown the AI system - called when server stops
     */
    public void shutdown() {
        LOGGER.info("Shutting down AI World Manager...");
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Cleanup all AI managers
        // Emotion system has no shutdown hook
        learningManager.shutdown();
        gossipManager.shutdown();
        planningManager.shutdown();
        questManager.shutdown();
        
        // Clear all state
        villagerStates.clear();
        lastAIUpdate.clear();
        
        LOGGER.info("AI World Manager shut down successfully");
    }
    
    /**
     * Initialize AI for a specific villager
     */
    public void initializeVillagerAI(@Nullable VillagerEntity villager) {
        if (villager == null) return;
        
        String uuid = villager.getUuidAsString();
        VillagerAIState state = new VillagerAIState();
        villagerStates.put(uuid, state);
        
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            // Initialize individual AI subsystems
            if (config.enableEmotions) {
                // Emotion system uses lazy state; no explicit initialization required
                VillagerEmotionSystem.getEmotionalState(villager);
            }
            if (config.enableLearning) {
                learningManager.initializeVillagerLearning(villager, data);
            }
            if (config.enableGossip) {
                gossipManager.initializeVillagerGossip(villager, data);
            }
            if (config.enableGOAP) {
                planningManager.initializeVillagerPlanning(villager, data);
            }
            if (config.enableQuests) {
                questManager.initializeVillagerQuests(villager, data);
            }
        }
        
        state.setContext("initialized_at", System.currentTimeMillis());
        LOGGER.debug("Initialized AI for villager {}", uuid);
    }
    
    /**
     * Update AI for a specific villager
     */
    public void updateVillagerAI(@Nullable VillagerEntity villager) {
        if (villager == null || villager.getWorld().isClient) return;
        
        String uuid = villager.getUuidAsString();
        VillagerAIState state = villagerStates.get(uuid);
        
        if (state == null) {
            initializeVillagerAI(villager);
            state = villagerStates.get(uuid);
        }
        
        if (!state.isAIActive) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Make copies for lambda capture
        final VillagerAIState finalState = state;
        final String finalUuid = uuid;
        final long finalCurrentTime = currentTime;
        
        // Thread-safe AI updates using server executor
        server.execute(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // Update emotions
                if (config.enableEmotions && shouldUpdateEmotion(finalState, finalCurrentTime)) {
                    VillagerEmotionSystem.updateEmotionalDecay(villager);
                    finalState.lastEmotionUpdate = System.currentTimeMillis();
                }
                
                // Update learning
                if (config.enableLearning && shouldUpdateLearning(finalState, finalCurrentTime)) {
                    learningManager.updateVillagerLearning(villager);
                    finalState.lastLearningUpdate = System.currentTimeMillis();
                }
                
                // Update gossip
                if (config.enableGossip && shouldUpdateGossip(finalState, finalCurrentTime)) {
                    gossipManager.updateVillagerGossip(villager);
                }
                
                // Update planning (GOAP)
                if (config.enableGOAP && shouldUpdateGOAP(finalState, finalCurrentTime)) {
                    planningManager.updateVillagerPlanning(villager);
                    finalState.lastGOAPUpdate = System.currentTimeMillis();
                }
                
                // Update quests
                if (config.enableQuests && shouldCheckQuests(finalState, finalCurrentTime)) {
                    questManager.updateVillagerQuests(villager);
                    finalState.lastQuestCheck = System.currentTimeMillis();
                }
                
                lastAIUpdate.put(finalUuid, System.currentTimeMillis());
                
                // Record performance metrics
                long executionTime = System.currentTimeMillis() - startTime;
                performanceMetrics.recordOperation("villager_ai_update", executionTime);
                
                if (executionTime > 50) { // Log slow operations
                    LOGGER.warn("Slow AI update for villager {}: {}ms", finalUuid, executionTime);
                }
                
            } catch (Exception e) {
                LOGGER.error("Error updating AI for villager {}", finalUuid, e);
                performanceMetrics.recordOperation("villager_ai_error", 1);
            }
        });
    }
    
    /**
     * Staggered AI updates to prevent performance spikes
     */
    private void performStaggeredAIUpdates() {
        // Get all villagers from all worlds
        List<VillagerEntity> allVillagers = new ArrayList<>();
        
        for (Map.Entry<String, VillagerAIState> entry : villagerStates.entrySet()) {
            String uuid = entry.getKey();
            WeakReference<VillagerEntity> ref = villagerCache.get(uuid);
            if (ref != null) {
                VillagerEntity villager = ref.get();
                if (villager != null && !villager.getWorld().isClient) {
                    allVillagers.add(villager);
                }
            }
        }
        
        if (allVillagers.isEmpty()) return;
        
        // Update a batch of villagers each tick to distribute load
        int updateCount = Math.min(MAX_UPDATES_PER_TICK, Math.max(MIN_UPDATES_PER_TICK, allVillagers.size() / 20));
        int startIndex = staggeredVillagerIndex.get();
        
        for (int i = 0; i < updateCount; i++) {
            int index = (startIndex + i) % allVillagers.size();
            VillagerEntity villager = allVillagers.get(index);
            try {
                updateVillagerAI(villager);
            } catch (Exception e) {
                LOGGER.warn("Error updating AI for villager {}: {}", villager.getUuidAsString(), e.getMessage());
            }
        }
        
        // Update index for next tick
        staggeredVillagerIndex.set((startIndex + updateCount) % allVillagers.size());
    }
    
    // Staggered AI update state - thread safe
    private final AtomicInteger staggeredVillagerIndex = new AtomicInteger(0);
    private static final int MIN_UPDATES_PER_TICK = 1;
    private static final int MAX_UPDATES_PER_TICK = 50; // configurable if needed
    
    // Villager cache for efficient lookups (package-private for VillagerEmotionManager access)
    final Map<String, WeakReference<VillagerEntity>> villagerCache = new ConcurrentHashMap<>();
    
    // Simple time-based cache for performance optimization
    private static class TimedCache<K, V> {
        private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
        private final long ttlMs;
        private final int maxSize;
        
        private static class CacheEntry<V> {
            final V value;
            final long timestamp;
            
            CacheEntry(V value) {
                this.value = value;
                this.timestamp = System.currentTimeMillis();
            }
            
            boolean isExpired(long ttlMs) {
                return (System.currentTimeMillis() - timestamp) > ttlMs;
            }
        }
        
        public TimedCache(long ttlMs, int maxSize) {
            this.ttlMs = ttlMs;
            this.maxSize = maxSize;
        }
        
        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null && !entry.isExpired(ttlMs)) {
                return entry.value;
            }
            cache.remove(key);
            return null;
        }
        
        public void put(K key, V value) {
            if (cache.size() >= maxSize) {
                // Simple LRU-like eviction - remove expired entries first
                cache.entrySet().removeIf(e -> e.getValue().isExpired(ttlMs));
                
                // If still full, remove oldest entry
                if (cache.size() >= maxSize) {
                    cache.entrySet().stream()
                        .min((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                        .ifPresent(entry -> cache.remove(entry.getKey()));
                }
            }
            cache.put(key, new CacheEntry<>(value));
        }
        
        public void clear() {
            cache.clear();
        }
        
        public int size() {
            return cache.size();
        }
    }
    
    // Performance caches
    private final TimedCache<String, Map<String, Object>> analyticsCache = new TimedCache<>(30000, 100); // 30 second TTL
    private final TimedCache<String, Boolean> validationCache = new TimedCache<>(60000, 200); // 1 minute TTL
    
    // Performance monitoring
    private static class PerformanceMetrics {
        private final Map<String, Long> operationCounts = new ConcurrentHashMap<>();
        private final Map<String, Long> totalExecutionTime = new ConcurrentHashMap<>();
        private final Map<String, Long> lastExecutionTime = new ConcurrentHashMap<>();
        
        public void recordOperation(String operation, long executionTimeMs) {
            operationCounts.merge(operation, 1L, Long::sum);
            totalExecutionTime.merge(operation, executionTimeMs, Long::sum);
            lastExecutionTime.put(operation, executionTimeMs);
        }
        
        public long getOperationCount(String operation) {
            return operationCounts.getOrDefault(operation, 0L);
        }
        
        public double getAverageExecutionTime(String operation) {
            long count = operationCounts.getOrDefault(operation, 0L);
            long total = totalExecutionTime.getOrDefault(operation, 0L);
            return count > 0 ? (double) total / count : 0.0;
        }
        
        public long getLastExecutionTime(String operation) {
            return lastExecutionTime.getOrDefault(operation, 0L);
        }
        
        public Map<String, Object> getMetrics() {
            Map<String, Object> metrics = new HashMap<>();
            for (String operation : operationCounts.keySet()) {
                Map<String, Object> operationMetrics = new HashMap<>();
                operationMetrics.put("count", getOperationCount(operation));
                operationMetrics.put("avgTime", getAverageExecutionTime(operation));
                operationMetrics.put("lastTime", getLastExecutionTime(operation));
                metrics.put(operation, operationMetrics);
            }
            return metrics;
        }
        
        public void reset() {
            operationCounts.clear();
            totalExecutionTime.clear();
            lastExecutionTime.clear();
        }
    }
    
    private final PerformanceMetrics performanceMetrics = new PerformanceMetrics();

    /**
     * Called once per server tick to update a subset of villagers.
     */
    public void tickStaggeredVillagerAI() {
        List<String> villagerUuids = new ArrayList<>(villagerStates.keySet());
        int totalVillagers = villagerUuids.size();
        if (totalVillagers == 0) return;

        // Calculate how many villagers to update per tick (e.g., 1/20th per tick for 20 TPS)
        int updatesPerTick = Math.max(MIN_UPDATES_PER_TICK, Math.min(MAX_UPDATES_PER_TICK, totalVillagers / 20));
        int startIdx = staggeredVillagerIndex.get();
        int endIdx = Math.min(startIdx + updatesPerTick, totalVillagers);

        for (int i = startIdx; i < endIdx; i++) {
            String uuid = villagerUuids.get(i);
            VillagerEntity villager = findVillagerByUuid(uuid);
            if (villager != null) {
                // Optionally reduce frequency for distant/unloaded villagers
                if (shouldUpdateVillagerThisTick(villager)) {
                    updateVillagerAI(villager);
                }
            } else {
                cleanupVillagerAI(uuid);
            }
        }

        staggeredVillagerIndex.set(endIdx >= totalVillagers ? 0 : endIdx);
    }

    /**
     * Determines if a villager should be updated this tick.
     * - Updates every tick if near a player or in a loaded chunk.
     * - Updates less frequently if far from players or in unloaded/player-less chunks.
     */
    private boolean shouldUpdateVillagerThisTick(VillagerEntity villager) {
        ServerWorld world = (villager.getWorld() instanceof ServerWorld) ? (ServerWorld) villager.getWorld() : null;
        if (world == null) return false;

        // Check if chunk is loaded and has players nearby
        boolean chunkLoaded = world.isChunkLoaded(villager.getBlockPos());
        double nearestPlayerDist = world.getClosestPlayer(villager, 64.0) != null
            ? villager.squaredDistanceTo(world.getClosestPlayer(villager, 64.0))
            : Double.MAX_VALUE;

        // If chunk is not loaded, skip update
        if (!chunkLoaded) return false;

        // If no player within 64 blocks, update only every 5 ticks
        if (nearestPlayerDist > 64 * 64) {
            // Use world time to stagger
            return (world.getTime() % 5 == 0);
        }

        // Otherwise, update every tick
        return true;
    }
    
    /**
     * Find villager by UUID using ServerVillagerManager's tracked map (O(1)).
     * Falls back to previous cache only as a fast-path; no world scans.
     */
    @Nullable
    private VillagerEntity findVillagerByUuid(@NotNull String uuid) {
        // Check cache first
        WeakReference<VillagerEntity> ref = villagerCache.get(uuid);
        if (ref != null) {
            VillagerEntity cached = ref.get();
            if (cached != null && !cached.isRemoved()) {
                return cached;
            }
            villagerCache.remove(uuid);
        }

        // Use ServerVillagerManager singleton for constant-time lookup
        try {
            java.util.UUID entityUuid = java.util.UUID.fromString(uuid);
            com.beeny.system.ServerVillagerManager svm = com.beeny.system.ServerVillagerManager.getInstance();
            VillagerEntity villager = svm.getVillager(entityUuid);
            if (villager != null && !villager.isRemoved()) {
                villagerCache.put(uuid, new java.lang.ref.WeakReference<>(villager));
                return villager;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid UUID string; no lookup possible
        }

        return null;
    }
    
    /**
     * Cleanup AI data for a villager
     */
    public void cleanupVillagerAI(@NotNull String villagerUuid) {
        villagerStates.remove(villagerUuid);
        lastAIUpdate.remove(villagerUuid);
        
        // Cleanup in subsystems
        VillagerEmotionSystem.clearEmotionalState(villagerUuid);
        learningManager.cleanupVillager(villagerUuid);
        gossipManager.cleanupVillager(villagerUuid);
        planningManager.cleanupVillager(villagerUuid);
        questManager.cleanupVillager(villagerUuid);
        
        LOGGER.debug("Cleaned up AI for villager {}", villagerUuid);
    }
    
    /**
     * Periodic maintenance tasks
     */
    private void performMaintenance() {
        // Clean up stale AI states
        long staleThreshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        villagerStates.entrySet().removeIf(entry -> {
            long lastUpdate = lastAIUpdate.getOrDefault(entry.getKey(), 0L);
            return lastUpdate < staleThreshold;
        });
        
        // Run subsystem maintenance
        // Emotion system is stateless per-instance; decay/maintenance handled elsewhere if needed
        learningManager.performMaintenance();
        gossipManager.performMaintenance();
        planningManager.performMaintenance();
        questManager.performMaintenance();
        
        LOGGER.debug("Performed AI maintenance - {} active villager states", villagerStates.size());
    }
    
    // Update condition checks
    private boolean shouldUpdateEmotion(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastEmotionUpdate) >= (config.emotionUpdateInterval * 1000L);
    }
    
    private boolean shouldUpdateGOAP(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastGOAPUpdate) >= (config.goapPlanningInterval * 1000L);
    }
    
    private boolean shouldUpdateLearning(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastLearningUpdate) >= (config.learningUpdateInterval * 1000L);
    }
    
    private boolean shouldCheckQuests(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastQuestCheck) >= (config.questGenerationInterval * 1000L);
    }
    
    private boolean shouldUpdateGossip(VillagerAIState state, long currentTime) {
        return (currentTime - state.lastEmotionUpdate) >= (config.gossipSpreadInterval * 1000L);
    }
    
    // Getters
    public MinecraftServer getServer() { return server; }
    public AIConfig getConfig() { return config; }
    public VillagerEmotionSystem getEmotionManager() { return emotionManager; }
    public VillagerLearningManager getLearningManager() { return learningManager; }
    public VillagerGossipManager getGossipManager() { return gossipManager; }
    public VillagerPlanningManager getPlanningManager() { return planningManager; }
    public VillagerQuestManager getQuestManager() { return questManager; }
    
    @Nullable
    public VillagerAIState getVillagerAIState(@Nullable VillagerEntity villager) {
        return villager != null ? villagerStates.get(villager.getUuidAsString()) : null;
    }
    
    @NotNull
    public Collection<VillagerEntity> getAllTrackedVillagers() {
        List<VillagerEntity> villagers = new ArrayList<>();
        for (String uuid : villagerStates.keySet()) {
            VillagerEntity villager = findVillagerByUuid(uuid);
            if (villager != null) {
                villagers.add(villager);
            }
        }
        return villagers;
    }
    
    /**
     * Get emotional state for a villager (compatibility method)
     */
    @Nullable
    public VillagerEmotionSystem.EmotionalState getVillagerEmotionalState(@Nullable VillagerEntity villager) {
        // Use the VillagerEmotionSystem to get the runtime emotional state
        return VillagerEmotionSystem.getEmotionalState(villager);
    }

    /**
     * Process emotional event for a villager (compatibility method)
     */
    public void processVillagerEmotionalEvent(@Nullable VillagerEntity villager, @NotNull VillagerEmotionSystem.EmotionalEvent event) {
        VillagerEmotionSystem.processEmotionalEvent(villager, event);
    }
    
    /**
     * Analytics and debugging
     */
    @NotNull
    public Map<String, Object> getAIAnalytics() {
        String cacheKey = "ai_analytics";
        Map<String, Object> cached = analyticsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("active_villager_ais", villagerStates.size());
        int worldCount = 0;
        for (ServerWorld world : server.getWorlds()) {
            worldCount++;
        }
        analytics.put("server_worlds", worldCount);
        analytics.put("config", config);
        
        // Add performance metrics
        analytics.put("performance", performanceMetrics.getMetrics());
        
        // Add cache statistics
        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("villager_cache_size", villagerCache.size());
        cacheStats.put("analytics_cache_size", analyticsCache.size());
        cacheStats.put("validation_cache_size", validationCache.size());
        analytics.put("cache_stats", cacheStats);
        
        // Add subsystem analytics
        // Emotion analytics can be derived from VillagerData; no direct manager analytics
        analytics.put("emotions", Map.of("note", "See VillagerData registry for distribution"));
        analytics.put("learning", learningManager.getAnalytics());
        analytics.put("gossip", gossipManager.getAnalytics());
        analytics.put("planning", planningManager.getAnalytics());
        analytics.put("quests", questManager.getAnalytics());
        
        analyticsCache.put(cacheKey, analytics);
        return analytics;
    }
    
    /**
     * Get performance metrics for monitoring
     */
    public Map<String, Object> getPerformanceMetrics() {
        return performanceMetrics.getMetrics();
    }
    
    /**
     * Reset performance metrics
     */
    public void resetPerformanceMetrics() {
        performanceMetrics.reset();
        LOGGER.info("Performance metrics reset");
    }

    /**
     * Adjust loneliness based on nearby villager proximity.
     * This was previously accidentally placed outside the class scope.
     */
    private void updateIsolationEffects(VillagerEntity villager, VillagerEmotionSystem.EmotionalState emotions) {
        if (villager == null || emotions == null || villager.getWorld() == null) return;

        // Use a reasonable detection radius; tweak as needed
        final double radius = 20.0;
        List<VillagerEntity> nearby = villager.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            villager.getBoundingBox().expand(radius),
            v -> v != villager
        );

        if (nearby.isEmpty()) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LONELINESS, 3.0f);
        } else if (nearby.size() > 5) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LONELINESS, -2.0f);
        }
    }
    
    private void processSocialEmotions(VillagerEntity villager, VillagerEmotionSystem.EmotionalState emotions, VillagerData data) {
        // Spouse and family influences
        if (!data.getSpouseId().isEmpty()) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LOVE, 5.0f);
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.LONELINESS, -10.0f);
        }
        
        if (!data.getChildrenIds().isEmpty()) {
            emotions.adjustEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS, 8.0f);
        }
    }
    
    public VillagerEmotionSystem.EmotionalState getEmotionalState(VillagerEntity villager) {
        if (villager == null) return null;
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) return null;
        com.beeny.data.EmotionalState ds = data.getEmotionalState();
        VillagerEmotionSystem.EmotionalState core = new VillagerEmotionSystem.EmotionalState();
        if (ds != null) {
            for (Map.Entry<String, Float> e : ds.getEmotions().entrySet()) {
                try {
                    VillagerEmotionSystem.EmotionType type = VillagerEmotionSystem.EmotionType.valueOf(e.getKey().toUpperCase());
                    core.setEmotion(type, e.getValue());
                } catch (IllegalArgumentException ignore) {}
            }
        }
        return core;
    }
    
    public void processEmotionalEvent(VillagerEntity villager, VillagerEmotionSystem.EmotionalEvent event) {
        if (villager == null || event == null) return;
        
        VillagerEmotionSystem.EmotionalState state = getEmotionalState(villager);
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        if (state == null || data == null) return;
        
        // Apply personality modifiers to emotional reactions
        float personalityMultiplier = getPersonalityEmotionalMultiplier(data.getPersonality().name(), event.primaryEmotion);
        float adjustedImpact = event.intensity * personalityMultiplier;
        
        state.adjustEmotion(event.primaryEmotion, adjustedImpact);

        // Feedback for argument/friendship events
        if (villager.getWorld() instanceof ServerWorld serverWorld) {
            if (event.primaryEmotion == VillagerEmotionSystem.EmotionType.ANGER) {
                // Argument feedback: angry particles, angry sound, name tag
                serverWorld.spawnParticles(ParticleTypes.ANGRY_VILLAGER,
                    villager.getX(), villager.getY() + 2, villager.getZ(),
                    12, 0.5, 0.5, 0.5, 0.1);
                serverWorld.playSound(null, villager.getBlockPos(),
                    SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1.0f, 0.8f);
                // Set temporary name with suffix
                setTemporaryNameWithReset(villager, data.getName(), " (Arguing)", Formatting.RED, 200);
            } else if (event.primaryEmotion == VillagerEmotionSystem.EmotionType.HAPPINESS ||
                       event.primaryEmotion == VillagerEmotionSystem.EmotionType.LOVE) {
                // Friendship feedback: happy/heart particles, positive sound, name tag
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 2, villager.getZ(),
                    10, 0.7, 0.7, 0.7, 0.2);
                serverWorld.spawnParticles(ParticleTypes.HEART,
                    villager.getX(), villager.getY() + 2, villager.getZ(),
                    6, 0.5, 0.5, 0.5, 0.1);
                serverWorld.playSound(null, villager.getBlockPos(),
                    SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 1.0f, 1.2f);
                // Set temporary name with suffix
                setTemporaryNameWithReset(villager, data.getName(), " (Friends)", Formatting.AQUA, 200);
            }
            // Feedback only for nearby players (handled by particles/sounds range)
        }

        // Update legacy happiness based on emotional state
        updateLegacyHappiness(data, state);
    }
    
    private float getPersonalityEmotionalMultiplier(String personality, VillagerEmotionSystem.EmotionType emotionType) {
        if (personality == null || emotionType == null) {
            return 1.0f;
        }
        return switch (personality) {
            case "Cheerful" -> emotionType == VillagerEmotionSystem.EmotionType.HAPPINESS ? 1.5f : 
                             emotionType == VillagerEmotionSystem.EmotionType.ANGER ? 0.5f : 1.0f;
            case "Grumpy" -> emotionType == VillagerEmotionSystem.EmotionType.ANGER ? 1.5f : 
                           emotionType == VillagerEmotionSystem.EmotionType.HAPPINESS ? 0.5f : 1.0f;
            case "Shy" -> emotionType == VillagerEmotionSystem.EmotionType.FEAR ? 1.5f : 
                        emotionType == VillagerEmotionSystem.EmotionType.EXCITEMENT ? 0.5f : 1.0f;
            case "Energetic" -> emotionType == VillagerEmotionSystem.EmotionType.EXCITEMENT ? 1.5f : 
                              emotionType == VillagerEmotionSystem.EmotionType.BOREDOM ? 0.3f : 1.0f;
            default -> 1.0f;
        };
    }
    
    // Schedules temporary name suffix and resets it after delayTicks (server ticks).
    // Debounces per-villager so rapid events refresh the timer.
    private final Map<UUID, Long> nameResetDeadlines = new ConcurrentHashMap<>();
    private final Map<UUID, String> baseNames = new ConcurrentHashMap<>();

    private void setTemporaryNameWithReset(VillagerEntity villager, String baseName, String suffix, Formatting color, int delayTicks) {
        if (!(villager.getWorld() instanceof ServerWorld serverWorld)) return;
        UUID id = villager.getUuid();

        // Store base name once (first time) to avoid stacking suffixes
        baseNames.putIfAbsent(id, baseName);

        // Apply temporary name
        MutableText name = Text.literal(baseName).append(Text.literal(suffix)).formatted(color);
        villager.setCustomName(name);

        // Compute/reset deadline using world time
        long currentTick = serverWorld.getTime();
        long deadline = currentTick + Math.max(1, delayTicks);
        nameResetDeadlines.put(id, deadline);

        // Ensure per-tick reset check is installed
        ensureNameResetTickerInstalled();
    }

    // Install a lightweight per-second ticker using existing scheduler; it runs on server thread via server.execute
    private volatile boolean nameResetTickerInstalled = false;

    private void ensureNameResetTickerInstalled() {
        if (nameResetTickerInstalled) return;
        synchronized (this) {
            if (nameResetTickerInstalled) return;
            // Run every 1 second to minimize overhead
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    server.execute(this::tickNameResets);
                } catch (Exception ignored) {}
            }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
            nameResetTickerInstalled = true;
        }
    }

    // Check deadlines and restore names
    private void tickNameResets() {
        if (nameResetDeadlines.isEmpty()) return;

        // Iterate cached villagers
        for (Map.Entry<String, WeakReference<VillagerEntity>> entry : villagerCache.entrySet()) {
            VillagerEntity v = entry.getValue().get();
            if (v == null || !(v.getWorld() instanceof ServerWorld sw)) continue;

            UUID id = v.getUuid();
            Long deadline = nameResetDeadlines.get(id);
            if (deadline == null) continue;

            long currentTick = sw.getTime();
            if (currentTick >= deadline) {
                // Reset to base name if still available
                String base = baseNames.get(id);
                if (base != null) {
                    v.setCustomName(Text.literal(base));
                } else {
                    // Fallback to entity default name
                    v.setCustomName(null);
                }
                // Clear tracking
                nameResetDeadlines.remove(id);
                baseNames.remove(id);
            }
        }

        // Best-effort cleanup for entries whose villager is no longer cached
        if (!nameResetDeadlines.isEmpty()) {
            nameResetDeadlines.keySet().removeIf(id -> {
                WeakReference<VillagerEntity> ref = villagerCache.get(id.toString());
                return ref == null || ref.get() == null;
            });
        }
    }

    private void updateLegacyHappiness(VillagerData data, VillagerEmotionSystem.EmotionalState state) {
        // Convert emotional state to legacy happiness value
        float happiness = state.getEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS);
        float contentment = state.getEmotion(VillagerEmotionSystem.EmotionType.CONTENTMENT);
        float stress = state.getEmotion(VillagerEmotionSystem.EmotionType.STRESS);
        
        int legacyHappiness = Math.round(happiness + (contentment * 0.5f) + (stress * 0.3f));
        legacyHappiness = Math.max(0, Math.min(100, legacyHappiness));
        
        data.setHappiness(legacyHappiness);
    }
    
    public void cleanupVillager(String uuid) {
        // Clear per-villager emotion state maintained by VillagerEmotionSystem
        VillagerEmotionSystem.clearEmotionalState(uuid);
    }
    
}