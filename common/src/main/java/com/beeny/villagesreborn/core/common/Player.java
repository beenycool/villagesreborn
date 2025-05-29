package com.beeny.villagesreborn.core.common;

import java.util.UUID;

/**
 * Simple Player abstraction for testing
 */
public class Player {
    private final UUID uuid;
    private final String name;
    private BlockPos position;

    public Player(UUID uuid) {
        this.uuid = uuid;
        this.name = "Player" + uuid.toString().substring(0, 8);
        this.position = new BlockPos(0, 64, 0);
    }

    public Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.position = new BlockPos(0, 64, 0);
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public BlockPos getBlockPos() {
        return position;
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }
    
    /**
     * Sends a message to the player
     * Platform implementations should override this method
     */
    public void sendMessage(String message) {
        // Default implementation - platform implementations should override
        System.out.println("To " + name + ": " + message);
    }
}