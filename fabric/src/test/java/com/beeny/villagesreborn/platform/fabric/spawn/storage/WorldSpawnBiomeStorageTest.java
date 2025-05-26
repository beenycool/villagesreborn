package com.beeny.villagesreborn.platform.fabric.spawn.storage;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldDataPersistent;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorldSpawnBiomeStorage
 */
class WorldSpawnBiomeStorageTest {
    
    @Mock
    private ServerWorld mockWorld;
    
    @Mock
    private VillagesRebornWorldDataPersistent mockPersistent;
    
    private WorldSpawnBiomeStorage storage;
    private SpawnBiomeChoiceData testChoice;
    private RegistryKey<World> testWorldKey;
    private RegistryKey<Biome> testBiomeKey;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test data
        testBiomeKey = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains"));
        testChoice = new SpawnBiomeChoiceData(testBiomeKey, System.currentTimeMillis());
        testWorldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", "overworld"));
        
        // Setup mocks
        when(mockWorld.getRegistryKey()).thenReturn(testWorldKey);
        
        storage = new WorldSpawnBiomeStorage(mockWorld);
    }
    
    @Test
    void testConstructorWithNullWorld() {
        assertThrows(IllegalArgumentException.class, () -> new WorldSpawnBiomeStorage(null));
    }
    
    @Test
    void testGetSpawnBiomeChoiceWhenEmpty() {
        // Mock the persistent data to return empty settings
        mockStatic(VillagesRebornWorldDataPersistent.class);
        when(VillagesRebornWorldDataPersistent.get(mockWorld)).thenReturn(mockPersistent);
        when(mockPersistent.hasSettings()).thenReturn(false);
        
        Optional<SpawnBiomeChoiceData> result = storage.getSpawnBiomeChoice(testWorldKey);
        
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetSpawnBiomeChoiceWhenPresent() {
        // Create test settings with spawn biome data
        VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
        Map<String, Object> customData = new HashMap<>();
        Map<String, Object> spawnBiomeMap = new HashMap<>();
        spawnBiomeMap.put("biome_namespace", "minecraft");
        spawnBiomeMap.put("biome_path", "plains");
        spawnBiomeMap.put("selection_timestamp", testChoice.getSelectionTimestamp());
        spawnBiomeMap.put("data_version", "2.0");
        customData.put("spawn_biome_choices", spawnBiomeMap);
        settings.setCustomData(customData);
        
        // Mock the persistent data
        try (var mockedStatic = mockStatic(VillagesRebornWorldDataPersistent.class)) {
            mockedStatic.when(() -> VillagesRebornWorldDataPersistent.get(mockWorld)).thenReturn(mockPersistent);
            when(mockPersistent.hasSettings()).thenReturn(true);
            when(mockPersistent.getSettings()).thenReturn(settings);
            
            Optional<SpawnBiomeChoiceData> result = storage.getSpawnBiomeChoice(testWorldKey);
            
            assertTrue(result.isPresent());
            assertEquals(testBiomeKey, result.get().getBiomeKey());
            assertEquals(testChoice.getSelectionTimestamp(), result.get().getSelectionTimestamp());
        }
    }
    
    @Test
    void testSetSpawnBiomeChoice() {
        VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
        
        try (var mockedStatic = mockStatic(VillagesRebornWorldDataPersistent.class)) {
            mockedStatic.when(() -> VillagesRebornWorldDataPersistent.get(mockWorld)).thenReturn(mockPersistent);
            when(mockPersistent.getSettings()).thenReturn(settings);
            
            storage.setSpawnBiomeChoice(testWorldKey, testChoice);
            
            verify(mockPersistent).setSettings(settings);
            verify(mockPersistent).markDirty();
            
            // Verify the data was stored correctly
            Map<String, Object> customData = settings.getCustomData();
            assertTrue(customData.containsKey("spawn_biome_choices"));
            assertTrue(customData.containsKey("spawn_biome_last_updated"));
        }
    }
    
    @Test
    void testSetSpawnBiomeChoiceWithNullChoice() {
        assertThrows(IllegalArgumentException.class, () -> storage.setSpawnBiomeChoice(testWorldKey, null));
    }
    
    @Test
    void testClearSpawnBiomeChoice() {
        VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
        Map<String, Object> customData = new HashMap<>();
        customData.put("spawn_biome_choices", new HashMap<>());
        customData.put("spawn_biome_last_updated", System.currentTimeMillis());
        settings.setCustomData(customData);
        
        try (var mockedStatic = mockStatic(VillagesRebornWorldDataPersistent.class)) {
            mockedStatic.when(() -> VillagesRebornWorldDataPersistent.get(mockWorld)).thenReturn(mockPersistent);
            when(mockPersistent.hasSettings()).thenReturn(true);
            when(mockPersistent.getSettings()).thenReturn(settings);
            
            storage.clearSpawnBiomeChoice(testWorldKey);
            
            verify(mockPersistent).setSettings(settings);
            verify(mockPersistent).markDirty();
            
            // Verify the data was removed
            Map<String, Object> updatedCustomData = settings.getCustomData();
            assertFalse(updatedCustomData.containsKey("spawn_biome_choices"));
            assertFalse(updatedCustomData.containsKey("spawn_biome_last_updated"));
        }
    }
    
    @Test
    void testHasSpawnBiomeChoice() {
        try (var mockedStatic = mockStatic(VillagesRebornWorldDataPersistent.class)) {
            mockedStatic.when(() -> VillagesRebornWorldDataPersistent.get(mockWorld)).thenReturn(mockPersistent);
            when(mockPersistent.hasSettings()).thenReturn(false);
            
            assertFalse(storage.hasSpawnBiomeChoice(testWorldKey));
        }
    }
    
    @Test
    void testPlayerBiomePreferenceMethods() {
        // World storage should not handle player preferences
        assertTrue(storage.getPlayerBiomePreference(java.util.UUID.randomUUID()).isEmpty());
        
        // These should not throw but should log warnings
        assertDoesNotThrow(() -> storage.setPlayerBiomePreference(java.util.UUID.randomUUID(), testChoice));
    }
    
    @Test
    void testGetWorld() {
        assertEquals(mockWorld, storage.getWorld());
    }
}