package com.beeny;
import com.beeny.constants.VillagerConstants;

import com.beeny.ai.AIWorldManagerRefactored;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerRelationshipManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import com.beeny.util.BoundingBoxUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class TickHandler {

    private static int tickCounter = 0;

    private TickHandler() {}

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % VillagerConstants.Relationship.TICKS_MARRIAGE_CHECK == 0) {
                server.getWorlds().forEach(world -> {
                    if (world instanceof ServerWorld serverWorld) {
                        checkForMarriages(serverWorld);
                    }
                });
            }

            if (tickCounter % VillagerConstants.Relationship.TICKS_AGE_UPDATE == 0) {
                server.getWorlds().forEach(world -> {
                    if (world instanceof ServerWorld serverWorld) {
                        updateVillagerAges(serverWorld);
                    }
                });
            }

            if (tickCounter % VillagerConstants.Relationship.TICKS_SCHEDULE_UPDATE == 0) {
                server.getWorlds().forEach(world -> {
                    if (world instanceof ServerWorld serverWorld) {
                        AIWorldManagerRefactored instance = AIWorldManagerRefactored.getInstance();
                        if (instance != null) {
                            instance.getScheduleManager().updateSchedules(serverWorld);
                        }
                    }
                });
            }

            if (tickCounter % VillagerConstants.Relationship.TICKS_RELATIONSHIP_CLEANUP == 0) {
                VillagerRelationshipManager.cleanupStaleProposalTimes();
            }
        });
    }

    private static void checkForMarriages(ServerWorld world) {
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;

        int searchRadius = Math.min(
            32,
            VillagersRebornConfig.getBoundingBoxSize() / 4
        );
        Box searchBox = createBoundingBoxAroundPlayers(players, searchRadius);

        List<VillagerEntity> villagers = world.getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            v -> true
        );

        // Performance optimization: limit marriages per tick to avoid O(N²) issues
        int maxMarriagesPerTick = 1;
        int marriagesThisTick = 0;

        // Use shuffled iteration to ensure fair distribution over time
        java.util.Collections.shuffle(villagers, ThreadLocalRandom.current());

        for (int i = 0; i < villagers.size() && marriagesThisTick < maxMarriagesPerTick; i++) {
            VillagerEntity villager1 = villagers.get(i);
            
            // Skip if already married to avoid unnecessary checks
            VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data1 != null && !data1.getSpouseId().isEmpty()) {
                continue;
            }

            // Only check nearby villagers within marriage distance to reduce complexity
            List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                villager1.getBoundingBox().expand(VillagerConstants.Relationship.MARRIAGE_DISTANCE_THRESHOLD),
                v -> v != villager1 && v.getUuid().compareTo(villager1.getUuid()) > 0 // Avoid duplicate pairs
            );

            for (VillagerEntity villager2 : nearbyVillagers) {
                // Early exit if we've hit our marriage limit
                if (marriagesThisTick >= maxMarriagesPerTick) {
                    break;
                }

                // Skip if villager2 is already married
                VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
                if (data2 != null && !data2.getSpouseId().isEmpty()) {
                    continue;
                }

                double distance = villager1.getPos().distanceTo(villager2.getPos());
                if (distance <= VillagerConstants.Relationship.MARRIAGE_DISTANCE_THRESHOLD) {
                    if (ThreadLocalRandom.current().nextFloat() < VillagerConstants.Relationship.MARRIAGE_RANDOM_CHANCE) {
                        VillagerRelationshipManager.attemptMarriage(villager1, villager2);
                        marriagesThisTick++;
                        break; // Move to next villager1 after successful marriage
                    }
                }
            }
        }
    }

    private static Box createBoundingBoxAroundPlayers(List<ServerPlayerEntity> players, int radius) {
        if (players.isEmpty()) {
            return new Box(
                VillagerConstants.Relationship.DEFAULT_EMPTY_BOX_MIN,
                VillagerConstants.Relationship.DEFAULT_EMPTY_BOX_MIN,
                VillagerConstants.Relationship.DEFAULT_EMPTY_BOX_MIN,
                VillagerConstants.Relationship.DEFAULT_EMPTY_BOX_MAX,
                VillagerConstants.Relationship.DEFAULT_EMPTY_BOX_MAX,
                VillagerConstants.Relationship.DEFAULT_EMPTY_BOX_MAX
            );
        }

        BlockPos pos = players.get(0).getBlockPos();
        Box box = BoundingBoxUtils.fromBlocks(pos, pos);
        box = BoundingBoxUtils.inflate(box, radius);
        box = BoundingBoxUtils.clampToWorld(
            box,
            Integer.MIN_VALUE, VillagerConstants.Relationship.WORLD_MIN_Y, Integer.MIN_VALUE,
            Integer.MAX_VALUE, VillagerConstants.Relationship.WORLD_MAX_Y, Integer.MAX_VALUE
        );

        for (int i = 1; i < players.size(); i++) {
            pos = players.get(i).getBlockPos();
            Box next = BoundingBoxUtils.inflate(BoundingBoxUtils.fromBlocks(pos, pos), radius);
            next = BoundingBoxUtils.clampToWorld(
                next,
                Integer.MIN_VALUE, VillagerConstants.Relationship.WORLD_MIN_Y, Integer.MIN_VALUE,
                Integer.MAX_VALUE, VillagerConstants.Relationship.WORLD_MAX_Y, Integer.MAX_VALUE
            );
            box = BoundingBoxUtils.union(box, next);
        }

        return box;
    }

    private static void updateVillagerAges(ServerWorld world) {
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) return;

        int searchRadius = VillagersRebornConfig.getBoundingBoxSize();
        Box searchBox = createBoundingBoxAroundPlayers(players, searchRadius);

        List<VillagerEntity> villagers = world.getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            v -> true
        );

        for (VillagerEntity villager : villagers) {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                data.setAge(data.getAge() + 1);

                if (data.getHappiness() > VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD) {
                    data.adjustHappiness(-VillagersRebornConfig.HAPPINESS_DECAY_RATE);
                } else if (data.getHappiness() < VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD) {
                    data.adjustHappiness(VillagersRebornConfig.HAPPINESS_RECOVERY_RATE);
                }
            }
        }
    }
}