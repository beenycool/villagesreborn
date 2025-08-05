package com.beeny.ai.planning;

import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for GOAP goals.
 * Goals define desired world states and their priorities.
 */
public abstract class GOAPGoal {
    public final String name;
    public final float priority;
    public final GOAPWorldState desiredState;
    protected final VillagerEntity villager;
    
    public GOAPGoal(String name, float priority, VillagerEntity villager) {
        this.name = name;
        this.priority = priority;
        this.villager = villager;
        this.desiredState = new GOAPWorldState.Simple();
        defineGoalState();
    }
    
    /**
     * Define what world state this goal desires.
     */
    protected abstract void defineGoalState();
    
    /**
     * Check if this goal is valid given the current world state.
     */
    public abstract boolean isValid(@NotNull GOAPWorldState currentState);
    
    /**
     * Calculate dynamic priority based on current conditions.
     */
    public abstract float calculateDynamicPriority(@NotNull GOAPWorldState currentState);
    
    /**
     * Get the final priority for this goal considering validity and dynamic factors.
     */
    public float getFinalPriority(@NotNull GOAPWorldState currentState) {
        if (!isValid(currentState)) return 0.0f;
        return priority * calculateDynamicPriority(currentState);
    }
}