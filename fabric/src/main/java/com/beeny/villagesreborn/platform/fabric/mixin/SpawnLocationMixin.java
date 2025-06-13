package com.beeny.villagesreborn.platform.fabric.mixin;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.ImprovedVillageSpawnManager;
import com.beeny.villagesreborn.platform.fabric.spawn.VillageAwareSpawnManager;
import com.beeny.villagesreborn.platform.fabric.spawn.managers.SpawnBiomeStorageManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.World;

/**
 * Mixin to intercept spawn location setting and use village-aware spawning
 * Fixed to perform synchronous spawn location finding to prevent race conditions
 */
@Mixin(ServerWorld.class)
public class SpawnLocationMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnLocationMixin.class);
    // Use a thread-safe set to track which ServerWorld instances are currently processing spawn
    private static final Set<ServerWorld> processingSpawnWorlds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Maximum time to spend searching for spawn location (in milliseconds)
    private static final long MAX_SPAWN_SEARCH_TIME = 5000; // 5 seconds

    @ModifyVariable(method = "setSpawnPos", at = @At("HEAD"), argsOnly = true)
    private BlockPos onSetSpawnPos(BlockPos originalPos) {
        ServerWorld world = (ServerWorld) (Object) this;
        
        // Prevent recursion if this specific world instance is already processing spawn
        if (processingSpawnWorlds.contains(world)) {
            return originalPos;
        }

        // Only apply this logic for the Overworld during initial world creation
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            return originalPos;
        }
        
        Optional<SpawnBiomeChoiceData> spawnChoice = SpawnBiomeStorageManager.getInstance().getWorldSpawnBiome(world);

        if (spawnChoice.isPresent()) {
            RegistryKey<Biome> chosenBiome = spawnChoice.get().getBiomeKey();
            LOGGER.info("Spawn biome choice detected: {}. Starting synchronous spawn search.", chosenBiome.getValue());
            
            // Mark this world as processing to prevent recursion
            processingSpawnWorlds.add(world);
            
            try {
                long startTime = System.currentTimeMillis();
                
                // Search for village spawn location synchronously with timeout
                ImprovedVillageSpawnManager villageSpawnManager = ImprovedVillageSpawnManager.getInstance();
                Optional<BlockPos> villageSpawnLocation = Optional.empty();
                
                try {
                    // Use a synchronous search with timeout to prevent blocking world generation indefinitely
                    villageSpawnLocation = villageSpawnManager.findVillageSpawnLocationSync(world, chosenBiome, MAX_SPAWN_SEARCH_TIME);
                } catch (Exception e) {
                    LOGGER.warn("Exception during village spawn search: {}", e.getMessage());
                }
                
                if (villageSpawnLocation.isPresent()) {
                    BlockPos newPos = villageSpawnLocation.get();
                    LOGGER.info("Found village-aware spawn location: {} in {}ms", newPos, System.currentTimeMillis() - startTime);
                    return newPos;
                }
                
                // Try fallback location synchronously
                VillageAwareSpawnManager fallbackManager = VillageAwareSpawnManager.getInstance();
                Optional<BlockPos> fallbackSpawnLocation = Optional.empty();
                
                try {
                    fallbackSpawnLocation = fallbackManager.findFallbackSpawnLocationSync(world, chosenBiome, MAX_SPAWN_SEARCH_TIME);
                } catch (Exception e) {
                    LOGGER.warn("Exception during fallback spawn search: {}", e.getMessage());
                }
                
                if (fallbackSpawnLocation.isPresent()) {
                    BlockPos newPos = fallbackSpawnLocation.get();
                    LOGGER.info("Found fallback spawn location: {} in {}ms", newPos, System.currentTimeMillis() - startTime);
                    return newPos;
                }
                
                LOGGER.info("No suitable spawn location found in {} within time limit. Using original spawn.", chosenBiome.getValue());
                
            } catch (Exception e) {
                LOGGER.error("Critical error during synchronous spawn search, using original position", e);
            } finally {
                processingSpawnWorlds.remove(world);
            }
        }
        
        return originalPos;
    }
} 