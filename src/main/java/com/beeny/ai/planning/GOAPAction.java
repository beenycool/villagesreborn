package com.beeny.ai.planning;

import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for GOAP actions.
 * Actions can be performed to change the world state toward achieving goals.
 */
public abstract class GOAPAction {
    public final String name;
    public final float baseCost;
    protected final VillagerEntity villager;
    
    public GOAPAction(String name, float baseCost, VillagerEntity villager) {
        this.name = name;
        this.baseCost = baseCost;
        this.villager = villager;
    }
    
    /**
     * Check if this action can be performed given the current world state.
     */
    public abstract boolean canPerform(@NotNull GOAPWorldState state);
    
    /**
     * Get the effects this action will have on the world state.
     */
    @NotNull
    public abstract GOAPWorldState getEffects(@NotNull GOAPWorldState state);
    
    /**
     * Get the cost of performing this action in the current state.
     */
    public abstract float getCost(@NotNull GOAPWorldState state);
    
    /**
     * Execute this action and modify the world state.
     * @return true if the action was successfully executed
     */
    public abstract boolean execute(@NotNull GOAPWorldState state);
    
    /**
     * Get the preconditions required for this action to be performed.
     */
    @NotNull
    public abstract GOAPWorldState getPreconditions();
}