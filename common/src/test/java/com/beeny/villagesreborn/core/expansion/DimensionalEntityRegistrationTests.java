package com.beeny.villagesreborn.core.expansion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.beeny.villagesreborn.core.expansion.entities.NetherVillager;
import com.beeny.villagesreborn.core.expansion.entities.EndVillager;
import com.beeny.villagesreborn.core.expansion.entities.DimensionalVillagerRegistry;

class DimensionalEntityRegistrationTests {
    
    @Mock
    private FabricEntityRegistry mockEntityRegistry;
    
    @Mock
    private DimensionSpawnRules mockSpawnRules;
    
    private DimensionalVillagerRegistry dimensionalRegistry;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dimensionalRegistry = new DimensionalVillagerRegistry(mockEntityRegistry, mockSpawnRules);
    }
    
    @Test
    @DisplayName("Should register NetherVillager with correct identifier and attributes")
    void testNetherVillagerRegistration() {
        // Given: Nether villager registration parameters
        String expectedIdentifier = "villagesreborn:nether_villager";
        EntityAttributes expectedAttributes = EntityAttributes.builder()
                .maxHealth(25.0)
                .movementSpeed(0.5)
                .fireResistance(true)
                .build();
        
        // When: registering nether villager
        dimensionalRegistry.registerNetherVillager();
        
        // Then: entity registry should be called with correct parameters
        verify(mockEntityRegistry).register(
                eq(expectedIdentifier),
                eq(NetherVillager.class),
                argThat(attrs -> attrs.getMaxHealth() == 25.0 && 
                               attrs.getMovementSpeed() == 0.5 && 
                               attrs.hasFireResistance())
        );
    }
    
    @Test
    @DisplayName("Should register EndVillager with correct identifier and attributes")
    void testEndVillagerRegistration() {
        // Given: End villager registration parameters
        String expectedIdentifier = "villagesreborn:end_villager";
        EntityAttributes expectedAttributes = EntityAttributes.builder()
                .maxHealth(30.0)
                .movementSpeed(0.6)
                .teleportResistance(true)
                .build();
        
        // When: registering end villager
        dimensionalRegistry.registerEndVillager();
        
        // Then: entity registry should be called with correct parameters
        verify(mockEntityRegistry).register(
                eq(expectedIdentifier),
                eq(EndVillager.class),
                argThat(attrs -> attrs.getMaxHealth() == 30.0 && 
                               attrs.getMovementSpeed() == 0.6 && 
                               attrs.hasTeleportResistance())
        );
    }
    
    @Test
    @DisplayName("Should apply dimension-specific spawn rules for Nether")
    void testNetherSpawnRulesApplication() {
        // Given: Nether dimension spawn configuration
        DimensionType netherDimension = DimensionType.NETHER;
        SpawnConfig netherSpawnConfig = SpawnConfig.builder()
                .minGroupSize(2)
                .maxGroupSize(4)
                .spawnChance(0.15)
                .requiredBiomes("nether_wastes", "crimson_forest")
                .build();
        
        // When: applying spawn rules
        dimensionalRegistry.applySpawnRules(netherDimension, NetherVillager.class, netherSpawnConfig);
        
        // Then: spawn rules should be configured correctly
        verify(mockSpawnRules).setSpawnConfig(
                eq(netherDimension),
                eq(NetherVillager.class),
                argThat(config -> config.getMinGroupSize() == 2 && 
                                config.getMaxGroupSize() == 4 && 
                                config.getSpawnChance() == 0.15)
        );
    }
    
    @Test
    @DisplayName("Should apply dimension-specific spawn rules for End")
    void testEndSpawnRulesApplication() {
        // Given: End dimension spawn configuration
        DimensionType endDimension = DimensionType.END;
        SpawnConfig endSpawnConfig = SpawnConfig.builder()
                .minGroupSize(1)
                .maxGroupSize(2)
                .spawnChance(0.08)
                .requiredBiomes("end_highlands", "end_midlands")
                .build();
        
        // When: applying spawn rules
        dimensionalRegistry.applySpawnRules(endDimension, EndVillager.class, endSpawnConfig);
        
        // Then: spawn rules should be configured correctly
        verify(mockSpawnRules).setSpawnConfig(
                eq(endDimension),
                eq(EndVillager.class),
                argThat(config -> config.getMinGroupSize() == 1 && 
                                config.getMaxGroupSize() == 2 && 
                                config.getSpawnChance() == 0.08)
        );
    }
    
    @Test
    @DisplayName("Should verify all dimensional entities are registered during initialization")
    void testCompleteRegistrationProcess() {
        // When: initializing dimensional registry
        dimensionalRegistry.initializeAllDimensionalEntities();
        
        // Then: all entity types should be registered
        verify(mockEntityRegistry, times(1)).register(
                eq("villagesreborn:nether_villager"),
                eq(NetherVillager.class),
                any(EntityAttributes.class)
        );
        
        verify(mockEntityRegistry, times(1)).register(
                eq("villagesreborn:end_villager"),
                eq(EndVillager.class),
                any(EntityAttributes.class)
        );
        
        // And: spawn rules should be applied for both dimensions
        verify(mockSpawnRules, times(1)).setSpawnConfig(
                eq(DimensionType.NETHER),
                eq(NetherVillager.class),
                any(SpawnConfig.class)
        );
        
        verify(mockSpawnRules, times(1)).setSpawnConfig(
                eq(DimensionType.END),
                eq(EndVillager.class),
                any(SpawnConfig.class)
        );
    }
    
    @Test
    @DisplayName("Should handle registration failures gracefully")
    void testRegistrationFailureHandling() {
        // Given: entity registry that throws exception
        doThrow(new RuntimeException("Registration failed"))
                .when(mockEntityRegistry)
                .register(anyString(), any(Class.class), any(EntityAttributes.class));
        
        // When/Then: registration should handle failure gracefully
        assertDoesNotThrow(() -> dimensionalRegistry.registerNetherVillager(),
                "Registration failure should be handled gracefully");
        
        // And: error should be logged (would need to verify logging in real implementation)
    }
}