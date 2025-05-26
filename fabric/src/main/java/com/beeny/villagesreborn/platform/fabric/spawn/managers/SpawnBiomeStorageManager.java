package com.beeny.villagesreborn.platform.fabric.spawn.managers;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.storage.PlayerSpawnBiomeStorage;
import com.beeny.villagesreborn.platform.fabric.spawn.storage.WorldSpawnBiomeStorage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central coordinator for spawn biome storage operations
 * Manages both world-level and player-level storage instances
 */
public class SpawnBiomeStorageManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnBiomeStorageManager.class);
    
    private final Map<RegistryKey<World>, WorldSpawnBiomeStorage> worldStorages = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSpawnBiomeStorage> playerStorages = new ConcurrentHashMap<>();
    
    private static SpawnBiomeStorageManager instance;
    
    private SpawnBiomeStorageManager() {}
    
    /**
     * Gets the singleton instance of the storage manager
     * @return The storage manager instance
     */
    public static SpawnBiomeStorageManager getInstance() {
        if (instance == null) {
            synchronized (SpawnBiomeStorageManager.class) {
                if (instance == null) {
                    instance = new SpawnBiomeStorageManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Resets the singleton instance (for testing)
     */
    public static void resetForTest() {
        instance = null;
    }
    
    /**
     * Gets the world storage for the specified world
     * @param world The server world
     * @return The world storage instance
     */
    public WorldSpawnBiomeStorage getWorldStorage(ServerWorld world) {
        if (world == null) {
            throw new IllegalArgumentException("ServerWorld cannot be null");
        }
        
        RegistryKey<World> worldId = world.getRegistryKey();
        return worldStorages.computeIfAbsent(worldId, key -> {
            WorldSpawnBiomeStorage storage = new WorldSpawnBiomeStorage(world);
            
            // Perform migration on first access
            try {
                storage.migrateFromStaticData();
            } catch (Exception e) {
                LOGGER.error("Failed to migrate static data for world {}", worldId, e);
            }
            
            LOGGER.debug("Created world storage for: {}", worldId);
            return storage;
        });
    }
    
    /**
     * Gets the player storage for the specified player
     * @param player The server player entity
     * @return The player storage instance
     */
    public PlayerSpawnBiomeStorage getPlayerStorage(ServerPlayerEntity player) {
        if (player == null) {
            throw new IllegalArgumentException("ServerPlayerEntity cannot be null");
        }
        
        UUID playerId = player.getUuid();
        return playerStorages.computeIfAbsent(playerId, key -> {
            PlayerSpawnBiomeStorage storage = new PlayerSpawnBiomeStorage(player);
            LOGGER.debug("Created player storage for: {}", playerId);
            return storage;
        });
    }
    
    /**
     * Sets the spawn biome choice for a world
     * @param world The server world
     * @param choice The spawn biome choice data
     */
    public void setWorldSpawnBiome(ServerWorld world, SpawnBiomeChoiceData choice) {
        if (world == null) {
            throw new IllegalArgumentException("ServerWorld cannot be null");
        }
        if (choice == null) {
            throw new IllegalArgumentException("SpawnBiomeChoiceData cannot be null");
        }
        
        WorldSpawnBiomeStorage storage = getWorldStorage(world);
        storage.setSpawnBiomeChoice(world.getRegistryKey(), choice);
        LOGGER.info("Set world spawn biome: {} for world {}", choice.getBiomeKey().getValue(), world.getRegistryKey().getValue());
    }
    
    /**
     * Gets the spawn biome choice for a world
     * @param world The server world
     * @return Optional containing the spawn biome choice, or empty if none set
     */
    public Optional<SpawnBiomeChoiceData> getWorldSpawnBiome(ServerWorld world) {
        if (world == null) {
            return Optional.empty();
        }
        
        WorldSpawnBiomeStorage storage = getWorldStorage(world);
        return storage.getSpawnBiomeChoice(world.getRegistryKey());
    }
    
    /**
     * Clears the spawn biome choice for a world
     * @param world The server world
     */
    public void clearWorldSpawnBiome(ServerWorld world) {
        if (world == null) {
            return;
        }
        
        WorldSpawnBiomeStorage storage = getWorldStorage(world);
        storage.clearSpawnBiomeChoice(world.getRegistryKey());
        LOGGER.info("Cleared world spawn biome for world {}", world.getRegistryKey().getValue());
    }
    
    /**
     * Checks if a world has a spawn biome choice set
     * @param world The server world
     * @return true if a choice exists, false otherwise
     */
    public boolean hasWorldSpawnBiome(ServerWorld world) {
        if (world == null) {
            return false;
        }
        
        WorldSpawnBiomeStorage storage = getWorldStorage(world);
        return storage.hasSpawnBiomeChoice(world.getRegistryKey());
    }
    
    /**
     * Sets the biome preference for a player
     * @param player The server player entity
     * @param preference The biome preference data
     */
    public void setPlayerBiomePreference(ServerPlayerEntity player, SpawnBiomeChoiceData preference) {
        if (player == null) {
            throw new IllegalArgumentException("ServerPlayerEntity cannot be null");
        }
        if (preference == null) {
            throw new IllegalArgumentException("SpawnBiomeChoiceData cannot be null");
        }
        
        PlayerSpawnBiomeStorage storage = getPlayerStorage(player);
        storage.setPlayerBiomePreference(player.getUuid(), preference);
        LOGGER.info("Set player biome preference: {} for player {}", preference.getBiomeKey().getValue(), player.getUuid());
    }
    
    /**
     * Gets the biome preference for a player
     * @param player The server player entity
     * @return Optional containing the biome preference, or empty if none set
     */
    public Optional<SpawnBiomeChoiceData> getPlayerBiomePreference(ServerPlayerEntity player) {
        if (player == null) {
            return Optional.empty();
        }
        
        PlayerSpawnBiomeStorage storage = getPlayerStorage(player);
        return storage.getPlayerBiomePreference(player.getUuid());
    }
    
    /**
     * Clears the biome preference for a player
     * @param player The server player entity
     */
    public void clearPlayerBiomePreference(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        
        PlayerSpawnBiomeStorage storage = getPlayerStorage(player);
        storage.clearPlayerBiomePreference(player.getUuid());
        LOGGER.info("Cleared player biome preference for player {}", player.getUuid());
    }
    
    /**
     * Checks if a player has a biome preference set
     * @param player The server player entity
     * @return true if a preference exists, false otherwise
     */
    public boolean hasPlayerBiomePreference(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        
        PlayerSpawnBiomeStorage storage = getPlayerStorage(player);
        return storage.hasPlayerBiomePreference(player.getUuid());
    }
    
    /**
     * Clears world data from cache (called when world unloads)
     * @param worldId The world identifier
     */
    public void clearWorldData(RegistryKey<World> worldId) {
        if (worldId != null) {
            worldStorages.remove(worldId);
            LOGGER.debug("Cleared world storage cache for: {}", worldId);
        }
    }
    
    /**
     * Clears player data from cache (called when player disconnects)
     * @param playerId The player UUID
     */
    public void clearPlayerData(UUID playerId) {
        if (playerId != null) {
            playerStorages.remove(playerId);
            LOGGER.debug("Cleared player storage cache for: {}", playerId);
        }
    }
    
    /**
     * Clears all cached data (for testing or shutdown)
     */
    public void clearAllData() {
        worldStorages.clear();
        playerStorages.clear();
        LOGGER.debug("Cleared all storage caches");
    }
    
    /**
     * Gets the number of cached world storages
     * @return The number of world storages
     */
    public int getWorldStorageCount() {
        return worldStorages.size();
    }
    
    /**
     * Gets the number of cached player storages
     * @return The number of player storages
     */
    public int getPlayerStorageCount() {
        return playerStorages.size();
    }
}