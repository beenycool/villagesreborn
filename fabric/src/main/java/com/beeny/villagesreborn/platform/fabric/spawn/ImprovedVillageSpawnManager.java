package com.beeny.villagesreborn.platform.fabric.spawn;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Improved village-aware spawn manager with better village detection
 * Uses Minecraft's structure system more effectively
 */
public class ImprovedVillageSpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImprovedVillageSpawnManager.class);
    
    private static final int SEARCH_RADIUS_CHUNKS = 10; // Much smaller to avoid hanging during world gen
    private static final int MIN_DISTANCE_FROM_VILLAGE = 15; // Very close to village
    private static final int MAX_DISTANCE_FROM_VILLAGE = 50; // Still quite close
    private static final int SAFE_SPAWN_Y_MIN = 60;
    private static final int SAFE_SPAWN_Y_MAX = 120;
    private static final int MAX_SEARCH_TIME_MS = 8000; // 8 second timeout
    
    private static ImprovedVillageSpawnManager instance;
    
    private ImprovedVillageSpawnManager() {}
    
    public static ImprovedVillageSpawnManager getInstance() {
        if (instance == null) {
            instance = new ImprovedVillageSpawnManager();
        }
        return instance;
    }
    
    /**
     * Finds a spawn location near a village in the specified biome
     */
    public CompletableFuture<Optional<BlockPos>> findVillageSpawnLocation(ServerWorld world, RegistryKey<Biome> biomeKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Searching for villages in biome: {} with improved detection", biomeKey.getValue());
                
                // Add a small delay to ensure world generation has progressed
                try {
                    Thread.sleep(500); // 0.5 second delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
                
                List<BlockPos> villagePositions = findAllVillages(world);
                LOGGER.info("Found {} total villages in world", villagePositions.size());
                
                if (villagePositions.isEmpty()) {
                    LOGGER.info("No villages found in world yet - this is normal for a new world");
                    return Optional.empty();
                }
                
                // Use the closest village regardless of biome for now
                BlockPos worldSpawn = world.getSpawnPos();
                BlockPos closestVillage = villagePositions.stream()
                    .min((v1, v2) -> Double.compare(
                        worldSpawn.getSquaredDistance(v1),
                        worldSpawn.getSquaredDistance(v2)
                    ))
                    .orElse(villagePositions.get(0));
                
                LOGGER.info("Selected closest village at {}", closestVillage);
                return findSpawnNearVillage(world, closestVillage);
                
            } catch (Exception e) {
                LOGGER.error("Error in improved village spawn detection", e);
                return Optional.empty();
            }
        });
    }
    
    /**
     * Finds all villages in the world using chunk scanning with timeout
     */
    private List<BlockPos> findAllVillages(ServerWorld world) {
        List<BlockPos> villages = new ArrayList<>();
        BlockPos worldCenter = world.getSpawnPos();
        
        int centerChunkX = worldCenter.getX() >> 4;
        int centerChunkZ = worldCenter.getZ() >> 4;
        
        long startTime = System.currentTimeMillis();
        
        // Scan chunks in a much smaller radius to avoid hanging
        for (int radius = 0; radius < SEARCH_RADIUS_CHUNKS; radius++) {
            // Check timeout to prevent hanging during world generation
            if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
                LOGGER.info("Village search completed after {}ms timeout, found {} villages", MAX_SEARCH_TIME_MS, villages.size());
                break;
            }
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Only check the edge of the current radius to avoid duplicates
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    
                    ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                    
                    try {
                        // More robust check to see if chunk is loaded AND accessible without forcing generation
                        if (!world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                            continue; // Skip unloaded chunks to avoid blocking world generation
                        }
                        
                        var chunk = world.getChunk(chunkPos.x, chunkPos.z);
                        var structureStarts = chunk.getStructureStarts();
                        
                        for (var entry : structureStarts.entrySet()) {
                            Structure structure = entry.getKey();
                            StructureStart structureStart = entry.getValue();
                            
                            if (isVillageStructure(structure) && structureStart.hasChildren()) {
                                BlockPos villageCenter = structureStart.getBoundingBox().getCenter();
                                villages.add(villageCenter);
                                LOGGER.info("Found village at: {}", villageCenter);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Error scanning chunk {}: {}", chunkPos, e.getMessage());
                    }
                }
            }
            
            // If we found villages, we can stop searching early
            if (!villages.isEmpty() && radius > 3) {
                LOGGER.info("Found villages early, stopping search at radius {}", radius);
                break;
            }
        }
        
        LOGGER.info("Village search completed in {}ms, found {} villages", 
                   System.currentTimeMillis() - startTime, villages.size());
        return villages;
    }
    
    /**
     * Improved village structure detection
     */
    private boolean isVillageStructure(Structure structure) {
        // Use string checking for village detection
        String typeName = structure.getType().toString().toLowerCase();
        return typeName.contains("village");
    }
    
    /**
     * Finds a safe spawn location very close to a village
     */
    private Optional<BlockPos> findSpawnNearVillage(ServerWorld world, BlockPos villageCenter) {
        LOGGER.info("Finding spawn location near village at {}", villageCenter);
        
        // Try positions very close to the village
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = MIN_DISTANCE_FROM_VILLAGE + 
                             Math.random() * (MAX_DISTANCE_FROM_VILLAGE - MIN_DISTANCE_FROM_VILLAGE);
            
            int x = villageCenter.getX() + (int)(Math.cos(angle) * distance);
            int z = villageCenter.getZ() + (int)(Math.sin(angle) * distance);
            
            // Find a safe Y level
            BlockPos safePos = findSafeYLevel(world, x, z);
            if (safePos != null) {
                double actualDistance = Math.sqrt(safePos.getSquaredDistance(villageCenter));
                LOGGER.info("Found safe spawn at {} (distance: {} from village)", safePos, actualDistance);
                return Optional.of(safePos);
            }
        }
        
        LOGGER.warn("Could not find safe spawn near village at {}", villageCenter);
        return Optional.empty();
    }
    
    /**
     * Finds a safe Y level at the given X,Z coordinates
     */
    private BlockPos findSafeYLevel(ServerWorld world, int x, int z) {
        // Start from a reasonable height and work down
        for (int y = SAFE_SPAWN_Y_MAX; y >= SAFE_SPAWN_Y_MIN; y--) {
            BlockPos testPos = new BlockPos(x, y, z);
            
            if (isSafeSpawnLocation(world, testPos)) {
                return testPos;
            }
        }
        
        return null;
    }
    
    /**
     * Checks if a location is safe for spawning
     */
    private boolean isSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        try {
            // Check if the block below is solid
            if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                return false;
            }
            
            // Check if the spawn position and above are air
            if (!world.getBlockState(pos).isAir()) {
                return false;
            }
            
            if (!world.getBlockState(pos.up()).isAir()) {
                return false;
            }
            
            // Check if it's not in water or lava
            if (!world.getFluidState(pos).isEmpty()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.debug("Error checking spawn safety at {}: {}", pos, e.getMessage());
            return false;
        }
    }
    

} 