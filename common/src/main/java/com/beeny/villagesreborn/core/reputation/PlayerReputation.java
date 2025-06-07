package com.beeny.villagesreborn.core.reputation;

import java.util.UUID;

public class PlayerReputation {
    private final UUID playerUuid;
    private int reputation;
    private ReputationLevel level;

    public PlayerReputation(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.reputation = 0;
        this.level = ReputationLevel.NEUTRAL;
    }

    public void addReputation(int amount) {
        this.reputation += amount;
        updateReputationLevel();
    }

    private void updateReputationLevel() {
        if (reputation >= 1000) {
            level = ReputationLevel.HERO;
        } else if (reputation >= 500) {
            level = ReputationLevel.FRIENDLY;
        } else if (reputation > -500) {
            level = ReputationLevel.NEUTRAL;
        } else if (reputation > -1000) {
            level = ReputationLevel.UNFRIENDLY;
        } else {
            level = ReputationLevel.VILLAIN;
        }
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getReputation() {
        return reputation;
    }

    public ReputationLevel getLevel() {
        return level;
    }

    public enum ReputationLevel {
        HERO,
        FRIENDLY,
        NEUTRAL,
        UNFRIENDLY,
        VILLAIN
    }
} 