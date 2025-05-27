package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for VillagerBrainManagerImpl
 */
@DisplayName("Villager Brain Manager Tests")
class VillagerBrainManagerTest {

    @Mock
    private VillagerBrainManagerImpl.VillagerBrainPersistence persistence;
    
    @Mock
    private Player player;
    
    @Mock
    private VillagerEntity villager;

    private VillagerBrainManagerImpl brainManager;
    private UUID villagerUUID;
    private UUID playerUUID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        brainManager = new VillagerBrainManagerImpl(persistence);
        
        villagerUUID = UUID.randomUUID();
        playerUUID = UUID.randomUUID();
        
        when(villager.getUUID()).thenReturn(villagerUUID);
        when(villager.getProfession()).thenReturn("farmer");
        when(player.getUUID()).thenReturn(playerUUID);
        when(player.getName()).thenReturn("TestPlayer");
    }

    @Test
    @DisplayName("Should create new brain when none exists")
    void shouldCreateNewBrainWhenNoneExists() {
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        
        VillagerBrain brain = brainManager.getBrain(villager);
        
        assertNotNull(brain);
        assertEquals(villagerUUID, brain.getVillagerUUID());
        verify(persistence).saveBrain(eq(villagerUUID), any(VillagerBrain.class));
    }

    @Test
    @DisplayName("Should load existing brain from persistence")
    void shouldLoadExistingBrainFromPersistence() {
        VillagerBrain existingBrain = new VillagerBrain(villagerUUID);
        when(persistence.loadBrain(villagerUUID)).thenReturn(existingBrain);
        
        VillagerBrain brain = brainManager.getBrain(villager);
        
        assertEquals(existingBrain, brain);
        verify(persistence, never()).saveBrain(any(), any());
    }

    @Test
    @DisplayName("Should cache brains for performance")
    void shouldCacheBrainsForPerformance() {
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        
        VillagerBrain brain1 = brainManager.getBrain(villager);
        VillagerBrain brain2 = brainManager.getBrain(villager);
        
        assertSame(brain1, brain2);
        verify(persistence, times(1)).loadBrain(villagerUUID);
    }

    @Test
    @DisplayName("Should save brain to persistence")
    void shouldSaveBrainToPersistence() {
        VillagerBrain brain = new VillagerBrain(villagerUUID);
        
        brainManager.saveBrain(villager, brain);
        
        verify(persistence).saveBrain(villagerUUID, brain);
    }

    @Test
    @DisplayName("Should process overheard positive messages")
    void shouldProcessOverheardPositiveMessages() {
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        
        brainManager.processOverheardMessage(villager, player, "That's awesome!");
        
        // Expects 2 saves: 1 for new brain creation, 1 for updated brain
        verify(persistence, times(2)).saveBrain(eq(villagerUUID), any(VillagerBrain.class));
    }

    @Test
    @DisplayName("Should process overheard negative messages")
    void shouldProcessOverheardNegativeMessages() {
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        
        brainManager.processOverheardMessage(villager, player, "That's terrible!");
        
        // Expects 2 saves: 1 for new brain creation, 1 for updated brain
        verify(persistence, times(2)).saveBrain(eq(villagerUUID), any(VillagerBrain.class));
    }

    @Test
    @DisplayName("Should generate profession-specific personality")
    void shouldGenerateProfessionSpecificPersonality() {
        when(villager.getProfession()).thenReturn("librarian");
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        
        VillagerBrain brain = brainManager.getBrain(villager);
        
        // Should have librarian-appropriate traits
        assertTrue(brain.getPersonalityTraits().getTraitInfluence(TraitType.CURIOSITY) > 0.5f);
        assertTrue(brain.getPersonalityTraits().getTraitInfluence(TraitType.HELPFULNESS) > 0.5f);
    }

    @Test
    @DisplayName("Should handle unknown profession")
    void shouldHandleUnknownProfession() {
        when(villager.getProfession()).thenReturn("unknown");
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        
        VillagerBrain brain = brainManager.getBrain(villager);
        
        assertNotNull(brain);
        // Should have randomized traits
        assertNotNull(brain.getPersonalityTraits());
    }

    @Test
    @DisplayName("Should provide brain statistics")
    void shouldProvideBrainStatistics() {
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        brainManager.getBrain(villager); // Create a brain
        
        Map<String, Object> stats = brainManager.getStats();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("cachedBrains"));
        assertTrue(stats.containsKey("averageMemorySize"));
        assertEquals(1, stats.get("cachedBrains"));
    }

    @Test
    @DisplayName("Should perform maintenance operations")
    void shouldPerformMaintenanceOperations() {
        when(persistence.loadBrain(villagerUUID)).thenReturn(null);
        VillagerBrain brain = brainManager.getBrain(villager);
        
        // Should not throw exception
        assertDoesNotThrow(() -> brainManager.performMaintenance());
    }
}