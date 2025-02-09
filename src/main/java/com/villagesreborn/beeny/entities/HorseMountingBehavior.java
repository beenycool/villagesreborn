package com.villagesreborn.beeny.entities;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;
import java.util.List;

public class HorseMountingBehavior extends Goal {
    private final WanderingTraderEntity trader;
    private HorseEntity targetHorse;

    public HorseMountingBehavior(WanderingTraderEntity trader) {
        this.trader = trader;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (trader.hasVehicle()) {
            return false; // Already riding something
        }

        List<HorseEntity> nearbyHorses = trader.getWorld().getEntitiesByClass(HorseEntity.class, 
            trader.getBoundingBox().expand(20.0D), 
            horse -> !horse.hasPassengers() && horse.isTame() // Find untamed, available horses
        );

        if (!nearbyHorses.isEmpty()) {
            targetHorse = nearbyHorses.get(0); // Just take the first available horse for now
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        return targetHorse != null && !trader.hasVehicle() && targetHorse.isAlive();
    }

    @Override
    public void start() {
        if (targetHorse != null) {
            Path path = trader.getNavigation().findPathToEntity(targetHorse, 0);
            if (path != null) {
                trader.getNavigation().startMovingAlong(path, 0.7D); // Move towards the horse
            }
        }
    }

    @Override
    public void tick() {
        if (targetHorse != null && trader.isInRange(targetHorse, 3.0D)) {
            trader.startRiding(targetHorse); // Mount the horse
        }
    }

    @Override
    public void stop() {
        trader.getNavigation().stop();
        targetHorse = null;
    }
}
