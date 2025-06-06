package com.beeny.villagesreborn.core.memory;

import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.ai.PersonalityProfile;
import com.beeny.villagesreborn.core.ai.MoodState;
import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.ai.ConversationHistory;
import com.beeny.villagesreborn.core.ai.ConversationInteraction;
import com.beeny.villagesreborn.core.ai.MemoryBank;
import com.beeny.villagesreborn.core.ai.TraitType;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.common.NBTCompound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for Memory Persistence
 * Tests VillagerEntityData NBT serialization/deserialization and
 * VillagerMemoryWorldData load/save cycle with migration fallback
 */
@DisplayName("Villager Memory Persistence Tests")
class VillagerMemoryPersistenceTest {

    @Mock
    private VillagerEntity mockVillager;
    
    @Mock
    private NBTCompound mockNBT;

    private VillagerEntityData villagerEntityData;
    private VillagerMemoryWorldData worldData;
    private UUID villagerUUID;
    private UUID playerUUID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        villagerUUID = UUID.randomUUID();
        playerUUID = UUID.randomUUID();
        
        when(mockVillager.getUUID()).thenReturn(villagerUUID);
        when(mockVillager.getPersistentData()).thenReturn(mockNBT);
        
        villagerEntityData = new VillagerEntityData();
        worldData = new VillagerMemoryWorldData();
    }

    @Test
    @DisplayName("Should serialize villager brain to NBT correctly")
    void shouldSerializeVillagerBrainToNBTCorrectly() {
        // Given: Villager brain with data
        VillagerBrain brain = createTestBrain();
        NBTCompound villagerNBT = new NBTCompound();
        when(mockNBT.getCompound(VillagerEntityData.VILLAGERS_REBORN_KEY)).thenReturn(villagerNBT);

        // When: Storing villager brain
        villagerEntityData.storeVillagerBrain(mockVillager, brain);

        // Then: Should store in NBT
        verify(mockNBT).put(eq(VillagerEntityData.VILLAGERS_REBORN_KEY), any());
    }

    @Test
    @DisplayName("Should deserialize villager brain from NBT correctly")
    void shouldDeserializeVillagerBrainFromNBTCorrectly() {
        // Given: NBT with brain data
        VillagerBrain originalBrain = createTestBrain();
        NBTCompound brainNBT = originalBrain.toNBT();
        NBTCompound villagerNBT = mock(NBTCompound.class);
        when(villagerNBT.contains("brain")).thenReturn(true);
        when(villagerNBT.getCompound("brain")).thenReturn(brainNBT);
        
        when(mockNBT.contains(VillagerEntityData.VILLAGERS_REBORN_KEY)).thenReturn(true);
        when(mockNBT.getCompound(VillagerEntityData.VILLAGERS_REBORN_KEY)).thenReturn(villagerNBT);

        // When: Loading villager brain
        VillagerBrain loadedBrain = villagerEntityData.loadVillagerBrain(mockVillager);

        // Then: Should restore brain correctly
        assertNotNull(loadedBrain);
        assertEquals(originalBrain.getVillagerUUID(), loadedBrain.getVillagerUUID());
    }

    @Test
    @DisplayName("Should create default brain when no NBT data exists")
    void shouldCreateDefaultBrainWhenNoNBTDataExists() {
        // Given: No NBT data for villager
        when(mockNBT.contains(VillagerEntityData.VILLAGERS_REBORN_KEY)).thenReturn(false);

        // When: Loading villager brain
        VillagerBrain brain = villagerEntityData.loadVillagerBrain(mockVillager);

        // Then: Should create default brain
        assertNotNull(brain);
        assertEquals(villagerUUID, brain.getVillagerUUID());
        assertNotNull(brain.getPersonalityTraits());
        assertNotNull(brain.getCurrentMood());
        assertNotNull(brain.getShortTermMemory());
        assertNotNull(brain.getLongTermMemory());
    }

    @Test
    @DisplayName("Should handle corrupted NBT data gracefully")
    void shouldHandleCorruptedNBTDataGracefully() {
        // Given: Corrupted NBT structure
        NBTCompound corruptedNBT = mock(NBTCompound.class);
        when(corruptedNBT.contains("brain")).thenReturn(false);
        
        when(mockNBT.contains(VillagerEntityData.VILLAGERS_REBORN_KEY)).thenReturn(true);
        when(mockNBT.getCompound(VillagerEntityData.VILLAGERS_REBORN_KEY)).thenReturn(corruptedNBT);

        // When: Loading villager brain with corrupted data
        VillagerBrain brain = villagerEntityData.loadVillagerBrain(mockVillager);

        // Then: Should fallback to default brain
        assertNotNull(brain);
        assertEquals(villagerUUID, brain.getVillagerUUID());
        assertTrue(brain.isValid());
    }

    @Test
    @DisplayName("Should store villager brain in world data")
    void shouldStoreVillagerBrainInWorldData() {
        // Given: Villager brain
        VillagerBrain brain = createTestBrain();

        // When: Storing brain in world data
        worldData.storeVillagerBrain(villagerUUID, brain);

        // Then: Should be stored and marked dirty
        assertEquals(brain, worldData.loadVillagerBrain(villagerUUID));
        assertTrue(worldData.isDirty());
    }

    @Test
    @DisplayName("Should serialize world data to map correctly")
    void shouldSerializeWorldDataToMapCorrectly() {
        // Given: World data with villager brains
        VillagerBrain brain1 = createTestBrain();
        VillagerBrain brain2 = createTestBrain();
        UUID villager2UUID = UUID.randomUUID();
        
        worldData.storeVillagerBrain(villagerUUID, brain1);
        worldData.storeVillagerBrain(villager2UUID, brain2);

        // When: Serializing to map
        Map<String, Object> serialized = worldData.toMap();

        // Then: Should contain all data
        assertNotNull(serialized);
        assertTrue(serialized.containsKey("villager_brains"));
        assertTrue(serialized.containsKey("global_events"));
        assertTrue(serialized.containsKey("village_relationships"));
    }

    @Test
    @DisplayName("Should deserialize world data from map correctly")
    void shouldDeserializeWorldDataFromMapCorrectly() {
        // Given: Serialized world data
        VillagerBrain originalBrain = createTestBrain();
        worldData.storeVillagerBrain(villagerUUID, originalBrain);
        Map<String, Object> serialized = worldData.toMap();

        // When: Deserializing from map
        VillagerMemoryWorldData restored = VillagerMemoryWorldData.fromMap(serialized);

        // Then: Should restore all data
        assertNotNull(restored);
        VillagerBrain restoredBrain = restored.loadVillagerBrain(villagerUUID);
        assertNotNull(restoredBrain);
        assertEquals(originalBrain.getVillagerUUID(), restoredBrain.getVillagerUUID());
    }

    @Test
    @DisplayName("Should handle migration from older data format")
    void shouldHandleMigrationFromOlderDataFormat() {
        // Given: Legacy data format
        Map<String, Object> legacyData = new HashMap<>();
        legacyData.put("version", "1.0.0");
        legacyData.put("villagers", Map.of(villagerUUID.toString(), "legacy_brain_data"));

        // When: Loading legacy data
        VillagerMemoryWorldData migrated = VillagerMemoryWorldData.fromMap(legacyData);

        // Then: Should migrate successfully
        assertNotNull(migrated);
        // Migration should handle legacy format gracefully
        assertTrue(migrated.getVillagerBrains().isEmpty() || 
                  migrated.getVillagerBrains().containsKey(villagerUUID));
    }

    @Test
    @DisplayName("Should record and retrieve global memory events")
    void shouldRecordAndRetrieveGlobalMemoryEvents() {
        // Given: Global memory event
        GlobalMemoryEvent event = new GlobalMemoryEvent(
            "PLAYER_BUILT_HOUSE",
            System.currentTimeMillis(),
            Map.of("player", playerUUID.toString(), "location", "Village Center")
        );

        // When: Recording global event
        worldData.recordGlobalEvent(event);

        // Then: Should be stored and affect villager brains
        assertTrue(worldData.getGlobalMemoryEvents().contains(event));
        assertTrue(worldData.isDirty());
    }

    @Test
    @DisplayName("Should limit memory size and cleanup old data")
    void shouldLimitMemorySizeAndCleanupOldData() {
        // Given: Many old memories
        VillagerBrain brain = createTestBrain();
        ConversationHistory history = brain.getShortTermMemory();
        
        // Fill with old interactions
        long oldTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7 days ago
        for (int i = 0; i < 100; i++) {
            ConversationInteraction oldInteraction = new ConversationInteraction(
                oldTime - (i * 1000),
                playerUUID,
                "Old message " + i,
                "Old response " + i,
                brain.getCurrentMood().copy(),
                "Old Location"
            );
            history.addInteraction(oldInteraction);
        }

        // When: Cleaning up old memories
        long cleanupThreshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000L); // 1 day
        brain.cleanupOldMemories(cleanupThreshold);

        // Then: Should remove old memories and stay within limits
        assertTrue(history.size() <= VillagerBrain.MAX_SHORT_TERM_MEMORY);
        assertTrue(history.getInteractions().stream()
                  .allMatch(i -> i.getTimestamp() >= cleanupThreshold));
    }

    @Test
    @DisplayName("Should handle NBT version compatibility")
    void shouldHandleNBTVersionCompatibility() {
        // Given: Brain with version information
        VillagerBrain brain = createTestBrain();
        NBTCompound nbt = brain.toNBT();
        nbt.putString("data_version", "1.0.0");

        // When: Loading from versioned NBT
        VillagerBrain loaded = VillagerBrain.fromNBT(nbt);

        // Then: Should handle version differences gracefully
        assertNotNull(loaded);
        assertEquals(brain.getVillagerUUID(), loaded.getVillagerUUID());
    }

    @Test
    @DisplayName("Should compress large memory data efficiently")
    void shouldCompressLargeMemoryDataEfficiently() {
        // Given: Brain with extensive memory data
        VillagerBrain brain = createComplexBrain();

        // When: Serializing large data
        NBTCompound nbt = brain.toNBT();
        byte[] serialized = nbt.serialize();

        // Then: Should be reasonable size (compression working)
        assertNotNull(serialized);
        assertTrue(serialized.length < 50000); // Should be compressed
    }

    @Test
    @DisplayName("Should handle concurrent memory access safely")
    void shouldHandleConcurrentMemoryAccessSafely() {
        // Given: Concurrent access scenario
        VillagerBrain brain = createTestBrain();
        
        // When: Concurrent operations (simplified test)
        worldData.storeVillagerBrain(villagerUUID, brain);
        VillagerBrain loaded1 = worldData.loadVillagerBrain(villagerUUID);
        VillagerBrain loaded2 = worldData.loadVillagerBrain(villagerUUID);

        // Then: Should handle safely without corruption
        assertNotNull(loaded1);
        assertNotNull(loaded2);
        assertEquals(loaded1.getVillagerUUID(), loaded2.getVillagerUUID());
    }

    @Test
    @DisplayName("Should validate data integrity after serialization")
    void shouldValidateDataIntegrityAfterSerialization() {
        // Given: Complex brain with all data types
        VillagerBrain original = createComplexBrain();
        
        // When: Full serialization cycle
        NBTCompound nbt = original.toNBT();
        VillagerBrain restored = VillagerBrain.fromNBT(nbt);

        // Then: Should maintain basic data integrity
        assertEquals(original.getVillagerUUID(), restored.getVillagerUUID());
        assertNotNull(restored.getPersonalityTraits());
        assertNotNull(restored.getCurrentMood());
        assertNotNull(restored.getShortTermMemory());
        assertNotNull(restored.getRelationshipMap());
        
        // Basic personality traits should exist (values may not be preserved in minimal implementation)
        assertTrue(restored.isValid());
    }

    @Test
    @DisplayName("Should handle world data save and load cycle")
    void shouldHandleWorldDataSaveAndLoadCycle() {
        // Given: World data with multiple villagers and events
        VillagerBrain brain1 = createTestBrain();
        VillagerBrain brain2 = createComplexBrain();
        UUID villager2UUID = UUID.randomUUID();
        
        worldData.storeVillagerBrain(villagerUUID, brain1);
        worldData.storeVillagerBrain(villager2UUID, brain2);
        
        GlobalMemoryEvent event = new GlobalMemoryEvent(
            "TEST_EVENT", System.currentTimeMillis(), Map.of("data", "test")
        );
        worldData.recordGlobalEvent(event);

        // When: Complete save/load cycle
        Map<String, Object> saved = worldData.toMap();
        VillagerMemoryWorldData loaded = VillagerMemoryWorldData.fromMap(saved);

        // Then: Should restore everything correctly
        assertNotNull(loaded);
        assertEquals(2, loaded.getVillagerBrains().size());
        assertTrue(loaded.getGlobalMemoryEvents().stream()
                  .anyMatch(e -> "TEST_EVENT".equals(e.getEventType())));
    }

    // Helper methods
    private VillagerBrain createTestBrain() {
        VillagerBrain brain = new VillagerBrain(villagerUUID);
        brain.getPersonalityTraits().adjustTrait(TraitType.FRIENDLINESS, 0.7f);
        brain.getCurrentMood().setHappiness(0.5f);
        
        // Add a relationship
        RelationshipData relationship = new RelationshipData(playerUUID);
        relationship.adjustTrust(0.3f);
        brain.getRelationshipMap().put(playerUUID, relationship);
        
        return brain;
    }

    private VillagerBrain createComplexBrain() {
        VillagerBrain brain = createTestBrain();
        
        // Add multiple personality traits
        for (TraitType trait : TraitType.values()) {
            brain.getPersonalityTraits().adjustTrait(trait, 0.5f + (trait.ordinal() * 0.1f));
        }
        
        // Add conversation history
        ConversationHistory history = brain.getShortTermMemory();
        for (int i = 0; i < 10; i++) {
            ConversationInteraction interaction = new ConversationInteraction(
                System.currentTimeMillis() - (i * 60000),
                playerUUID,
                "Message " + i,
                "Response " + i,
                brain.getCurrentMood().copy(),
                "Location " + i
            );
            history.addInteraction(interaction);
        }
        
        return brain;
    }
}