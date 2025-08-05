package com.beeny.commands.base;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.util.CommandMessageUtils;
import com.beeny.util.VillagerDataUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Base class for villager commands providing common utility methods
 */
public abstract class BaseVillagerCommand {
    
    // Search radius constants
    protected static final double NEAREST_SEARCH_RADIUS = 10.0;
    protected static final double LIST_SEARCH_RADIUS = 50.0;
    protected static final double FIND_SEARCH_RADIUS = 100.0;
    protected static final double STATS_SEARCH_RADIUS = 200.0;
    
    /**
     * Gets the villager the player is looking at or closest to the command source
     */
    protected static VillagerEntity getTargetVillager(ServerCommandSource source) {
        if (source.getPlayer() != null) {
            HitResult hitResult = source.getPlayer().raycast(10.0, 1.0f, false);
            if (hitResult instanceof EntityHitResult entityHitResult) {
                Entity hitEntity = entityHitResult.getEntity();
                if (hitEntity instanceof VillagerEntity) {
                    return (VillagerEntity) hitEntity;
                }
            }
        }
        return null;
    }
    
    /**
     * Finds the nearest villager to the command source
     */
    protected static VillagerEntity findNearestVillager(ServerCommandSource source) {
        return getAllVillagersInArea(source, NEAREST_SEARCH_RADIUS)
            .stream()
            .min(Comparator.comparingDouble(villager -> 
                source.getPosition().squaredDistanceTo(villager.getPos())))
            .orElse(null);
    }
    
    /**
     * Gets all villagers in a specified area around the command source
     */
    protected static List<VillagerEntity> getAllVillagersInArea(ServerCommandSource source, double radius) {
        World world = source.getWorld();
        Vec3d sourcePos = source.getPosition();
        
        return world.getEntitiesByClass(
            VillagerEntity.class,
            new Box(
                sourcePos.x - radius, sourcePos.y - radius, sourcePos.z - radius,
                sourcePos.x + radius, sourcePos.y + radius, sourcePos.z + radius
            ),
            v -> true
        );
    }
    
    /**
     * Gets only named villagers in the specified area
     */
    protected static List<VillagerEntity> getNamedVillagersInArea(ServerCommandSource source, double radius) {
        return getAllVillagersInArea(source, radius)
            .stream()
            .filter(villager -> {
                VillagerData data = VillagerDataUtils.getVillagerDataOrNull(villager);
                return data != null && !data.getName().isEmpty();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Gets villager name by UUID, returns null if not found
     */
    protected static String getVillagerNameById(ServerCommandSource source, String villagerUuid) {
        if (villagerUuid == null || villagerUuid.isEmpty()) {
            return null;
        }
        
        try {
            UUID uuid = UUID.fromString(villagerUuid);
            List<VillagerEntity> allVillagers = getAllVillagersInArea(source, 1000.0); // Large search
            
            return allVillagers.stream()
                .filter(v -> v.getUuid().equals(uuid))
                .findFirst()
                .map(VillagerDataUtils::getVillagerName)
                .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Validates that an entity is a villager
     */
    protected static boolean validateVillager(Entity entity, ServerCommandSource source) {
        if (!(entity instanceof VillagerEntity)) {
            CommandMessageUtils.sendError(source, "Entity is not a villager");
            return false;
        }
        return true;
    }
    
    /**
     * Validates that a villager has data attached
     */
    protected static boolean validateVillagerData(VillagerEntity villager, ServerCommandSource source) {
        if (!VillagerDataUtils.hasVillagerData(villager)) {
            CommandMessageUtils.sendError(source, "Villager has no data");
            return false;
        }
        return true;
    }
    
    /**
     * Gets villager data safely with error messaging
     */
    protected static Optional<VillagerData> getVillagerDataSafely(VillagerEntity villager, ServerCommandSource source) {
        Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
        if (dataOpt.isEmpty()) {
            CommandMessageUtils.sendError(source, "Villager has no data");
        }
        return dataOpt;
    }
    
    /**
     * Checks if the world is a server world
     */
    protected static boolean validateServerWorld(World world, ServerCommandSource source) {
        if (!(world instanceof ServerWorld)) {
            CommandMessageUtils.sendError(source, "Command can only be executed in server world");
            return false;
        }
        return true;
    }
}