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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registry;

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
        try {
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
                               (typeName.contains("jigsaw") && (
                                   typeName.contains("village") || 
                                   className.contains("village")
                               ));
            
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
    
    private boolean isGenericTag(TagKey<Biome> tag) {
        String path = tag.id().getPath();
        return path.contains("overworld") ||
               path.contains("common") ||
               path.startsWith("has_structure") ||
               path.contains("spawns_");
    }

    private boolean isInTargetBiome(ServerWorld world, BlockPos pos, RegistryKey<Biome> biomeKey) {
        try {
            var actualBiome = world.getBiome(pos);
            boolean isMatch = actualBiome.matchesKey(biomeKey);
            
            if (isMatch) {
                LOGGER.info("Found matching biome at {}: {}", pos, biomeKey.getValue());
            } else {
                LOGGER.trace("Biome mismatch at {}: expected {}, found {}", 
                            pos, biomeKey.getValue(), actualBiome.getKey().map(RegistryKey::getValue).orElse(null));
            }
            
            return isMatch;
        } catch (Exception e) {
            LOGGER.error("Error checking biome at {}: {}", pos, e.getMessage());
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
        // Try a wider Y range and be more lenient about what's "safe"
        for (int y = 120; y >= 50; y--) {
            BlockPos testPos = new BlockPos(pos.getX(), y, pos.getZ());
            
            if (isSafeSpawnLocation(world, testPos)) {
                return testPos;
            }
        }
        
        // If no "perfect" safe location found, try to find any solid ground
        for (int y = 120; y >= 50; y--) {
            BlockPos testPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockPos groundPos = testPos.down();
            
            try {
                // More lenient check - just need solid ground and air above
                if (world.getBlockState(groundPos).isSolidBlock(world, groundPos) && 
                    world.getBlockState(testPos).isAir()) {
                    LOGGER.info("Found suitable spawn location (lenient check) at {}", testPos);
                    return testPos;
                }
            } catch (Exception e) {
                LOGGER.debug("Error checking lenient spawn at {}: {}", testPos, e.getMessage());
            }
        }
        
        LOGGER.warn("Could not find any spawn location at X={}, Z={}", pos.getX(), pos.getZ());
        return null;
    }
    
    private boolean isSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        try {
            BlockPos groundPos = pos.down();
            
            // Check if there's solid ground below
            if (!world.getBlockState(groundPos).isSolidBlock(world, groundPos)) {
                return false;
            }
            
            // Check if spawn position is clear (air or replaceable)
            var spawnState = world.getBlockState(pos);
            if (!spawnState.isAir() && !spawnState.canReplace(null)) {
                return false;
            }
            
            // Check if space above is clear
            var aboveState = world.getBlockState(pos.up());
            if (!aboveState.isAir() && !aboveState.canReplace(null)) {
                return false;
            }
            
            // Check for dangerous fluids (but allow water - players can swim)
            var fluidState = spawnState.getFluidState();
            if (!fluidState.isEmpty() && !fluidState.isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
                return false; // Avoid lava and other dangerous fluids
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
            
            long startTime = System.currentTimeMillis();
            final long MAX_SEARCH_TIME = 10000; // Reduced to 10 seconds since spawn detection is now more lenient
            
            int chunksChecked = 0;
            int biomesChecked = 0;

            // Search in expanding circles of chunks, with controlled chunk loading
            for (int radius = 1; radius <= 40; radius += 2) { // Increased radius to 40 chunks (~640 blocks)
                // Check timeout to prevent hanging
                if (System.currentTimeMillis() - startTime > MAX_SEARCH_TIME) {
                    LOGGER.warn("Fallback search timeout reached after {}ms, checked {} chunks, {} biomes", 
                               MAX_SEARCH_TIME, chunksChecked, biomesChecked);
                    break;
                }
                
                LOGGER.debug("Searching fallback at radius {} chunks", radius);
                
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        // Only check the edge of the circle to be efficient
                        if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                            continue;
                        }

                        ChunkPos chunkPos = new ChunkPos(worldSpawnChunkX + dx, worldSpawnChunkZ + dz);
                        chunksChecked++;
                        
                        try {
                            // For fallback search, we need to load some chunks to find biomes
                            // but we do it more aggressively than the village search
                            WorldChunk chunk;
                            if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                                chunk = world.getChunk(chunkPos.x, chunkPos.z);
                            } else if (radius <= 20) {
                                // Increased force-load radius to 20 chunks (~320 blocks) for biome searching
                                LOGGER.trace("Force loading chunk {} for biome search", chunkPos);
                                chunk = world.getChunk(chunkPos.x, chunkPos.z);
                            } else {
                                // For very distant chunks, skip to avoid excessive loading
                                continue;
                            }
                            
                            // Get the center of the chunk at a reasonable height for biome checking
                            BlockPos chunkSamplePos = new BlockPos(chunkPos.getStartX() + 8, SAFE_SPAWN_Y, chunkPos.getStartZ() + 8);
                            biomesChecked++;

                            // Check the biome at the center of the chunk
                            if (isInTargetBiome(world, chunkSamplePos, biomeKey)) {
                                // Found a chunk in the right biome, now find a safe spot in it
                                BlockPos safePos = findSafeYLevel(world, chunkSamplePos);
                                if (safePos != null) {
                                    LOGGER.info("Found fallback spawn location at {} in chunk {} after {}ms", 
                                               safePos, chunkPos, System.currentTimeMillis() - startTime);
                                    return Optional.of(safePos);
                                } else {
                                    LOGGER.debug("Found target biome but no safe spawn location in chunk {}", chunkPos);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Error checking chunk {} for fallback spawn: {}", chunkPos, e.getMessage());
                        }
                    }
                }
                
                // Log progress every few radii
                if (radius % 10 == 1) { // Log progress at radius 1, 11, 21, etc.
                    LOGGER.debug("Fallback search progress: radius {}, checked {} chunks, {} biomes, {}ms elapsed", 
                                radius, chunksChecked, biomesChecked, System.currentTimeMillis() - startTime);
                }
            }

            LOGGER.warn("Could not find fallback spawn location in biome: {} after {}ms (checked {} chunks, {} biomes)", 
                       biomeKey.getValue(), System.currentTimeMillis() - startTime, chunksChecked, biomesChecked);
            return Optional.empty();
        });
    }
}