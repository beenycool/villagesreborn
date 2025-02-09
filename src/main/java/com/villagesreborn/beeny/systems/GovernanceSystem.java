package com.villagesreborn.beeny.systems;

import java.util.concurrent.*;

public class GovernanceSystem {
    private float taxRate = 0.0f;
    private final ExecutorService policyExecutor = Executors.newSingleThreadExecutor();
    private final ConcurrentMap<String, Float> policyEffects = new ConcurrentHashMap<>();
    private final RoleManager roleManager;

    public GovernanceSystem(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setTaxRate(float rate) {
        PlayerRole currentRole = roleManager.getCurrentRole();
        if (currentRole == PlayerRole.TRADER) {
            // Trader role gets a tax rate bonus
            taxRate = Math.max(0.0f, Math.min(rate * 0.9f, 0.3f)); // Example: 10% less tax
        } else if (currentRole == PlayerRole.MAYOR) {
            // Mayor role can set higher taxes
            taxRate = Math.max(0.0f, Math.min(rate * 1.2f, 0.5f)); // Example: 20% higher tax limit
        }
         else {
            taxRate = Math.max(0.0f, Math.min(rate, 0.3f));
        }
    }

    public void applyPolicy(String policyName, float effect) {
        policyEffects.put(policyName, effect);
    }

    public void scheduleLeadershipChange(Runnable changeLogic) {
        policyExecutor.submit(changeLogic);
    }

    public float calculateEconomicImpact() {
        return taxRate + (float)policyEffects.values().stream().mapToDouble(f -> f).sum();
    }

    public void handlePlayerLeadershipAction(LeadershipAction action) {
    }
    
    public void handleMoodChange(Villager villager, EmotionManager.EmotionalState newState) {
        policyExecutor.submit(() -> {
            String logEntry = String.format("Villager %s mood changed to %s", 
                                          villager.hashCode(), newState);
            policyEffects.compute("moodImpact", (k, v) -> 
                v != null ? v + newState.ordinal() * 0.1f : newState.ordinal() * 0.1f);
        });
    }

    public interface LeadershipAction {
        void execute();
        String getDescription();
    }

    public void handleTaskCompletion(Task task) {
        policyExecutor.submit(() -> {
            float rewardFactor = 0.01f; // Example reward factor
            float taxAdjustment = task.getBaseReward() * rewardFactor;
            adjustTaxes(taxAdjustment, "Task Completion Reward");
        });
    }

    public void handleTaskFailure(Task task) {
        policyExecutor.submit(() -> {
            float penaltyFactor = 0.02f; // Example penalty factor, higher than reward
            float taxAdjustment = -task.getBaseCost() * penaltyFactor; // Negative adjustment
            adjustTaxes(taxAdjustment, "Task Failure Penalty");
        });
    }

    private void adjustTaxes(float adjustment, String reason) {
        taxRate = Math.max(0.0f, taxRate + adjustment);
        policyEffects.compute("economicEvents", (k, v) ->
                v != null ? v + adjustment : adjustment);
        Logger.info(String.format("Tax rate adjusted by %.2f due to %s. New rate: %.2f", adjustment, reason, taxRate));
    }
}
