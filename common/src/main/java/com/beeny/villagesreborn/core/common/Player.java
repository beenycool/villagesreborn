package com.beeny.villagesreborn.core.common;

import java.util.UUID;

/**
 * Simple Player abstraction for testing
 */
public class Player {
    private final UUID uuid;
    private BlockPos position;

    public Player(UUID uuid) {
        this.uuid = uuid;
        this.position = new BlockPos(0, 64, 0);
    }

    public UUID getUUID() {
        return uuid;
    }

    public BlockPos getBlockPos() {
        return position;
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }
}