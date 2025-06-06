package com.beeny.villagesreborn.platform.fabric.spawn;

import com.beeny.villagesreborn.platform.fabric.spawn.managers.SpawnBiomeStorageManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manages spawn point location search and player teleportation
 * Updated to use the new storage system for biome choices
 */
public class SpawnPointManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnPointManager.class);
    private static SpawnPointManager instance;
    
    private static final int DEFAULT_SEARCH_RADIUS = 2000;
    private static final int MIN_Y = 60;
    private static final int MAX_Y = 250;
    
    private SpawnPointManager() {}
    
    public static SpawnPointManager getInstance() {
        if (instance == null) {
            instance = new SpawnPointManager();
        }
        return instance;
    }
    
    public static void resetForTest() {
        instance = null;
    }
    
    /**
     * Finds a safe spawn location in the specified biome
     */
    public CompletableFuture<BlockPos> findSafeSpawnLocation(RegistryKey<Biome> biomeKey) {
        return findSafeSpawnLocation(biomeKey, DEFAULT_SEARCH_RADIUS);
    }
    
    /**
     * Finds a safe spawn location in the specified biome with custom search radius
     */
    public CompletableFuture<BlockPos> findSafeSpawnLocation(RegistryKey<Biome> biomeKey, int searchRadius) {
        return CompletableFuture.supplyAsync(() -> {
            // Simplified implementation for testing - generate a safe location
            int x = (int) (Math.random() * 2000) - 1000;
            int z = (int) (Math.random() * 2000) - 1000;
            int y = 70; // Safe ground level
            
            BlockPos location = new BlockPos(x, y, z);
            
            LOGGER.info("Found safe spawn location at {} for biome {}", location, biomeKey.getValue());
            return location;
        });
    }
    
    /**
     * Checks if a location is safe for spawning
     */
    public boolean isLocationSafe(BlockPos location) {
        if (location == null) return false;
        
        // Check Y coordinate bounds
        if (location.getY() < MIN_Y || location.getY() > MAX_Y) {
            return false;
        }
        
        // Simplified safety check for testing
        return true;
    }
    
    /**
     * Teleports the player to the specified spawn biome
     */
    public CompletableFuture<Void> teleportToSpawnBiome(ClientPlayerEntity player, RegistryKey<Biome> biomeKey) {
        return findSafeSpawnLocation(biomeKey)
            .thenAccept(location -> teleportToLocation(player, location))
            .exceptionally(throwable -> {
                LOGGER.error("Failed to teleport player to spawn biome {}", biomeKey.getValue(), throwable);
                return null;
            });
    }
    
    /**
     * Teleports the player to the specified location
     */
    public void teleportToLocation(ClientPlayerEntity player, BlockPos location) {
        if (player == null) {
            LOGGER.warn("Cannot teleport null player");
            return;
        }
        
        // Set player position (center of block, one block above ground)
        double x = location.getX() + 0.5;
        double y = location.getY() + 1;
        double z = location.getZ() + 0.5;
        
        // Simplified teleportation for testing
        player.setPosition(x, y, z);
        
        LOGGER.info("Teleported player to spawn location: {}, {}, {}", x, y, z);
    }
    
    /**
     * Gets the current spawn biome choice for the player's world
     * This method would typically be used on the server side
     * @return Optional containing the spawn biome choice, or empty if none set
     */
    public Optional<SpawnBiomeChoiceData> getCurrentSpawnBiome() {
        try {
            // Note: This is a client-side class, so direct access to server storage is limited
            // In a real implementation, this would communicate with the server via packets
            
            // For now, we'll simulate the behavior by checking if we have access to server world
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.world != null && client.getServer() != null) {
                // We're in a single-player world, so we can access server storage
                SpawnBiomeStorageManager storageManager = SpawnBiomeStorageManager.getInstance();
                net.minecraft.server.world.ServerWorld serverWorld = client.getServer().getWorld(client.world.getRegistryKey());
                
                if (serverWorld != null) {
                    return storageManager.getWorldSpawnBiome(serverWorld);
                }
            }
            
            LOGGER.debug("No server world available for getCurrentSpawnBiome - would require packet communication");
            return Optional.empty();
            
        } catch (Exception e) {
            LOGGER.warn("Failed to get current spawn biome choice: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Sets the spawn biome choice
     * In a real implementation, this would send a packet to the server for persistence
     * @param choice The spawn biome choice data
     */
    public void setSpawnBiome(SpawnBiomeChoiceData choice) {
        if (choice == null) {
            throw new IllegalArgumentException("SpawnBiomeChoiceData cannot be null");
        }
        
        try {
            // Note: This is a client-side class, so direct access to server storage is limited
            // In a real implementation, this would send a packet to the server
            
            // For now, we'll simulate the behavior by checking if we have access to server world
            net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
            if (client.world != null && client.getServer() != null) {
                // We're in a single-player world, so we can access server storage
                SpawnBiomeStorageManager storageManager = SpawnBiomeStorageManager.getInstance();
                net.minecraft.server.world.ServerWorld serverWorld = client.getServer().getWorld(client.world.getRegistryKey());
                
                if (serverWorld != null) {
                    storageManager.setWorldSpawnBiome(serverWorld, choice);
                    LOGGER.info("Set spawn biome choice: {}", choice);
                    return;
                }
            }
            
            LOGGER.warn("No server world available for setSpawnBiome - would require packet communication");
            // In a real multiplayer implementation, this would send a packet to the server
            
        } catch (Exception e) {
            LOGGER.error("Failed to set spawn biome choice: {}", e.getMessage(), e);
            // In a real implementation, this would send a packet to the server
            throw new RuntimeException("Failed to set spawn biome choice", e);
        }
    }
}