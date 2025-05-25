package com.beeny.villagesreborn.core.expansion;

public interface FabricEntityRegistry {
    void register(String identifier, Class<?> entityClass, EntityAttributes attributes);
}