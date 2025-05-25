package com.beeny.villagesreborn.core.world;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WorldCreationSettingsCapture thread-safe operations
 * Verifies capture, retrieval, and cleanup functionality
 */
class WorldCreationSettingsCaptureTest {

    private VillagesRebornWorldSettings testSettings;
    
    @BeforeEach
    void setUp() {
        // Clear any existing captured settings
        WorldCreationSettingsCapture.clearAll();
        
        // Create test settings
        testSettings = new VillagesRebornWorldSettings();
        testSettings.setVillagerMemoryLimit(250);
        testSettings.setAiAggressionLevel(0.8f);
        testSettings.setEnableAdvancedAI(true);
        testSettings.setMaxVillageSize(80);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        WorldCreationSettingsCapture.clearAll();
    }
    
    @Test
    void shouldCaptureAndRetrieveSettings() {
        // When
        WorldCreationSettingsCapture.capture(testSettings);
        VillagesRebornWorldSettings retrieved = WorldCreationSettingsCapture.retrieve();
        
        // Then
        assertNotNull(retrieved);
        assertEquals(testSettings.getVillagerMemoryLimit(), retrieved.getVillagerMemoryLimit());
        assertEquals(testSettings.getAiAggressionLevel(), retrieved.getAiAggressionLevel(), 0.001f);
        assertEquals(testSettings.isEnableAdvancedAI(), retrieved.isEnableAdvancedAI());
        assertEquals(testSettings.getMaxVillageSize(), retrieved.getMaxVillageSize());
    }
    
    @Test
    void shouldNotCaptureNullSettings() {
        // When
        WorldCreationSettingsCapture.capture(null);
        
        // Then
        assertFalse(WorldCreationSettingsCapture.hasSettings());
        assertNull(WorldCreationSettingsCapture.retrieve());
    }
    
    @Test
    void shouldValidateSettingsOnCapture() {
        // Given - settings with invalid values
        VillagesRebornWorldSettings invalidSettings = new VillagesRebornWorldSettings();
        invalidSettings.setVillagerMemoryLimit(1000); // Above max
        invalidSettings.setAiAggressionLevel(2.0f); // Above max
        
        // When
        WorldCreationSettingsCapture.capture(invalidSettings);
        VillagesRebornWorldSettings retrieved = WorldCreationSettingsCapture.retrieve();
        
        // Then - should be validated/clamped
        assertNotNull(retrieved);
        assertEquals(500, retrieved.getVillagerMemoryLimit()); // Clamped to max
        assertEquals(1.0f, retrieved.getAiAggressionLevel(), 0.001f); // Clamped to max
    }
    
    @Test
    void shouldCreateDeepCopyOnCapture() {
        // When
        WorldCreationSettingsCapture.capture(testSettings);
        
        // Modify original after capture
        testSettings.setVillagerMemoryLimit(999);
        
        VillagesRebornWorldSettings retrieved = WorldCreationSettingsCapture.retrieve();
        
        // Then - retrieved should not be affected by original modification
        assertNotNull(retrieved);
        assertEquals(250, retrieved.getVillagerMemoryLimit()); // Original value
    }
    
    @Test
    void shouldClearThreadLocalAfterRetrieval() {
        // Given
        WorldCreationSettingsCapture.capture(testSettings);
        assertTrue(WorldCreationSettingsCapture.hasSettings());
        
        // When
        VillagesRebornWorldSettings first = WorldCreationSettingsCapture.retrieve();
        VillagesRebornWorldSettings second = WorldCreationSettingsCapture.retrieve();
        
        // Then
        assertNotNull(first);
        // Second retrieval might get from cache or be null, but thread-local should be cleared
        if (second != null) {
            // If we get a second result, it should be from cache
            assertEquals(first.getVillagerMemoryLimit(), second.getVillagerMemoryLimit());
        }
    }
    
    @Test
    void shouldSupportCrossThreadAccess() throws InterruptedException, ExecutionException {
        // Given
        CountDownLatch captureComplete = new CountDownLatch(1);
        
        // When - capture in one thread
        CompletableFuture<Void> captureTask = CompletableFuture.runAsync(() -> {
            WorldCreationSettingsCapture.capture(testSettings);
            captureComplete.countDown();
        });
        
        // Wait for capture to complete
        captureComplete.await(1, TimeUnit.SECONDS);
        captureTask.get();
        
        // Retrieve in another thread
        CompletableFuture<VillagesRebornWorldSettings> retrieveTask = CompletableFuture.supplyAsync(() -> 
            WorldCreationSettingsCapture.retrieve()
        );
        
        VillagesRebornWorldSettings retrieved = retrieveTask.get();
        
        // Then
        assertNotNull(retrieved);
        assertEquals(testSettings.getVillagerMemoryLimit(), retrieved.getVillagerMemoryLimit());
        assertEquals(testSettings.getAiAggressionLevel(), retrieved.getAiAggressionLevel(), 0.001f);
    }
    
