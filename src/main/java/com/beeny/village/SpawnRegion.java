package com.beeny.village;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.BiomeKeys;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class SpawnRegion {
    private final Culture culture;
    private final BlockPos center;
    private final int radius;

    public SpawnRegion(String cultureName, BlockPos center, int radius) {
        // Convert String to Culture object
        this.culture = new Culture(Culture.CultureType.valueOf(cultureName.toUpperCase()));
        this.center = center;
        this.radius = radius;
    }
    
    public SpawnRegion(Culture culture, BlockPos center, int radius) {
        this.culture = culture;
        this.center = center;
        this.radius = radius;
    }

    public boolean canSpawnInDimension(World world) {
        RegistryKey<World> dimension = world.getRegistryKey();
        Culture.CultureType cultureType = culture.getType();
        
        return switch (cultureType) {
            case NETHER -> World.NETHER.equals(dimension);
            case END -> World.END.equals(dimension);
            default -> World.OVERWORLD.equals(dimension);
        };
    }

    public boolean canSpawnInBiome(Biome biome, World world) {
        if (!canSpawnInDimension(world)) {
            return false;
        }

        Culture.CultureType cultureType = culture.getType();

        // Handle dimension-specific cultures with biome specificity
        if (World.NETHER.equals(world.getRegistryKey())) {
            if (cultureType != Culture.CultureType.NETHER) return false;
            
            // Specific Nether biome checks
            return world.getBiome(center).matchesKey(BiomeKeys.CRIMSON_FOREST) || 
                   world.getBiome(center).matchesKey(BiomeKeys.WARPED_FOREST) || 
                   world.getBiome(center).matchesKey(BiomeKeys.SOUL_SAND_VALLEY) || 
                   world.getBiome(center).matchesKey(BiomeKeys.NETHER_WASTES) || 
                   world.getBiome(center).matchesKey(BiomeKeys.BASALT_DELTAS);
        }
        
        if (World.END.equals(world.getRegistryKey())) {
            if (cultureType != Culture.CultureType.END) return false;
            
            // End villages should only spawn in the outer islands, not the central island
            // Check if we're at least 1000 blocks from the center (0,0)
            int distanceFromCenter = (int) Math.sqrt(center.getSquaredDistance(0, center.getY(), 0));
            return distanceFromCenter > 1000 && 
                   (world.getBiome(center).matchesKey(BiomeKeys.END_MIDLANDS) || 
                    world.getBiome(center).matchesKey(BiomeKeys.END_HIGHLANDS) ||
                    world.getBiome(center).matchesKey(BiomeKeys.END_BARRENS));
        }

        // Get biome at position for overworld cultures
        return switch (cultureType) {
            case EGYPTIAN -> world.getBiome(center).matchesKey(BiomeKeys.DESERT);
            case ROMAN -> world.getBiome(center).matchesKey(BiomeKeys.PLAINS);
            case VICTORIAN -> world.getBiome(center).matchesKey(BiomeKeys.FOREST);
            case MODERN -> world.getBiome(center).matchesKey(BiomeKeys.WINDSWEPT_HILLS);
            default -> false;
        };
    }

    public boolean isValidSpawnLocation(World world, BlockPos pos) {
        // Basic checks
        if (!world.isChunkLoaded(pos)) {
            return false;
        }

        Culture.CultureType cultureType = culture.getType();
        
        // Dimension-specific checks
        if (cultureType == Culture.CultureType.NETHER) {
            return isValidNetherSpawn(world, pos);
        } else if (cultureType == Culture.CultureType.END) {
            return isValidEndSpawn(world, pos);
        }

        // Overworld checks
        return isValidOverworldSpawn(world, pos);
    }

    private boolean isValidNetherSpawn(World world, BlockPos pos) {
        // Check for netherrack platform and open space
        Block ground = world.getBlockState(pos.down()).getBlock();
        
        // Get biome-specific requirements
        if (world.getBiome(pos).matchesKey(BiomeKeys.CRIMSON_FOREST)) {
            // Crimson villages need more space for their unique structures
            return ground.getDefaultState().isOpaque() && 
                   hasEnoughSpace(world, pos, 8, 5);
        } else if (world.getBiome(pos).matchesKey(BiomeKeys.WARPED_FOREST)) {
            // Warped villages have taller structures
            return ground.getDefaultState().isOpaque() && 
                   hasEnoughSpace(world, pos, 7, 6);
        } else if (world.getBiome(pos).matchesKey(BiomeKeys.SOUL_SAND_VALLEY)) {
            // Soul Sand villages are wider and shorter
            return ground.getDefaultState().isOpaque() && 
                   hasEnoughSpace(world, pos, 9, 3);
        } else {
            // Default nether check
            return ground.getDefaultState().isOpaque() && 
                   hasEnoughSpace(world, pos, 6, 4);
        }
    }

    private boolean isValidEndSpawn(World world, BlockPos pos) {
        // Check for end stone platform and enough space for end structures
        Block ground = world.getBlockState(pos.down()).getBlock();
        
        // End villages need a lot of space for their unique floating structures
        return ground.getDefaultState().isOpaque() && 
               hasEnoughSpace(world, pos, 12, 8);
    }

    private boolean isValidOverworldSpawn(World world, BlockPos pos) {
        // Standard overworld checks with culture-specific requirements
        Block ground = world.getBlockState(pos.down()).getBlock();
        
        Culture.CultureType cultureType = culture.getType();
        
        return switch (cultureType) {
            case EGYPTIAN -> ground.getDefaultState().isOpaque() && 
                          hasEnoughSpace(world, pos, 7, 3); // Egyptian needs wider footprint
            case ROMAN -> ground.getDefaultState().isOpaque() && 
                       hasEnoughSpace(world, pos, 6, 4); // Roman needs taller buildings
            case VICTORIAN -> ground.getDefaultState().isOpaque() && 
                           hasEnoughSpace(world, pos, 5, 5); // Victorian has tall, narrow buildings
            case MODERN -> ground.getDefaultState().isOpaque() && 
                     hasEnoughSpace(world, pos, 4, 7); // Modern has skyscrapers
            default -> ground.getDefaultState().isOpaque() && 
                     hasEnoughSpace(world, pos, 5, 3); // Default
        };
    }

    private boolean hasEnoughSpace(World world, BlockPos pos, int width, int height) {
        for (int x = -width/2; x <= width/2; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = -width/2; z <= width/2; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (!world.getBlockState(checkPos).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Culture getCulture() {
        return culture;
    }
    
    public String getCultureName() {
        return culture.toString();
    }
    
    /**
     * Gets the culture as a string, useful for compatibility with code expecting a string
     * @return The culture as a string
     */
    public String getCultureAsString() {
        return culture.getName();
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public List<BlockPos> getCulturalStructures() {
        return new ArrayList<>(); // Placeholder implementation
    }

    public Map<String, BlockPos> getCulturalStructuresByType() {
        return new HashMap<>(); // Placeholder implementation
    }

    public List<BlockPos> getPointsOfInterest() {
        return new ArrayList<>(); // Placeholder implementation
    }
    
    /**
     * Gets the district at the specified position
     * @param pos The position to check
     * @return The district name or null if not in a district
     */
    public String getDistrictAtPosition(BlockPos pos) {
        // Placeholder implementation
        return isWithinRegion(pos) ? "village_center" : null;
    }

    public boolean isWithinRegion(BlockPos pos) {
        double distance = pos.getSquaredDistance(center);
        return distance <= radius * radius;
    }

    public void addPointOfInterest(BlockPos poi) {
        // Placeholder implementation
    }
}
