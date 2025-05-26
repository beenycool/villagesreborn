package com.beeny.villagesreborn.platform.fabric.spawn.persistence;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SpawnBiomeNBTHandler
 */
class SpawnBiomeNBTHandlerTest {
    
    private SpawnBiomeChoiceData testChoice;
    private RegistryKey<Biome> testBiomeKey;
    private long testTimestamp;
    
    @BeforeEach
    void setUp() {
        testBiomeKey = RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "plains"));
        testTimestamp = System.currentTimeMillis();
        testChoice = new SpawnBiomeChoiceData(testBiomeKey, testTimestamp);
    }
    
    @Test
    void testSerializeToNbt() {
        NbtCompound nbt = SpawnBiomeNBTHandler.serializeToNbt(testChoice);
        
        assertNotNull(nbt);
        assertEquals("minecraft", nbt.getString("biome_namespace"));
        assertEquals("plains", nbt.getString("biome_path"));
        assertEquals(testTimestamp, nbt.getLong("selection_timestamp"));
        assertEquals("2.0", nbt.getString("data_version"));
    }
    
    @Test
    void testSerializeToNbtWithNullChoice() {
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.serializeToNbt(null));
    }
    
    @Test
    void testDeserializeFromNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("biome_namespace", "minecraft");
        nbt.putString("biome_path", "plains");
        nbt.putLong("selection_timestamp", testTimestamp);
        nbt.putString("data_version", "2.0");
        
        SpawnBiomeChoiceData result = SpawnBiomeNBTHandler.deserializeFromNbt(nbt);
        
        assertNotNull(result);
        assertEquals(testBiomeKey, result.getBiomeKey());
        assertEquals(testTimestamp, result.getSelectionTimestamp());
    }
    
    @Test
    void testDeserializeFromNbtWithMissingData() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("biome_namespace", "minecraft");
        // Missing biome_path
        
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.deserializeFromNbt(nbt));
    }
    
    @Test
    void testDeserializeFromNbtWithEmptyStrings() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("biome_namespace", "");
        nbt.putString("biome_path", "plains");
        
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.deserializeFromNbt(nbt));
    }
    
    @Test
    void testDeserializeFromNbtWithInvalidTimestamp() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("biome_namespace", "minecraft");
        nbt.putString("biome_path", "plains");
        nbt.putLong("selection_timestamp", -1); // Invalid timestamp
        
        SpawnBiomeChoiceData result = SpawnBiomeNBTHandler.deserializeFromNbt(nbt);
        
        // Should use current timestamp for backward compatibility
        assertTrue(result.getSelectionTimestamp() > 0);
    }
    
    @Test
    void testSerializeToMap() {
        Map<String, Object> map = SpawnBiomeNBTHandler.serializeToMap(testChoice);
        
        assertNotNull(map);
        assertEquals("minecraft", map.get("biome_namespace"));
        assertEquals("plains", map.get("biome_path"));
        assertEquals(testTimestamp, map.get("selection_timestamp"));
        assertEquals("2.0", map.get("data_version"));
    }
    
    @Test
    void testSerializeToMapWithNullChoice() {
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.serializeToMap(null));
    }
    
    @Test
    void testDeserializeFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("biome_namespace", "minecraft");
        map.put("biome_path", "plains");
        map.put("selection_timestamp", testTimestamp);
        map.put("data_version", "2.0");
        
        SpawnBiomeChoiceData result = SpawnBiomeNBTHandler.deserializeFromMap(map);
        
        assertNotNull(result);
        assertEquals(testBiomeKey, result.getBiomeKey());
        assertEquals(testTimestamp, result.getSelectionTimestamp());
    }
    
    @Test
    void testDeserializeFromMapWithMissingData() {
        Map<String, Object> map = new HashMap<>();
        map.put("biome_namespace", "minecraft");
        // Missing biome_path
        
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.deserializeFromMap(map));
    }
    
    @Test
    void testDeserializeFromMapWithNullValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("biome_namespace", null);
        map.put("biome_path", "plains");
        
        assertThrows(IllegalArgumentException.class, () -> SpawnBiomeNBTHandler.deserializeFromMap(map));
    }
    
    @Test
    void testDeserializeFromMapWithInvalidTimestamp() {
        Map<String, Object> map = new HashMap<>();
        map.put("biome_namespace", "minecraft");
        map.put("biome_path", "plains");
        map.put("selection_timestamp", "invalid"); // Not a number
        
        SpawnBiomeChoiceData result = SpawnBiomeNBTHandler.deserializeFromMap(map);
        
        // Should use current timestamp when timestamp is invalid
        assertTrue(result.getSelectionTimestamp() > 0);
    }
    
    @Test
    void testIsValidNbt() {
        NbtCompound validNbt = new NbtCompound();
        validNbt.putString("biome_namespace", "minecraft");
        validNbt.putString("biome_path", "plains");
        
        assertTrue(SpawnBiomeNBTHandler.isValidNbt(validNbt));
        
        NbtCompound invalidNbt = new NbtCompound();
        invalidNbt.putString("biome_namespace", "minecraft");
        // Missing biome_path
        
        assertFalse(SpawnBiomeNBTHandler.isValidNbt(invalidNbt));
        assertFalse(SpawnBiomeNBTHandler.isValidNbt(null));
    }
    
    @Test
    void testIsValidMap() {
        Map<String, Object> validMap = new HashMap<>();
        validMap.put("biome_namespace", "minecraft");
        validMap.put("biome_path", "plains");
        
        assertTrue(SpawnBiomeNBTHandler.isValidMap(validMap));
        
        Map<String, Object> invalidMap = new HashMap<>();
        invalidMap.put("biome_namespace", "minecraft");
        // Missing biome_path
        
        assertFalse(SpawnBiomeNBTHandler.isValidMap(invalidMap));
        assertFalse(SpawnBiomeNBTHandler.isValidMap(null));
        
        Map<String, Object> wrongTypeMap = new HashMap<>();
        wrongTypeMap.put("biome_namespace", 123); // Should be string
        wrongTypeMap.put("biome_path", "plains");
        
        assertFalse(SpawnBiomeNBTHandler.isValidMap(wrongTypeMap));
    }
    
    @Test
    void testRoundTripNbt() {
        // Test serialization and deserialization preserve data
        NbtCompound nbt = SpawnBiomeNBTHandler.serializeToNbt(testChoice);
        SpawnBiomeChoiceData roundTrip = SpawnBiomeNBTHandler.deserializeFromNbt(nbt);
        
        assertEquals(testChoice.getBiomeKey(), roundTrip.getBiomeKey());
        assertEquals(testChoice.getSelectionTimestamp(), roundTrip.getSelectionTimestamp());
    }
    
    @Test
    void testRoundTripMap() {
        // Test serialization and deserialization preserve data
        Map<String, Object> map = SpawnBiomeNBTHandler.serializeToMap(testChoice);
        SpawnBiomeChoiceData roundTrip = SpawnBiomeNBTHandler.deserializeFromMap(map);
        
        assertEquals(testChoice.getBiomeKey(), roundTrip.getBiomeKey());
        assertEquals(testChoice.getSelectionTimestamp(), roundTrip.getSelectionTimestamp());
    }
}