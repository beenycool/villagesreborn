package com.beeny.util;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Utility methods for spawning particles.
 */
public class ParticleUtils {

    /**
     * Spawn happiness particles around a position (typically a villager).
     */
    public static void spawnHappinessParticles(ServerWorld world, Vec3d pos, int count) {
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
            pos.x, pos.y + 1, pos.z,
            count, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Spawn heart particles between two positions.
     */
    public static void spawnHeartParticles(ServerWorld world, Vec3d pos1, Vec3d pos2, int count) {
        Vec3d midpoint = pos1.add(pos2).multiply(0.5);
        world.spawnParticles(ParticleTypes.HEART,
            midpoint.x, Math.max(pos1.y, pos2.y) + 1, midpoint.z,
            count, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Spawn custom particles around a position.
     */
    public static void spawnParticles(ServerWorld world, Vec3d pos,
                                      ParticleEffect particle,
                                      int count, double spread) {
        world.spawnParticles(particle,
            pos.x, pos.y, pos.z,
            count, spread, spread, spread, 0.1);
    }

    private ParticleUtils() {
        // no instances
    }
}