package com.villagesreborn.beeny.entities;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PatrolBehavior extends Goal {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Villager villager;
    private final double patrolSpeed;
    private List<Vec3d> patrolWaypoints = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private static final int VILLAGE_RADIUS = 64; // Example radius, adjust as needed
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TICKS = 20; // 20 ticks = 1 second

    public PatrolBehavior(Villager villager, double patrolSpeed) {
        this.villager = villager;
        this.patrolSpeed = patrolSpeed;
    }

    @Override
    public boolean canStart() {
        return villager.getVillagerRole() == VillagerRole.PATROLLER && !villager.isBusy();
    }

    @Override
    public void tick() {
        LOGGER.debug("PatrolBehavior tick for villager {}", villager.getUuid());

        if (patrolWaypoints.isEmpty()) {
            LOGGER.debug("Patrol waypoints are empty, generating new waypoints for villager {}", villager.getUuid());
            generatePatrolWaypoints();
            if (patrolWaypoints.isEmpty()) {
                LOGGER.warn("Failed to generate patrol waypoints for villager {}", villager.getUuid());
                return;
            }
        }

        if (!patrolWaypoints.isEmpty()) {
            Vec3d currentWaypoint = patrolWaypoints.get(currentWaypointIndex);
            double distanceToWaypoint = villager.getPos().distanceTo(currentWaypoint);
            LOGGER.debug("Villager {} moving to waypoint {}, distance: {}", villager.getUuid(), currentWaypoint, distanceToWaypoint);


            if (distanceToWaypoint < 1.5D) { // Arrived at waypoint
                LOGGER.debug("Villager {} arrived at waypoint {}", villager.getUuid(), currentWaypoint);
                currentWaypointIndex = (currentWaypointIndex + 1) % patrolWaypoints.size(); // Move to next waypoint
        } else {
            LOGGER.debug("Ranged attack conditions not met for villager {}", villager.getUuid());
        }
         LOGGER.debug("Attack cooldown reset for villager {}", villager.getUuid());
    }
        }


        // If hostile mob detected within radius:
        if (villager.getTarget() != null) {
            if (villager.getMainHandStack().getItem() != Items.BOW) {
                villager.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            if (villager.getMainHandStack().getItem() == Items.BOW) {
                executeRangedAttack(villager.getTarget());
            }
        }
    }

    private void generatePatrolWaypoints() {
        LOGGER.debug("Generating patrol waypoints for villager {}", villager.getUuid());
        World world = villager.getWorld();
        List<Villager> allVillagers = world.getEntitiesByType(Villager.class, entity -> true); // Get all villagers in the world

        if (allVillagers.isEmpty()) {
            LOGGER.warn("No villagers found to calculate village center, patrol waypoints will not be generated for villager {}", villager.getUuid());
            return;
        }

        Vec3d villageCenter = new Vec3d(0, 0, 0);
        try {
            for (Villager v : allVillagers) {
                villageCenter = villageCenter.add(v.getPos());
            }
            villageCenter = villageCenter.multiply(1.0 / allVillagers.size()); // Average position
        } catch (Exception e) {
            LOGGER.error("Error calculating village center for patrol waypoints for villager {}", villager.getUuid(), e);
            return;
        }


        double maxDistSq = 0;
        for (Villager v : allVillagers) {
            double distSq = v.getPos().squaredDistanceTo(villageCenter);
            maxDistSq = Math.max(maxDistSq, distSq);
        }
        double villageRadius = Math.sqrt(maxDistSq) + VILLAGE_RADIUS; // Add VILLAGE_RADIUS offset
        LOGGER.debug("Calculated village center: {}, radius: {} for villager {}", villageCenter, villageRadius, villager.getUuid());


        for (int i = 0; i < 12; i++) { // Generate 12 waypoints for patrol
            double angle = 2 * Math.PI * i / 12;
            double x = villageCenter.x + villageRadius * Math.cos(angle);
            double z = villageCenter.z + villageRadius * Math.sin(angle);
            // find y at ground level
            int groundY = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z);
            Vec3d waypoint = new Vec3d(x, groundY, z);
            patrolWaypoints.add(waypoint);
            LOGGER.debug("Generated waypoint {} for villager {}", waypoint, villager.getUuid());
        }
    }

    @Override
    public boolean shouldContinue() {
        return villager.getVillagerRole() == VillagerRole.PATROLLER;
    }

    private void executeRangedAttack(MobEntity target) {
        LOGGER.debug("executeRangedAttack called for villager {} targeting {}", villager.getUuid(), target.getUuid());
        if (attackCooldown > 0) {
            attackCooldown--;
            LOGGER.debug("Ranged attack on cooldown for villager {}, cooldown remaining: {}", villager.getUuid(), attackCooldown);
            return; // Cooldown still active
        }
        if (villager.getLookControl().isLookingAt(target)) {
             villager.getLookControl().lookAt(target, 30.0F, 30.0F);
        }

        if (villager.canSee(target)) {
            ServerWorld world = (ServerWorld) villager.getWorld();
            ItemStack bowStack = villager.getMainHandStack();
            ItemStack arrowStack = findArrowInInventory();

            if (arrowStack.isEmpty()) {
                LOGGER.warn("No arrows found in inventory for villager {}, cannot perform ranged attack", villager.getUuid());
                return; // No arrows in inventory
            }

            villager.setCurrentHand(Hand.MAIN_HAND);
            PersistentProjectileEntity arrowEntity = createArrow(world, villager, target);
            try {
                world.spawnEntity(arrowEntity);
            } catch (Exception e) {
                LOGGER.error("Error spawning arrow entity for villager {}", villager.getUuid(), e);
                return;
            }


            world.playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.ENTITY_SKELETON_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

            arrowStack.decrement(1); // Consume arrow

            bowStack.damage(1, villager, (v) -> v.sendToolBreakStatus(Hand.MAIN_HAND));
            if (bowStack.getDamage() >= bowStack.getMaxDamage() - 1) { // -1 to prevent item disappearing before decrement
                bowStack.decrement(1);
            }
            attackCooldown = ATTACK_COOLDOWN_TICKS; // Reset cooldown
            LOGGER.debug("Ranged attack executed by villager {} on target {}, cooldown reset", villager.getUuid(), target.getUuid());
             LOGGER.debug("Attack cooldown reset for villager {}", villager.getUuid());
        }
    }

    private PersistentProjectileEntity createArrow(ServerWorld world, Villager shooter, MobEntity target) {
        ItemStack arrowItem = new ItemStack(Items.ARROW); // Arrow type
        PersistentProjectileEntity arrowEntity = new PersistentProjectileEntity(net.minecraft.entity.EntityType.ARROW, shooter, world);
        arrowEntity.setItem(arrowItem);
        double targetYOffset = target instanceof LivingEntity ? target.getEyeY() - 0.1D : target.getBoundingBox().maxY - 0.1D;
        arrowEntity.setVelocity(target.getX() - shooter.getX(), targetYOffset - arrowEntity.getY(), target.getZ() - shooter.getZ(), 1.0F, 14 - world.getDifficulty().getId() * 4); // Velocity and spread
        return arrowEntity;
    }

    private ItemStack findArrowInInventory() {
        if (villager instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) villager;
            if (player.getInventory().contains(Items.ARROW.getDefaultStack())) {
                for (int i = 0; i < player.getInventory().size(); ++i) {
                    ItemStack itemStack = player.getInventory().getStack(i);
                    if (itemStack.getItem() == Items.ARROW) {
                        return itemStack;
                    }
                }
            }
        } else {
             // For non-player villagers, check main inventory (SimpleInventory in VillagerBank)
            ItemStack arrowStack;
            VillagerBank bank = villager.getBank();
            if (bank != null) {
                SimpleInventory inventory = bank.getInventory();
                for (int i = 0; i < inventory.size(); i++) {
                    arrowStack = inventory.getStack(i);
                     if (arrowStack.getItem() == Items.ARROW && !arrowStack.isEmpty()) {
                        return arrowStack;
                    }
                }
            }
        }
       return ItemStack.EMPTY; // No arrows found
    }
}
