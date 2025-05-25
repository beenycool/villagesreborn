package com.beeny.villagesreborn.core.world;

import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for VillagesRebornWorldSettings Map serialization/deserialization
 * Verifies data integrity and validation behavior
 */
@ExtendWith(MockitoExtension.class)
class VillagesRebornWorldSettingsTest {

    @Mock
    private HardwareInfo mockHardwareInfo;
    
    private VillagesRebornWorldSettings testSettings;
    
    @BeforeEach
    void setUp() {
        testSettings = new VillagesRebornWorldSettings();
        testSettings.setVillagerMemoryLimit(200);
        testSettings.setAiAggressionLevel(0.7f);
        testSettings.setEnableAdvancedAI(true);
        testSettings.setAutoExpansionEnabled(false);
        testSettings.setMaxVillageSize(75);
        testSettings.setExpansionRate(1.5f);
        testSettings.setElectionsEnabled(true);
        testSettings.setAssistantVillagersEnabled(false);
        testSettings.setDynamicTradingEnabled(true);
        testSettings.setVillagerRelationships(false);
        testSettings.setAdaptivePerformance(true);
        testSettings.setTickOptimizationLevel(2);
    }
    
    @Test
    void shouldCreateDefaultsBasedOnHardwareTier() {
        // Given
        when(mockHardwareInfo.getHardwareTier()).thenReturn(HardwareTier.HIGH);
        
        // When
        VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.createDefaults(mockHardwareInfo);
        
        // Then
        assertEquals(300, settings.getVillagerMemoryLimit()); // HIGH tier default
        assertTrue(settings.isEnableAdvancedAI()); // HIGH tier default
        assertEquals(1, settings.getTickOptimizationLevel()); // HIGH tier default
        assertEquals(100, settings.getMaxVillageSize()); // HIGH tier default
        assertEquals(1.5f, settings.getExpansionRate(), 0.001f); // HIGH tier default
    }
    
    @Test
    void shouldCreateLowTierDefaults() {
        // Given
        HardwareInfo lowTierHardware = new HardwareInfo(4, 2, false, HardwareTier.LOW);
        
        // When
        VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.createDefaults(lowTierHardware);
        
        // Then
        assertEquals(50, settings.getVillagerMemoryLimit()); // LOW tier default
        assertFalse(settings.isEnableAdvancedAI()); // LOW tier default
        assertEquals(3, settings.getTickOptimizationLevel()); // LOW tier default
        assertEquals(25, settings.getMaxVillageSize()); // LOW tier default
        assertEquals(0.5f, settings.getExpansionRate(), 0.001f); // LOW tier default
    }
    
    @Test
    void shouldCreateMediumTierDefaults() {
        // Given
        when(mockHardwareInfo.getHardwareTier()).thenReturn(HardwareTier.MEDIUM);
        
        // When
        VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.createDefaults(mockHardwareInfo);
        
        // Then
        assertEquals(150, settings.getVillagerMemoryLimit()); // MEDIUM tier default
        assertTrue(settings.isEnableAdvancedAI()); // MEDIUM tier default
        assertEquals(2, settings.getTickOptimizationLevel()); // MEDIUM tier default
        assertEquals(50, settings.getMaxVillageSize()); // MEDIUM tier default
        assertEquals(1.0f, settings.getExpansionRate(), 0.001f); // MEDIUM tier default
    }
    
    @Test
    void shouldCreateHighTierDefaults() {
        // Given
        HardwareInfo highTierHardware = new HardwareInfo(32, 16, true, HardwareTier.HIGH);
        
        // When
        VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.createDefaults(highTierHardware);
        
        // Then
        assertEquals(300, settings.getVillagerMemoryLimit()); // HIGH tier default
        assertTrue(settings.isEnableAdvancedAI()); // HIGH tier default
        assertEquals(1, settings.getTickOptimizationLevel()); // HIGH tier default
        assertEquals(100, settings.getMaxVillageSize()); // HIGH tier default
        assertEquals(1.5f, settings.getExpansionRate(), 0.001f); // HIGH tier default
    }
    
    @Test
    void shouldCreateUnknownTierDefaults() {
        // Given
        when(mockHardwareInfo.getHardwareTier()).thenReturn(HardwareTier.UNKNOWN);
        
        // When
        VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.createDefaults(mockHardwareInfo);
        
        // Then
        assertEquals(100, settings.getVillagerMemoryLimit()); // UNKNOWN tier default
        assertFalse(settings.isEnableAdvancedAI()); // UNKNOWN tier default
        assertEquals(2, settings.getTickOptimizationLevel()); // UNKNOWN tier default
        assertEquals(40, settings.getMaxVillageSize()); // UNKNOWN tier default
        assertEquals(0.8f, settings.getExpansionRate(), 0.001f); // UNKNOWN tier default
    }
    
