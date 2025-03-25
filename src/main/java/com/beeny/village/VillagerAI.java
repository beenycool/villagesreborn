package com.beeny.village;

import com.beeny.ai.LLMService;
import com.beeny.config.VillagesConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import com.beeny.village.event.VillageEvent;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.api.VillagerActivityCallback;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.util.math.Vec3d;

public class VillagerAI {
    // ... (rest of the code until the addPvPGoals method)

    public void addPvPGoals() {
        if (!VillagesConfig.getInstance().isVillagerPvPEnabled()) {
            return;
        }

        if (pvpEnabled) {
            return;
        }

        RevengeGoal revengeGoal = new RevengeGoal((PathAwareEntity)villager, VillagerEntity.class);
        addGoalToVillager(villager, 1, revengeGoal);

        ActiveTargetGoal<VillagerEntity> targetGoal = new ActiveTargetGoal<>(
            (MobEntity)villager,
            VillagerEntity.class,
            10,
            true,
            false,
            (LivingEntity livingEntity) -> {
                if (!(livingEntity instanceof VillagerEntity otherVillager)) {
                    return false;
                }
                RelationshipType relationship = getRelationshipWith(otherVillager.getUuid());
                return relationship == RelationshipType.ENEMY ||
                       (relationship == RelationshipType.RIVAL &&
                        otherVillager.squaredDistanceTo(villager) < 5);
            },
            null
        );

        addGoalToVillager(villager, 2, targetGoal);
        pvpGoals.put(villager.getUuid(), targetGoal);
        pvpEnabled = true;

        if (((MobEntity)(Object)villager).getTarget() != null) {
            villager.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED, 100, 1, false, false));
        }

        LOGGER.debug("Added PvP goals to villager: {}", villager.getName().getString());
    }

    // ... (rest of the code until the isSafeLocation method)

    private boolean isSafeLocation(ServerWorld world, BlockPos pos) {
        boolean hasRoof = false;
        for (int y = 1; y <= 4; y++) {
            if (!world.getBlockState(pos.up(y)).isAir()) {
                hasRoof = true;
                break;
            }
        }

        if (!hasRoof) return false;

        List<Monster> threats = world.getEntitiesByClass(
            Monster.class,
            new Box(pos).expand(8),
            (Monster monster) -> true
        );

        return threats.isEmpty();
    }

    // ... (rest of the code remains unchanged)
}
