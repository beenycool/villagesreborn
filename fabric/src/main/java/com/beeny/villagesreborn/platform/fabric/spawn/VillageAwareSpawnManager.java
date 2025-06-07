package com.beeny.villagesreborn.platform.fabric.spawn;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.StructureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.world.biome.Biome;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class VillageAwareSpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillageAwareSpawnManager.class);
    
    private static final int VILLAGE_SEARCH_RADIUS = 5000; // blocks - increased search radius
    private static final int CHUNK_SEARCH_RADIUS = VILLAGE_SEARCH_RADIUS / 16; // chunks
    private static final int MIN_DISTANCE_FROM_VILLAGE = 20; // minimum distance from village center - much closer
    private static final int MAX_DISTANCE_FROM_VILLAGE = 80; // maximum distance from village center - much closer
    private static final int SAFE_SPAWN_Y = 70; // safe Y level for spawning
    
    private static VillageAwareSpawnManager instance;
    
    private VillageAwareSpawnManager() {}
    
    public static VillageAwareSpawnManager getInstance() {
        if (instance == null) {
            instance = new VillageAwareSpawnManager();
        }
        return instance;
    }
    
    public CompletableFuture<Optional<BlockPos>> findVillageSpawnLocation(ServerWorld world, RegistryKey<Biome> biomeKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Searching for villages in biome: {}", biomeKey.getValue());
                
                List<BlockPos> villagePositions = findVillagesInBiome(world, biomeKey);
                
                if (villagePositions.isEmpty()) {
                    LOGGER.warn("No villages found in biome: {}", biomeKey.getValue());
                    return Optional.empty();
                }
                
                LOGGER.info("Found {} villages in biome: {}", villagePositions.size(), biomeKey.getValue());
                
                BlockPos worldSpawn = world.getSpawnPos();
                BlockPos bestVillage = villagePositions.stream()
                    .min((v1, v2) -> Double.compare(
                        worldSpawn.getSquaredDistance(v1),
                        worldSpawn.getSquaredDistance(v2)
                    ))
                    .orElse(villagePositions.get(0));
                
                LOGGER.info("Selected village at {} for spawn placement", bestVillage);
                
                Optional<BlockPos> spawnLocation = findSafeLocationNearVillage(world, bestVillage, biomeKey);
                
                if (spawnLocation.isPresent()) {
                    LOGGER.info("Found safe spawn location at {} near village at {}", 
                               spawnLocation.get(), bestVillage);
                } else {
                    LOGGER.warn("Could not find safe spawn location near village at {}", bestVillage);
                }
                
                return spawnLocation;
                
            } catch (Exception e) {
                LOGGER.error("Error finding village spawn location in biome: {}", biomeKey.getValue(), e);
                return Optional.empty();
            }
        });
    }
    
    private List<BlockPos> findVillagesInBiome(ServerWorld world, RegistryKey<Biome> biomeKey) {
        List<BlockPos> villagePositions = new ArrayList<>();
        BlockPos worldCenter = world.getSpawnPos();
        
        for (int chunkX = -CHUNK_SEARCH_RADIUS; chunkX <= CHUNK_SEARCH_RADIUS; chunkX++) {
            for (int chunkZ = -CHUNK_SEARCH_RADIUS; chunkZ <= CHUNK_SEARCH_RADIUS; chunkZ++) {
                ChunkPos chunkPos = new ChunkPos(
                    worldCenter.getX() / 16 + chunkX,
                    worldCenter.getZ() / 16 + chunkZ
                );
                
                Optional<BlockPos> villagePos = findVillageInChunk(world, chunkPos, biomeKey);
                villagePos.ifPresent(villagePositions::add);
            }
        }
        
        return villagePositions;
    }
    
    private Optional<BlockPos> findVillageInChunk(ServerWorld world, ChunkPos chunkPos, RegistryKey<Biome> biomeKey) {
        try {
            WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            var structureStarts = chunk.getStructureStarts();
            
            for (var entry : structureStarts.entrySet()) {
                Structure structure = entry.getKey();
                StructureStart structureStart = entry.getValue();
                
                if (isVillageStructure(structure) && structureStart.hasChildren()) {
                    BlockPos structurePos = structureStart.getBoundingBox().getCenter();
                    
                    if (isInTargetBiome(world, structurePos, biomeKey)) {
                        LOGGER.debug("Found village at {} in biome {}", structurePos, biomeKey.getValue());
                        return Optional.of(structurePos);
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Error checking chunk {} for villages: {}", chunkPos, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    private boolean isVillageStructure(Structure structure) {
        StructureType<?> type = structure.getType();
        String typeName = type.toString().toLowerCase();
        return typeName.contains("village") || 
               typeName.contains("plains_village") ||
               typeName.contains("desert_village") ||
               typeName.contains("savanna_village") ||
               typeName.contains("taiga_village") ||
               typeName.contains("snowy_village");
    }
    
    private boolean isInTargetBiome(ServerWorld world, BlockPos pos, RegistryKey<Biome> biomeKey) {
        try {
            RegistryKey<Biome> actualBiome = world.getBiome(pos).getKey().orElse(null);
            
            if (actualBiome == null) {
                LOGGER.debug("Could not determine biome at {}", pos);
                return false;
            }
            
            boolean isMatch = actualBiome.equals(biomeKey);
            LOGGER.debug("Biome check at {}: expected {}, found {}, match: {}", 
                        pos, biomeKey.getValue(), actualBiome.getValue(), isMatch);
            
            return isMatch;
        } catch (Exception e) {
            LOGGER.debug("Error checking biome at {}: {}", pos, e.getMessage());
            return false;
        }
    }
    
    private Optional<BlockPos> findSafeLocationNearVillage(ServerWorld world, BlockPos villageCenter, RegistryKey<Biome> biomeKey) {
        for (int attempts = 0; attempts < 20; attempts++) {
            double angle = Math.random() * 2 * Math.PI;
            double distance = MIN_DISTANCE_FROM_VILLAGE + 
                             Math.random() * (MAX_DISTANCE_FROM_VILLAGE - MIN_DISTANCE_FROM_VILLAGE);
            
            int x = villageCenter.getX() + (int)(Math.cos(angle) * distance);
            int z = villageCenter.getZ() + (int)(Math.sin(angle) * distance);
            
            BlockPos testPos = new BlockPos(x, SAFE_SPAWN_Y, z);
            BlockPos safePos = findSafeYLevel(world, testPos);
            
            if (safePos != null && isInTargetBiome(world, safePos, biomeKey)) {
                return Optional.of(safePos);
            }
        }
        
        return Optional.empty();
    }
    
    private BlockPos findSafeYLevel(ServerWorld world, BlockPos pos) {
        for (int y = 100; y >= 60; y--) {
            BlockPos testPos = new BlockPos(pos.getX(), y, pos.getZ());
            
            if (isSafeSpawnLocation(world, testPos)) {
                return testPos;
            }
        }
        
        return null;
    }
    
    private boolean isSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        try {
            if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                return false;
            }
            
            if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) {
                return false;
            }
            
            if (world.getBlockState(pos).getFluidState().isStill()) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.debug("Error checking spawn safety at {}: {}", pos, e.getMessage());
            return false;
        }
    }
    
    public CompletableFuture<Optional<BlockPos>> findFallbackSpawnLocation(ServerWorld world, RegistryKey<Biome> biomeKey) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Searching for fallback spawn location in biome: {}", biomeKey.getValue());

            BlockPos worldSpawn = world.getSpawnPos();
            int worldSpawnChunkX = worldSpawn.getX() >> 4;
            int worldSpawnChunkZ = worldSpawn.getZ() >> 4;

            // Search in expanding circles of chunks, which is much faster
            for (int radius = 5; radius <= 150; radius += 5) { // 150 chunks is ~2400 blocks
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        // Only check the edge of the circle to be efficient
                        if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                            continue;
                        }

                        ChunkPos chunkPos = new ChunkPos(worldSpawnChunkX + dx, worldSpawnChunkZ + dz);
                        // Get the center of the chunk at a reasonable height for biome checking
                        BlockPos chunkSamplePos = new BlockPos(chunkPos.getStartX() + 8, SAFE_SPAWN_Y, chunkPos.getStartZ() + 8);

                        // Check the biome at the center of the chunk
                        if (isInTargetBiome(world, chunkSamplePos, biomeKey)) {
                            // Found a chunk in the right biome, now find a safe spot in it
                            BlockPos safePos = findSafeYLevel(world, chunkSamplePos);
                            if (safePos != null) {
                                LOGGER.info("Found fallback spawn location at {} in chunk {}", safePos, chunkPos);
                                return Optional.of(safePos);
                            }
                        }
                    }
                }
            }

            LOGGER.warn("Could not find fallback spawn location in biome: {}", biomeKey.getValue());
            return Optional.empty();
        });
    }
}