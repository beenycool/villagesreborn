package com.beeny.villagesreborn.core.reputation;

import java.util.UUID;

public class ReputationEventHandler {

    private final PlayerReputationManager reputationManager;

    public ReputationEventHandler(PlayerReputationManager reputationManager) {
        this.reputationManager = reputationManager;
    }

    public void onQuestCompleted(UUID playerUuid, String questId) {
        // Example: Award reputation for completing a quest
        // The amount could be based on the quest's difficulty or importance
        reputationManager.addReputation(playerUuid, 50);
    }
    
    public void onVillagerHarmed(UUID playerUuid, UUID villagerUuid) {
        reputationManager.addReputation(playerUuid, -10);
    }
} 