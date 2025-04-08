package com.beeny.village;

import com.beeny.config.VillagesConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Handles the practical effects of village and villager relationships
 * This includes allied villagers helping each other and hostile villagers attacking
 */
public class VillageRelationshipEffects {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageRelationshipEffects");
    private static final Random RANDOM = new Random();
    private static final VillageRelationshipEffects INSTANCE = new VillageRelationshipEffects();
    
    // Configuration values
    private static final int ALLY_CHECK_RADIUS = 20; // Blocks to check for allies in need
    private static final int HOSTILE_CHECK_RADIUS = 32; // Blocks to check for hostile villagers
    private static final int HEAL_COOLDOWN = 1200; // 1 minute cooldown between healing actions
    
    private VillageRelationshipEffects() {}
    
    public static VillageRelationshipEffects getInstance() {
        return INSTANCE;
    }
    
    /**
     * Process ally effects for a villager
     * @param villager The villager to process ally effects for
     */
    public void processAllyEffects(VillagerEntity villager) {
        if (!villager.isAlive() || villager.getWorld().isClient()) return;
        
        ServerWorld world = (ServerWorld) villager.getWorld();
        VillagerManager vm = VillagerManager.getInstance();
        VillagerAI villagerAI = vm.getVillagerAI(villager.getUuid());
        
        if (villagerAI == null || villagerAI.isBusy()) return;
        
        // Check for nearby allies in need
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            new Box(villager.getBlockPos()).expand(ALLY_CHECK_RADIUS),
            v -> v != villager && v.isAlive()
        );
        
