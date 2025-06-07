package com.beeny.villagesreborn.core.reputation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerReputationManager {

    private final Map<UUID, PlayerReputation> reputations = new HashMap<>();

    public PlayerReputation getReputation(UUID playerUuid) {
        return reputations.computeIfAbsent(playerUuid, PlayerReputation::new);
    }

    public void addReputation(UUID playerUuid, int amount) {
        getReputation(playerUuid).addReputation(amount);
    }
} 