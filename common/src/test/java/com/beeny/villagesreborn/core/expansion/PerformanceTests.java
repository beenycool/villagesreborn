package com.beeny.villagesreborn.core.expansion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic performance tests for the optimized village expansion system.
 */
class PerformanceTests {
    
    private VillageExpansionManager expansionManager;
    private Object mockWorld;
    
    @BeforeEach
    void setUp() {
        expansionManager = new VillageExpansionManager();
        mockWorld = new Object();
    }
    
    @Test
    @DisplayName("Should provide performance statistics")
    void testPerformanceStats() {
        // When: Getting performance statistics
        Map<String, Object> stats = expansionManager.getPerformanceStats();
        
        // Then: Statistics should be available
        assertNotNull(stats, "Performance stats should be available");
        assertTrue(stats.containsKey("cacheHits"), "Should track cache hits");
        assertTrue(stats.containsKey("cacheMisses"), "Should track cache misses");
        assertTrue(stats.containsKey("totalExpansionChecks"), "Should track total checks");
    }
    
    @Test
    @DisplayName("Should handle multiple expansion requests efficiently")
    void testMultipleExpansions() {
        // Given: Multiple expansion requests
        int requestCount = 50;
        
        long startTime = System.currentTimeMillis();
        
        // When: Processing expansion requests
        for (int i = 0; i < requestCount; i++) {
            UUID villageId = UUID.randomUUID();
            expansionManager.processVillageExpansion(mockWorld, villageId, 30, "plains");
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Then: Should complete in reasonable time
        assertTrue(processingTime < 5000, 
                  "Processing should complete quickly, took: " + processingTime + "ms");
    }
    
    @Test
    @DisplayName("Should demonstrate cache effectiveness")
    void testCacheEffectiveness() {
        // Given: Repeated requests for same biome
        String biome = "forest";
        
        // When: Processing multiple requests for same biome
        for (int i = 0; i < 10; i++) {
            UUID villageId = UUID.randomUUID();
            expansionManager.processVillageExpansion(mockWorld, villageId, 25, biome);
        }
        
        // Then: Cache should have been utilized
        Map<String, Object> stats = expansionManager.getPerformanceStats();
        Long cacheHits = (Long) stats.get("cacheHits");
        
        assertNotNull(cacheHits, "Cache hits should be tracked");
        assertTrue(cacheHits > 0, "Cache should have been used");
    }
} 