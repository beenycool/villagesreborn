package com.beeny.villagesreborn.platform.fabric.spawn.managers;

import com.beeny.villagesreborn.platform.fabric.testing.IntProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified unit tests for SpawnBiomeStorageManager without complex mocking
 */
class SpawnBiomeStorageManagerSimpleTest {
    
    private SpawnBiomeStorageManager manager;
    
    @BeforeAll
    static void setUpClass() {
        // Initialize IntProvider to prevent NoClassDefFoundError
        IntProvider.reset();
    }
    
    @BeforeEach
    void setUp() {
        // Reset singleton
        SpawnBiomeStorageManager.resetForTest();
        manager = SpawnBiomeStorageManager.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        SpawnBiomeStorageManager.resetForTest();
    }
    
    @Test
    void testSingletonPattern() {
        SpawnBiomeStorageManager instance1 = SpawnBiomeStorageManager.getInstance();
        SpawnBiomeStorageManager instance2 = SpawnBiomeStorageManager.getInstance();
        
        assertSame(instance1, instance2);
    }
    
    @Test
    void testGetWorldStorageWithNullWorld() {
        assertThrows(IllegalArgumentException.class, () -> manager.getWorldStorage(null));
    }
    
    @Test
    void testGetPlayerStorageWithNullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> manager.getPlayerStorage(null));
    }
    
    @Test
    void testSetWorldSpawnBiomeWithNullWorld() {
        assertThrows(IllegalArgumentException.class, () -> manager.setWorldSpawnBiome(null, null));
    }
    
    @Test
    void testGetWorldSpawnBiomeWithNullWorld() {
        var result = manager.getWorldSpawnBiome(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testHasWorldSpawnBiomeWithNullWorld() {
        assertFalse(manager.hasWorldSpawnBiome(null));
    }
    
    @Test
    void testClearWorldSpawnBiomeWithNullWorld() {
        // Should not throw
        assertDoesNotThrow(() -> manager.clearWorldSpawnBiome(null));
    }
    
    @Test
    void testSetPlayerBiomePreferenceWithNullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> manager.setPlayerBiomePreference(null, null));
    }
    
    @Test
    void testGetPlayerBiomePreferenceWithNullPlayer() {
        var result = manager.getPlayerBiomePreference(null);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testHasPlayerBiomePreferenceWithNullPlayer() {
        assertFalse(manager.hasPlayerBiomePreference(null));
    }
    
    @Test
    void testClearPlayerBiomePreferenceWithNullPlayer() {
        // Should not throw
        assertDoesNotThrow(() -> manager.clearPlayerBiomePreference(null));
    }
    
    @Test
    void testClearWorldData() {
        // Should not throw
        assertDoesNotThrow(() -> manager.clearWorldData(null));
    }
    
    @Test
    void testClearPlayerData() {
        // Should not throw
        assertDoesNotThrow(() -> manager.clearPlayerData(null));
    }
    
    @Test
    void testClearAllData() {
        // Should not throw
        assertDoesNotThrow(() -> manager.clearAllData());
    }
    
    @Test
    void testThreadSafety() throws InterruptedException {
        // Test that multiple threads can safely access the singleton
        Thread[] threads = new Thread[10];
        SpawnBiomeStorageManager[] instances = new SpawnBiomeStorageManager[10];
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> instances[index] = SpawnBiomeStorageManager.getInstance());
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // All instances should be the same
        for (int i = 1; i < instances.length; i++) {
            assertSame(instances[0], instances[i]);
        }
    }
}