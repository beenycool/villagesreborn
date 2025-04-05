package com.beeny.accessors;

import net.minecraft.nbt.NbtCompound;

/**
 * Interface for accessing persistent data added by mixin.
 */
public interface ServerPlayerEntityAccessor {
    /**
     * Gets the custom persistent NBT data compound for VillagesReborn.
     * Ensures the compound is created if it doesn't exist.
     * @return The mod-specific NBT data compound.
     */
    NbtCompound villagesreborn_getPersistentData();

    // Optional: A setter might be useful if you need to replace the entire compound,
    // but usually modifying the returned compound is sufficient.
    // void villagesreborn_setPersistentData(NbtCompound data);
}