package com.beeny.villagesreborn.platform.fabric.spawn;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
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

import com.mojang.datafixers.util.Pair;
import java.util.function.Predicate;

/**
 * Village-aware spawn manager that finds spawn locations near villages
 * Provides fallback spawn location finding when no villages are available
 */
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
                String actualBiomeName = actualBiome.getKey()
                    .map(k -> k.getValue().toString())
                    .orElse("unknown");
                LOGGER.trace("Biome mismatch at {}: expected {}, found {}", 
                            pos, biomeKey.getValue(), actualBiomeName);
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
        // Strategy 1: Try the standard range first
        for (int y = 120; y >= 50; y--) {
            BlockPos testPos = new BlockPos(pos.getX(), y, pos.getZ());
            
            if (isSafeSpawnLocation(world, testPos)) {
                return testPos;
            }
        }
        
        // Strategy 2: More lenient check - just need solid ground and air above
        LOGGER.debug("Standard safety checks failed, trying lenient checks at X={}, Z={}", pos.getX(), pos.getZ());
        for (int y = 140; y >= 40; y--) {
            BlockPos testPos = new BlockPos(pos.getX(), y, pos.getZ());
            BlockPos groundPos = testPos.down();
            
            try {
                // More lenient check - accept any solid or opaque block as ground
                var groundState = world.getBlockState(groundPos);
                if ((groundState.isSolidBlock(world, groundPos) || groundState.isOpaque()) && 
                    world.getBlockState(testPos).isAir()) {
                    LOGGER.info("Found suitable spawn location (lenient check) at {}", testPos);
                    return testPos;
                }
            } catch (Exception e) {
                LOGGER.debug("Error checking lenient spawn at {}: {}", testPos, e.getMessage());
            }
        }
        
        // Strategy 3: Find the highest solid block and place spawn above it
        LOGGER.debug("Lenient checks failed, finding highest solid block at X={}, Z={}", pos.getX(), pos.getZ());
        for (int y = 140; y >= 10; y--) {
            BlockPos groundPos = new BlockPos(pos.getX(), y, pos.getZ());
            try {
                var groundState = world.getBlockState(groundPos);
                if (groundState.isSolidBlock(world, groundPos) || groundState.isOpaque()) {
                    BlockPos spawnPos = groundPos.up();
                    // Make sure there's enough space above
                    if (world.getBlockState(spawnPos).isAir() && 
                        world.getBlockState(spawnPos.up()).isAir()) {
                        LOGGER.info("Found spawn on highest solid block at {}", spawnPos);
                        return spawnPos;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Error checking highest solid block at Y={}: {}", y, e.getMessage());
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

            try {
                // Use vanilla's highly optimized biome locator
                LOGGER.info("Using locateBiome to find {}", biomeKey.getValue());
                
                // Create a predicate that matches the target biome
                Predicate<RegistryEntry<Biome>> biomePredicate = (biomeEntry) -> biomeEntry.matchesKey(biomeKey);
                
                // Use the correct locateBiome method signature - returns a Pair
                Pair<BlockPos, RegistryEntry<Biome>> result = world.locateBiome(biomePredicate, world.getSpawnPos(), 6400, 32, 64);
                
                if (result != null) {
                    BlockPos biomePos = result.getFirst();
                    LOGGER.info("Found target biome {} at {}", biomeKey.getValue(), biomePos);
                    
                    // Now that we found the biome, find a safe spawn location there
                    BlockPos safePos = findSafeYLevel(world, biomePos);
                    if (safePos != null) {
                        LOGGER.info("Found fallback spawn location at {}", safePos);
                        return Optional.of(safePos);
                    } else {
                        LOGGER.warn("Found biome but no safe location at {}", biomePos);
                    }
                } else {
                    LOGGER.warn("Could not find biome {} using locateBiome", biomeKey.getValue());
                }

            } catch (Exception e) {
                LOGGER.error("Error during fallback spawn search", e);
            }

            LOGGER.warn("Could not find fallback spawn location in biome: {}", biomeKey.getValue());
            return Optional.empty();
        });
    }

    /**
     * Synchronous version of fallback spawn location finding with timeout
     * Added to support the fixed SpawnLocationMixin
     */
    public Optional<BlockPos> findFallbackSpawnLocationSync(ServerWorld world, RegistryKey<Biome> biomeKey, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting synchronous fallback spawn search for biome: {} with timeout: {}ms", biomeKey.getValue(), timeoutMs);
        
        try {
            // Try to get async result with timeout
            CompletableFuture<Optional<BlockPos>> future = findFallbackSpawnLocation(world, biomeKey);
            return future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.warn("Fallback spawn search timed out after {}ms for biome: {}", timeoutMs, biomeKey.getValue());
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Error during synchronous fallback spawn search for biome: {}", biomeKey.getValue(), e);
            return Optional.empty();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.debug("Synchronous fallback spawn search completed in {}ms", duration);
        }
    }
}