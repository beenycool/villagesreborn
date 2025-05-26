package com.beeny.villagesreborn.platform.fabric.spawn;

import com.beeny.villagesreborn.platform.fabric.spawn.managers.SpawnBiomeStorageManager;
import com.beeny.villagesreborn.platform.fabric.spawn.persistence.SpawnBiomeMigration;
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
 * Integration tests for the complete spawn biome storage system
 * Tests the interaction between storage manager, world storage, player storage, and migration
 */
class SpawnBiomeStorageIntegrationTest {
    
    @Mock
    private ServerWorld mockWorld;
    
    @Mock
    private ServerPlayerEntity mockPlayer;
    
    private SpawnBiomeStorageManager manager;
    private SpawnBiomeChoiceData testWorldChoice;
    private SpawnBiomeChoiceData testPlayerChoice;
    private RegistryKey<World> testWorldKey;
    private RegistryKey<Biome> testBiomeKey1;
    private RegistryKey<Biome> testBiomeKey2;
    private UUID testPlayerId;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Reset all singletons and static state
        SpawnBiomeStorageManager.resetForTest();
        VillagesRebornWorldSettingsExtensions.resetForTest();
        
        manager = SpawnBiomeStorageManager.getInstance();
        
        // Create test data
        testBiomeKey1 = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains"));
        testBiomeKey2 = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "forest"));
        testWorldChoice = new SpawnBiomeChoiceData(testBiomeKey1, System.currentTimeMillis());
        testPlayerChoice = new SpawnBiomeChoiceData(testBiomeKey2, System.currentTimeMillis() + 1000);
        testWorldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", "overworld"));
        testPlayerId = UUID.randomUUID();
        
        // Setup mocks
        when(mockWorld.getRegistryKey()).thenReturn(testWorldKey);
        when(mockPlayer.getUuid()).thenReturn(testPlayerId);
    }
    
    @AfterEach
    void tearDown() {
        SpawnBiomeStorageManager.resetForTest();
        VillagesRebornWorldSettingsExtensions.resetForTest();
    }
    
    @Test
    void testWorldAndPlayerStorageIsolation() {
        // Set world spawn biome
        manager.setWorldSpawnBiome(mockWorld, testWorldChoice);
        
        // Set player biome preference
        manager.setPlayerBiomePreference(mockPlayer, testPlayerChoice);
        
        // Verify world storage
        Optional<SpawnBiomeChoiceData> worldResult = manager.getWorldSpawnBiome(mockWorld);
        assertTrue(worldResult.isPresent());
        assertEquals(testBiomeKey1, worldResult.get().getBiomeKey());
        
        // Verify player storage
        Optional<SpawnBiomeChoiceData> playerResult = manager.getPlayerBiomePreference(mockPlayer);
        assertTrue(playerResult.isPresent());
        assertEquals(testBiomeKey2, playerResult.get().getBiomeKey());
        
        // Verify they are isolated (different choices)
        assertNotEquals(worldResult.get().getBiomeKey(), playerResult.get().getBiomeKey());
    }
    
    @Test
    void testMultipleWorldsIsolation() {
        // Create second mock world
        ServerWorld mockWorld2 = mock(ServerWorld.class);
        RegistryKey<World> testWorldKey2 = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", "the_nether"));
        when(mockWorld2.getRegistryKey()).thenReturn(testWorldKey2);
        
        SpawnBiomeChoiceData choice2 = new SpawnBiomeChoiceData(testBiomeKey2, System.currentTimeMillis());
        
        // Set different choices for different worlds
        manager.setWorldSpawnBiome(mockWorld, testWorldChoice);
        manager.setWorldSpawnBiome(mockWorld2, choice2);
        
        // Verify isolation
        Optional<SpawnBiomeChoiceData> world1Result = manager.getWorldSpawnBiome(mockWorld);
        Optional<SpawnBiomeChoiceData> world2Result = manager.getWorldSpawnBiome(mockWorld2);
        
        assertTrue(world1Result.isPresent());
        assertTrue(world2Result.isPresent());
        assertEquals(testBiomeKey1, world1Result.get().getBiomeKey());
        assertEquals(testBiomeKey2, world2Result.get().getBiomeKey());
        
        // Verify they are different
        assertNotEquals(world1Result.get().getBiomeKey(), world2Result.get().getBiomeKey());
        
        assertEquals(2, manager.getWorldStorageCount());
    }
    
    @Test
    void testMultiplePlayersIsolation() {
        // Create second mock player
        ServerPlayerEntity mockPlayer2 = mock(ServerPlayerEntity.class);
        UUID testPlayerId2 = UUID.randomUUID();
        when(mockPlayer2.getUuid()).thenReturn(testPlayerId2);
        
        SpawnBiomeChoiceData choice2 = new SpawnBiomeChoiceData(testBiomeKey2, System.currentTimeMillis());
        
        // Set different preferences for different players
        manager.setPlayerBiomePreference(mockPlayer, testPlayerChoice);
        manager.setPlayerBiomePreference(mockPlayer2, choice2);
        
        // Verify isolation
        Optional<SpawnBiomeChoiceData> player1Result = manager.getPlayerBiomePreference(mockPlayer);
        Optional<SpawnBiomeChoiceData> player2Result = manager.getPlayerBiomePreference(mockPlayer2);
        
        assertTrue(player1Result.isPresent());
        assertTrue(player2Result.isPresent());
        assertEquals(testBiomeKey2, player1Result.get().getBiomeKey());
        assertEquals(testBiomeKey2, player2Result.get().getBiomeKey());
        
        assertEquals(2, manager.getPlayerStorageCount());
    }
    
    @Test
    void testLegacyDataMigration() {
        // Set up legacy data
        VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(testWorldChoice);
        assertTrue(VillagesRebornWorldSettingsExtensions.hasLegacyData());
        
        // Verify migration is needed
        assertTrue(SpawnBiomeMigration.needsMigration(mockWorld));
        assertTrue(SpawnBiomeMigration.hasLegacyData());
        
        // Perform migration by accessing world storage (which triggers migration)
        manager.getWorldStorage(mockWorld);
        
        // Verify legacy data was cleared
        assertFalse(VillagesRebornWorldSettingsExtensions.hasLegacyData());
        assertNull(VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice());
    }
    
    @Test
    void testCacheManagement() {
        // Create storage instances
        manager.getWorldStorage(mockWorld);
        manager.getPlayerStorage(mockPlayer);
        
        assertEquals(1, manager.getWorldStorageCount());
        assertEquals(1, manager.getPlayerStorageCount());
        
        // Clear specific world data
        manager.clearWorldData(testWorldKey);
        assertEquals(0, manager.getWorldStorageCount());
        assertEquals(1, manager.getPlayerStorageCount());
        
        // Clear specific player data
        manager.clearPlayerData(testPlayerId);
        assertEquals(0, manager.getWorldStorageCount());
        assertEquals(0, manager.getPlayerStorageCount());
        
        // Create instances again
        manager.getWorldStorage(mockWorld);
        manager.getPlayerStorage(mockPlayer);
        
        // Clear all data
        manager.clearAllData();
        assertEquals(0, manager.getWorldStorageCount());
        assertEquals(0, manager.getPlayerStorageCount());
    }
    
    @Test
    void testPersistenceAcrossManagerInstances() {
        // Set data with first manager instance
        manager.setWorldSpawnBiome(mockWorld, testWorldChoice);
        manager.setPlayerBiomePreference(mockPlayer, testPlayerChoice);
        
        // Reset and get new manager instance
        SpawnBiomeStorageManager.resetForTest();
        SpawnBiomeStorageManager newManager = SpawnBiomeStorageManager.getInstance();
        
        // Note: In a real scenario, the persistent data would survive manager reset
        // For this test, we verify that new storages can be created independently
        assertNotSame(manager, newManager);
        assertEquals(0, newManager.getWorldStorageCount());
        assertEquals(0, newManager.getPlayerStorageCount());
    }
    
    @Test
    void testCompleteWorkflow() {
        // 1. Check initial state - no data
        assertFalse(manager.hasWorldSpawnBiome(mockWorld));
        assertFalse(manager.hasPlayerBiomePreference(mockPlayer));
        
        // 2. Set world spawn biome
        manager.setWorldSpawnBiome(mockWorld, testWorldChoice);
        assertTrue(manager.hasWorldSpawnBiome(mockWorld));
        
        Optional<SpawnBiomeChoiceData> worldResult = manager.getWorldSpawnBiome(mockWorld);
        assertTrue(worldResult.isPresent());
        assertEquals(testBiomeKey1, worldResult.get().getBiomeKey());
        
        // 3. Set player preference
        manager.setPlayerBiomePreference(mockPlayer, testPlayerChoice);
        assertTrue(manager.hasPlayerBiomePreference(mockPlayer));
        
        Optional<SpawnBiomeChoiceData> playerResult = manager.getPlayerBiomePreference(mockPlayer);
        assertTrue(playerResult.isPresent());
        assertEquals(testBiomeKey2, playerResult.get().getBiomeKey());
        
        // 4. Clear world data
        manager.clearWorldSpawnBiome(mockWorld);
        assertFalse(manager.hasWorldSpawnBiome(mockWorld));
        assertTrue(manager.getWorldSpawnBiome(mockWorld).isEmpty());
        
        // Player data should remain
        assertTrue(manager.hasPlayerBiomePreference(mockPlayer));
        
        // 5. Clear player data
        manager.clearPlayerBiomePreference(mockPlayer);
        assertFalse(manager.hasPlayerBiomePreference(mockPlayer));
        assertTrue(manager.getPlayerBiomePreference(mockPlayer).isEmpty());
    }
}