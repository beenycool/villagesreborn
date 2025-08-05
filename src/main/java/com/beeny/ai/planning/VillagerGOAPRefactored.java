package com.beeny.ai.planning;

import com.beeny.ai.core.AISubsystem;
import com.beeny.ai.planning.goals.VillagerGoals;
import com.beeny.ai.planning.actions.VillagerActions;
import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refactored GOAP system for villagers.
 * Clean, focused implementation using separated components.
 */
public class VillagerGOAPRefactored implements AISubsystem {
    
    // Instance-based manager for villager GOAP systems
    private final Map<String, List<GOAPGoal>> villagerGoals = new ConcurrentHashMap<>();
    private final Map<String, List<GOAPAction>> villagerActions = new ConcurrentHashMap<>();
    private final Map<String, List<GOAPAction>> currentPlans = new ConcurrentHashMap<>();
    private final Map<String, Integer> planExecutionIndex = new ConcurrentHashMap<>();
    
    @Override
    public void initializeVillager(@NotNull VillagerEntity villager, @NotNull VillagerData data) {
        String uuid = villager.getUuidAsString();
        
        // Initialize goals based on villager data and personality
        List<GOAPGoal> goals = new ArrayList<>();
        goals.add(new VillagerGoals.IncreaseHappinessGoal(villager));
        goals.add(new VillagerGoals.SocializeGoal(villager));
        goals.add(new VillagerGoals.WorkGoal(villager));
        goals.add(new VillagerGoals.FindLoveGoal(villager));
        goals.add(new VillagerGoals.LearnGossipGoal(villager));
        
        villagerGoals.put(uuid, goals);
        
        // Initialize available actions
        List<GOAPAction> actions = new ArrayList<>();
        actions.add(new VillagerActions.SocializeAction(villager));
        actions.add(new VillagerActions.WorkAction(villager));
        actions.add(new VillagerActions.GossipAction(villager));
        
        villagerActions.put(uuid, actions);
    }
    
    @Override
    public void updateVillager(@NotNull VillagerEntity villager) {
        String uuid = villager.getUuidAsString();
        GOAPWorldState currentState = VillagerWorldStateBuilder.buildCurrentWorldState(villager);
        
        // Get current plan or create new one
        List<GOAPAction> currentPlan = currentPlans.get(uuid);
        int executionIndex = planExecutionIndex.getOrDefault(uuid, 0);
        
        boolean needNewPlan = false;
        
        if (currentPlan == null || executionIndex >= currentPlan.size()) {
            needNewPlan = true;
        } else {
            // Check if current action can still be performed
            GOAPAction currentAction = currentPlan.get(executionIndex);
            if (!currentAction.canPerform(currentState)) {
                needNewPlan = true;
            }
        }
        
        if (needNewPlan) {
            GOAPGoal bestGoal = selectBestGoal(villager, currentState);
            if (bestGoal != null) {
                List<GOAPAction> newPlan = GOAPPlanner.plan(currentState, bestGoal, villagerActions.get(uuid));
                currentPlans.put(uuid, newPlan);
                planExecutionIndex.put(uuid, 0);
                executionIndex = 0;
                currentPlan = newPlan;
            }
        }
        
        // Execute current action
        if (currentPlan != null && executionIndex < currentPlan.size()) {
            GOAPAction currentAction = currentPlan.get(executionIndex);
            if (currentAction.execute(currentState)) {
                planExecutionIndex.put(uuid, executionIndex + 1);
            }
        }
    }
    
    @Override
    public void cleanupVillager(@NotNull String villagerUuid) {
        villagerGoals.remove(villagerUuid);
        villagerActions.remove(villagerUuid);
        currentPlans.remove(villagerUuid);
        planExecutionIndex.remove(villagerUuid);
    }
    
    @Override
    public boolean needsUpdate(@NotNull VillagerEntity villager) {
        // GOAP should always be evaluating and planning
        return true;
    }
    
    @Override
    public long getUpdateInterval() {
        return com.beeny.config.ConfigManager.getInt("aiGoapUpdateInterval", 3000);
    }
    
    @Override
    @NotNull
    public String getSubsystemName() {
        return "GOAP_Planning";
    }
    
    @Override
    public int getPriority() {
        return 30; // Medium-low priority - runs after more critical systems
    }
    
    @Override
    public void performMaintenance() {
        // Clean up stale planning data periodically
        // Remove plans for villagers that no longer exist
    }
    
    @Override
    public void shutdown() {
        villagerGoals.clear();
        villagerActions.clear();
        currentPlans.clear();
        planExecutionIndex.clear();
    }
    
    @Override
    @NotNull
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("total_villager_goals", villagerGoals.size());
        analytics.put("total_active_plans", currentPlans.size());
        analytics.put("average_plan_length", currentPlans.values().stream()
            .mapToInt(List::size)
            .average()
            .orElse(0.0));
        return analytics;
    }
    
    private GOAPGoal selectBestGoal(@NotNull VillagerEntity villager, @NotNull GOAPWorldState currentState) {
        String uuid = villager.getUuidAsString();
        List<GOAPGoal> goals = villagerGoals.get(uuid);
        if (goals == null) return null;
        
        return goals.stream()
            .filter(goal -> goal.isValid(currentState))
            .max(Comparator.comparing(goal -> goal.getFinalPriority(currentState)))
            .orElse(null);
    }
}