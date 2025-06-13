package com.beeny.villagesreborn.core.expansion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance optimizer for village expansion system.
 * Provides caching, batching, and memory management optimizations.
 */
public class ExpansionPerformanceOptimizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpansionPerformanceOptimizer.class);
    
    // Cache for frequently accessed data
    private final Map<String, Object> operationCache = new ConcurrentHashMap<>();
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    // Performance tracking
    private final AtomicLong totalOperations = new AtomicLong(0);
    private volatile long lastCleanup = System.currentTimeMillis();
    
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutes
    private static final int MAX_CACHE_SIZE = 1000;
    
    /**
     * Caches the result of an expensive operation.
     */
    public void cacheResult(String key, Object result) {
        if (operationCache.size() >= MAX_CACHE_SIZE) {
            performCacheCleanup();
        }
        operationCache.put(key, result);
    }
    
    /**
     * Gets a cached result if available.
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedResult(String key, Class<T> type) {
        Object cached = operationCache.get(key);
        if (cached != null && type.isInstance(cached)) {
            cacheHits.incrementAndGet();
            return type.cast(cached);
        }
        cacheMisses.incrementAndGet();
        return null;
    }
    
    /**
     * Checks if cache cleanup is needed and performs it.
     */
    public void checkAndCleanup() {
        totalOperations.incrementAndGet();
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup > CLEANUP_INTERVAL) {
            performCacheCleanup();
            lastCleanup = currentTime;
        }
    }
    
    /**
     * Performs cache cleanup to free memory.
     */
    private void performCacheCleanup() {
        if (operationCache.size() > MAX_CACHE_SIZE / 2) {
            // Remove roughly half the cache entries
            List<String> keysToRemove = new ArrayList<>();
            int count = 0;
            
            for (String key : operationCache.keySet()) {
                if (count++ >= operationCache.size() / 2) {
                    break;
                }
                keysToRemove.add(key);
            }
            
            keysToRemove.forEach(operationCache::remove);
            LOGGER.debug("Cache cleanup completed, removed {} entries", keysToRemove.size());
        }
    }
    
    /**
     * Gets performance statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", operationCache.size());
        stats.put("cacheHits", cacheHits.get());
        stats.put("cacheMisses", cacheMisses.get());
        stats.put("totalOperations", totalOperations.get());
        
        long totalCacheOperations = cacheHits.get() + cacheMisses.get();
        if (totalCacheOperations > 0) {
            stats.put("cacheHitRatio", (double)cacheHits.get() / totalCacheOperations);
        } else {
            stats.put("cacheHitRatio", 0.0);
        }
        
        return stats;
    }
    
    /**
     * Clears all cached data.
     */
    public void clearCache() {
        operationCache.clear();
        LOGGER.debug("Performance optimizer cache cleared");
    }
} 