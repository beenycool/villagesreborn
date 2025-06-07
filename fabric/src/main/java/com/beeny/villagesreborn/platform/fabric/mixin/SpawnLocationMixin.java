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
import net.minecraft.world.World;

/**
 * Mixin to intercept spawn location setting and use village-aware spawning
 */
@Mixin(ServerWorld.class)
public class SpawnLocationMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnLocationMixin.class);
    private static final ThreadLocal<Boolean> isProcessingSpawn = ThreadLocal.withInitial(() -> false);

    @ModifyVariable(method = "setSpawnPos", at = @At("HEAD"), argsOnly = true)
    private BlockPos onSetSpawnPos(BlockPos originalPos) {
        // Prevent recursion if we call setSpawnPos again
        if (isProcessingSpawn.get()) {
            return originalPos;
        }

        ServerWorld world = (ServerWorld) (Object) this;
        
        // Only apply this logic for the Overworld during initial world creation
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            return originalPos;
        }
        
        Optional<SpawnBiomeChoiceData> spawnChoice = SpawnBiomeStorageManager.getInstance().getWorldSpawnBiome(world);

        if (spawnChoice.isPresent()) {
            isProcessingSpawn = true;
            try {
                RegistryKey<Biome> chosenBiome = spawnChoice.get().getBiomeKey();
                LOGGER.info("Starting synchronous search for spawn location in biome: {}", chosenBiome.getValue());

                // First, try to find a village
                ImprovedVillageSpawnManager villageSpawnManager = ImprovedVillageSpawnManager.getInstance();
                villageSpawnManager.findVillageSpawnLocation(world, chosenBiome).thenAccept(villageSpawnLocation -> {
                    if (villageSpawnLocation.isPresent()) {
                        BlockPos newPos = villageSpawnLocation.get();
                        LOGGER.info("Found village-aware spawn location: {}. World loading will continue.", newPos);
                        world.getServer().execute(() -> world.setSpawnPos(newPos, 0.0F));
                    } else {
                        LOGGER.info("No village found, starting fallback search in biome: {}", chosenBiome.getValue());
                        VillageAwareSpawnManager fallbackManager = VillageAwareSpawnManager.getInstance();
                        fallbackManager.findFallbackSpawnLocation(world, chosenBiome).thenAccept(fallbackSpawnLocation -> {
                            if (fallbackSpawnLocation.isPresent()) {
                                BlockPos newPos = fallbackSpawnLocation.get();
                                LOGGER.info("Found fallback spawn location: {}. World loading will continue.", newPos);
                                world.getServer().execute(() -> world.setSpawnPos(newPos, 0.0F));
                            } else {
                                LOGGER.warn("Could not find any suitable spawn location in {}. Using original location.", chosenBiome.getValue());
                            }
                        });
                    }
                });
                // If no village, find a fallback location
                LOGGER.info("No village found, starting synchronous fallback search in biome: {}", chosenBiome.getValue());
                VillageAwareSpawnManager fallbackManager = VillageAwareSpawnManager.getInstance();
                Optional<BlockPos> fallbackSpawnLocation = fallbackManager.findFallbackSpawnLocation(world, chosenBiome).join();

                if (fallbackSpawnLocation.isPresent()) {
                    BlockPos newPos = fallbackSpawnLocation.get();
                    LOGGER.info("Found fallback spawn location: {}. World loading will continue.", newPos);
                    return newPos;
                }

                LOGGER.warn("Could not find any suitable spawn location in {}. Using original location.", chosenBiome.getValue());
                return originalPos;

            } catch (Exception e) {
                LOGGER.error("Error during synchronous spawn search. Using original location.", e);
                return originalPos;
            } finally {
                isProcessingSpawn = false;
            }
        }
        
        return originalPos;
    }
} 