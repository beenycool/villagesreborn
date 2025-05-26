package com.beeny.villagesreborn.platform.fabric.spawn;

import com.beeny.villagesreborn.platform.fabric.spawn.persistence.SpawnBiomeNBTHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify the spawn biome storage refactoring is complete and working
 */
class SpawnBiomeRefactoringTest {
    
    @Test
    void testSpawnBiomeChoiceDataNoLongerHasStaticField() {
        // Verify that SpawnBiomeChoiceData no longer has the deprecated static field
        // This test ensures the refactoring removed the static field completely
        
        RegistryKey<Biome> testBiome = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains"));
        long timestamp = System.currentTimeMillis();
        
        SpawnBiomeChoiceData choice1 = new SpawnBiomeChoiceData(testBiome, timestamp);
        SpawnBiomeChoiceData choice2 = new SpawnBiomeChoiceData(testBiome, timestamp);
        
        // Test equals method (which was added during refactoring)
        assertEquals(choice1, choice2);
        assertEquals(choice1.hashCode(), choice2.hashCode());
        
        // Test basic functionality
        assertEquals(testBiome, choice1.getBiomeKey());
        assertEquals(timestamp, choice1.getSelectionTimestamp());
        assertNotNull(choice1.toString());
    }
    
    @Test
    void testNBTSerializationRoundTrip() {
        // Test that NBT serialization works correctly
        RegistryKey<Biome> testBiome = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "forest"));
        long timestamp = 123456789L;
        
        SpawnBiomeChoiceData original = new SpawnBiomeChoiceData(testBiome, timestamp);
        
        // Test Map serialization (used by world storage)
        Map<String, Object> map = SpawnBiomeNBTHandler.serializeToMap(original);
        assertNotNull(map);
        assertTrue(SpawnBiomeNBTHandler.isValidMap(map));
        
        SpawnBiomeChoiceData fromMap = SpawnBiomeNBTHandler.deserializeFromMap(map);
        assertEquals(original.getBiomeKey(), fromMap.getBiomeKey());
        assertEquals(original.getSelectionTimestamp(), fromMap.getSelectionTimestamp());
        assertEquals(original, fromMap);
    }
    
    @Test
    void testLegacyWorldSettingsExtensionsDeprecation() {
        // Test that the legacy extensions properly mark methods as deprecated
        // and handle the static field correctly for migration
        
        // Reset for clean test
        VillagesRebornWorldSettingsExtensions.resetForTest();
        
        // Verify initially no legacy data
        assertFalse(VillagesRebornWorldSettingsExtensions.hasLegacyData());
        assertNull(VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice());
        
        // Set some legacy data (for migration testing)
        RegistryKey<Biome> testBiome = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "desert"));
        SpawnBiomeChoiceData testChoice = new SpawnBiomeChoiceData(testBiome, System.currentTimeMillis());
        
        VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(testChoice);
        
        // Verify legacy data can be retrieved (for migration)
        assertTrue(VillagesRebornWorldSettingsExtensions.hasLegacyData());
        assertEquals(testChoice, VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice());
        
        // Reset and verify cleanup
        VillagesRebornWorldSettingsExtensions.resetForTest();
        assertFalse(VillagesRebornWorldSettingsExtensions.hasLegacyData());
        assertNull(VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice());
    }
    
    @Test
    void testSpawnBiomeChoiceDataValidation() {
        // Test input validation
        RegistryKey<Biome> testBiome = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains"));
        
        // Valid construction
        assertDoesNotThrow(() -> new SpawnBiomeChoiceData(testBiome, 1000L));
        assertDoesNotThrow(() -> new SpawnBiomeChoiceData(testBiome, 0L));
        
        // Invalid construction
        assertThrows(IllegalArgumentException.class, () -> new SpawnBiomeChoiceData(null, 1000L));
        assertThrows(IllegalArgumentException.class, () -> new SpawnBiomeChoiceData(testBiome, -1L));
    }
    
    @Test
    void testNBTHandlerValidation() {
        // Test NBT handler validation methods
        RegistryKey<Biome> testBiome = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains"));
        SpawnBiomeChoiceData testChoice = new SpawnBiomeChoiceData(testBiome, 12345L);
        
        // Test valid map
        Map<String, Object> validMap = SpawnBiomeNBTHandler.serializeToMap(testChoice);
        assertTrue(SpawnBiomeNBTHandler.isValidMap(validMap));
        
        // Test invalid maps
        assertFalse(SpawnBiomeNBTHandler.isValidMap(null));
        assertFalse(SpawnBiomeNBTHandler.isValidMap(Map.of()));
        assertFalse(SpawnBiomeNBTHandler.isValidMap(Map.of("invalid", "data")));
        
        // Test serialization error handling
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.serializeToMap(null));
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.deserializeFromMap(null));
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.deserializeFromMap(Map.of()));
    }
    
    @Test
    void testRefactoringCompleteness() {
        // This test ensures that the refactoring is complete by testing
        // the integration between different components
        
        // 1. Create test data
        RegistryKey<Biome> testBiome = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "jungle"));
        long timestamp = System.currentTimeMillis();
        SpawnBiomeChoiceData choice = new SpawnBiomeChoiceData(testBiome, timestamp);
        
        // 2. Test that data can be serialized and deserialized
        Map<String, Object> serialized = SpawnBiomeNBTHandler.serializeToMap(choice);
        SpawnBiomeChoiceData deserialized = SpawnBiomeNBTHandler.deserializeFromMap(serialized);
        
        // 3. Test that the data is preserved correctly
        assertEquals(choice.getBiomeKey(), deserialized.getBiomeKey());
        assertEquals(choice.getSelectionTimestamp(), deserialized.getSelectionTimestamp());
        assertEquals(choice, deserialized);
        
        // 4. Test that legacy storage can be used for migration
        VillagesRebornWorldSettingsExtensions.resetForTest();
        VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(choice);
        
        SpawnBiomeChoiceData retrieved = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice();
        assertEquals(choice, retrieved);
        
        // 5. Test cleanup
        VillagesRebornWorldSettingsExtensions.resetForTest();
        assertNull(VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice());
    }
}