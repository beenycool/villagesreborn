package com.beeny.villagesreborn.platform.fabric.spawn;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.world.chunk.WorldChunk;

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
    
    private static final int SEARCH_RADIUS_CHUNKS = 25; // Increased from 10 to 25 chunks (~400 blocks)
    private static final int MIN_DISTANCE_FROM_VILLAGE = 15; // Very close to village
    private static final int MAX_DISTANCE_FROM_VILLAGE = 50; // Still quite close
    private static final int SAFE_SPAWN_Y_MIN = 60;
    private static final int SAFE_SPAWN_Y_MAX = 120;
    private static final int MAX_SEARCH_TIME_MS = 20000; // Increased to 20 seconds for a more thorough async search
    
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
        
        LOGGER.info("Starting village search from chunk ({}, {}) with radius {} chunks", 
                   centerChunkX, centerChunkZ, SEARCH_RADIUS_CHUNKS);
        
        // Scan chunks in a reasonable radius with controlled chunk loading
        for (int radius = 0; radius < SEARCH_RADIUS_CHUNKS; radius++) {
            // Check timeout to prevent hanging during world generation
            if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME_MS) {
                LOGGER.info("Village search completed after {}ms timeout, found {} villages", MAX_SEARCH_TIME_MS, villages.size());
                break;
            }
            
            int chunksChecked = 0;
            int structuresFound = 0;
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Only check the edge of the current radius to avoid duplicates
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    
                    ChunkPos chunkPos = new ChunkPos(centerChunkX + dx, centerChunkZ + dz);
                    chunksChecked++;
                    
                    try {
                        // For village searching, we need to load chunks to find structures
                        // but we do it carefully and limit the scope
                        WorldChunk chunk;
                        if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                            chunk = world.getChunk(chunkPos.x, chunkPos.z);
                        } else if (radius <= 10) {
                            // Increased force-load radius from 3 to 10 chunks (~160 blocks)
                            LOGGER.debug("Force loading chunk {} for village search", chunkPos);
                            chunk = world.getChunk(chunkPos.x, chunkPos.z);
                        } else {
                            // Skip unloaded chunks beyond the aggressive search radius
                            continue;
                        }
                        
                        var structureStarts = chunk.getStructureStarts();
                        structuresFound += structureStarts.size();
                        
                        for (var entry : structureStarts.entrySet()) {
                            Structure structure = entry.getKey();
                            StructureStart structureStart = entry.getValue();
                            
                            LOGGER.debug("Found structure in chunk {}: type={}, hasChildren={}", 
                                        chunkPos, structure.getType(), structureStart.hasChildren());
                            
                            if (isVillageStructure(structure) && structureStart.hasChildren()) {
                                BlockPos villageCenter = structureStart.getBoundingBox().getCenter();
                                villages.add(villageCenter);
                                LOGGER.info("Found village at: {} in chunk {}", villageCenter, chunkPos);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Error scanning chunk {}: {}", chunkPos, e.getMessage());
                    }
                }
            }
            
            LOGGER.debug("Radius {} complete: checked {} chunks, found {} structures, {} villages total", 
                        radius, chunksChecked, structuresFound, villages.size());
            
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
        try {
            // Get the structure type and convert to string for analysis
            StructureType<?> type = structure.getType();
            String typeName = type.toString().toLowerCase();
            String className = structure.getClass().getSimpleName().toLowerCase();
            
            // More comprehensive village detection
            boolean isVillage = typeName.contains("village") ||
                               className.contains("village") ||
                               typeName.contains("plains_village") ||
                               typeName.contains("desert_village") ||
                               typeName.contains("savanna_village") ||
                               typeName.contains("taiga_village") ||
                               typeName.contains("snowy_village") ||
                               typeName.contains("jigsaw") && (
                                   typeName.contains("village") || 
                                   className.contains("village")
                               );
            
            // Add debug logging to understand what structures we're finding
            if (isVillage) {
                LOGGER.info("Detected village structure: type={}, class={}", typeName, className);
            } else {
                LOGGER.debug("Non-village structure: type={}, class={}", typeName, className);
            }
            
            return isVillage;
        } catch (Exception e) {
            LOGGER.debug("Error checking structure type: {}", e.getMessage());
            return false;
        }
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