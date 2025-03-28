package com.beeny.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Interface for accessing protected members of ServerPlayerEntity
 */
public interface ServerPlayerEntityAccessor {
    /**
     * Gets the TrackedData for persistent data
     * This is a static method to avoid having to cast to the accessor
     * This accessor is used to save and load player's mod-specific data
     */
    static TrackedData<NbtCompound> getPersistentDataTracker() {
        return ServerPlayerEntity.PERSISTENT_DATA;
    }
}