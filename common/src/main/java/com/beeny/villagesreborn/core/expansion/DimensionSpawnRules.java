package com.beeny.villagesreborn.core.expansion;

public interface DimensionSpawnRules {
    void setSpawnConfig(DimensionType dimension, Class<?> entityClass, SpawnConfig config);
}