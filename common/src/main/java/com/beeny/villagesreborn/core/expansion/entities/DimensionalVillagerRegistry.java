package com.beeny.villagesreborn.core.expansion.entities;

import com.beeny.villagesreborn.core.expansion.*;

public class DimensionalVillagerRegistry {
    private final FabricEntityRegistry entityRegistry;
    private final DimensionSpawnRules spawnRules;
    
    public DimensionalVillagerRegistry(FabricEntityRegistry entityRegistry, DimensionSpawnRules spawnRules) {
        this.entityRegistry = entityRegistry;
        this.spawnRules = spawnRules;
    }
    
    public void registerNetherVillager() {
        EntityAttributes attributes = EntityAttributes.builder()
                .maxHealth(25.0)
                .movementSpeed(0.5)
                .fireResistance(true)
                .build();
        
        try {
            entityRegistry.register("villagesreborn:nether_villager", NetherVillager.class, attributes);
        } catch (Exception e) {
            // Handle registration failure gracefully
        }
    }
    
    public void registerEndVillager() {
        EntityAttributes attributes = EntityAttributes.builder()
                .maxHealth(30.0)
                .movementSpeed(0.6)
                .teleportResistance(true)
                .build();
        
        try {
            entityRegistry.register("villagesreborn:end_villager", EndVillager.class, attributes);
        } catch (Exception e) {
            // Handle registration failure gracefully
        }
    }
    
    public void applySpawnRules(DimensionType dimension, Class<?> entityClass, SpawnConfig config) {
        spawnRules.setSpawnConfig(dimension, entityClass, config);
    }
    
    public void initializeAllDimensionalEntities() {
        registerNetherVillager();
        registerEndVillager();
        
        // Apply spawn rules
        SpawnConfig netherConfig = SpawnConfig.builder()
                .minGroupSize(2)
                .maxGroupSize(4)
                .spawnChance(0.15)
                .requiredBiomes("nether_wastes", "crimson_forest")
                .build();
        
        SpawnConfig endConfig = SpawnConfig.builder()
                .minGroupSize(1)
                .maxGroupSize(2)
                .spawnChance(0.08)
                .requiredBiomes("end_highlands", "end_midlands")
                .build();
        
        applySpawnRules(DimensionType.NETHER, NetherVillager.class, netherConfig);
        applySpawnRules(DimensionType.END, EndVillager.class, endConfig);
    }
}