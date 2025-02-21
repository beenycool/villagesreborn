package com.beeny.api;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface VillagerActivityCallback {
    Event<VillagerActivityCallback> EVENT = EventFactory.createArrayBacked(VillagerActivityCallback.class,
        (listeners) -> (villager, world, activity, pos) -> {
            for (VillagerActivityCallback listener : listeners) {
                listener.onActivity(villager, world, activity, pos);
            }
        });

    void onActivity(VillagerEntity villager, ServerWorld world, String activity, BlockPos pos);
}