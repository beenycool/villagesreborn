package com.beeny.villagesreborn.core.util;

import com.beeny.villagesreborn.core.config.WorldSettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manages performance optimizations based on world settings
 * Controls adaptive performance, tick optimization, and resource management
 */
public class PerformanceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceManager.class);
    private static PerformanceManager instance;
    
    private final Map<Object, PerformanceState> worldStates = new ConcurrentHashMap<>();
    private long lastPerformanceCheck = System.currentTimeMillis();
    private static final long PERFORMANCE_CHECK_INTERVAL = 5000; // 5 seconds
    
    private PerformanceManager() {}
    
    public static PerformanceManager getInstance() {
        if (instance == null) {
            synchronized (PerformanceManager.class) {
                if (instance == null) {
                    instance = new PerformanceManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Processes performance optimizations for a world based on settings
     */
    public void processPerformanceOptimizations(Object world) {
        if (world == null) {
            return;
        }
        
        WorldSettingsManager settingsManager = WorldSettingsManager.getInstance();
        PerformanceState state = getOrCreatePerformanceState(world);
        
        // Check if adaptive performance is enabled
        if (settingsManager.isAdaptivePerformanceEnabled(world)) {
            processAdaptivePerformance(world, state);
        }
        
        // Apply tick optimization level
        int tickOptimizationLevel = settingsManager.getTickOptimizationLevel(world);
        applyTickOptimization(state, tickOptimizationLevel);
        
        // Update performance metrics
        updatePerformanceMetrics(state);
        
        LOGGER.debug("Processed performance optimizations for world with optimization level: {}", 
                    tickOptimizationLevel);
    }
    
    /**
     * Processes adaptive performance adjustments
     */
    private void processAdaptivePerformance(Object world, PerformanceState state) {
        long currentTime = System.currentTimeMillis();
        
        // Only check performance periodically
        if (currentTime - lastPerformanceCheck < PERFORMANCE_CHECK_INTERVAL) {
            return;
        }
        
        lastPerformanceCheck = currentTime;
        
        // Measure current performance
        PerformanceMetrics metrics = measurePerformance();
        state.setCurrentMetrics(metrics);
        
        // Adjust performance based on measurements
        if (metrics.getAverageTPS() < 15.0) {
            // Poor performance - increase optimizations
            state.increaseOptimizationLevel();
            LOGGER.info("Poor performance detected (TPS: {}), increasing optimization level to: {}", 
                       metrics.getAverageTPS(), state.getAdaptiveOptimizationLevel());
        } else if (metrics.getAverageTPS() > 19.0 && state.getAdaptiveOptimizationLevel() > 0) {
            // Good performance - can reduce optimizations
            state.decreaseOptimizationLevel();
            LOGGER.info("Good performance detected (TPS: {}), decreasing optimization level to: {}", 
                       metrics.getAverageTPS(), state.getAdaptiveOptimizationLevel());
        }
    }
    
    /**
     * Applies tick optimization based on the optimization level
     */
    private void applyTickOptimization(PerformanceState state, int optimizationLevel) {
        state.setTickOptimizationLevel(optimizationLevel);
        
        switch (optimizationLevel) {
            case 0 -> {
                // No optimization - full processing
                state.setVillagerAITickRate(1); // Every tick
                state.setExpansionCheckRate(1); // Every tick
                state.setMemoryCleanupRate(1200); // Every minute
            }
            case 1 -> {
                // Light optimization
                state.setVillagerAITickRate(2); // Every 2 ticks
                state.setExpansionCheckRate(5); // Every 5 ticks
                state.setMemoryCleanupRate(600); // Every 30 seconds
            }
            case 2 -> {
                // Medium optimization
                state.setVillagerAITickRate(4); // Every 4 ticks
                state.setExpansionCheckRate(10); // Every 10 ticks
                state.setMemoryCleanupRate(400); // Every 20 seconds
            }
            case 3 -> {
                // Heavy optimization
                state.setVillagerAITickRate(8); // Every 8 ticks
                state.setExpansionCheckRate(20); // Every 20 ticks
                state.setMemoryCleanupRate(200); // Every 10 seconds
            }
        }
    }
    
    /**
     * Measures current performance metrics
     */
    private PerformanceMetrics measurePerformance() {
        // This would be implemented with actual performance measurement
        // For now, return mock data
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100.0;
        
        // Mock TPS calculation - in real implementation this would measure actual TPS
        double mockTPS = 20.0 - (memoryUsagePercent / 10.0); // Simulate TPS drop with memory usage
        mockTPS = Math.max(5.0, Math.min(20.0, mockTPS)); // Clamp between 5-20 TPS
        
        return new PerformanceMetrics(mockTPS, memoryUsagePercent, usedMemory);
    }
    
    /**
     * Updates performance metrics for a world
     */
    private void updatePerformanceMetrics(PerformanceState state) {
        state.updateTimestamp();
        
        // Clean up old performance data if needed
        if (state.shouldCleanupMemory()) {
            performMemoryCleanup(state);
        }
    }
    
    /**
     * Performs memory cleanup based on optimization settings
     */
    private void performMemoryCleanup(PerformanceState state) {
        LOGGER.debug("Performing memory cleanup for world");
        
        // This would implement actual memory cleanup
        // For now, just update the cleanup timestamp
        state.updateLastMemoryCleanup();
        
        // Suggest garbage collection if memory usage is high
        PerformanceMetrics metrics = state.getCurrentMetrics();
        if (metrics != null && metrics.getMemoryUsagePercent() > 80.0) {
            System.gc(); // Suggest garbage collection
            LOGGER.info("Suggested garbage collection due to high memory usage: {}%", 
                       metrics.getMemoryUsagePercent());
        }
    }
    
    /**
     * Checks if a specific operation should be processed this tick
     */
    public boolean shouldProcessThisTick(Object world, OperationType operationType, long currentTick) {
        PerformanceState state = worldStates.get(world);
        if (state == null) {
            return true; // Default to processing if no state
        }
        
        return switch (operationType) {
            case VILLAGER_AI -> currentTick % state.getVillagerAITickRate() == 0;
            case VILLAGE_EXPANSION -> currentTick % state.getExpansionCheckRate() == 0;
            case MEMORY_CLEANUP -> currentTick % state.getMemoryCleanupRate() == 0;
        };
    }
    
    /**
     * Gets or creates a performance state for a world
     */
    private PerformanceState getOrCreatePerformanceState(Object world) {
        return worldStates.computeIfAbsent(world, w -> new PerformanceState());
    }
    
    /**
     * Gets the current performance state for a world
     */
    public PerformanceState getPerformanceState(Object world) {
        return worldStates.get(world);
    }
    
    /**
     * Removes performance state for a world (cleanup)
     */
    public void removePerformanceState(Object world) {
        worldStates.remove(world);
    }
    
    /**
     * Clears all performance states (for shutdown, etc.)
     */
    public void clearAllStates() {
        worldStates.clear();
    }
    
    /**
     * Represents the current performance state of a world
     */
    public static class PerformanceState {
        private int tickOptimizationLevel = 1;
        private int adaptiveOptimizationLevel = 0;
        private int villagerAITickRate = 2;
        private int expansionCheckRate = 5;
        private int memoryCleanupRate = 600;
        private PerformanceMetrics currentMetrics;
        private long lastMemoryCleanup = System.currentTimeMillis();
        private long lastUpdate = System.currentTimeMillis();
        
        // Getters and setters
        public int getTickOptimizationLevel() { return tickOptimizationLevel; }
        public void setTickOptimizationLevel(int tickOptimizationLevel) { this.tickOptimizationLevel = tickOptimizationLevel; }
        public int getAdaptiveOptimizationLevel() { return adaptiveOptimizationLevel; }
        public void increaseOptimizationLevel() { this.adaptiveOptimizationLevel = Math.min(3, adaptiveOptimizationLevel + 1); }
        public void decreaseOptimizationLevel() { this.adaptiveOptimizationLevel = Math.max(0, adaptiveOptimizationLevel - 1); }
        public int getVillagerAITickRate() { return villagerAITickRate; }
        public void setVillagerAITickRate(int villagerAITickRate) { this.villagerAITickRate = villagerAITickRate; }
        public int getExpansionCheckRate() { return expansionCheckRate; }
        public void setExpansionCheckRate(int expansionCheckRate) { this.expansionCheckRate = expansionCheckRate; }
        public int getMemoryCleanupRate() { return memoryCleanupRate; }
        public void setMemoryCleanupRate(int memoryCleanupRate) { this.memoryCleanupRate = memoryCleanupRate; }
        public PerformanceMetrics getCurrentMetrics() { return currentMetrics; }
        public void setCurrentMetrics(PerformanceMetrics currentMetrics) { this.currentMetrics = currentMetrics; }
        public long getLastMemoryCleanup() { return lastMemoryCleanup; }
        public void updateLastMemoryCleanup() { this.lastMemoryCleanup = System.currentTimeMillis(); }
        public long getLastUpdate() { return lastUpdate; }
        public void updateTimestamp() { this.lastUpdate = System.currentTimeMillis(); }
        
        public boolean shouldCleanupMemory() {
            return (System.currentTimeMillis() - lastMemoryCleanup) > (memoryCleanupRate * 50L); // Convert ticks to ms
        }
    }
    
    /**
     * Represents performance metrics
     */
    public static class PerformanceMetrics {
        private final double averageTPS;
        private final double memoryUsagePercent;
        private final long memoryUsageBytes;
        private final long timestamp;
        
        public PerformanceMetrics(double averageTPS, double memoryUsagePercent, long memoryUsageBytes) {
            this.averageTPS = averageTPS;
            this.memoryUsagePercent = memoryUsagePercent;
            this.memoryUsageBytes = memoryUsageBytes;
            this.timestamp = System.currentTimeMillis();
        }
        
        public double getAverageTPS() { return averageTPS; }
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public long getMemoryUsageBytes() { return memoryUsageBytes; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Enum for different operation types that can be optimized
     */
    public enum OperationType {
        VILLAGER_AI,
        VILLAGE_EXPANSION,
        MEMORY_CLEANUP
    }
} 