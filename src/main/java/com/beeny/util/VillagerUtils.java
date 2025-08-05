package com.beeny.util;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.ServerVillagerManager;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class VillagerUtils {
    
    // Cache for nearby villager lookups to reduce performance impact
    private static final Cache<String, List<VillagerEntity>> nearbyCache =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(5, TimeUnit.SECONDS)
                    .concurrencyLevel(4)
                    .build();
    
    /**
     * Get nearby villagers using ServerVillagerManager for optimal performance.
     * This method leverages the tracked villager list instead of scanning world chunks.
     */
    public static List<VillagerEntity> getNearbyVillagersOptimized(VillagerEntity center, double range) {
        if (center.getWorld().isClient) {
            return List.of();
        }
        
        String cacheKey = center.getUuidAsString() + "_opt_" + (int)range;
        List<VillagerEntity> cached = nearbyCache.getIfPresent(cacheKey);
        
        if (cached != null) {
            // Filter out dead villagers from cache
            return cached.stream()
                .filter(VillagerEntity::isAlive)
                .filter(v -> v.getWorld() == center.getWorld()) // Ensure same world
                .toList();
        }
        
        // Use ServerVillagerManager's tracked villagers for efficient lookup
        List<VillagerEntity> nearby = new ArrayList<>();
        Vec3d centerPos = center.getPos();
        double rangeSquared = range * range;
        
        try {
            ServerVillagerManager manager = ServerVillagerManager.getInstance();
            if (manager != null) {
                // Get all tracked villagers and filter by distance
                var trackedVillagers = manager.getAllTrackedVillagers();
                for (VillagerEntity villager : trackedVillagers) {
                    if (villager != center && 
                        villager.isAlive() && 
                        villager.getWorld() == center.getWorld()) {
                        
                        double distanceSquared = villager.getPos().squaredDistanceTo(centerPos);
                        if (distanceSquared <= rangeSquared) {
                            nearby.add(villager);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to world scan if ServerVillagerManager is not available
            return getNearbyVillagersFallback(center, range);
        }
        
        // Cache the result
        nearbyCache.put(cacheKey, nearby);
        
        return nearby;
    }
    
    /**
     * Fallback method using world scan (less efficient, but guaranteed to work)
     */
    private static List<VillagerEntity> getNearbyVillagersFallback(VillagerEntity center, double range) {
        return center.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(center.getBlockPos(), center.getBlockPos()).expand(range),
            v -> v != center && v.isAlive()
        );
    }
    
    /**
     * Get nearby villagers with caching to improve performance
     */
    public static List<VillagerEntity> getNearbyVillagers(VillagerEntity center, double range) {
        if (center.getWorld().isClient) {
            return List.of();
        }
        
        String cacheKey = center.getUuidAsString() + "_" + (int)range;
        List<VillagerEntity> cached = nearbyCache.getIfPresent(cacheKey);
        
        if (cached != null) {
            // Filter out dead villagers from cache
            return cached.stream()
                .filter(VillagerEntity::isAlive)
                .toList();
        }
        
        // Perform fresh lookup
        List<VillagerEntity> nearby = center.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(center.getBlockPos(), center.getBlockPos()).expand(range),
            v -> v != center && v.isAlive()
        );
        
        // Cache the result (Guava will handle expiration)
        nearbyCache.put(cacheKey, nearby);
        
        return nearby;
    }
    
    /**
     * Get nearby players
     */
    public static List<PlayerEntity> getNearbyPlayers(VillagerEntity center, double range) {
        if (center.getWorld().isClient) {
            return List.of();
        }
        
        return center.getWorld().getEntitiesByClass(
            PlayerEntity.class,
            BoundingBoxUtils.fromBlocks(center.getBlockPos(), center.getBlockPos()).expand(range),
            PlayerEntity::isAlive
        );
    }
    
    /**
     * Spawn happiness particles around a villager
     */
    public static void spawnHappinessParticles(ServerWorld world, Vec3d pos, int count) {
        ParticleUtils.spawnHappinessParticles(world, pos, count);
    }
    
    /**
     * Spawn heart particles between two positions
     */
    public static void spawnHeartParticles(ServerWorld world, Vec3d pos1, Vec3d pos2, int count) {
        ParticleUtils.spawnHeartParticles(world, pos1, pos2, count);
    }
    
    /**
     * Spawn custom particles around a position
     */
    public static void spawnParticles(ServerWorld world, Vec3d pos,
                                      net.minecraft.particle.ParticleEffect particle,
                                      int count, double spread) {
        ParticleUtils.spawnParticles(world, pos, particle, count, spread);
    }
    
    /**
     * Play sound at villager position
     */
    public static void playVillagerSound(ServerWorld world, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, pos, sound, SoundCategory.NEUTRAL, volume, pitch);
    }
    
    /**
     * Get villager data safely
     */
    public static VillagerData getVillagerData(VillagerEntity villager) {
        return villager.getAttached(Villagersreborn.VILLAGER_DATA);
    }
    
    /**
     * Apply happiness change to multiple villagers
     */
    public static void adjustHappinessForNearbyVillagers(List<VillagerEntity> villagers, int happinessChange) {
        for (VillagerEntity villager : villagers) {
            VillagerData data = getVillagerData(villager);
            if (data != null) {
                data.adjustHappiness(happinessChange);
            }
        }
    }
    
    /**
     * Check if it's a specific time of day
     */
    public static boolean isTimeOfDay(ServerWorld world, TimeOfDay timeOfDay) {
        long time = world.getTimeOfDay() % 24000;
        return switch (timeOfDay) {
            case DAWN -> time >= 0 && time < 2000;
            case MORNING -> time >= 2000 && time < 6000;
            case NOON -> time >= 6000 && time < 8000;
            case AFTERNOON -> time >= 8000 && time < 12000;
            case DUSK -> time >= 12000 && time < 14000;
            case NIGHT -> time >= 14000 && time < 24000;
        };
    }
    
    public enum TimeOfDay {
        DAWN, MORNING, NOON, AFTERNOON, DUSK, NIGHT
    }
    
    /**
     * Check if villager should perform random action
     */
    public static boolean shouldPerformRandomAction(float probability) {
        return ThreadLocalRandom.current().nextFloat() < probability;
    }
    
    /**
     * Get random element from array
     */
    public static <T> T getRandomElement(T[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }
    
    // Guava cache auto-expires entries; no manual cleanup needed.
    
    /**
     * Clear all caches (for cleanup when villagers die, etc.)
     */
    public static void clearCaches() {
        nearbyCache.invalidateAll();
    }
    
    /**
     * Check if villager is in a safe, well-lit area
     */
    public static boolean isInSafeArea(VillagerEntity villager) {
        ServerWorld world = (ServerWorld) villager.getWorld();
        BlockPos pos = villager.getBlockPos();
        
        // Check light level
        int lightLevel = world.getLightLevel(pos);
        if (lightLevel < 8) return false;
        
        // Check for nearby shelter (simplified)
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -1; y <= 3; y++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (world.getBlockState(checkPos).isOpaque()) {
                        return true; // Found some kind of shelter
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get happiness description with emoji
     */
    public static String getHappinessEmoji(int happiness) {
        if (happiness >= 90) return "😊";
        if (happiness >= 70) return "🙂";
        if (happiness >= 50) return "😐";
        if (happiness >= 30) return "😕";
        if (happiness >= 10) return "😞";
        return "😢";
    }
}