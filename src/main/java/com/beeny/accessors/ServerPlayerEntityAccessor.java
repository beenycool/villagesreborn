package com.beeny.accessors;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Interface for accessing protected members of ServerPlayerEntity,
 * used by mixins to access persistent data.
 */
public interface ServerPlayerEntityAccessor {
    /**
     * Gets the persistent NBT data for the player
     * @return The player's persistent NBT data
     */
    NbtCompound getPersistentData();

    /**
     * Sets the persistent NBT data for the player
     * @param data The NBT data to set
     */
    void setPersistentData(NbtCompound data);

    /**
     * Gets the TrackedData reference for persistent data
     * @return The TrackedData field reference
     */
    TrackedData<NbtCompound> getPersistentDataField();
}
