package com.beeny.villagesreborn.core.expansion;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a resource allocation plan for village expansion
 */
public class ResourcePlan {
    
    private final Map<ResourceCost.ResourceType, ResourceAllocation> allocations;
    private final int totalBudget;
    private final int priorityPhases;
    
    public ResourcePlan(int totalBudget, int priorityPhases) {
        this.allocations = new HashMap<>();
        this.totalBudget = totalBudget;
        this.priorityPhases = priorityPhases;
    }
    
    public ResourcePlan addAllocation(ResourceCost.ResourceType type, int amount, int phase) {
        allocations.put(type, new ResourceAllocation(amount, phase));
        return this;
    }
    
    public ResourceAllocation getAllocation(ResourceCost.ResourceType type) {
        return allocations.get(type);
    }
    
    public Map<ResourceCost.ResourceType, ResourceAllocation> getAllAllocations() {
        return new HashMap<>(allocations);
    }
    
    public int getTotalBudget() {
        return totalBudget;
    }
    
    public int getPriorityPhases() {
        return priorityPhases;
    }
    
    public boolean canAfford(ResourceCost cost) {
        for (Map.Entry<ResourceCost.ResourceType, Integer> entry : cost.getAllCosts().entrySet()) {
            ResourceAllocation allocation = allocations.get(entry.getKey());
            if (allocation == null || allocation.amount < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    public int getTotalAllocated() {
        return allocations.values().stream()
                          .mapToInt(alloc -> alloc.amount)
                          .sum();
    }
    
    @Override
    public String toString() {
        return String.format("ResourcePlan{budget=%d, phases=%d, allocations=%s}", 
                           totalBudget, priorityPhases, allocations);
    }
    
    /**
     * Represents allocation of a specific resource type
     */
    public static class ResourceAllocation {
        private final int amount;
        private final int phase;
        
        public ResourceAllocation(int amount, int phase) {
            this.amount = amount;
            this.phase = phase;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public int getPhase() {
            return phase;
        }
        
        @Override
        public String toString() {
            return String.format("ResourceAllocation{amount=%d, phase=%d}", amount, phase);
        }
    }
}