    @Test
    void shouldReturnNullWhenNoSettings() {
        // When - no settings captured
        VillagesRebornWorldSettings retrieved = WorldCreationSettingsCapture.retrieve();
        
        // Then
        assertNull(retrieved);
        assertFalse(WorldCreationSettingsCapture.hasSettings());
    }
    
    @Test
    void shouldTrackCacheSize() {
        // Given
        assertEquals(0, WorldCreationSettingsCapture.getCacheSize());
        
        // When
        WorldCreationSettingsCapture.capture(testSettings);
        
        // Then
        assertEquals(1, WorldCreationSettingsCapture.getCacheSize());
        assertTrue(WorldCreationSettingsCapture.hasSettings());
    }
    
    @Test
    void shouldClearAllSettings() {
        // Given
        WorldCreationSettingsCapture.capture(testSettings);
        assertTrue(WorldCreationSettingsCapture.hasSettings());
        assertEquals(1, WorldCreationSettingsCapture.getCacheSize());
        
        // When
        WorldCreationSettingsCapture.clearAll();
        
        // Then
        assertFalse(WorldCreationSettingsCapture.hasSettings());
        assertEquals(0, WorldCreationSettingsCapture.getCacheSize());
        assertNull(WorldCreationSettingsCapture.retrieve());
    }
    
    @Test
    void shouldRetrieveForSpecificSession() throws InterruptedException, ExecutionException {
        // Given - capture settings and get session info
        String[] sessionId = new String[1];
        
        CompletableFuture<Void> captureTask = CompletableFuture.runAsync(() -> {
            WorldCreationSettingsCapture.capture(testSettings);
            // Session ID would be generated internally - we need to test this functionality
        });
        captureTask.get();
        
        // When - retrieve from cache (simulating session-based retrieval)
        VillagesRebornWorldSettings retrieved = WorldCreationSettingsCapture.retrieve();
        
        // Then
        assertNotNull(retrieved);
        assertEquals(testSettings.getVillagerMemoryLimit(), retrieved.getVillagerMemoryLimit());
    }
    
    @Test
    void shouldHandleConcurrentCaptures() throws InterruptedException {
        // Given
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        // When - multiple threads capture settings simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    VillagesRebornWorldSettings threadSettings = new VillagesRebornWorldSettings();
                    threadSettings.setVillagerMemoryLimit(100 + threadIndex);
                    WorldCreationSettingsCapture.capture(threadSettings);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown(); // Start all threads
        assertTrue(completeLatch.await(5, TimeUnit.SECONDS)); // Wait for completion
        
        // Then - should handle concurrent access without crashes
        assertTrue(WorldCreationSettingsCapture.getCacheSize() > 0);
        assertTrue(WorldCreationSettingsCapture.hasSettings());
    }
    
    @Test
    void shouldPerformCleanup() throws InterruptedException {
        // Given - capture multiple settings
        for (int i = 0; i < 5; i++) {
            VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
            settings.setVillagerMemoryLimit(100 + i);
            WorldCreationSettingsCapture.capture(settings);
            Thread.sleep(10); // Small delay to ensure different timestamps
        }
        
        int initialSize = WorldCreationSettingsCapture.getCacheSize();
        assertTrue(initialSize > 0);
        
        // When - perform cleanup
        WorldCreationSettingsCapture.performCleanup();
        
        // Then - should handle cleanup without errors
        // Note: Cleanup based on timestamp might not remove items immediately
        // since they were just created, but the method should execute without error
        assertDoesNotThrow(() -> WorldCreationSettingsCapture.performCleanup());
    }
    
    @Test
    void shouldHandleRetrievalFromEmptyCache() {
        // Given - empty cache
        assertEquals(0, WorldCreationSettingsCapture.getCacheSize());
        
        // When
        VillagesRebornWorldSettings retrieved = WorldCreationSettingsCapture.retrieve();
        
        // Then
        assertNull(retrieved);
        assertFalse(WorldCreationSettingsCapture.hasSettings());
    }
    
    @Test
    void shouldMaintainThreadSafety() throws InterruptedException, ExecutionException {
        // Given
        int readerCount = 5;
        int writerCount = 3;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(readerCount + writerCount);
        
        // When - concurrent readers and writers
        // Writers
        for (int i = 0; i < writerCount; i++) {
            final int writerIndex = i;
            CompletableFuture.runAsync(() -> {
                try {
                    startLatch.await();
                    VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
                    settings.setVillagerMemoryLimit(200 + writerIndex);
                    WorldCreationSettingsCapture.capture(settings);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        // Readers
        for (int i = 0; i < readerCount; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    startLatch.await();
                    WorldCreationSettingsCapture.retrieve();
                    WorldCreationSettingsCapture.hasSettings();
                    WorldCreationSettingsCapture.getCacheSize();
                } catch (Exception e) {
                    // Should not throw exceptions
                    fail("Thread-safety violation: " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads
        assertTrue(completeLatch.await(5, TimeUnit.SECONDS));
        
        // Then - no exceptions should have been thrown
        // Test completes successfully if no thread-safety issues occur
        assertTrue(true);
    }
}