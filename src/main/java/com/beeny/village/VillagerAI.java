package com.beeny.village;

import com.beeny.ai.LLMService;
import com.beeny.config.VillagesConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    // Relationship type enum used by Relationship class
    public enum RelationshipType {
        FRIEND,
        FAMILY,
        RIVAL,
        NEUTRAL,
        ENEMY // Added missing value
    }
    
    // ActivitySchedule class used by CulturalEvolution class
    public static class ActivitySchedule {
        public final String activity;
        public final BlockPos location;
        public final int duration;
        
        public ActivitySchedule(String activity, BlockPos location, int duration) {
            this.activity = activity;
            this.location = location;
            this.duration = duration;
        }
    }
    
    private VillagerEntity villager;
    private String personality;
    private String currentActivity;
    private int happiness = 50;
    private boolean busy = false;
    private boolean pvpEnabled = false;
    private Map<UUID, ActiveTargetGoal<? extends LivingEntity>> pvpGoals = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerAI.class);
    
    // Store active goals to manage them
    private Map<String, Goal> activeGoals = new HashMap<>();
    private Map<String, Integer> goalPriorities = new HashMap<>();
    
    public VillagerAI(VillagerEntity villager, String personality) {
        this.villager = villager;
        this.personality = personality;
        this.currentActivity = "idle";
    }
    
    public VillagerEntity getVillager() {
        return villager;
    }
    
    public String getPersonality() {
        return personality;
    }
    
    public String getCurrentActivity() {
        return currentActivity;
    }
    
    public int getHappiness() {
        return happiness;
    }
    
    public void adjustHappiness(int delta) {
        this.happiness = Math.max(0, Math.min(100, this.happiness + delta));
    }
    
    public boolean isBusy() {
        return busy;
    }
    
    public void setBusy(boolean busy) {
        this.busy = busy;
    }
    
    public void updateActivity(String activity) {
        this.currentActivity = activity;
    }
    
    public void updateActivityBasedOnTime(ServerWorld world) {
        long time = world.getTimeOfDay() % 24000;
        if (time < 1000 || time > 13000) {
            updateActivity("sleeping");
        } else if (time > 11500) {
            updateActivity("returning_home");
        } else if (time > 9000) {
            updateActivity("working");
        } else {
            updateActivity("socializing");
        }
    }
    
    public CompletableFuture<String> generateBehavior(String situation) {
        Map<String, String> context = new HashMap<>();
        context.put("timeout", "60");

        String prompt = String.format("Villager personality: %s, Situation: %s", personality, situation);
        return LLMService.getInstance().generateResponse(prompt, context)
            .thenApply(this::parseBehaviorAndUpdateGoals)
            .exceptionally(ex -> {
                LOGGER.error("Error generating behavior", ex);
                return "Default behavior";
            });
    }
    
    /**
     * Parses the behavior string from LLM and updates the villager's goals accordingly
     * @param behaviorString The behavior string from the LLM
     * @return The original behavior string for logging purposes
     */
    public String parseBehaviorAndUpdateGoals(String behaviorString) {
        if (behaviorString == null || behaviorString.isEmpty()) {
            return "Empty behavior";
        }
        
        LOGGER.debug("Parsing behavior for {}: {}", villager.getName().getString(), behaviorString);
        
        try {
            Map<String, String> parsedBehavior = parseBehaviorString(behaviorString);
            
            // Get action and target from the parsed behavior
            String action = parsedBehavior.getOrDefault("ACTION", "").toLowerCase();
            String target = parsedBehavior.getOrDefault("TARGET", "").toLowerCase();
            
            // Clear previous goals that might conflict
            clearGoalsForNewBehavior(action);
            
            // Apply the new goals based on action and target
            applyBehaviorGoals(action, target);
            
        } catch (Exception e) {
            LOGGER.error("Error parsing behavior string", e);
        }
        
        return behaviorString;
    }
    
    /**
     * Parses a behavior string into a map of key-value pairs
     * @param behaviorString The behavior string from the LLM
     * @return A map of parsed keys and values
     */
    private Map<String, String> parseBehaviorString(String behaviorString) {
        Map<String, String> result = new HashMap<>();
        
        // Define patterns to extract key-value pairs
        // Looking for format like "ACTION: work", "TARGET: nearest_farm", etc.
        Pattern pattern = Pattern.compile("(\\w+):\\s*(.+)(?:\\n|$)");
        Matcher matcher = pattern.matcher(behaviorString);
        
        while (matcher.find()) {
            String key = matcher.group(1).trim().toUpperCase();
            String value = matcher.group(2).trim();
            result.put(key, value);
            LOGGER.debug("Parsed behavior component: {} = {}", key, value);
        }
        
        return result;
    }
    
    /**
     * Clears goals that might conflict with the new behavior
     * @param action The new action to be performed
     */
    private void clearGoalsForNewBehavior(String action) {
        // If we have a MobEntity reference, we can use that to clear goals
        if (villager instanceof MobEntity) {
            MobEntity mobEntity = (MobEntity) villager;
            
            // First, remove any active goals we're tracking
            for (Goal goal : activeGoals.values()) {
                removeGoal(goal);
            }
            
            activeGoals.clear();
            goalPriorities.clear();
        } else {
            LOGGER.warn("Cannot clear goals for entity: Not a MobEntity");
        }
    }
    
    /**
     * Applies goals based on the parsed behavior
     * @param action The action to perform
     * @param target The target of the action
     */
    private void applyBehaviorGoals(String action, String target) {
        if (action.isEmpty()) {
            LOGGER.warn("Empty action in behavior, no goals applied");
            return;
        }
        
        switch (action) {
            case "work":
                applyWorkGoals(target);
                break;
            case "socialize":
            case "talk":
                applySocializeGoals(target);
                break;
            case "flee":
            case "run":
            case "escape":
                applyFleeGoals(target);
                break;
            case "wander":
            case "explore":
                applyWanderGoals(target);
                break;
            case "rest":
            case "sleep":
                applyRestGoals(target);
                break;
            case "trade":
                applyTradeGoals(target);
                break;
            case "follow":
                applyFollowGoals(target);
                break;
            case "look":
            case "watch":
                applyLookGoals(target);
                break;
            default:
                LOGGER.info("Unknown action: {}, defaulting to wander behavior", action);
                applyWanderGoals("around");
                break;
        }
    }
    
    /**
     * Applies goals related to working
     * @param target The target location or type
     */
    private void applyWorkGoals(String target) {
        // Update the activity
        updateActivity("working");
        
        // Work behavior typically involves going to job site
        if (target.contains("farm")) {
            // For farmers, try to find farmland
            findAndMoveToBlockType(Blocks.FARMLAND, 5, 1);
        } else if (target.contains("job") || target.contains("work")) {
            // Go to profession workstation
            moveToWorkstation();
        } else {
            // Default work behavior - wander near home with occasional pauses
            Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.6D);
            addGoal(wanderGoal, 3, "work_wander");
        }
        
        // Add a look goal with low priority
        Goal lookGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 8.0F, 0.02F);
        addGoal(lookGoal, 5, "work_look");
    }
    
    /**
     * Applies goals related to socializing
     * @param target The target entity or location
     */
    private void applySocializeGoals(String target) {
        updateActivity("socializing");
        
        // Higher priority for looking at entities
        Goal lookGoal = new LookAtEntityGoal(villager, VillagerEntity.class, 8.0F, 0.8F);
        addGoal(lookGoal, 2, "socialize_look_villager");
        
        // Also look at players
        Goal lookPlayerGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 8.0F, 0.4F);
        addGoal(lookPlayerGoal, 3, "socialize_look_player");
        
        // Lower priority wandering to move around the village
        Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.5D);
        addGoal(wanderGoal, 4, "socialize_wander");
    }
    
    /**
     * Applies goals related to fleeing
     * @param target What to flee from
     */
    private void applyFleeGoals(String target) {
        updateActivity("fleeing");
        
        // Priority for escape danger
        Goal escapeGoal = new EscapeDangerGoal((PathAwareEntity)villager, 1.2D);
        addGoal(escapeGoal, 1, "flee_danger");
        
        if (target.contains("player")) {
            // Avoid players
            Goal avoidPlayerGoal = new AvoidEntityGoal<>((PathAwareEntity)villager, PlayerEntity.class, 8.0F, 1.0D, 1.2D);
            addGoal(avoidPlayerGoal, 2, "flee_player");
        } else if (target.contains("monster") || target.contains("danger")) {
            // Avoid monsters - this is a generic goal
            addGoal(escapeGoal, 1, "flee_monster");
        }
        
        // Add a wander far goal for more distance
        Goal wanderFarGoal = new WanderAroundFarGoal((PathAwareEntity)villager, 1.0D);
        addGoal(wanderFarGoal, 3, "flee_wander");
    }
    
    /**
     * Applies goals related to wandering/exploring
     * @param target The target area or direction
     */
    private void applyWanderGoals(String target) {
        updateActivity("exploring");
        
        // Wander goals with different speeds based on target
        if (target.contains("far") || target.contains("explore")) {
            Goal wanderFarGoal = new WanderAroundFarGoal((PathAwareEntity)villager, 0.8D);
            addGoal(wanderFarGoal, 2, "wander_far");
        } else {
            Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.6D);
            addGoal(wanderGoal, 2, "wander_normal");
        }
        
        // Add look goals with lower priority
        Goal lookGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 8.0F, 0.02F);
        addGoal(lookGoal, 4, "wander_look");
    }
    
    /**
     * Applies goals related to resting/sleeping
     * @param target The target location
     */
    private void applyRestGoals(String target) {
        updateActivity("resting");
        
        // Lower speed wandering, occasional looks
        Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.3D);
        addGoal(wanderGoal, 3, "rest_wander");
        
        // If target specifies bed or home, try to go there
        if (target.contains("bed") || target.contains("home")) {
            // Try to find a bed or move toward home
            findAndMoveToBlockType(Blocks.RED_BED, 10, 1);
            // Could add more bed types here
        }
    }
    
    /**
     * Applies goals related to trading
     * @param target The target entity
     */
    private void applyTradeGoals(String target) {
        updateActivity("trading");
        
        // Higher priority for looking at entities
        Goal lookGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 8.0F, 0.8F);
        addGoal(lookGoal, 1, "trade_look");
        
        // Minimal wandering - should stay near trading spot
        Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.3D);
        addGoal(wanderGoal, 5, "trade_wander");
    }
    
    /**
     * Applies goals related to following
     * @param target What to follow
     */
    private void applyFollowGoals(String target) {
        updateActivity("following");
        
        if (target.contains("player")) {
            // Follow players - since we can't easily add FollowPlayerGoal
            // We'll simulate with a look goal and a wander goal
            Goal lookPlayerGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 12.0F, 0.9F);
            addGoal(lookPlayerGoal, 1, "follow_look_player");
            
            // Add a fast wander
            Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.8D);
            addGoal(wanderGoal, 2, "follow_wander");
        } else if (target.contains("villager")) {
            // Follow other villagers
            Goal lookVillagerGoal = new LookAtEntityGoal(villager, VillagerEntity.class, 12.0F, 0.9F);
            addGoal(lookVillagerGoal, 1, "follow_look_villager");
            
            // Add a fast wander
            Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.7D);
            addGoal(wanderGoal, 2, "follow_wander");
        }
    }
    
    /**
     * Applies goals related to looking/watching
     * @param target What to look at
     */
    private void applyLookGoals(String target) {
        updateActivity("watching");
        
        if (target.contains("player")) {
            // Look at players
            Goal lookPlayerGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 12.0F, 0.95F);
            addGoal(lookPlayerGoal, 1, "look_player");
        } else if (target.contains("villager")) {
            // Look at villagers
            Goal lookVillagerGoal = new LookAtEntityGoal(villager, VillagerEntity.class, 12.0F, 0.95F);
            addGoal(lookVillagerGoal, 1, "look_villager");
        } else {
            // Look around generally
            Goal lookPlayerGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 8.0F, 0.4F);
            addGoal(lookPlayerGoal, 2, "look_general_player");
            
            Goal lookVillagerGoal = new LookAtEntityGoal(villager, VillagerEntity.class, 8.0F, 0.4F);
            addGoal(lookVillagerGoal, 3, "look_general_villager");
        }
        
        // Very slow wandering
        Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.2D);
        addGoal(wanderGoal, 5, "look_wander");
    }
    
    /**
     * Helper method to find a specific block type and move to it
     * @param blockType The block to look for
     * @param searchRadius The radius to search in
     * @param priority The priority for the goal
     */
    private void findAndMoveToBlockType(Block blockType, int searchRadius, int priority) {
        ServerWorld serverWorld = (ServerWorld) villager.getWorld();
        BlockPos villagerPos = villager.getBlockPos();
        
        // Look for the block in a radius around the villager
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = villagerPos.add(x, y, z);
                    if (serverWorld.getBlockState(checkPos).getBlock() == blockType) {
                        // Found a matching block, create a goal to move to it
                        moveToPosition(checkPos, 0.5D, priority);
                        return;
                    }
                }
            }
        }
        
        // If no matching block was found, just wander
        Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.5D);
        addGoal(wanderGoal, priority + 1, "wander_fallback");
    }
    
    /**
     * Helper method to move to the villager's workstation
     */
    private void moveToWorkstation() {
        // Try to get the workstation from the villager's brain
        villager.getBrain().getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.JOB_SITE)
            .ifPresent(pos -> {
                moveToPosition(pos, 0.5D, 1);
                LOGGER.debug("Moving villager {} to workstation at {}", villager.getName().getString(), pos);
            });
    }
    
    /**
     * Helper method to move to a specific position
     * @param pos The position to move to
     * @param speed The movement speed
     * @param priority The priority for the goal
     */
    private void moveToPosition(BlockPos pos, double speed, int priority) {
        // Create a custom MoveToTargetPosGoal, tuned for immediate movement
        class SimpleMoveToGoal extends MoveToTargetPosGoal {
            private final BlockPos targetPos;
            
            public SimpleMoveToGoal(PathAwareEntity entity, double speed, BlockPos targetPos) {
                super(entity, speed, 1, 1); // reduced range for more precise movement
                this.targetPos = targetPos;
            }
            
            @Override
            protected boolean isTargetPos(net.minecraft.world.WorldView world, BlockPos pos) {
                return true; // Always consider our target valid
            }
            
            @Override
            public void start() {
                this.mob.getNavigation().startMovingTo(
                    targetPos.getX() + 0.5, 
                    targetPos.getY(), 
                    targetPos.getZ() + 0.5, 
                    this.speed
                );
            }
            
            @Override
            protected BlockPos getTargetPos() {
                return targetPos;
            }
        }
        
        SimpleMoveToGoal moveGoal = new SimpleMoveToGoal((PathAwareEntity)villager, speed, pos);
        addGoal(moveGoal, priority, "move_to_pos");
    }

    /**
     * Adds a goal to the villager with the given priority and identifier
     * @param goal The goal to add
     * @param priority The priority (lower number = higher priority)
     * @param id An identifier for tracking the goal
     */
    private void addGoal(Goal goal, int priority, String id) {
        try {
            // Check if the villager is a MobEntity (it should be)
            if (villager instanceof MobEntity) {
                MobEntity mobEntity = (MobEntity) villager;
                
                // Using reflection to access the goal selector and add the goal
                try {
                    // Get the goalSelector field
                    java.lang.reflect.Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                    goalSelectorField.setAccessible(true);
                    
                    // Get the GoalSelector instance
                    GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(mobEntity);
                    
                    // Add the goal
                    goalSelector.add(priority, goal);
                    
                    // Store the goal for later management
                    activeGoals.put(id, goal);
                    goalPriorities.put(id, priority);
                    
                    LOGGER.debug("Added goal {} with priority {} to villager {}", 
                        goal.getClass().getSimpleName(), priority, villager.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("Failed to add goal using reflection: {}", e.getMessage());
                }
            } else {
                LOGGER.warn("Cannot add goal to entity: Not a MobEntity");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to add goal to villager: {}", e.getMessage());
        }
    }
    
    /**
     * Removes a goal from the villager
     * @param goal The goal to remove
     */
    private void removeGoal(Goal goal) {
        try {
            // Check if the villager is a MobEntity (it should be)
            if (villager instanceof MobEntity) {
                MobEntity mobEntity = (MobEntity) villager;
                
                // Using reflection to access the goal selector and remove the goal
                try {
                    // Get the goalSelector field
                    java.lang.reflect.Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                    goalSelectorField.setAccessible(true);
                    
                    // Get the GoalSelector instance
                    GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(mobEntity);
                    
                    // Get the goals field from GoalSelector
                    java.lang.reflect.Field goalsField = GoalSelector.class.getDeclaredField("goals");
                    goalsField.setAccessible(true);
                    
                    // Get the goals set
                    Set<PrioritizedGoal> goals = (Set<PrioritizedGoal>) goalsField.get(goalSelector);
                    
                    // Create a copy to avoid concurrent modification
                    Set<PrioritizedGoal> copy = new HashSet<>(goals);
                    
                    // Find and remove the goal
                    for (PrioritizedGoal prioritizedGoal : copy) {
                        // Use reflection to get the goal field
                        java.lang.reflect.Field goalField = PrioritizedGoal.class.getDeclaredField("goal");
                        goalField.setAccessible(true);
                        Goal goalObj = (Goal) goalField.get(prioritizedGoal);
                        
                        if (goalObj == goal) {
                            goalSelector.remove(goal);
                            LOGGER.debug("Removed goal {} from villager {}", 
                                goal.getClass().getSimpleName(), villager.getName().getString());
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to remove goal using reflection: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove goal from villager: {}", e.getMessage());
        }
    }
    
    private Map<UUID, RelationshipType> relationships = new ConcurrentHashMap<>();
    
    public void addRelationship(UUID targetVillager, RelationshipType type) {
        relationships.put(targetVillager, type);
        adjustHappiness(type == RelationshipType.FRIEND || type == RelationshipType.FAMILY ? 5 : -5);
    }

    public void recordTheft(UUID thiefId, String itemDescription, BlockPos location) {
        // Implementation for recording theft
        LOGGER.info("Theft recorded: Thief={}, Item={}, Location={}", thiefId, itemDescription, location);
    }

    public int getFriendshipLevel() {
        long friendCount = relationships.values().stream()
            .filter(r -> r == RelationshipType.FRIEND || r == RelationshipType.FAMILY)
            .count();
        return (int)Math.min(10, friendCount);
    }

    public void updateProfessionSkill(String skill) {
        // Implementation for updating profession skill
        LOGGER.info("Updated profession skill: {}", skill);
    }

    public RelationshipType getRelationshipWith(UUID villagerId) {
        return relationships.getOrDefault(villagerId, RelationshipType.NEUTRAL);
    }

    public CompletableFuture<String> generateDialogue(String input, Object context) {
        Map<String, String> contextMap = new HashMap<>();
        contextMap.put("timeout", "45");
        
        String prompt = String.format(
            "Villager personality: %s, Happiness: %d, Activity: %s, Input: %s",
            personality, happiness, currentActivity, input
        );
        return LLMService.getInstance().generateResponse(prompt, contextMap)
            .exceptionally(ex -> "Hello there!");
    }

    public void learnFromEvent(VillageEvent event, String reflection) {
        // Implementation for learning from events
        LOGGER.info("Learning from event: {}, Reflection={}", event, reflection);
    }

    public String getSocialSummary() {
        int friends = (int)relationships.values().stream()
            .filter(r -> r == RelationshipType.FRIEND)
            .count();
        int family = (int)relationships.values().stream()
            .filter(r -> r == RelationshipType.FAMILY)
            .count();
        int rivals = (int)relationships.values().stream()
            .filter(r -> r == RelationshipType.RIVAL)
            .count();
        int enemies = (int)relationships.values().stream()
            .filter(r -> r == RelationshipType.ENEMY)
            .count();
        
        return String.format("Friends: %d, Family: %d, Rivals: %d, Enemies: %d", 
            friends, family, rivals, enemies);
    }

    public void setHappiness(int happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }

    public void addPvPGoals() {
        if (!VillagesConfig.getInstance().isVillagerPvPEnabled()) {
            return;
        }

        if (pvpEnabled) {
            return;
        }

        RevengeGoal revengeGoal = new RevengeGoal((PathAwareEntity)villager, VillagerEntity.class);
        addGoal(revengeGoal, 1, "pvp_revenge");

        // Custom targeting goal for villager PvP
        class VillagerTargetGoal extends ActiveTargetGoal<VillagerEntity> {
            private final VillagerEntity owner;
            
            VillagerTargetGoal(VillagerEntity owner) {
                super((MobEntity)(Object)owner, VillagerEntity.class, true);
                this.owner = owner;
            }
            
            @Override
            public boolean canStart() {
                return super.canStart();  // Let parent handle basic checks
            }

            @Override
            public boolean shouldContinue() {
                if (!super.shouldContinue()) {
                    return false;
                }
                
                if (this.targetEntity instanceof VillagerEntity otherVillager) {
                    RelationshipType relationship = getRelationshipWith(otherVillager.getUuid());
                    return relationship == RelationshipType.ENEMY ||
                          (relationship == RelationshipType.RIVAL &&
                           otherVillager.squaredDistanceTo(owner) < 5);
                }
                return false;
            }
        }

        ActiveTargetGoal<VillagerEntity> targetGoal = new VillagerTargetGoal(villager);

        addGoal(targetGoal, 2, "pvp_target");
        pvpGoals.put(villager.getUuid(), targetGoal);
        pvpEnabled = true;

        if (((MobEntity)(Object)villager).getTarget() != null) {
            villager.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED, 100, 1, false, false));
        }

        LOGGER.debug("Added PvP goals to villager: {}", villager.getName().getString());
    }

    private boolean isSafeLocation(ServerWorld world, BlockPos pos) {
        boolean hasRoof = false;
        for (int y = 1; y <= 4; y++) {
            if (!world.getBlockState(pos.up(y)).isAir()) {
                hasRoof = true;
                break;
            }
        }

        if (!hasRoof) return false;

        // Simplified approach - do a simpler query since we can't get the predicate right
        // This still achieves the same result by checking for monsters in the area
        List<Entity> entities = world.getOtherEntities(null, new Box(pos).expand(8));
        for (Entity entity : entities) {
            if (entity instanceof Monster) {
                return false;
            }
        }

        return true;
    }
}
