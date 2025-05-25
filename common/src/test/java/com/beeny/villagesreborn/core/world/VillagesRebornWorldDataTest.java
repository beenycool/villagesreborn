package com.beeny.villagesreborn.core.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VillagesRebornWorldData persistence functionality
 * Verifies save/load operations and world data management
 */
@ExtendWith(MockitoExtension.class)
class VillagesRebornWorldDataTest {
    
    private VillagesRebornWorldSettings testSettings;
    private VillagesRebornWorldData worldData;
    
    @BeforeEach
    void setUp() {
        testSettings = new VillagesRebornWorldSettings();
        testSettings.setVillagerMemoryLimit(300);
        testSettings.setAiAggressionLevel(0.6f);
        testSettings.setEnableAdvancedAI(true);
        testSettings.setMaxVillageSize(90);
        testSettings.setExpansionRate(1.2f);
        
        worldData = new VillagesRebornWorldData(testSettings);
    }
    
    @Test
    void shouldCreateWithDefaultConstructor() {
        // When
        VillagesRebornWorldData data = new VillagesRebornWorldData();
        
        // Then
        assertNotNull(data);
        assertFalse(data.hasSettings());
        assertNull(data.getSettings());
        assertTrue(data.getCreatedTimestamp() > 0);
        assertEquals("2.0", data.getDataVersion());
    }
    
    @Test
    void shouldCreateWithSettings() {
        // When
        VillagesRebornWorldData data = new VillagesRebornWorldData(testSettings);
        
        // Then
        assertNotNull(data);
        assertTrue(data.hasSettings());
        assertNotNull(data.getSettings());
        assertEquals(testSettings.getVillagerMemoryLimit(), data.getSettings().getVillagerMemoryLimit());
        assertEquals(testSettings.getAiAggressionLevel(), data.getSettings().getAiAggressionLevel(), 0.001f);
    }
    
    @Test
    void shouldSerializeToMap() {
        // When
        Map<String, Object> map = worldData.toMap();
        
        // Then
        assertTrue(map.containsKey("version"));
        assertTrue(map.containsKey("created"));
        assertTrue(map.containsKey("last_modified"));
        assertTrue(map.containsKey("settings"));
        
        assertEquals("2.0", map.get("version"));
        assertTrue(((Number) map.get("created")).longValue() > 0);
        assertTrue(((Number) map.get("last_modified")).longValue() > 0);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> settingsMap = (Map<String, Object>) map.get("settings");
        assertNotNull(settingsMap);
        assertEquals(300, ((Number) settingsMap.get("villager_memory_limit")).intValue());
        assertEquals(0.6f, ((Number) settingsMap.get("ai_aggression_level")).floatValue(), 0.001f);
    }
    
    @Test
    void shouldDeserializeFromMap() {
        // Given
        Map<String, Object> map = worldData.toMap();
        
        // When
        VillagesRebornWorldData data = VillagesRebornWorldData.fromMap(map);
        
        // Then
        assertNotNull(data);
        assertTrue(data.hasSettings());
        assertEquals("2.0", data.getDataVersion());
        
        VillagesRebornWorldSettings settings = data.getSettings();
        assertEquals(testSettings.getVillagerMemoryLimit(), settings.getVillagerMemoryLimit());
        assertEquals(testSettings.getAiAggressionLevel(), settings.getAiAggressionLevel(), 0.001f);
        assertEquals(testSettings.isEnableAdvancedAI(), settings.isEnableAdvancedAI());
    }
    
    @Test
    void shouldHandleEmptyMap() {
        // Given
        Map<String, Object> emptyMap = new java.util.HashMap<>();
        
        // When
        VillagesRebornWorldData data = VillagesRebornWorldData.fromMap(emptyMap);
        
        // Then
        assertNotNull(data);
        assertFalse(data.hasSettings());
        assertEquals("2.0", data.getDataVersion()); // Should use default
        assertTrue(data.getCreatedTimestamp() > 0); // Should set current time
    }
    
    @Test
    void shouldHandleCorruptedMap() {
        // Given
        Map<String, Object> corruptedMap = new java.util.HashMap<>();
        corruptedMap.put("settings", "invalid_settings_format");
        
        // When
        VillagesRebornWorldData data = VillagesRebornWorldData.fromMap(corruptedMap);
        
        // Then
        assertNotNull(data);
        // Should create new instance when parsing fails
        assertEquals("2.0", data.getDataVersion());
        assertTrue(data.getCreatedTimestamp() > 0);
    }
    
