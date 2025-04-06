package com.beeny.data;

import com.beeny.accessors.ServerPlayerEntityAccessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement; // Import NbtElement
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

        // Check if the compound exists using contains
        if (!modDataRoot.contains(MOD_ID, NbtElement.COMPOUND_TYPE)) {
            modDataRoot.put(MOD_ID, new NbtCompound());
        }

        // Get the compound directly
        return modDataRoot.getCompound(MOD_ID);
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
