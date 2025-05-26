package com.beeny.villagesreborn.platform.fabric.spawn.storage;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.persistence.SpawnBiomeNBTHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Player-level implementation of spawn biome storage using player NBT data
 */
public class PlayerSpawnBiomeStorage implements SpawnBiomeStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerSpawnBiomeStorage.class);
    
    private final ServerPlayerEntity player;
    private static final String PLAYER_NBT_KEY = "villagesreborn_spawn_biome_preference";
    
    public PlayerSpawnBiomeStorage(ServerPlayerEntity player) {
        if (player == null) {
            throw new IllegalArgumentException("ServerPlayerEntity cannot be null");
        }
        this.player = player;
    }
    
    @Override
    public Optional<SpawnBiomeChoiceData> getSpawnBiomeChoice(RegistryKey<World> worldId) {
        // Player storage doesn't handle world-level choices
        return Optional.empty();
    }
    
    @Override
    public void setSpawnBiomeChoice(RegistryKey<World> worldId, SpawnBiomeChoiceData choice) {
        // Player storage doesn't handle world-level choices
        LOGGER.warn("PlayerSpawnBiomeStorage cannot handle world-level choices. Use WorldSpawnBiomeStorage instead.");
    }
    
    @Override
    public void clearSpawnBiomeChoice(RegistryKey<World> worldId) {
        // Player storage doesn't handle world-level choices
        LOGGER.warn("PlayerSpawnBiomeStorage cannot handle world-level choices. Use WorldSpawnBiomeStorage instead.");
    }
    
    @Override
    public boolean hasSpawnBiomeChoice(RegistryKey<World> worldId) {
        // Player storage doesn't handle world-level choices
        return false;
    }
    
    @Override
    public Optional<SpawnBiomeChoiceData> getPlayerBiomePreference(UUID playerId) {
        try {
            // Verify this is the correct player
            if (!player.getUuid().equals(playerId)) {
                LOGGER.warn("Player UUID mismatch: expected {}, got {}", player.getUuid(), playerId);
                return Optional.empty();
            }
            
            NbtCompound playerData = new NbtCompound();
            player.writeNbt(playerData);
            
            if (!playerData.contains(PLAYER_NBT_KEY)) {
                return Optional.empty();
            }
            
            NbtCompound biomeNbt = playerData.getCompound(PLAYER_NBT_KEY);
            
            if (!SpawnBiomeNBTHandler.isValidNbt(biomeNbt)) {
                return Optional.empty();
            }
            
            SpawnBiomeChoiceData choice = SpawnBiomeNBTHandler.deserializeFromNbt(biomeNbt);
            LOGGER.debug("Retrieved player biome preference for {}: {}", playerId, choice);
            return Optional.of(choice);
            
        } catch (Exception e) {
            LOGGER.error("Failed to get player biome preference for {}", playerId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void setPlayerBiomePreference(UUID playerId, SpawnBiomeChoiceData preference) {
        if (preference == null) {
            throw new IllegalArgumentException("SpawnBiomeChoiceData cannot be null");
        }
        
        try {
            // Verify this is the correct player
            if (!player.getUuid().equals(playerId)) {
                throw new IllegalArgumentException("Player UUID mismatch: expected " + player.getUuid() + ", got " + playerId);
            }
            
            NbtCompound playerData = new NbtCompound();
            player.writeNbt(playerData);
            
            NbtCompound biomeNbt = SpawnBiomeNBTHandler.serializeToNbt(preference);
            playerData.put(PLAYER_NBT_KEY, biomeNbt);
            
            player.readNbt(playerData);
            
            LOGGER.info("Saved player biome preference for {}: {}", playerId, preference);
            
        } catch (Exception e) {
            LOGGER.error("Failed to set player biome preference for {}", playerId, e);
            throw new RuntimeException("Failed to save player biome preference", e);
        }
    }
    
    @Override
    public void migrateFromStaticData() {
        // Player storage doesn't need to migrate from static data
        LOGGER.debug("Player storage does not handle static data migration");
    }
    
    @Override
    public boolean hasLegacyData() {
        // Player storage doesn't have legacy data concerns
        return false;
    }
    
    /**
     * Clears the player's biome preference
     * @param playerId The player UUID
     */
    public void clearPlayerBiomePreference(UUID playerId) {
        try {
            // Verify this is the correct player
            if (!player.getUuid().equals(playerId)) {
                LOGGER.warn("Player UUID mismatch: expected {}, got {}", player.getUuid(), playerId);
                return;
            }
            
            NbtCompound playerData = new NbtCompound();
            player.writeNbt(playerData);
            
            if (playerData.contains(PLAYER_NBT_KEY)) {
                playerData.remove(PLAYER_NBT_KEY);
                player.readNbt(playerData);
                LOGGER.info("Cleared player biome preference for {}", playerId);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to clear player biome preference for {}", playerId, e);
        }
    }
    
    /**
     * Checks if the player has a biome preference set
     * @param playerId The player UUID
     * @return true if a preference exists, false otherwise
     */
    public boolean hasPlayerBiomePreference(UUID playerId) {
        return getPlayerBiomePreference(playerId).isPresent();
    }
    
    /**
     * Gets the server player entity this storage is associated with
     * @return The server player entity
     */
    public ServerPlayerEntity getPlayer() {
        return player;
    }
}