package com.beeny.villagesreborn.platform.fabric.world;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.VillagesRebornWorldSettingsExtensions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for spawn biome persistence functionality
 */
class SpawnBiomePersistenceTest {
    
    private NbtCompound testNbt;
    private SpawnBiomeChoiceData testChoiceData;
    
    @BeforeEach
    void setUp() {
        testNbt = new NbtCompound();
        testChoiceData = new SpawnBiomeChoiceData(
            BiomeKeys.PLAINS,
            System.currentTimeMillis()
        );
        VillagesRebornWorldSettingsExtensions.resetForTest();
    }
    
    @Test
    void testWriteSpawnBiomeChoiceToNbt() {
        // Given: A spawn biome choice
        VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(testChoiceData);
        
        // When: Writing to NBT
        VillagesRebornWorldSettingsExtensions.writeToNbt(testNbt);
        
        // Then: NBT should contain the biome data
        assertTrue(testNbt.contains("spawn_biome_namespace"));
        assertTrue(testNbt.contains("spawn_biome_path"));
        assertTrue(testNbt.contains("spawn_biome_selection_time"));
        
        assertEquals("minecraft", testNbt.getString("spawn_biome_namespace"));
        assertEquals("plains", testNbt.getString("spawn_biome_path"));
        assertEquals(testChoiceData.getSelectionTimestamp(), 
                    testNbt.getLong("spawn_biome_selection_time"));
    }
    
    @Test
    void testReadSpawnBiomeChoiceFromNbt() {
        // Given: NBT with spawn biome data
        testNbt.putString("spawn_biome_namespace", "minecraft");
        testNbt.putString("spawn_biome_path", "forest");
        testNbt.putLong("spawn_biome_selection_time", 12345L);
        
        // When: Reading from NBT
        VillagesRebornWorldSettingsExtensions.readFromNbt(testNbt);
        
        // Then: The spawn biome choice should be loaded
        SpawnBiomeChoiceData loaded = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice();
        assertNotNull(loaded);
        assertEquals(BiomeKeys.FOREST, loaded.getBiomeKey());
        assertEquals(12345L, loaded.getSelectionTimestamp());
    }
    
    @Test
    void testReadFromEmptyNbt() {
        // Given: Empty NBT
        // When: Reading from NBT
        VillagesRebornWorldSettingsExtensions.readFromNbt(testNbt);
        
        // Then: No spawn biome choice should be loaded
        assertNull(VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice());
    }
    
    @Test
    void testReadFromIncompleteNbt() {
        // Given: NBT with only partial data
        testNbt.putString("spawn_biome_namespace", "minecraft");
        // Missing path
        
        // When: Reading from NBT
        VillagesRebornWorldSettingsExtensions.readFromNbt(testNbt);
        
        // Then: No spawn biome choice should be loaded
        assertNull(VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice());
    }
    
    @Test
    void testMigrationWithoutTimestamp() {
        // Given: NBT with old format (no timestamp)
        testNbt.putString("spawn_biome_namespace", "minecraft");
        testNbt.putString("spawn_biome_path", "taiga");
        
        // When: Reading from NBT
        VillagesRebornWorldSettingsExtensions.readFromNbt(testNbt);
        
        // Then: A default timestamp should be assigned
        SpawnBiomeChoiceData loaded = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice();
        assertNotNull(loaded);
        assertEquals(BiomeKeys.TAIGA, loaded.getBiomeKey());
        assertTrue(loaded.getSelectionTimestamp() > 0);
    }
    
    @Test
    void testWriteWithNullChoice() {
        // Given: No spawn biome choice set
        // When: Writing to NBT
        VillagesRebornWorldSettingsExtensions.writeToNbt(testNbt);
        
        // Then: NBT should remain empty
        assertFalse(testNbt.contains("spawn_biome_namespace"));
        assertFalse(testNbt.contains("spawn_biome_path"));
    }
    
    @Test
    void testPersistenceRoundTrip() {
        // Given: A spawn biome choice
        RegistryKey<Biome> originalBiome = BiomeKeys.DESERT;
        long originalTimestamp = System.currentTimeMillis();
        SpawnBiomeChoiceData originalChoice = new SpawnBiomeChoiceData(originalBiome, originalTimestamp);
        
        VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(originalChoice);
        
        // When: Writing and then reading
        VillagesRebornWorldSettingsExtensions.writeToNbt(testNbt);
        VillagesRebornWorldSettingsExtensions.resetForTest();
        VillagesRebornWorldSettingsExtensions.readFromNbt(testNbt);
        
        // Then: The loaded choice should match the original
        SpawnBiomeChoiceData loaded = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice();
        assertNotNull(loaded);
        assertEquals(originalBiome, loaded.getBiomeKey());
        assertEquals(originalTimestamp, loaded.getSelectionTimestamp());
    }
    
    @Test
    void testCustomBiome() {
        // Given: A custom biome choice
        RegistryKey<Biome> customBiome = RegistryKey.of(RegistryKeys.BIOME, 
            Identifier.of("custom_mod", "custom_biome"));
        SpawnBiomeChoiceData customChoice = new SpawnBiomeChoiceData(customBiome, 67890L);
        
        VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(customChoice);
        
        // When: Writing and reading
        VillagesRebornWorldSettingsExtensions.writeToNbt(testNbt);
        VillagesRebornWorldSettingsExtensions.resetForTest();
        VillagesRebornWorldSettingsExtensions.readFromNbt(testNbt);
        
        // Then: The custom biome should be preserved
        SpawnBiomeChoiceData loaded = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice();
        assertNotNull(loaded);
        assertEquals(customBiome, loaded.getBiomeKey());
        assertEquals(67890L, loaded.getSelectionTimestamp());
    }
}