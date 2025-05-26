package com.beeny.villagesreborn.core.expansion;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the resource cost for building construction
 */
public class ResourceCost {
    
    public enum ResourceType {
        WOOD,
        STONE,
        IRON,
        FOOD,
        EMERALD
    }
    
    private final Map<ResourceType, Integer> costs;
    
    public ResourceCost() {
        this.costs = new HashMap<>();
    }
    
    public ResourceCost(Map<ResourceType, Integer> costs) {
        this.costs = new HashMap<>(costs);
    }
    
    public ResourceCost addCost(ResourceType type, int amount) {
        costs.put(type, costs.getOrDefault(type, 0) + amount);
        return this;
    }
    
    public int getCost(ResourceType type) {
        return costs.getOrDefault(type, 0);
    }
    
    public Map<ResourceType, Integer> getAllCosts() {
        return new HashMap<>(costs);
    }
    
    public boolean isEmpty() {
        return costs.isEmpty() || costs.values().stream().allMatch(cost -> cost <= 0);
    }
    
    public int getTotalCost() {
        return costs.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    @Override
    public String toString() {
        return "ResourceCost{" + costs + "}";
    }
}