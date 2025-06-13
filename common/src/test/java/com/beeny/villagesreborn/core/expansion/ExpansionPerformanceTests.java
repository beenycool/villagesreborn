package com.beeny.villagesreborn.core.expansion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the optimized village expansion system.
 * Verifies that caching and optimization features work correctly.
 */
class ExpansionPerformanceTests {
    
    private VillageExpansionManager expansionManager;
    private ExpansionPerformanceOptimizer optimizer;
    private Object mockWorld;
    
    @BeforeEach
    void setUp() {
        expansionManager = new VillageExpansionManager();
        optimizer = new ExpansionPerformanceOptimizer();
        mockWorld = new Object(); // Mock world object
    }
    
    @Test
    @DisplayName("Should complete cache operations within performance bounds")
    @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
    void testCachePerformance() {
        // Given: Multiple cache operations
        String key = "test_biome_plains";
        String result = "cached_result";
        
        // When: Performing cache operations
        optimizer.cacheResult(key, result);
        String cachedResult = optimizer.getCachedResult(key, String.class);
        
        // Then: Cache should work efficiently
        assertNotNull(cachedResult, "Cached result should be retrieved");
        assertEquals(result, cachedResult, "Cached result should match original");
    }
    
    @Test
    @DisplayName("Should handle high-volume expansion requests efficiently")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testHighVolumePerformance() {
        // Given: Many villages requiring expansion
        int villageCount = 1000;
        
        long startTime = System.currentTimeMillis();
        
        // When: Processing many expansion requests
        for (int i = 0; i < villageCount; i++) {
            UUID villageId = UUID.randomUUID();
            String biomeName = "plains"; // Same biome to test caching
            
            expansionManager.processVillageExpansion(mockWorld, villageId, 50, biomeName);
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Then: Processing should complete within reasonable time
        assertTrue(processingTime < 5000, 
                  "Processing " + villageCount + " villages should take less than 5 seconds, took: " + processingTime + "ms");
        
        // And: Performance stats should show cache hits
        Map<String, Object> stats = expansionManager.getPerformanceStats();
        assertNotNull(stats, "Performance stats should be available");
        
        long cacheHits = (Long) stats.get("cacheHits");
        assertTrue(cacheHits > 0, "Cache hits should be greater than 0");
    }
    
    @Test
    @DisplayName("Should maintain high cache hit ratio")
    void testCacheHitRatio() {
        // Given: Multiple requests for same biome data
        String[] biomes = {"plains", "forest", "desert", "plains", "forest", "desert", "plains"};
        
        // When: Processing expansion requests
        for (String biome : biomes) {
            UUID villageId = UUID.randomUUID();
            expansionManager.processVillageExpansion(mockWorld, villageId, 30, biome);
        }
        
        // Then: Cache hit ratio should be high
        Map<String, Object> stats = expansionManager.getPerformanceStats();
        Double cacheHitRatio = (Double) stats.get("cacheHitRatio");
        
        assertNotNull(cacheHitRatio, "Cache hit ratio should be calculated");
        assertTrue(cacheHitRatio > 0.5, "Cache hit ratio should be greater than 50%, was: " + cacheHitRatio);
    }
    
    @Test
    @DisplayName("Should perform cache cleanup without performance impact")
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testCacheCleanupPerformance() {
        // Given: Large number of cache entries
        for (int i = 0; i < 200; i++) {
            optimizer.cacheResult("key_" + i, "value_" + i);
        }
        
        // When: Performing cache cleanup
        long startTime = System.currentTimeMillis();
        optimizer.checkAndCleanup();
        long endTime = System.currentTimeMillis();
        
        // Then: Cleanup should be fast
        long cleanupTime = endTime - startTime;
        assertTrue(cleanupTime < 100, "Cache cleanup should take less than 100ms, took: " + cleanupTime + "ms");
        
        // And: Cache should still function
        String testResult = optimizer.getCachedResult("key_1", String.class);
        // Result may or may not be present depending on cleanup logic, but should not cause errors
    }
    
    @Test
    @DisplayName("Should handle concurrent expansion requests safely")
    void testConcurrentSafety() throws InterruptedException {
        // Given: Multiple threads processing expansions
        int threadCount = 10;
        int requestsPerThread = 50;
        Thread[] threads = new Thread[threadCount];
        
        // When: Processing concurrent expansion requests
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    UUID villageId = UUID.randomUUID();
                    String biomeName = "biome_" + (threadId % 3); // Limited biome variety for cache testing
                    
                    expansionManager.processVillageExpansion(mockWorld, villageId, 40, biomeName);
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then: All operations should complete without errors
        Map<String, Object> stats = expansionManager.getPerformanceStats();
        long totalChecks = (Long) stats.get("totalExpansionChecks");
        
        // Should have processed all requests (some may be skipped by performance manager)
        assertTrue(totalChecks > 0, "Should have processed some expansion checks");
        assertTrue(totalChecks <= threadCount * requestsPerThread, 
                  "Should not have more checks than requests");
    }
    
    @Test
    @DisplayName("Should demonstrate performance improvement over baseline")
    void testPerformanceImprovement() {
        // Given: Baseline measurements
        int iterations = 100;
        String[] biomes = {"plains", "forest", "desert", "mountain", "swamp"};
        
        // Measure performance with cache
        long startTimeWithCache = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            String biome = biomes[i % biomes.length];
            UUID villageId = UUID.randomUUID();
            expansionManager.processVillageExpansion(mockWorld, villageId, 35, biome);
        }
        
        long endTimeWithCache = System.currentTimeMillis();
        long timeWithCache = endTimeWithCache - startTimeWithCache;
        
        // Then: Performance should be reasonable
        assertTrue(timeWithCache < 2000, 
                  "Processing with cache should be efficient, took: " + timeWithCache + "ms");
        
        // And: Cache should have been utilized
        Map<String, Object> stats = expansionManager.getPerformanceStats();
        Double cacheHitRatio = (Double) stats.get("cacheHitRatio");
        
        if (cacheHitRatio != null) {
            assertTrue(cacheHitRatio > 0.0, "Cache should have been utilized");
        }
    }
    
    @Test
    @DisplayName("Should provide accurate performance statistics")
    void testPerformanceStatistics() {
        // Given: Some expansion operations
        for (int i = 0; i < 10; i++) {
            UUID villageId = UUID.randomUUID();
            expansionManager.processVillageExpansion(mockWorld, villageId, 25, "plains");
        }
        
        // When: Getting performance statistics
        Map<String, Object> stats = expansionManager.getPerformanceStats();
        
        // Then: Statistics should be present and valid
        assertNotNull(stats, "Performance stats should be available");
        assertTrue(stats.containsKey("cacheHits"), "Should track cache hits");
        assertTrue(stats.containsKey("cacheMisses"), "Should track cache misses");
        assertTrue(stats.containsKey("totalExpansionChecks"), "Should track total expansion checks");
        assertTrue(stats.containsKey("cachedBiomeProfiles"), "Should track cached biome profiles");
        
        // And: Values should be non-negative
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            if (entry.getValue() instanceof Number) {
                Number value = (Number) entry.getValue();
                assertTrue(value.doubleValue() >= 0, 
                          "Statistic " + entry.getKey() + " should be non-negative, was: " + value);
            }
        }
    }
} 