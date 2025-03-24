package com.beeny.village;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.BiomeKeys;

public class SpawnRegion {
    private final Culture culture;
    private final BlockPos center;
    private final int radius;

    public SpawnRegion(Culture culture, BlockPos center, int radius) {
        this.culture = culture;
        this.center = center;
        this.radius = radius;
    }

    public boolean canSpawnInDimension(World world) {
        RegistryKey<World> dimension = world.getRegistryKey();
        return switch (culture) {
            case NETHER -> World.NETHER.equals(dimension);
            case END -> World.END.equals(dimension);
            default -> World.OVERWORLD.equals(dimension);
        };
    }

    public boolean canSpawnInBiome(Biome biome, World world) {
        if (!canSpawnInDimension(world)) {
            return false;
        }

        // Handle dimension-specific cultures
        if (World.NETHER.equals(world.getRegistryKey())) {
            return culture == Culture.NETHER;
        }
        if (World.END.equals(world.getRegistryKey())) {
            return culture == Culture.END;
        }

        // Get biome at position
        return switch (culture) {
            case EGYPTIAN -> world.getBiome(center).matchesKey(BiomeKeys.DESERT);
            case ROMAN -> world.getBiome(center).matchesKey(BiomeKeys.PLAINS);
            case VICTORIAN -> world.getBiome(center).matchesKey(BiomeKeys.FOREST);
            case NYC -> world.getBiome(center).matchesKey(BiomeKeys.WINDSWEPT_HILLS);
            default -> false;
        };
    }

    public boolean isValidSpawnLocation(World world, BlockPos pos) {
        // Basic checks
        if (!world.isChunkLoaded(pos)) {
            return false;
        }

        // Dimension-specific checks
        if (culture == Culture.NETHER) {
            return isValidNetherSpawn(world, pos);
        } else if (culture == Culture.END) {
            return isValidEndSpawn(world, pos);
        }

        // Overworld checks
        return isValidOverworldSpawn(world, pos);
    }

    private boolean isValidNetherSpawn(World world, BlockPos pos) {
        // Check for netherrack platform and open space
        Block ground = world.getBlockState(pos.down()).getBlock();
        return ground.getDefaultState().isOpaque() && 
               hasEnoughSpace(world, pos, 6, 4);
    }

    private boolean isValidEndSpawn(World world, BlockPos pos) {
        // Check for end stone platform
        Block ground = world.getBlockState(pos.down()).getBlock();
        return ground.getDefaultState().isOpaque() && 
               hasEnoughSpace(world, pos, 8, 6);
    }

    private boolean isValidOverworldSpawn(World world, BlockPos pos) {
        // Standard overworld checks
        Block ground = world.getBlockState(pos.down()).getBlock();
        return ground.getDefaultState().isOpaque() && 
               hasEnoughSpace(world, pos, 5, 3);
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

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }
}
