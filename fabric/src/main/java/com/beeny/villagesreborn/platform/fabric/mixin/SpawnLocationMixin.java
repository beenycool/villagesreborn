package com.beeny.villagesreborn.platform.fabric.mixin;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.ImprovedVillageSpawnManager;
import com.beeny.villagesreborn.platform.fabric.spawn.VillageAwareSpawnManager;
import com.beeny.villagesreborn.platform.fabric.spawn.managers.SpawnBiomeStorageManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collections;
import net.minecraft.world.World;

/**
 * Mixin to intercept spawn location setting and use village-aware spawning
 */
@Mixin(ServerWorld.class)
public class SpawnLocationMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnLocationMixin.class);
    // Use a thread-safe set to track which ServerWorld instances are currently processing spawn
    private static final Set<ServerWorld> processingSpawnWorlds = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
            LOGGER.info("Spawn biome choice detected: {}. Starting asynchronous spawn search.", chosenBiome.getValue());
            
            // Instead of blocking world generation, schedule the spawn search for later
            // and let the world generation continue with the original spawn point
            CompletableFuture.runAsync(() -> {
                try {
                    LOGGER.info("Searching for better spawn location in background for biome: {}", chosenBiome.getValue());
                    
                    // Search for village spawn location
                    ImprovedVillageSpawnManager villageSpawnManager = ImprovedVillageSpawnManager.getInstance();
                    Optional<BlockPos> villageSpawnLocation = villageSpawnManager.findVillageSpawnLocation(world, chosenBiome).join();
                    
                    if (villageSpawnLocation.isPresent()) {
                        BlockPos newPos = villageSpawnLocation.get();
                        LOGGER.info("Found village-aware spawn location: {}. Updating spawn point.", newPos);
                        
                        // Update spawn point after world generation is complete
                        processingSpawnWorlds.add(world);
                        try {
                            world.setSpawnPos(newPos, 0.0f);
                        } finally {
                            processingSpawnWorlds.remove(world);
                        }
                        return;
                    }
                    
                    // Try fallback location
                    VillageAwareSpawnManager fallbackManager = VillageAwareSpawnManager.getInstance();
                    Optional<BlockPos> fallbackSpawnLocation = fallbackManager.findFallbackSpawnLocation(world, chosenBiome).join();
                    
                    if (fallbackSpawnLocation.isPresent()) {
                        BlockPos newPos = fallbackSpawnLocation.get();
                        LOGGER.info("Found fallback spawn location: {}. Updating spawn point.", newPos);
                        
                        // Update spawn point after world generation is complete
                        processingSpawnWorlds.add(world);
                        try {
                            world.setSpawnPos(newPos, 0.0f);
                        } finally {
                            processingSpawnWorlds.remove(world);
                        }
                        return;
                    }
                    
                    LOGGER.info("No suitable spawn location found in {}. Keeping original spawn.", chosenBiome.getValue());
                    
                } catch (Exception e) {
                    LOGGER.error("Error during asynchronous spawn search", e);
                }
            });
            
            LOGGER.info("World generation continuing with original spawn point. Better spawn will be set asynchronously.");
        }
        
        return originalPos;
    }
} 