package com.beeny.ai.core;

import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages AI subsystems with proper resource management and lifecycle control.
 * Replaces the god object pattern with a focused subsystem coordinator.
 */
public class AISubsystemManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AISubsystemManager.class);
    
    private final MinecraftServer server;
    private final List<AISubsystem> subsystems;
    private final Map<String, Long> lastUpdateTimes;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService updateExecutor;
    
    // Resource limits to prevent memory leaks
    private static final int MAX_TRACKED_VILLAGERS = 10000;
    private static final long CLEANUP_INTERVAL_MS = 300000; // 5 minutes
    private static final long STALE_VILLAGER_THRESHOLD_MS = 3600000; // 1 hour
    
    // Performance tracking
    private final AtomicLong totalUpdates = new AtomicLong(0);
    private final AtomicLong totalUpdateTime = new AtomicLong(0);
    private final Map<String, AtomicLong> subsystemUpdateCounts = new ConcurrentHashMap<>();
    
    // Tracked villagers with weak references to prevent memory leaks
    private final Map<String, VillagerTracker> trackedVillagers = new ConcurrentHashMap<>();
    
    private static class VillagerTracker {
        private final java.lang.ref.WeakReference<VillagerEntity> villagerRef;
        private final long creationTime;
        private volatile long lastAccessTime;
        private final Set<String> activeSubsystems = ConcurrentHashMap.newKeySet();
        
        public VillagerTracker(VillagerEntity villager) {
            this.villagerRef = new java.lang.ref.WeakReference<>(villager);
            this.creationTime = System.currentTimeMillis();
            this.lastAccessTime = creationTime;
        }
        
        @Nullable
        public VillagerEntity getVillager() {
            VillagerEntity villager = villagerRef.get();
            if (villager != null) {
                lastAccessTime = System.currentTimeMillis();
            }
            return villager;
        }
        
        public boolean isStale() {
            return (System.currentTimeMillis() - lastAccessTime) > STALE_VILLAGER_THRESHOLD_MS ||
                   villagerRef.get() == null;
        }
        
        public void addActiveSubsystem(String subsystemName) {
            activeSubsystems.add(subsystemName);
        }
        
        public void removeActiveSubsystem(String subsystemName) {
            activeSubsystems.remove(subsystemName);
        }
        
        public boolean hasActiveSubsystems() {
            return !activeSubsystems.isEmpty();
        }
    }
    
    public AISubsystemManager(@NotNull MinecraftServer server) {
        this.server = server;
        this.subsystems = new ArrayList<>();
        this.lastUpdateTimes = new ConcurrentHashMap<>();
        
        // Create thread pools with reasonable limits
        int threadPoolSize = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.updateExecutor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "AISubsystem-Worker");
            t.setDaemon(true);
            return t;
        });
        
        LOGGER.info("AISubsystemManager initialized with {} worker threads", threadPoolSize);
    }
    
    /**
     * Register a subsystem for management
     */
    public void registerSubsystem(@NotNull AISubsystem subsystem) {
        if (subsystems.contains(subsystem)) {
            LOGGER.warn("Subsystem {} already registered", subsystem.getSubsystemName());
            return;
        }
        
        subsystems.add(subsystem);
        subsystems.sort(Comparator.comparingInt(AISubsystem::getPriority));
        subsystemUpdateCounts.put(subsystem.getSubsystemName(), new AtomicLong(0));
        
        LOGGER.info("Registered AI subsystem: {} (priority: {})", 
                   subsystem.getSubsystemName(), subsystem.getPriority());
    }
    
    /**
     * Start the subsystem manager
     */
    public void start() {
        // Start cleanup scheduler
        scheduler.scheduleAtFixedRate(this::performMaintenance, 
                                    CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        // Start performance reporting
        scheduler.scheduleAtFixedRate(this::reportPerformanceStats, 
                                    60000, 60000, TimeUnit.MILLISECONDS);
        
        LOGGER.info("AISubsystemManager started with {} subsystems", subsystems.size());
    }
    
    /**
     * Initialize AI for a villager across all subsystems
     */
    public void initializeVillager(@NotNull VillagerEntity villager) {
        if (trackedVillagers.size() >= MAX_TRACKED_VILLAGERS) {
            LOGGER.warn("Maximum tracked villagers reached ({}), cleaning up stale entries", 
                       MAX_TRACKED_VILLAGERS);
            cleanupStaleVillagers();
        }
        
        String uuid = villager.getUuidAsString();
        VillagerTracker tracker = trackedVillagers.computeIfAbsent(uuid, k -> new VillagerTracker(villager));
        
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            LOGGER.warn("Villager {} has no VillagerData attachment, skipping AI initialization", uuid);
            return;
        }
        
        // Initialize subsystems in priority order
        for (AISubsystem subsystem : subsystems) {
            if (!subsystem.isEnabled()) continue;
            
            try {
                subsystem.initializeVillager(villager, data);
                tracker.addActiveSubsystem(subsystem.getSubsystemName());
                LOGGER.debug("Initialized {} for villager {}", subsystem.getSubsystemName(), uuid);
            } catch (Exception e) {
                LOGGER.error("Failed to initialize {} for villager {}", 
                           subsystem.getSubsystemName(), uuid, e);
            }
        }
    }
    
    /**
     * Update AI for a villager using efficient scheduling
     */
    public void updateVillager(@NotNull VillagerEntity villager) {
        String uuid = villager.getUuidAsString();
        VillagerTracker tracker = trackedVillagers.get(uuid);
        
        if (tracker == null) {
            initializeVillager(villager);
            tracker = trackedVillagers.get(uuid);
        }
        
        if (tracker == null) return;
        
        long currentTime = System.currentTimeMillis();
        
        // Submit update tasks for subsystems that need updates
        List<CompletableFuture<Void>> updateTasks = new ArrayList<>();
        
        for (AISubsystem subsystem : subsystems) {
            if (!subsystem.isEnabled() || !subsystem.needsUpdate(villager)) continue;
            
            String subsystemKey = uuid + ":" + subsystem.getSubsystemName();
            Long lastUpdate = lastUpdateTimes.get(subsystemKey);
            
            if (lastUpdate == null || (currentTime - lastUpdate) >= subsystem.getUpdateInterval()) {
                CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                    long startTime = System.nanoTime();
                    try {
                        subsystem.updateVillager(villager);
                        lastUpdateTimes.put(subsystemKey, currentTime);
                        subsystemUpdateCounts.get(subsystem.getSubsystemName()).incrementAndGet();
                    } catch (Exception e) {
                        LOGGER.error("Error updating {} for villager {}", 
                                   subsystem.getSubsystemName(), uuid, e);
                    } finally {
                        long duration = System.nanoTime() - startTime;
                        totalUpdateTime.addAndGet(duration);
                        totalUpdates.incrementAndGet();
                    }
                }, updateExecutor);
                
                updateTasks.add(task);
            }
        }
        
        // Don't wait for completion to avoid blocking the main thread
        if (!updateTasks.isEmpty()) {
            LOGGER.debug("Submitted {} update tasks for villager {}", updateTasks.size(), uuid);
        }
    }
    
    /**
     * Clean up AI state for a villager
     */
    public void cleanupVillager(@NotNull String villagerUuid) {
        VillagerTracker tracker = trackedVillagers.remove(villagerUuid);
        if (tracker == null) return;
        
        // Cleanup in all subsystems
        for (AISubsystem subsystem : subsystems) {
            try {
                subsystem.cleanupVillager(villagerUuid);
                LOGGER.debug("Cleaned up {} for villager {}", subsystem.getSubsystemName(), villagerUuid);
            } catch (Exception e) {
                LOGGER.error("Error cleaning up {} for villager {}", 
                           subsystem.getSubsystemName(), villagerUuid, e);
            }
        }
        
        // Remove from update tracking
        for (AISubsystem subsystem : subsystems) {
            String subsystemKey = villagerUuid + ":" + subsystem.getSubsystemName();
            lastUpdateTimes.remove(subsystemKey);
        }
        
        LOGGER.debug("Cleaned up AI state for villager {}", villagerUuid);
    }
    
    /**
     * Perform maintenance tasks
     */
    private void performMaintenance() {
        try {
            cleanupStaleVillagers();
            
            // Run subsystem maintenance
            for (AISubsystem subsystem : subsystems) {
                if (!subsystem.isEnabled()) continue;
                
                try {
                    subsystem.performMaintenance();
                } catch (Exception e) {
                    LOGGER.error("Error in maintenance for {}", subsystem.getSubsystemName(), e);
                }
            }
            
            LOGGER.debug("Performed maintenance - {} tracked villagers remain", trackedVillagers.size());
        } catch (Exception e) {
            LOGGER.error("Error during maintenance", e);
        }
    }
    
    /**
     * Clean up stale villager references
     */
    private void cleanupStaleVillagers() {
        int cleaned = 0;
        Iterator<Map.Entry<String, VillagerTracker>> iterator = trackedVillagers.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, VillagerTracker> entry = iterator.next();
            VillagerTracker tracker = entry.getValue();
            
            if (tracker.isStale()) {
                String uuid = entry.getKey();
                iterator.remove();
                
                // Clean up related data
                for (AISubsystem subsystem : subsystems) {
                    String subsystemKey = uuid + ":" + subsystem.getSubsystemName();
                    lastUpdateTimes.remove(subsystemKey);
                }
                
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            LOGGER.info("Cleaned up {} stale villager trackers", cleaned);
        }
    }
    
    /**
     * Report performance statistics
     */
    private void reportPerformanceStats() {
        long updates = totalUpdates.get();
        long totalTime = totalUpdateTime.get();
        
        if (updates > 0) {
            double avgUpdateTime = (totalTime / 1_000_000.0) / updates; // Convert to milliseconds
            LOGGER.info("AI Performance: {} updates, avg {:.2f}ms per update, {} tracked villagers", 
                       updates, avgUpdateTime, trackedVillagers.size());
            
            // Log subsystem-specific stats
            for (AISubsystem subsystem : subsystems) {
                long count = subsystemUpdateCounts.get(subsystem.getSubsystemName()).get();
                if (count > 0) {
                    LOGGER.debug("{}: {} updates", subsystem.getSubsystemName(), count);
                }
            }
        }
    }
    
    /**
     * Get comprehensive analytics
     */
    @NotNull
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("tracked_villagers", trackedVillagers.size());
        analytics.put("total_updates", totalUpdates.get());
        analytics.put("avg_update_time_ms", totalUpdates.get() > 0 ? 
                     (totalUpdateTime.get() / 1_000_000.0) / totalUpdates.get() : 0.0);
        
        // Subsystem analytics
        Map<String, Object> subsystemAnalytics = new HashMap<>();
        for (AISubsystem subsystem : subsystems) {
            Map<String, Object> subsystemData = new HashMap<>();
            subsystemData.put("enabled", subsystem.isEnabled());
            subsystemData.put("priority", subsystem.getPriority());
            subsystemData.put("update_interval_ms", subsystem.getUpdateInterval());
            subsystemData.put("update_count", subsystemUpdateCounts.get(subsystem.getSubsystemName()).get());
            
            try {
                subsystemData.putAll(subsystem.getAnalytics());
            } catch (Exception e) {
                subsystemData.put("analytics_error", e.getMessage());
            }
            
            subsystemAnalytics.put(subsystem.getSubsystemName(), subsystemData);
        }
        analytics.put("subsystems", subsystemAnalytics);
        
        return analytics;
    }
    
    /**
     * Shutdown the manager and all subsystems
     */
    public void shutdown() {
        LOGGER.info("Shutting down AISubsystemManager...");
        
        // Shutdown subsystems in reverse priority order
        List<AISubsystem> reversedSubsystems = new ArrayList<>(subsystems);
        Collections.reverse(reversedSubsystems);
        
        for (AISubsystem subsystem : reversedSubsystems) {
            try {
                subsystem.shutdown();
                LOGGER.debug("Shut down subsystem: {}", subsystem.getSubsystemName());
            } catch (Exception e) {
                LOGGER.error("Error shutting down subsystem {}", subsystem.getSubsystemName(), e);
            }
        }
        
        // Shutdown executors
        scheduler.shutdown();
        updateExecutor.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!updateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                updateExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            updateExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear all state
        trackedVillagers.clear();
        lastUpdateTimes.clear();
        subsystemUpdateCounts.clear();
        
        LOGGER.info("AISubsystemManager shutdown complete");
    }
    
    /**
     * Get all registered subsystems
     */
    @NotNull
    public List<AISubsystem> getSubsystems() {
        return new ArrayList<>(subsystems);
    }
    
    /**
     * Get server instance
     */
    @NotNull
    public MinecraftServer getServer() {
        return server;
    }
}