package com.beeny.villagesreborn.core.common;

import java.util.UUID;

/**
 * Represents a villager entity in the game world
 */
public interface VillagerEntity {
    
    /**
     * Gets the unique identifier for this villager
     * @return the villager's UUID
     */
    UUID getUUID();
    
    /**
     * Gets the current block position of the villager
     * @return the block position
     */
    BlockPos getBlockPos();
    
    /**
     * Gets the villager's profession (farmer, librarian, etc.)
     * @return the profession string
     */
    String getProfession();
    
    /**
     * Gets the villager's display name
     * @return the display name
     */
    String getName();
    
    /**
     * Gets the persistent NBT data for this villager
     * @return the NBT compound containing persistent data
     */
    NBTCompound getPersistentData();
}