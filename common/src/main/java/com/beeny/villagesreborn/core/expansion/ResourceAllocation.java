package com.beeny.villagesreborn.core.expansion;

/**
 * Represents resource allocation for village expansion
 */
public class ResourceAllocation {
    private final int woodCost;
    private final int stoneCost;
    private final int foodCost;
    private final int ironCost;
    
    public ResourceAllocation(int woodCost, int stoneCost, int foodCost) {
        this(woodCost, stoneCost, foodCost, 0);
    }
    
    public ResourceAllocation(int woodCost, int stoneCost, int foodCost, int ironCost) {
        this.woodCost = Math.max(0, woodCost);
        this.stoneCost = Math.max(0, stoneCost);
        this.foodCost = Math.max(0, foodCost);
        this.ironCost = Math.max(0, ironCost);
    }
    
    public int getWoodCost() {
        return woodCost;
    }
    
    public int getStoneCost() {
        return stoneCost;
    }
    
    public int getFoodCost() {
        return foodCost;
    }
    
    public int getIronCost() {
        return ironCost;
    }
    
    public int getTotalCost() {
        return woodCost + stoneCost + foodCost + ironCost;
    }
    
    /**
     * Combines this allocation with another
     */
    public ResourceAllocation add(ResourceAllocation other) {
        return new ResourceAllocation(
            this.woodCost + other.woodCost,
            this.stoneCost + other.stoneCost,
            this.foodCost + other.foodCost,
            this.ironCost + other.ironCost
        );
    }
    
    /**
     * Scales this allocation by a factor
     */
    public ResourceAllocation scale(double factor) {
        return new ResourceAllocation(
            (int) (this.woodCost * factor),
            (int) (this.stoneCost * factor),
            (int) (this.foodCost * factor),
            (int) (this.ironCost * factor)
        );
    }
    
    @Override
    public String toString() {
        return String.format("ResourceAllocation{wood=%d, stone=%d, food=%d, iron=%d, total=%d}", 
                           woodCost, stoneCost, foodCost, ironCost, getTotalCost());
    }
}