    @Test
    void shouldSerializeAndDeserializeAllFields() {
        // When
        Map<String, Object> map = testSettings.toMap();
        VillagesRebornWorldSettings deserializedSettings = VillagesRebornWorldSettings.fromMap(map);
        
        // Then
        assertEquals(testSettings.getVillagerMemoryLimit(), deserializedSettings.getVillagerMemoryLimit());
        assertEquals(testSettings.getAiAggressionLevel(), deserializedSettings.getAiAggressionLevel(), 0.001f);
        assertEquals(testSettings.isEnableAdvancedAI(), deserializedSettings.isEnableAdvancedAI());
        assertEquals(testSettings.isAutoExpansionEnabled(), deserializedSettings.isAutoExpansionEnabled());
        assertEquals(testSettings.getMaxVillageSize(), deserializedSettings.getMaxVillageSize());
        assertEquals(testSettings.getExpansionRate(), deserializedSettings.getExpansionRate(), 0.001f);
        assertEquals(testSettings.isElectionsEnabled(), deserializedSettings.isElectionsEnabled());
        assertEquals(testSettings.isAssistantVillagersEnabled(), deserializedSettings.isAssistantVillagersEnabled());
        assertEquals(testSettings.isDynamicTradingEnabled(), deserializedSettings.isDynamicTradingEnabled());
        assertEquals(testSettings.isVillagerRelationships(), deserializedSettings.isVillagerRelationships());
        assertEquals(testSettings.isAdaptivePerformance(), deserializedSettings.isAdaptivePerformance());
        assertEquals(testSettings.getTickOptimizationLevel(), deserializedSettings.getTickOptimizationLevel());
        assertEquals(testSettings.getVersion(), deserializedSettings.getVersion());
    }
    
    @Test
    void shouldCreateDeepCopy() {
        // When
        VillagesRebornWorldSettings copy = testSettings.copy();
        
        // Then
        assertNotSame(testSettings, copy);
        assertEquals(testSettings.getVillagerMemoryLimit(), copy.getVillagerMemoryLimit());
        assertEquals(testSettings.getAiAggressionLevel(), copy.getAiAggressionLevel(), 0.001f);
        assertEquals(testSettings.isEnableAdvancedAI(), copy.isEnableAdvancedAI());
        
        // Modify original to ensure independence
        testSettings.setVillagerMemoryLimit(999);
        assertNotEquals(testSettings.getVillagerMemoryLimit(), copy.getVillagerMemoryLimit());
    }
    
    @Test
    void shouldValidateAndClampValues() {
        // Given - settings with out-of-range values
        VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
        settings.setVillagerMemoryLimit(1000); // Above max (500)
        settings.setAiAggressionLevel(2.0f); // Above max (1.0)
        settings.setMaxVillageSize(5); // Below min (10)
        settings.setExpansionRate(-0.5f); // Below min (0.1)
        settings.setTickOptimizationLevel(10); // Above max (3)
        
        // When
        settings.validate();
        
        // Then - values should be clamped to valid ranges
        assertEquals(500, settings.getVillagerMemoryLimit()); // Clamped to max
        assertEquals(1.0f, settings.getAiAggressionLevel(), 0.001f); // Clamped to max
        assertEquals(10, settings.getMaxVillageSize()); // Clamped to min
        assertEquals(0.1f, settings.getExpansionRate(), 0.001f); // Clamped to min
        assertEquals(3, settings.getTickOptimizationLevel()); // Clamped to max
    }
    
    @Test
    void shouldHandleEmptyMapWithDefaults() {
        // Given
        Map<String, Object> emptyMap = new java.util.HashMap<>();
        
        // When
        VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.fromMap(emptyMap);
        
        // Then
        assertNotNull(settings);
        assertEquals(150, settings.getVillagerMemoryLimit()); // Default value
        assertEquals(0.3f, settings.getAiAggressionLevel(), 0.001f); // Default value
        assertTrue(settings.isEnableAdvancedAI()); // Default value
        assertEquals("2.0", settings.getVersion()); // Default version
    }
    
    @Test
    void shouldHandleCorruptedMap() {
        // Given
        Map<String, Object> corruptedMap = new java.util.HashMap<>();
        corruptedMap.put("villager_memory_limit", "invalid_integer"); // Wrong type
        
        // When
        VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.fromMap(corruptedMap);
        
        // Then
        assertNotNull(settings);
        // Should return defaults when parsing fails
        assertEquals(150, settings.getVillagerMemoryLimit());
    }
    
    @Test
    void shouldPreserveVersionInformation() {
        // Given
        long originalTimestamp = testSettings.getCreatedTimestamp();
        
        // When
        Map<String, Object> map = testSettings.toMap();
        VillagesRebornWorldSettings deserializedSettings = VillagesRebornWorldSettings.fromMap(map);
        
        // Then
        assertEquals("2.0", deserializedSettings.getVersion());
        assertEquals(originalTimestamp, deserializedSettings.getCreatedTimestamp());
    }
    