    @Test
    void shouldUpdateSettings() {
        // Given
        VillagesRebornWorldSettings newSettings = new VillagesRebornWorldSettings();
        newSettings.setVillagerMemoryLimit(400);
        newSettings.setAiAggressionLevel(0.9f);
        long originalTimestamp = worldData.getLastModifiedTimestamp();
        
        // When
        worldData.setSettings(newSettings);
        
        // Then
        assertTrue(worldData.hasSettings());
        assertEquals(400, worldData.getSettings().getVillagerMemoryLimit());
        assertEquals(0.9f, worldData.getSettings().getAiAggressionLevel(), 0.001f);
        assertTrue(worldData.getLastModifiedTimestamp() >= originalTimestamp);
    }
    
    @Test
    void shouldUpdateLastModifiedTimestamp() {
        // Given
        long originalTimestamp = worldData.getLastModifiedTimestamp();
        
        // When
        try {
            Thread.sleep(10); // Ensure time difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        worldData.setSettings(testSettings);
        
        // Then
        assertTrue(worldData.getLastModifiedTimestamp() > originalTimestamp);
    }
    
    @Test
    void shouldCreateCopy() {
        // When
        VillagesRebornWorldData copy = worldData.copy();
        
        // Then
        assertNotSame(worldData, copy);
        assertEquals(worldData.getDataVersion(), copy.getDataVersion());
        assertEquals(worldData.getCreatedTimestamp(), copy.getCreatedTimestamp());
        assertEquals(worldData.getLastModifiedTimestamp(), copy.getLastModifiedTimestamp());
        
        if (worldData.hasSettings()) {
            assertTrue(copy.hasSettings());
            assertNotSame(worldData.getSettings(), copy.getSettings());
            assertEquals(worldData.getSettings().getVillagerMemoryLimit(), copy.getSettings().getVillagerMemoryLimit());
        }
    }
    
    @Test
    void shouldRoundTripThroughMap() {
        // When
        Map<String, Object> map = worldData.toMap();
        VillagesRebornWorldData deserializedData = VillagesRebornWorldData.fromMap(map);
        
        // Then
        assertTrue(deserializedData.hasSettings());
        assertEquals(worldData.getDataVersion(), deserializedData.getDataVersion());
        assertEquals(worldData.getCreatedTimestamp(), deserializedData.getCreatedTimestamp());
        
        VillagesRebornWorldSettings originalSettings = worldData.getSettings();
        VillagesRebornWorldSettings deserializedSettings = deserializedData.getSettings();
        
        assertEquals(originalSettings.getVillagerMemoryLimit(), deserializedSettings.getVillagerMemoryLimit());
        assertEquals(originalSettings.getAiAggressionLevel(), deserializedSettings.getAiAggressionLevel(), 0.001f);
        assertEquals(originalSettings.isEnableAdvancedAI(), deserializedSettings.isEnableAdvancedAI());
        assertEquals(originalSettings.getMaxVillageSize(), deserializedSettings.getMaxVillageSize());
        assertEquals(originalSettings.getExpansionRate(), deserializedSettings.getExpansionRate(), 0.001f);
    }
    
    @Test
    void shouldHandleNullSettings() {
        // Given
        VillagesRebornWorldData data = new VillagesRebornWorldData();
        
        // When
        data.setSettings(null);
        
        // Then
        assertFalse(data.hasSettings());
        assertNull(data.getSettings());
    }
    
    @Test
    void shouldProvideToStringRepresentation() {
        // When
        String toString = worldData.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("VillagesRebornWorldData"));
        assertTrue(toString.contains("dataVersion="));
        assertTrue(toString.contains("createdTimestamp="));
        assertTrue(toString.contains("lastModifiedTimestamp="));
        assertTrue(toString.contains("hasSettings="));
    }
    
    @Test
    void shouldPreserveDataVersion() {
        // Given
        String originalVersion = worldData.getDataVersion();
        
        // When
        Map<String, Object> map = worldData.toMap();
        VillagesRebornWorldData deserializedData = VillagesRebornWorldData.fromMap(map);
        
        // Then
        assertEquals(originalVersion, deserializedData.getDataVersion());
    }
    
    @Test
    void shouldPreserveTimestamps() {
        // Given
        long originalCreated = worldData.getCreatedTimestamp();
        long originalModified = worldData.getLastModifiedTimestamp();
        
        // When
        Map<String, Object> map = worldData.toMap();
        VillagesRebornWorldData deserializedData = VillagesRebornWorldData.fromMap(map);
        
        // Then
        assertEquals(originalCreated, deserializedData.getCreatedTimestamp());
        // Note: Last modified timestamp gets updated during serialization
        assertTrue(deserializedData.getLastModifiedTimestamp() >= originalModified);
    }
}