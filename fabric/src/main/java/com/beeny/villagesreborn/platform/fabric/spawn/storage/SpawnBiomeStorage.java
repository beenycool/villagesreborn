package com.beeny.villagesreborn.platform.fabric.spawn.storage;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Core storage interface for spawn biome choices
 * Provides platform-agnostic storage API for both world-level and player-level data
 */
public interface SpawnBiomeStorage {
    
    /**
     * Gets the spawn biome choice for a specific world
     * @param worldId The world identifier
     * @return Optional containing the spawn biome choice, or empty if none set
     */
    Optional<SpawnBiomeChoiceData> getSpawnBiomeChoice(RegistryKey<World> worldId);
    
    /**
     * Sets the spawn biome choice for a specific world
     * @param worldId The world identifier
     * @param choice The spawn biome choice data
     */
    void setSpawnBiomeChoice(RegistryKey<World> worldId, SpawnBiomeChoiceData choice);
    
    /**
     * Clears the spawn biome choice for a specific world
     * @param worldId The world identifier
     */
    void clearSpawnBiomeChoice(RegistryKey<World> worldId);
    
    /**
     * Checks if a spawn biome choice exists for a specific world
     * @param worldId The world identifier
     * @return true if a choice exists, false otherwise
     */
    boolean hasSpawnBiomeChoice(RegistryKey<World> worldId);
    
    /**
     * Gets the player-specific biome preference
     * @param playerId The player UUID
     * @return Optional containing the player's biome preference, or empty if none set
     */
    Optional<SpawnBiomeChoiceData> getPlayerBiomePreference(UUID playerId);
    
    /**
     * Sets the player-specific biome preference
     * @param playerId The player UUID
     * @param preference The biome preference data
     */
    void setPlayerBiomePreference(UUID playerId, SpawnBiomeChoiceData preference);
    
    /**
     * Migrates data from legacy static storage
     */
    void migrateFromStaticData();
    
    /**
     * Checks if legacy data exists that needs migration
     * @return true if legacy data exists, false otherwise
     */
    boolean hasLegacyData();
}