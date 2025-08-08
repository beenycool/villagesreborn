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

        for (int i = 0; i < villagers.size(); i++) {
            VillagerEntity villager1 = villagers.get(i);
            boolean married = false;

            for (int j = i + 1; j < villagers.size(); j++) {
                VillagerEntity villager2 = villagers.get(j);

                double distance = villager1.getPos().distanceTo(villager2.getPos());
                if (distance <= VillagerConstants.Relationship.MARRIAGE_DISTANCE_THRESHOLD) {
                    if (ThreadLocalRandom.current().nextFloat() < VillagerConstants.Relationship.MARRIAGE_RANDOM_CHANCE) {
                        VillagerRelationshipManager.attemptMarriage(villager1, villager2);
                        married = true;
                        break; // Exit inner loop after successful marriage
                    }
                }
            }
            if (married) continue; // Optional: skip remaining checks for this villager
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