        for (VillagerEntity nearbyVillager : nearbyVillagers) {
            UUID nearbyId = nearbyVillager.getUuid();
            // Check if this is an ally (friend or family)
            VillagerAI.RelationshipType relationship = villagerAI.getRelationshipWith(nearbyId);
            
            if (relationship == VillagerAI.RelationshipType.FRIEND || 
                relationship == VillagerAI.RelationshipType.FAMILY) {
                
                // Check if ally needs help
                if (isInNeedOfHelp(nearbyVillager)) {
                    helpAlly(villager, villagerAI, nearbyVillager, world);
                    break; // Only help one ally at a time
                }
            }
        }
    }
    
    /**
     * Process hostile effects for a villager
     * @param villager The villager to process hostile effects for
     */
    public void processHostileEffects(VillagerEntity villager) {
        if (!villager.isAlive() || villager.getWorld().isClient()) return;
        if (!VillagesConfig.getInstance().getGameplaySettings().isVillagerPvPEnabled()) return;
        
        ServerWorld world = (ServerWorld) villager.getWorld();
        VillagerManager vm = VillagerManager.getInstance();
        VillagerAI villagerAI = vm.getVillagerAI(villager.getUuid());
        
        if (villagerAI == null || villagerAI.isBusy()) return;
        
        // Check for nearby rivals/enemies
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            new Box(villager.getBlockPos()).expand(HOSTILE_CHECK_RADIUS),
            v -> v != villager && v.isAlive()
        );
        
        for (VillagerEntity nearbyVillager : nearbyVillagers) {
            UUID nearbyId = nearbyVillager.getUuid();
            VillagerAI.RelationshipType relationship = villagerAI.getRelationshipWith(nearbyId);
            
            // If this is an enemy, potentially attack (if PvP is enabled)
            if (relationship == VillagerAI.RelationshipType.ENEMY) {
                // Enable PvP goals if not already enabled
                if (!villagerAI.isPvPEnabled()) {
                    villagerAI.addPvPGoals();
                }
                
                // Set the target if close enough
                if (villager.squaredDistanceTo(nearbyVillager) < 12*12 && RANDOM.nextInt(3) == 0) {
                    if (villager instanceof MobEntity mobEntity) {
                        mobEntity.setTarget(nearbyVillager);
                        break; // Only target one enemy at a time
                    }
                }
            }
            // For rivals, just display negative particles occasionally
            else if (relationship == VillagerAI.RelationshipType.RIVAL && RANDOM.nextInt(20) == 0) {
                if (villager.squaredDistanceTo(nearbyVillager) < 8*8) {
                    showRivalryEffects(villager, nearbyVillager, world);
                }
            }
        }
    }
    
    /**
     * Check if a villager needs help (low health, being attacked, etc.)
     */
    private boolean isInNeedOfHelp(VillagerEntity villager) {
        // Check health - if below 40% they need help
        if (villager.getHealth() < villager.getMaxHealth() * 0.4f) {
            return true;
        }
        
        // Check if being attacked
        if (villager.getAttacker() != null && villager.getAttacker().isAlive()) {
            return true;
        }
        
        // Check brain memory for danger
        if (villager.getBrain().getOptionalMemory(MemoryModuleType.HURT_BY).isPresent() ||
            villager.getBrain().getOptionalMemory(MemoryModuleType.HURT_BY_ENTITY).isPresent()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Help an ally villager in need
     */
    private void helpAlly(VillagerEntity helper, VillagerAI helperAI, VillagerEntity allyInNeed, ServerWorld world) {
        // Check if on healing cooldown
        if (helper.age % HEAL_COOLDOWN != 0 && RANDOM.nextInt(5) != 0) return;
        
        helperAI.setBusy(true);
        
        // Move to help ally
        helperAI.updateActivity("helping_ally");
        BlockPos allyPos = allyInNeed.getBlockPos();
        
        // If helper has healing items, use them
        if (helper.getHealth() > helper.getMaxHealth() * 0.8f) {
            // Go help the ally
            moveToHelp(helper, allyInNeed);
            
            // Give positive effects if close enough
            if (helper.squaredDistanceTo(allyInNeed) < 3*3) {
                giveHelpEffects(helper, allyInNeed, world);
            }
        }
        
        // Check if ally is under attack - if so, defend them!
        LivingEntity attacker = allyInNeed.getAttacker();
        if (attacker != null && attacker.isAlive() && VillagesConfig.getInstance().getGameplaySettings().isVillagerPvPEnabled()) {
            // If the attacker is another villager and we have PvP enabled
            if (attacker instanceof VillagerEntity enemyVillager &&
                helperAI.getRelationshipWith(enemyVillager.getUuid()) != VillagerAI.RelationshipType.FRIEND &&
                helperAI.getRelationshipWith(enemyVillager.getUuid()) != VillagerAI.RelationshipType.FAMILY) {
                
                // Enable PvP goals if not already enabled
                if (!helperAI.isPvPEnabled()) {
                    helperAI.addPvPGoals();
                }
                
                // Set the enemy villager as target
                if (helper instanceof MobEntity mobEntity) {
                    mobEntity.setTarget(enemyVillager);
                }
            }
        }
        
        // After a short time, allow the villager to resume normal activities
        if (RANDOM.nextInt(10) == 0) {
            helperAI.setBusy(false);
        }
    }
    
    /**
     * Move a villager to help an ally
     */
    private void moveToHelp(VillagerEntity helper, VillagerEntity ally) {
        // Calculate a position near the ally
        double offset = 1.0 + RANDOM.nextDouble();
        double angle = RANDOM.nextDouble() * Math.PI * 2;
        
        double targetX = ally.getX() + Math.cos(angle) * offset;
        double targetZ = ally.getZ() + Math.sin(angle) * offset;
        
        // Move to this position
        helper.getNavigation().startMovingTo(targetX, ally.getY(), targetZ, 1.0);
        
        // Look at the ally
        helper.getLookControl().lookAt(ally, 30.0F, 30.0F);
    }
    
    /**
     * Give help effects from one villager to another
     */
    private void giveHelpEffects(VillagerEntity helper, VillagerEntity ally, ServerWorld world) {
        // Healing effect - grant regeneration to the ally
        ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
        
        // Show particle effects between them
        Vec3d helperPos = helper.getPos().add(0, 1.0, 0);
        Vec3d allyPos = ally.getPos().add(0, 1.0, 0);
        Vec3d particlePos = helperPos.add(allyPos.subtract(helperPos).multiply(0.5));
        
        world.spawnParticles(
            ParticleTypes.HEART,
            particlePos.x, particlePos.y, particlePos.z,
            5, 0.2, 0.2, 0.2, 0.02
        );
        
        // Animation of giving an item (just visual)
        if (RANDOM.nextInt(3) == 0) {
            ItemStack healingItem = new ItemStack(Items.APPLE);
            helper.setStackInHand(Hand.MAIN_HAND, healingItem);
            helper.swingHand(Hand.MAIN_HAND);
            
            // Clear the hand after a delay
            helper.getWorld().getServer().execute(() -> {
                helper.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            });
        }
    }
    
    /**
     * Show rivalry/hostility effects between villagers
     */
    private void showRivalryEffects(VillagerEntity villager1, VillagerEntity villager2, ServerWorld world) {
        // Calculate midpoint between villagers
        double x = (villager1.getX() + villager2.getX()) / 2;
        double y = (villager1.getY() + villager2.getY()) / 2 + 1;
        double z = (villager1.getZ() + villager2.getZ()) / 2;
        
        // Show angry particles
        world.spawnParticles(
            ParticleTypes.ANGRY_VILLAGER,
            x, y, z,
            3, 0.3, 0.3, 0.3, 0.02
        );
        
        // Make them look at each other
        villager1.getLookControl().lookAt(villager2, 30.0F, 30.0F);
        villager2.getLookControl().lookAt(villager1, 30.0F, 30.0F);
    }
    
    /**
     * Process inter-village relationship effects
     * Handles allied or hostile villages interactions
     */
    public void processVillageRelationships(BlockPos village1, BlockPos village2) {
        VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
        VillageInfluenceManager.VillageRelationship relationship = vim.getVillageRelationship(village1, village2);
        
        if (relationship == null) return;
        
        VillageInfluenceManager.RelationshipStatus status = relationship.getStatus();
        
        // For allied villages, villagers should help each other more readily
        if (status == VillageInfluenceManager.RelationshipStatus.ALLIED) {
            // Boost relationship between individual villagers from allied villages
            boostInterVillageRelationships(village1, village2, true);
        }
        // For hostile villages, villagers may actively antagonize each other
        else if (status == VillageInfluenceManager.RelationshipStatus.HOSTILE) {
            // Decrease relationship between individual villagers from hostile villages
            boostInterVillageRelationships(village1, village2, false);
        }
    }
    
    /**
     * Boost (or reduce) relationships between villagers of different villages
     */
    private void boostInterVillageRelationships(BlockPos village1, BlockPos village2, boolean positive) {
        // Only process occasionally to avoid performance issues
        if (RANDOM.nextInt(20) != 0) return;
        
        VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
        VillagerManager vm = VillagerManager.getInstance();
        
        // Get a random villager from each village
        Optional<VillagerEntity> optVillager1 = getRandomVillagerFromVillage(village1);
        Optional<VillagerEntity> optVillager2 = getRandomVillagerFromVillage(village2);
        
        if (optVillager1.isPresent() && optVillager2.isPresent()) {
            VillagerEntity villager1 = optVillager1.get();
            VillagerEntity villager2 = optVillager2.get();
            
            VillagerAI ai1 = vm.getVillagerAI(villager1.getUuid());
            
            if (ai1 != null) {
                // Set relationship based on village relationship
                VillagerAI.RelationshipType type = positive ? 
                    VillagerAI.RelationshipType.FRIEND : 
                    VillagerAI.RelationshipType.RIVAL;
                
                // Check if they don't already have the opposite relationship type
                VillagerAI.RelationshipType currentRelationship = ai1.getRelationshipWith(villager2.getUuid());
                
                // Only update if not already the target type or an extreme version of it
                if ((positive && currentRelationship != VillagerAI.RelationshipType.FRIEND && 
                     currentRelationship != VillagerAI.RelationshipType.FAMILY) ||
                    (!positive && currentRelationship != VillagerAI.RelationshipType.RIVAL && 
                     currentRelationship != VillagerAI.RelationshipType.ENEMY)) {
                    
                    // Update relationships
                    ai1.addRelationship(villager2.getUuid(), type);
                    
                    VillagerAI ai2 = vm.getVillagerAI(villager2.getUuid());
                    if (ai2 != null) {
                        ai2.addRelationship(villager1.getUuid(), type);
                    }
                }
            }
        }
    }
    
    /**
     * Get a random villager from a village
     */
    private Optional<VillagerEntity> getRandomVillagerFromVillage(BlockPos villageCenter) {
        for (ServerWorld world : VillagerManager.getInstance().getServer().getWorlds()) {
            // Check if world contains this village center
            if (world.isChunkLoaded(villageCenter.getX() >> 4, villageCenter.getZ() >> 4)) {
                List<VillagerEntity> villagers = world.getEntitiesByClass(
                    VillagerEntity.class,
                    new Box(villageCenter).expand(64),
                    villager -> true
                );
                
                if (!villagers.isEmpty()) {
                    return Optional.of(villagers.get(RANDOM.nextInt(villagers.size())));
                }
            }
        }
        
        return Optional.empty();
    }
}