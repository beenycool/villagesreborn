package com.beeny.villagesreborn.platform.fabric.spawn.managers;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.storage.PlayerSpawnBiomeStorage;
import com.beeny.villagesreborn.platform.fabric.spawn.storage.WorldSpawnBiomeStorage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpawnBiomeStorageManager
 */
class SpawnBiomeStorageManagerTest {
    
    @Mock
    private ServerWorld mockWorld;
    
    @Mock
    private ServerPlayerEntity mockPlayer;
    
    private SpawnBiomeStorageManager manager;
    private SpawnBiomeChoiceData testChoice;
    private RegistryKey<World> testWorldKey;
    private RegistryKey<Biome> testBiomeKey;
    private UUID testPlayerId;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Reset singleton
        SpawnBiomeStorageManager.resetForTest();
        manager = SpawnBiomeStorageManager.getInstance();
        
        // Create test data
        testBiomeKey = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains"));
        testChoice = new SpawnBiomeChoiceData(testBiomeKey, System.currentTimeMillis());
        testWorldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", "overworld"));
        testPlayerId = UUID.randomUUID();
        
        // Setup mocks
        when(mockWorld.getRegistryKey()).thenReturn(testWorldKey);
        when(mockPlayer.getUuid()).thenReturn(testPlayerId);
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
    void testGetWorldStorage() {
        WorldSpawnBiomeStorage storage = manager.getWorldStorage(mockWorld);
        
        assertNotNull(storage);
        assertEquals(mockWorld, storage.getWorld());
        
        // Should return same instance for same world
        WorldSpawnBiomeStorage storage2 = manager.getWorldStorage(mockWorld);
        assertSame(storage, storage2);
        
        assertEquals(1, manager.getWorldStorageCount());
    }
    
    @Test
    void testGetPlayerStorageWithNullPlayer() {
        assertThrows(IllegalArgumentException.class, () -> manager.getPlayerStorage(null));
    }
    
    @Test
    void testGetPlayerStorage() {
        PlayerSpawnBiomeStorage storage = manager.getPlayerStorage(mockPlayer);
        
        assertNotNull(storage);
        assertEquals(mockPlayer, storage.getPlayer());
        
        // Should return same instance for same player
        PlayerSpawnBiomeStorage storage2 = manager.getPlayerStorage(mockPlayer);
        assertSame(storage, storage2);
        
        assertEquals(1, manager.getPlayerStorageCount());
    }
    
    @Test
    void testSetWorldSpawnBiomeWithNullWorld() {
        assertThrows(IllegalArgumentException.class, () -> manager.setWorldSpawnBiome(null, testChoice));
    }
    
    @Test
    void testSetWorldSpawnBiomeWithNullChoice() {
        assertThrows(IllegalArgumentException.class, () -> manager.setWorldSpawnBiome(mockWorld, null));
    }
    
    @Test
    void testGetWorldSpawnBiomeWithNullWorld() {
        Optional<SpawnBiomeChoiceData> result = manager.getWorldSpawnBiome(null);
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
        assertThrows(IllegalArgumentException.class, () -> manager.setPlayerBiomePreference(null, testChoice));
    }
    
    @Test
    void testSetPlayerBiomePreferenceWithNullChoice() {
        assertThrows(IllegalArgumentException.class, () -> manager.setPlayerBiomePreference(mockPlayer, null));
    }
    
    @Test
    void testGetPlayerBiomePreferenceWithNullPlayer() {
        Optional<SpawnBiomeChoiceData> result = manager.getPlayerBiomePreference(null);
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
        // Create a storage instance first
        manager.getWorldStorage(mockWorld);
        assertEquals(1, manager.getWorldStorageCount());
        
        // Clear it
        manager.clearWorldData(testWorldKey);
        assertEquals(0, manager.getWorldStorageCount());
        
        // Should not throw with null
        assertDoesNotThrow(() -> manager.clearWorldData(null));
    }
    
    @Test
    void testClearPlayerData() {
        // Create a storage instance first
        manager.getPlayerStorage(mockPlayer);
        assertEquals(1, manager.getPlayerStorageCount());
        
        // Clear it
        manager.clearPlayerData(testPlayerId);
        assertEquals(0, manager.getPlayerStorageCount());
        
        // Should not throw with null
        assertDoesNotThrow(() -> manager.clearPlayerData(null));
    }
    
    @Test
    void testClearAllData() {
        // Create some storage instances
        manager.getWorldStorage(mockWorld);
        manager.getPlayerStorage(mockPlayer);
        
        assertEquals(1, manager.getWorldStorageCount());
        assertEquals(1, manager.getPlayerStorageCount());
        
        // Clear all
        manager.clearAllData();
        
        assertEquals(0, manager.getWorldStorageCount());
        assertEquals(0, manager.getPlayerStorageCount());
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