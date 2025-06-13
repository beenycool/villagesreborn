package com.beeny.villagesreborn.platform.fabric.spawn;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.entry.RegistryEntry;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.mojang.datafixers.util.Pair;

/**
 * Improved village-aware spawn manager with better village detection
 * Uses Minecraft's structure system more effectively
 */
public class ImprovedVillageSpawnManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImprovedVillageSpawnManager.class);
    
    private static final int SEARCH_RADIUS_CHUNKS = 50; // Increased from 25 to 50 chunks (~800 blocks) for rarer biomes
    private static final int MIN_DISTANCE_FROM_VILLAGE = 15; // Very close to village
    private static final int MAX_DISTANCE_FROM_VILLAGE = 50; // Still quite close
    private static final int SAFE_SPAWN_Y_MIN = 60;
    private static final int SAFE_SPAWN_Y_MAX = 120;
    private static final int MAX_SEARCH_TIME_MS = 60000; // Increased to 60 seconds for a more thorough async search
    
    private static ImprovedVillageSpawnManager instance;
    
    private ImprovedVillageSpawnManager() {}
    
    public static ImprovedVillageSpawnManager getInstance() {
        if (instance == null) {
            instance = new ImprovedVillageSpawnManager();
        }
        return instance;
    }
    
    /**
     * Finds a spawn location using a hybrid, multi-strategy engine for maximum efficiency and reliability.
     */
    public CompletableFuture<Optional<BlockPos>> findVillageSpawnLocation(ServerWorld world, RegistryKey<Biome> biomeKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("🚀 Hybrid Search Engine starting for biome: {}", biomeKey.getValue());

                // Strategy 1: The "Fast Path". Find the biome, then do a quick village search.
                LOGGER.info("[Phase 1/2] Attempting quick Biome-First scan...");
                Optional<BlockPos> biomeCenter = findNearestBiome(world, biomeKey);
                if (biomeCenter.isPresent()) {
                    Optional<BlockPos> villagePos = findVillageNear(world, biomeCenter.get(), biomeKey);
                    if (villagePos.isPresent()) {
                        LOGGER.info("✓ Fast Path SUCCESS: Found village at {} after locating biome.", villagePos.get());
                        return findSpawnNearVillage(world, villagePos.get(), biomeKey);
                    }
                    LOGGER.info("...Fast Path miss: Biome found, but no village nearby. Proceeding to Phase 2.");
                } else {
                    LOGGER.info("...Fast Path miss: Could not locate biome nearby. Proceeding to Phase 2.");
                }
                
                // Strategy 2: The "Power Search". If the fast path fails, use a more exhaustive, progressive search.
                LOGGER.info("[Phase 2/2] Attempting exhaustive Progressive Structure Scan...");
                Optional<BlockPos> villagePos = findVillageWithProgressiveSearch(world, biomeKey);
                if (villagePos.isPresent()) {
                    LOGGER.info("✓ Power Search SUCCESS: Found village via progressive scanning.");
                    return findSpawnNearVillage(world, villagePos.get(), biomeKey);
                }

                LOGGER.warn("✗ Hybrid Search Engine failed. No village found in biome {}. Will use fallback manager.", biomeKey.getValue());
                return Optional.empty();
                
            } catch (Exception e) {
                LOGGER.error("Error during Hybrid Search Engine execution", e);
                return Optional.empty();
            }
        });
    }
    
    /**
     * [HYBRID-HELPER] Uses vanilla's optimized locator to find the nearest biome instance.
     */
    private Optional<BlockPos> findNearestBiome(ServerWorld world, RegistryKey<Biome> biomeKey) {
        try {
            Predicate<RegistryEntry<Biome>> biomePredicate = entry -> entry.matchesKey(biomeKey);
            Pair<BlockPos, RegistryEntry<Biome>> result = world.locateBiome(biomePredicate, world.getSpawnPos(), 6400, 64, 64);
            return Optional.ofNullable(result).map(Pair::getFirst);
        } catch (Exception e) {
            LOGGER.error("Error in findNearestBiome: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * [HYBRID-HELPER] Searches for a village in a targeted area around a known biome location.
     */
    private Optional<BlockPos> findVillageNear(ServerWorld world, BlockPos center, RegistryKey<Biome> biomeKey) {
        // Search a 1280-block radius. This is our quick, targeted search.
        BlockPos villagePos = world.locateStructure(StructureTags.VILLAGE, center, 1280, false);
        if (villagePos != null && isInTargetBiome(world, villagePos, biomeKey)) {
            return Optional.of(villagePos);
        }
        return Optional.empty();
    }
    
    /**
     * [HYBRID-HELPER] Exhaustive search that expands its radius until a village in the correct biome is found.
     */
    private Optional<BlockPos> findVillageWithProgressiveSearch(ServerWorld world, RegistryKey<Biome> biomeKey) {
        String biomeType = getBiomeType(biomeKey);
        int[] searchRadii = getSearchRadiiForBiome(biomeType);
        BlockPos center = world.getSpawnPos();

        LOGGER.info("...Starting progressive search for '{}' villages with radii: {}", biomeType, searchRadii);

        for (int radius : searchRadii) {
            LOGGER.debug("......Searching radius: {} blocks", radius);
            BlockPos villagePos = world.locateStructure(StructureTags.VILLAGE, center, radius, false);
            if (villagePos != null && isInTargetBiome(world, villagePos, biomeKey)) {
                 LOGGER.info("......SUCCESS: Found village at {} within {} block radius.", villagePos, radius);
                return Optional.of(villagePos);
            }
        }
        LOGGER.warn("......Progressive search completed. No '{}' village found.", biomeType);
        return Optional.empty();
    }
    
    private String getBiomeType(RegistryKey<Biome> biomeKey) {
        String biomeName = biomeKey.getValue().getPath().toLowerCase();
        if (biomeName.contains("desert")) return "desert";
        if (biomeName.contains("savanna")) return "savanna";
        if (biomeName.contains("taiga")) return "taiga";
        if (biomeName.contains("snowy") || biomeName.contains("ice")) return "snowy";
        if (biomeName.contains("plains")) return "plains";
        return "generic";
    }

    private int[] getSearchRadiiForBiome(String biomeType) {
        switch (biomeType) {
            case "desert":
            case "savanna":
                return new int[]{800, 1600, 2400, 3200};
            case "taiga":
            case "snowy":
                return new int[]{1000, 2000, 3000};
            case "plains":
                return new int[]{400, 800, 1200};
            default:
                return new int[]{500, 1000, 1500};
        }
    }
    
    /**
     * Checks if a position is in the target biome
     */
    private boolean isInTargetBiome(ServerWorld world, BlockPos pos, RegistryKey<Biome> biomeKey) {
        try {
            var actualBiome = world.getBiome(pos);
            boolean isMatch = actualBiome.matchesKey(biomeKey);
            
            if (isMatch) {
                LOGGER.info("Confirmed biome match at {}: {}", pos, biomeKey.getValue());
            } else {
                String actualBiomeName = actualBiome.getKey()
                    .map(k -> k.getValue().toString())
                    .orElse("unknown");
                LOGGER.debug("Biome mismatch at {}: expected {}, found {}", 
                            pos, biomeKey.getValue(), actualBiomeName);
            }
            
            return isMatch;
        } catch (Exception e) {
            LOGGER.error("Error checking biome at {}: {}", pos, e.getMessage());
            return false;
        }
    }
    
    /**
     * Finds a safe spawn location very close to a village
     */
    private Optional<BlockPos> findSpawnNearVillage(ServerWorld world, BlockPos villageCenter, RegistryKey<Biome> biomeKey) {
        LOGGER.info("Finding spawn location near village at {} in biome {}", villageCenter, biomeKey.getValue());
        
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
                // Verify the spawn location is in the correct biome
                if (isInTargetBiome(world, safePos, biomeKey)) {
                double actualDistance = Math.sqrt(safePos.getSquaredDistance(villageCenter));
                    LOGGER.info("Found safe spawn at {} (distance: {} from village) in correct biome {}", 
                               safePos, actualDistance, biomeKey.getValue());
                return Optional.of(safePos);
                } else {
                    LOGGER.debug("Spawn location at {} is not in target biome {}, trying another location", 
                                safePos, biomeKey.getValue());
                }
            }
        }
        
        LOGGER.warn("Could not find safe spawn near village at {} in biome {}", villageCenter, biomeKey.getValue());
        return Optional.empty();
    }
    
    /**
     * Finds a safe Y level at the given X,Z coordinates
     */
    private BlockPos findSafeYLevel(ServerWorld world, int x, int z) {
        // Strategy 1: Try the standard range first
        for (int y = SAFE_SPAWN_Y_MAX; y >= SAFE_SPAWN_Y_MIN; y--) {
            BlockPos testPos = new BlockPos(x, y, z);
            
            if (isSafeSpawnLocation(world, testPos)) {
                return testPos;
            }
        }
        
        // Strategy 2: If standard range fails, try a wider range with more lenient checks
        LOGGER.debug("Standard Y range failed, trying extended range at X={}, Z={}", x, z);
        for (int y = 140; y >= 40; y--) {
            BlockPos testPos = new BlockPos(x, y, z);
            
            if (isLenientSafeSpawnLocation(world, testPos)) {
                LOGGER.info("Found safe spawn using lenient check at {}", testPos);
                return testPos;
            }
        }
        
        // Strategy 3: Find the highest solid block and place spawn above it
        LOGGER.debug("Lenient checks failed, finding highest solid block at X={}, Z={}", x, z);
        for (int y = 140; y >= 10; y--) {
            BlockPos groundPos = new BlockPos(x, y, z);
            if (world.getBlockState(groundPos).isSolidBlock(world, groundPos)) {
                BlockPos spawnPos = groundPos.up();
                // Make sure there's enough space above
                if (world.getBlockState(spawnPos).isAir() && 
                    world.getBlockState(spawnPos.up()).isAir()) {
                    LOGGER.info("Found spawn on highest solid block at {}", spawnPos);
                    return spawnPos;
                }
            }
        }
        
        LOGGER.warn("Could not find any safe Y level at X={}, Z={}", x, z);
        return null;
    }
    
    /**
     * More lenient check for safe spawn locations, useful for difficult terrain
     */
    private boolean isLenientSafeSpawnLocation(ServerWorld world, BlockPos pos) {
        try {
            BlockPos groundPos = pos.down();
            
            // Check if there's any solid-ish block below (including sand, gravel, etc.)
            var groundState = world.getBlockState(groundPos);
            if (!groundState.isSolidBlock(world, groundPos) && 
                !groundState.isOpaque()) {
                return false;
            }
            
            // Check if spawn position has breathable space
            var spawnState = world.getBlockState(pos);
            if (!spawnState.isAir() && 
                !spawnState.canReplace(null) &&
                !spawnState.isReplaceable()) {
                return false;
            }
            
            // Check if space above is clear
            var aboveState = world.getBlockState(pos.up());
            if (!aboveState.isAir() && 
                !aboveState.canReplace(null) &&
                !aboveState.isReplaceable()) {
                return false;
            }
            
            // Allow spawning in/near water, but avoid lava
            var fluidState = spawnState.getFluidState();
            if (!fluidState.isEmpty()) {
                // Check if it's water (safe) or lava (dangerous)
                if (fluidState.isIn(net.minecraft.registry.tag.FluidTags.LAVA)) {
                    return false; // Never spawn in lava
                }
                // Water is okay - player can swim
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.debug("Error in lenient spawn safety check at {}: {}", pos, e.getMessage());
            return false;
        }
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
     * Synchronous version of village spawn location finding with timeout
     * Added to support the fixed SpawnLocationMixin
     */
    public Optional<BlockPos> findVillageSpawnLocationSync(ServerWorld world, RegistryKey<Biome> biomeKey, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("Starting synchronous village spawn search for biome: {} with timeout: {}ms", biomeKey.getValue(), timeoutMs);
        
        try {
            // Try to get async result with timeout
            CompletableFuture<Optional<BlockPos>> future = findVillageSpawnLocation(world, biomeKey);
            return future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
        } catch (java.util.concurrent.TimeoutException e) {
            LOGGER.warn("Village spawn search timed out after {}ms for biome: {}", timeoutMs, biomeKey.getValue());
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Error during synchronous village spawn search for biome: {}", biomeKey.getValue(), e);
            return Optional.empty();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.debug("Synchronous village spawn search completed in {}ms", duration);
        }
    }
} 