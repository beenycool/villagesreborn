package com.beeny.data;

import com.beeny.accessors.ServerPlayerEntityAccessor;
import net.minecraft.nbt.NbtCompound;
// Remove NbtElement import if not needed, ensure NbtCompound is imported
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Manages persistent player data for VillagesReborn.
 */
public class PlayerDataManager {

    private static final String MOD_ID = "villagesreborn";

    /**
     * Gets or creates the persistent data compound for the given player.
     * @param player The player entity.
     * @return The mod-specific NBT data compound.
     */
    public static NbtCompound getOrCreatePersistentData(ServerPlayerEntity player) {
        ServerPlayerEntityAccessor accessor = (ServerPlayerEntityAccessor) player;
        NbtCompound modDataRoot = accessor.villagesreborn_getPersistentData();

        // Check if the compound exists by trying to get it and checking if the Optional is present
        if (!modDataRoot.getCompound(MOD_ID).isPresent()) {
            modDataRoot.put(MOD_ID, new NbtCompound());
        }

        // We know the compound exists because we created it above if it didn't.
        return modDataRoot.getCompound(MOD_ID).get();
    }

    /**
     * Saves the given data to the player's persistent data compound.
     * @param player The player entity.
     * @param data The data to save.
     */
    public static void savePersistentData(ServerPlayerEntity player, NbtCompound data) {
        ServerPlayerEntityAccessor accessor = (ServerPlayerEntityAccessor) player;
        NbtCompound modDataRoot = accessor.villagesreborn_getPersistentData();
        modDataRoot.put(MOD_ID, data);
    }
}
