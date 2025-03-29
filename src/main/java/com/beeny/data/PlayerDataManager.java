package com.beeny.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerDataManager {
    private static final String MOD_ID = "villagesreborn";

    public static NbtCompound getOrCreatePersistentData(ServerPlayerEntity player) {
        if (!player.getPersistentData().contains(MOD_ID)) {
            NbtCompound modData = new NbtCompound();
            player.getPersistentData().put(MOD_ID, modData);
        }

        return player.getPersistentData().getCompound(MOD_ID);
    }

    public static void savePersistentData(ServerPlayerEntity player, NbtCompound data) {
        NbtCompound persistentData = player.getPersistentData();
        persistentData.put(MOD_ID, data);
        // player.getDataTracker().set(ServerPlayerEntityAccessor.getPersistentDataTracker(), persistentData); // Not needed
    }
}