    @Test
    void shouldSerializeWithCorrectMapStructure() {
        // When
        Map<String, Object> map = testSettings.toMap();
        
        // Then
        assertTrue(map.containsKey("villager_memory_limit"));
        assertTrue(map.containsKey("ai_aggression_level"));
        assertTrue(map.containsKey("enable_advanced_ai"));
        assertTrue(map.containsKey("auto_expansion_enabled"));
        assertTrue(map.containsKey("max_village_size"));
        assertTrue(map.containsKey("expansion_rate"));
        assertTrue(map.containsKey("elections_enabled"));
        assertTrue(map.containsKey("assistant_villagers_enabled"));
        assertTrue(map.containsKey("dynamic_trading_enabled"));
        assertTrue(map.containsKey("villager_relationships"));
        assertTrue(map.containsKey("adaptive_performance"));
        assertTrue(map.containsKey("tick_optimization_level"));
        assertTrue(map.containsKey("version"));
        assertTrue(map.containsKey("created_timestamp"));
        
        assertEquals(200, ((Number) map.get("villager_memory_limit")).intValue());
        assertEquals(0.7f, ((Number) map.get("ai_aggression_level")).floatValue(), 0.001f);
        assertTrue((Boolean) map.get("enable_advanced_ai"));
        assertFalse((Boolean) map.get("auto_expansion_enabled"));
        assertEquals(75, ((Number) map.get("max_village_size")).intValue());
        assertEquals(1.5f, ((Number) map.get("expansion_rate")).floatValue(), 0.001f);
        assertTrue((Boolean) map.get("elections_enabled"));
        assertFalse((Boolean) map.get("assistant_villagers_enabled"));
        assertTrue((Boolean) map.get("dynamic_trading_enabled"));
        assertFalse((Boolean) map.get("villager_relationships"));
        assertTrue((Boolean) map.get("adaptive_performance"));
        assertEquals(2, ((Number) map.get("tick_optimization_level")).intValue());
        assertEquals("2.0", map.get("version"));
    }
    
    @Test
    void shouldProvideToStringRepresentation() {
        // When
        String toString = testSettings.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("VillagesRebornWorldSettings"));
        assertTrue(toString.contains("villagerMemoryLimit="));
        assertTrue(toString.contains("aiAggressionLevel="));
        assertTrue(toString.contains("enableAdvancedAI="));
    }
    
    @Test
    void shouldReturnCorrectDefaultVersion() {
        // When
        VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
        
        // Then
        assertEquals("2.0", settings.getVersion());
    }
    
    @Test
    void shouldHaveValidCreatedTimestamp() {
        // When
        VillagesRebornWorldSettings settings = new VillagesRebornWorldSettings();
        
        // Then
        assertTrue(settings.getCreatedTimestamp() > 0);
        assertTrue(settings.getCreatedTimestamp() <= System.currentTimeMillis());
    }
    
    @Test
    void shouldRoundTripThroughMap() {
        // When
        Map<String, Object> map = testSettings.toMap();
        VillagesRebornWorldSettings roundTripSettings = VillagesRebornWorldSettings.fromMap(map);
        
        // Then
        assertEquals(testSettings.getVillagerMemoryLimit(), roundTripSettings.getVillagerMemoryLimit());
        assertEquals(testSettings.getAiAggressionLevel(), roundTripSettings.getAiAggressionLevel(), 0.001f);
        assertEquals(testSettings.isEnableAdvancedAI(), roundTripSettings.isEnableAdvancedAI());
        assertEquals(testSettings.isAutoExpansionEnabled(), roundTripSettings.isAutoExpansionEnabled());
        assertEquals(testSettings.getMaxVillageSize(), roundTripSettings.getMaxVillageSize());
        assertEquals(testSettings.getExpansionRate(), roundTripSettings.getExpansionRate(), 0.001f);
        assertEquals(testSettings.isElectionsEnabled(), roundTripSettings.isElectionsEnabled());
        assertEquals(testSettings.isAssistantVillagersEnabled(), roundTripSettings.isAssistantVillagersEnabled());
        assertEquals(testSettings.isDynamicTradingEnabled(), roundTripSettings.isDynamicTradingEnabled());
        assertEquals(testSettings.isVillagerRelationships(), roundTripSettings.isVillagerRelationships());
        assertEquals(testSettings.isAdaptivePerformance(), roundTripSettings.isAdaptivePerformance());
        assertEquals(testSettings.getTickOptimizationLevel(), roundTripSettings.getTickOptimizationLevel());
        assertEquals(testSettings.getVersion(), roundTripSettings.getVersion());
        assertEquals(testSettings.getCreatedTimestamp(), roundTripSettings.getCreatedTimestamp());
    }
}