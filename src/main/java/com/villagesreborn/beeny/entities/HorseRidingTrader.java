package com.villagesreborn.beeny.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.PrioritizedGoals;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.world.World;

public class HorseRidingTrader extends WanderingTraderEntity {

    public HorseRidingTrader(EntityType<? extends WanderingTraderEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(0, new HorseMountingBehavior(this)); // Priority 0 to mount horse first
    }

    public static HorseRidingTrader create(World world) {
        return new HorseRidingTrader(EntityType.WANDERING_TRADER, world);
    }
}
