package com.beeny.ai.planning;

import java.util.*;

/**
 * A* search-based GOAP planner.
 * Finds optimal sequence of actions to achieve goals.
 */
public class GOAPPlanner {
    
    private static class PlanNode {
        final GOAPWorldState state;
        final PlanNode parent;
        final GOAPAction action;
        final float gCost;
        float fCost;
        
        PlanNode(GOAPWorldState state, PlanNode parent, GOAPAction action, float gCost) {
            this.state = state;
            this.parent = parent;
            this.action = action;
            this.gCost = gCost;
        }
    }
    
    /**
     * Plan a sequence of actions to achieve the given goal.
     * 
     * @param currentState The current world state
     * @param goal The goal to achieve
     * @param availableActions Available actions to choose from
     * @return List of actions to execute, or fallback plan if planning fails
     */
    public static List<GOAPAction> plan(GOAPWorldState currentState, GOAPGoal goal, List<GOAPAction> availableActions) {
        // A* search for action sequence
        PriorityQueue<PlanNode> openSet = new PriorityQueue<>(Comparator.comparing(n -> n.fCost));
        Set<String> closedSet = new HashSet<>();
        
        PlanNode startNode = new PlanNode(currentState, null, null, 0);
        openSet.add(startNode);

        // Configurable limits to prevent infinite loops
        final int MAX_ITERATIONS = com.beeny.config.ConfigManager.getInt("goapMaxIterations", 1000);
        final long MAX_TIME_MS = com.beeny.config.ConfigManager.getInt("goapMaxTimeMs", 25);
        final int MAX_OPEN_SET_SIZE = com.beeny.config.ConfigManager.getInt("goapMaxOpenSetSize", 500);
        
        int iterations = 0;
        long startTime = System.currentTimeMillis();
        
        while (!openSet.isEmpty()) {
            // Safety checks
            iterations++;
            if (iterations > MAX_ITERATIONS ||
                (System.currentTimeMillis() - startTime) > MAX_TIME_MS ||
                openSet.size() > MAX_OPEN_SET_SIZE) {
                
                com.beeny.Villagersreborn.LOGGER.warn(
                    "GOAP planner aborted: iterations={}, timeMs={}, openSetSize={}. Limits: iter={}, timeMs={}, openSet={}",
                    iterations, (System.currentTimeMillis() - startTime), openSet.size(),
                    MAX_ITERATIONS, MAX_TIME_MS, MAX_OPEN_SET_SIZE
                );
                return createFallbackPlan(currentState, goal, availableActions);
            }

            PlanNode current = openSet.poll();
            
            String stateKey = ((GOAPWorldState.Simple) current.state).generateStateKey();
            if (closedSet.contains(stateKey)) continue;
            closedSet.add(stateKey);
            
            // Check if goal is satisfied
            if (((GOAPWorldState.Simple) current.state).satisfies(goal.desiredState)) {
                return reconstructPath(current);
            }
            
            // Explore possible actions
            for (GOAPAction action : availableActions) {
                if (action.canPerform(current.state)) {
                    GOAPWorldState newState = applyAction(current.state, action);
                    float newCost = current.gCost + action.getCost(current.state);
                    float heuristic = calculateHeuristic(newState, goal.desiredState);
                    
                    PlanNode neighbor = new PlanNode(newState, current, action, newCost);
                    neighbor.fCost = newCost + heuristic;
                    
                    String neighborKey = ((GOAPWorldState.Simple) newState).generateStateKey();
                    if (!closedSet.contains(neighborKey) && openSet.size() < MAX_OPEN_SET_SIZE) {
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        // No plan found, return fallback
        return createFallbackPlan(currentState, goal, availableActions);
    }
    
    private static GOAPWorldState applyAction(GOAPWorldState currentState, GOAPAction action) {
        GOAPWorldState newState = currentState.copy();
        GOAPWorldState effects = action.getEffects(currentState);
        ((GOAPWorldState.Simple) newState).applyEffects(effects);
        return newState;
    }
    
    private static float calculateHeuristic(GOAPWorldState current, GOAPWorldState goal) {
        int unsatisfiedGoals = 0;
        for (String key : goal.getKeys()) {
            if (!current.hasKey(key)) {
                unsatisfiedGoals++;
            } else {
                // Compare values using the interface methods
                Object currentValue = getStateValue(current, key);
                Object goalValue = getStateValue(goal, key);
                if (!Objects.equals(currentValue, goalValue)) {
                    unsatisfiedGoals++;
                }
            }
        }
        return unsatisfiedGoals;
    }
    
    private static Object getStateValue(GOAPWorldState state, String key) {
        // Try different types to get the actual value
        if (state.hasKey(key)) {
            // Try boolean first
            try {
                return state.getBool(key);
            } catch (Exception ignored) {}
            
            // Try int
            try {
                return state.getInt(key);
            } catch (Exception ignored) {}
            
            // Try float
            try {
                return state.getFloat(key);
            } catch (Exception ignored) {}
            
            // Default to string
            return state.getString(key);
        }
        return null;
    }
    
    private static List<GOAPAction> reconstructPath(PlanNode endNode) {
        List<GOAPAction> path = new ArrayList<>();
        PlanNode current = endNode;
        
        while (current.parent != null) {
            path.add(0, current.action);
            current = current.parent;
        }
        
        return path;
    }
    
    /**
     * Creates a simple fallback plan when full planning fails.
     */
    private static List<GOAPAction> createFallbackPlan(GOAPWorldState currentState, GOAPGoal goal, List<GOAPAction> availableActions) {
        List<GOAPAction> fallbackPlan = new ArrayList<>();
        
        // Find any action that can be performed and might help with the goal
        for (GOAPAction action : availableActions) {
            if (action.canPerform(currentState)) {
                GOAPWorldState effects = action.getEffects(currentState);
                // Check if this action helps with any goal state
                for (String key : goal.desiredState.getKeys()) {
                    if (effects.hasKey(key)) {
                        fallbackPlan.add(action);
                        return fallbackPlan; // Return single-action plan
                    }
                }
            }
        }
        
        // If no helpful action found, just pick first available action
        for (GOAPAction action : availableActions) {
            if (action.canPerform(currentState)) {
                fallbackPlan.add(action);
                break;
            }
        }
        
        return fallbackPlan;
    }
}