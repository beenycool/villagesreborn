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
// Remove AvoidEntityGoal import as it doesn't exist in this version
// Define a custom implementation below instead
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
import net.minecraft.registry.Registries; // Import Registries
// Remove RegistryKey import if not needed
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.util.math.Vec3d;

public class VillagerAI {
    // Custom implementation of AvoidEntityGoal for 1.21.4 since it might be renamed or restructured
    public static class AvoidEntityGoal<T extends LivingEntity> extends Goal {
        protected final PathAwareEntity mob;
        private final double walkSpeedModifier;
        private final double sprintSpeedModifier;
        protected T targetEntity;
        protected final float fleeDistance;
        protected final Class<T> classToAvoid;
        protected final Predicate<LivingEntity> avoidPredicate;
        protected final Predicate<LivingEntity> predicateOnAvoidEntity;
        
        public AvoidEntityGoal(PathAwareEntity mob, Class<T> entityClassToAvoid, float distance, double walkSpeed, double sprintSpeed) {
            this.mob = mob;
            this.classToAvoid = entityClassToAvoid;
            this.avoidPredicate = (entity) -> true;
            this.predicateOnAvoidEntity = (entity) -> true;
            this.fleeDistance = distance;
            this.walkSpeedModifier = walkSpeed;
            this.sprintSpeedModifier = sprintSpeed;
        }
        
        @Override
        public boolean canStart() {
            List<T> list = this.mob.getWorld().getEntitiesByClass(
                this.classToAvoid, 
                this.mob.getBoundingBox().expand(this.fleeDistance, 3.0, this.fleeDistance), 
                (entity) -> this.predicateOnAvoidEntity.test(entity)
            );
            
            if (list.isEmpty()) {
                return false;
            }
            
            this.targetEntity = getNearestEntity(list, this.mob);
            return true;
        }
        
        private T getNearestEntity(List<T> entities, Entity entity) {
            if (entities.isEmpty()) {
                return null;
            }
            
            double shortestDistance = Double.MAX_VALUE;
            T closestEntity = null;
            
            for (T e : entities) {
                double distance = entity.squaredDistanceTo(e);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    closestEntity = e;
                }
            }
            
            return closestEntity;
        }
        
        @Override
        public void start() {
            this.flee();
        }
        
        protected void flee() {
            Vec3d targetPos = getFurthestPosition();
            if (targetPos != null) {
                this.mob.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, this.walkSpeedModifier);
            }
        }
        
        private Vec3d getFurthestPosition() {
            Vec3d mobPos = this.mob.getPos();
            Vec3d targetPos = this.targetEntity.getPos();
            
            // Calculate vector away from target
            Vec3d direction = mobPos.subtract(targetPos).normalize();
            
            // Scale by flee distance
            double distance = 10.0; // how far to try to run
            direction = direction.multiply(distance);
            
            // Calculate destination
            return mobPos.add(direction);
        }
        
        @Override
        public boolean shouldContinue() {
            return this.targetEntity != null && 
                   this.targetEntity.isAlive() && 
                   this.mob.squaredDistanceTo(this.targetEntity) < this.fleeDistance * this.fleeDistance;
        }
    }

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
    
    // Track LLM-suggested activities and their durations
    private String llmSuggestedActivity = null;
    private long llmActivityExpiration = 0;
    private long lastActivityTransition = 0;
    
    // Track active behavior execution
    private String activeBehavior = null;
    private long behaviorStartTime = 0;
    private long behaviorDuration = 0;
    private Map<String, Object> behaviorParams = new HashMap<>();
    
    // Store profession skills
    private Map<String, Float> professionSkills = new HashMap<>();
    
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
        if (!activity.equals(currentActivity)) {
            // Only log if activity actually changes
            LOGGER.debug("Villager {} activity changed from {} to {}", 
                villager.getName().getString(), currentActivity, activity);
            
            // Fire the activity change callback for other systems to respond
            if (villager.getWorld() instanceof ServerWorld serverWorld) {
                VillagerActivityCallback.EVENT.invoker().onActivity(
                    villager, serverWorld, activity, villager.getBlockPos());
            }
        }
        this.currentActivity = activity;
    }
    
    /**
     * Sets an activity suggested by the LLM with an expiration time
     * @param activity The suggested activity
     * @param durationTicks How long the activity should last in game ticks
     */
    public void setLLMSuggestedActivity(String activity, long durationTicks) {
        this.llmSuggestedActivity = activity;
        this.llmActivityExpiration = villager.getWorld().getTime() + durationTicks;
        updateActivity(activity);
        LOGGER.debug("LLM suggested activity {} for {} ticks for villager {}", 
            activity, durationTicks, villager.getName().getString());
    }
    
    /**
     * Updates the villager's activity based on time of day, considering cultural context
     * and any LLM-suggested activities.
     * 
     * @param world The server world
     */
    public void updateActivityBasedOnTime(ServerWorld world) {
        long currentTime = world.getTimeOfDay();
        long timeSinceLastTransition = currentTime - lastActivityTransition;
        
        // Check if there's an active LLM-suggested activity
        if (llmSuggestedActivity != null && currentTime < llmActivityExpiration) {
            // The LLM-suggested activity is still valid, keep it
            return;
        } else if (llmSuggestedActivity != null) {
            // The LLM-suggested activity has expired
            llmSuggestedActivity = null;
        }
        
        // Get cultural context for more specific behaviors
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        String culture = region != null ? region.getCultureAsString().toLowerCase() : "generic";
        
        // Apply time-based default behavior
        String newActivity;
        long timeOfDay = currentTime % 24000;
        
        if (timeOfDay < 1000 || timeOfDay > 13000) {
            newActivity = "sleeping";
        } else if (timeOfDay > 11500) {
            newActivity = "returning_home";
        } else if (timeOfDay > 9000) {
            newActivity = "working";
        } else {
            newActivity = "socializing";
        }
        
        // Don't change activity if we're busy with something important
        if (busy) {
            return;
        }
        
        // Apply the new activity if it's different from the current one
        if (!newActivity.equals(currentActivity)) {
            // Record the transition time
            lastActivityTransition = currentTime;
            
            // Update the activity
            updateActivity(newActivity);
            
            // Apply appropriate behavior goals based on the new activity
            switch (newActivity) {
                case "sleeping":
                    applyRestGoals("bed");
                    break;
                case "returning_home":
                    // Find and move to home location
                    villager.getBrain().getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.HOME)
                        .ifPresent(pos -> moveToPosition(pos, 0.6D, 1));
                    break;
                case "working":
                    // Apply work goals appropriate for their profession
                    applyWorkGoals("job");
                    break;
                case "socializing":
                    // Apply socialize goals
                    applySocializeGoals("villager");
                    break;
                default:
                    // Default to wandering
                    applyWanderGoals("around");
            }
            
            // For major activity transitions, potentially generate LLM behavior
            if ((timeOfDay == 1000 || timeOfDay == 9000 || timeOfDay == 13000) && 
                    Math.random() < 0.3) { // 30% chance to use LLM at key transitions
                String situation = String.format("It's %s and this villager is %s in a %s village", 
                    getTimeDescription(timeOfDay), newActivity, culture);
                
                generateBehavior(situation)
                    .thenAccept(behavior -> {
                        // The behavior will be automatically applied when the LLM responds
                        LOGGER.debug("Generated behavior for {} during activity transition: {}", 
                            villager.getName().getString(), behavior);
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Error generating behavior during activity transition", e);
                        return null;
                    });
            }
        }
    }
    
    /**
     * Provides a human-readable description of the time of day
     * @param timeOfDay The time in ticks (0-24000)
     * @return A description of the time
     */
    private String getTimeDescription(long timeOfDay) {
        if (timeOfDay < 1000) return "dawn";
        if (timeOfDay < 6000) return "morning";
        if (timeOfDay < 9000) return "midday";
        if (timeOfDay < 11500) return "afternoon";
        if (timeOfDay < 13000) return "evening";
        return "night";
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
            .ifPresent(globalPos -> {
                // Check if workstation is in the same dimension
                if (globalPos.dimension().equals(villager.getWorld().getRegistryKey())) {
                    BlockPos pos = globalPos.pos();
                    moveToPosition(pos, 0.5D, 1);
                    LOGGER.debug("Moving villager {} to workstation at {}", villager.getName().getString(), pos);
                }
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
     * Helper method to move to a GlobalPos (which contains dimension and position)
     * @param globalPos The GlobalPos to move to
     * @param speed The movement speed
     * @param priority The priority for the goal
     */
    private void moveToPosition(net.minecraft.util.math.GlobalPos globalPos, double speed, int priority) {
        // Extract the BlockPos from the GlobalPos
        BlockPos pos = globalPos.pos();
        
        // Only move if we're in the right dimension
        if (!globalPos.dimension().equals(villager.getWorld().getRegistryKey())) {
            LOGGER.warn("Attempted to move villager to a position in a different dimension");
            return;
        }
        
        // Use the existing BlockPos method
        moveToPosition(pos, speed, priority);
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
    
    /**
     * Checks if the villager has PvP enabled
     * @return true if the villager can engage in PvP
     */
    public boolean isPvPEnabled() {
        return pvpEnabled;
    }
    
    /**
     * Main tick method for behavior execution
     * This should be called every server tick for each villager
     * @param world The server world
     */
    public void tickBehavior(ServerWorld world) {
        if (villager == null || !villager.isAlive()) {
            return;
        }
        
        // Check if we're executing a specific behavior
        if (activeBehavior != null) {
            // Check if behavior has timed out
            if (world.getTime() - behaviorStartTime > behaviorDuration) {
                completeBehavior();
                return;
            }
            
            // Execute the active behavior
            executeBehavior(world);
        } else {
            // Update time-based activities if no specific behavior is active
            if (world.getTime() % 100 == 0 && !busy) { // Only check every 5 seconds if not busy
                updateActivityBasedOnTime(world);
            }
            
            // Random chance to generate a new behavior if not busy
            if (!busy && world.getTime() % 200 == 0 && Math.random() < 0.1) {
                generateRandomBehavior(world);
            }
        }
    }
    
    /**
     * Execute the currently active behavior
     * @param world The server world
     */
    private void executeBehavior(ServerWorld world) {
        if (activeBehavior == null) return;
        
        switch (activeBehavior) {
            case "working":
                executeWorkBehavior(world);
                break;
            case "socializing":
                executeSocializeBehavior(world);
                break;
            case "trading":
                executeTradeBehavior(world);
                break;
            case "celebrating":
                executeCelebrationBehavior(world);
                break;
            case "fleeing":
                executeFleeingBehavior(world);
                break;
            case "sleeping":
                executeSleepingBehavior(world);
                break;
            default:
                // Default to idle behavior
                executeIdleBehavior(world);
                break;
        }
        
        // Broadcast villager AI state to nearby clients for visuals
        if (world.getTime() % 20 == 0) { // Once per second
            com.beeny.network.VillagesNetwork.broadcastVillagerAIState(
                villager, activeBehavior, villager.getBlockPos());
        }
    }
    
    /**
     * Start a new behavior with parameters
     * @param behaviorName The behavior to start
     * @param durationTicks How long the behavior should last
     * @param params Additional parameters for the behavior
     */
    public void startBehavior(String behaviorName, long durationTicks, Map<String, Object> params) {
        this.activeBehavior = behaviorName;
        this.behaviorStartTime = villager.getWorld().getTime();
        this.behaviorDuration = durationTicks;
        this.behaviorParams = params != null ? params : new HashMap<>();
        
        LOGGER.debug("Villager {} starting behavior: {} for {} ticks", 
            villager.getName().getString(), behaviorName, durationTicks);
            
        // Update activity to match behavior
        updateActivity(behaviorName);
        
        // Set busy state during behavior execution
        setBusy(true);
        
        // Apply initial goals based on the behavior
        applyInitialBehaviorGoals(behaviorName);
    }
    
    /**
     * Complete the current behavior and clean up
     */
    private void completeBehavior() {
        LOGGER.debug("Villager {} completed behavior: {}", 
            villager.getName().getString(), activeBehavior);
            
        // Reset state
        this.activeBehavior = null;
        this.behaviorParams.clear();
        
        // Clear goals
        clearGoalsForNewBehavior("");
        
        // Update to a default activity
        updateActivity("idle");
        
        // No longer busy
        setBusy(false);
    }
    
    /**
     * Apply initial goals for a behavior
     * @param behaviorName The behavior to set up
     */
    private void applyInitialBehaviorGoals(String behaviorName) {
        // Clear previous goals
        clearGoalsForNewBehavior(behaviorName);
        
        // Apply appropriate goals based on behavior
        switch (behaviorName) {
            case "working":
                applyWorkGoals("job");
                break;
            case "socializing":
                applySocializeGoals("villager");
                break;
            case "trading":
                applyTradeGoals("player");
                break;
            case "celebrating":
                applyWanderGoals("around");
                break;
            case "fleeing":
                applyFleeGoals("danger");
                break;
            case "sleeping":
                applyRestGoals("bed");
                break;
            default:
                applyWanderGoals("around");
                break;
        }
    }
    
    /**
     * Generate a random behavior based on context
     * @param world The server world
     */
    private void generateRandomBehavior(ServerWorld world) {
        // Get time of day and cultural context
        long timeOfDay = world.getTimeOfDay() % 24000;
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        String culture = region != null ? region.getCultureAsString().toLowerCase() : "generic";
        
        // Generate a situation prompt for the LLM
        String situation = String.format("It's %s and this villager is in a %s village near %s", 
            getTimeDescription(timeOfDay), 
            culture,
            getNearbyDescription(world));
        
        // Generate behavior using LLM
        generateBehavior(situation)
            .thenAccept(behavior -> {
                // The parseBehaviorAndUpdateGoals will handle setting up the behavior
                LOGGER.debug("Generated random behavior for {}: {}", 
                    villager.getName().getString(), behavior);
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating random behavior", e);
                return null;
            });
    }
    
    /**
     * Get a description of nearby entities and blocks
     * @param world The server world
     * @return A description of what's nearby
     */
    private String getNearbyDescription(ServerWorld world) {
        BlockPos pos = villager.getBlockPos();
        
        // Check for nearby players
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
            PlayerEntity.class, 
            new Box(pos).expand(10), 
            player -> true
        );
        
        if (!nearbyPlayers.isEmpty()) {
            return "some players";
        }
        
        // Check for nearby villagers
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class, 
            new Box(pos).expand(8), 
            v -> v != villager
        );
        
        if (!nearbyVillagers.isEmpty()) {
            return "other villagers";
        }
        
        // Check profession-specific locations
        // Revert to original getProfession call
        VillagerProfession profession = villager.getVillagerData().getProfession(); // Re-applying based on user info
        
        // Compare VillagerProfession objects (assuming getProfession returns VillagerProfession)
        // Compare the profession's key to the constant key
        // Compare the profession's key to the constant key
        // Compare the profession's registry key to the constant key
        // Compare the profession's registry key to the constant key
        if (Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.FARMER) {
            return "farmland";
        } else if (Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.LIBRARIAN) {
            return "bookshelves";
        } else if (Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.ARMORER || Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.WEAPONSMITH || Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.TOOLSMITH) {
            return "forge";
        }
        
        return "their village";
    }
    
    // Behavior execution methods for specific behaviors
    
    private void executeWorkBehavior(ServerWorld world) {
        // Execute working behavior
        // Revert to original getProfession call
        VillagerProfession profession = villager.getVillagerData().getProfession(); // Re-applying based on user info
        
        // Profession-specific work behaviors
        // Compare VillagerProfession objects
        // Compare the profession's key to the constant key
        // Compare the profession's key to the constant key
        // Compare the profession's registry key to the constant key
        // Compare the profession's registry key to the constant key
        if (Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.FARMER) {
            // Occasionally show farming particles
            if (world.getRandom().nextInt(20) == 0) {
                world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 1, villager.getZ(),
                    3, 0.5, 0.5, 0.5, 0.02
                );
            }
        } else if (Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.LIBRARIAN) {
            // Occasionally show reading particles
            if (world.getRandom().nextInt(30) == 0) {
                world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    villager.getX(), villager.getY() + 1, villager.getZ(),
                    2, 0.5, 0.5, 0.5, 0.01
                );
            }
        } else if (Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.ARMORER || Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.WEAPONSMITH || Registries.VILLAGER_PROFESSION.getKey(profession).orElse(null) == VillagerProfession.TOOLSMITH) {
            // Occasionally show smithing particles
            if (world.getRandom().nextInt(15) == 0) {
                world.spawnParticles(
                    ParticleTypes.FLAME,
                    villager.getX(), villager.getY() + 1, villager.getZ(),
                    2, 0.3, 0.3, 0.3, 0.01
                );
            }
        }
        
        // Make sure we occasionally look around while working
        if (world.getRandom().nextInt(50) == 0) {
            // Look at random position
            double lookX = villager.getX() + world.getRandom().nextDouble() * 10 - 5;
            double lookY = villager.getY() + world.getRandom().nextDouble() * 2;
            double lookZ = villager.getZ() + world.getRandom().nextDouble() * 10 - 5;
            villager.getLookControl().lookAt(lookX, lookY, lookZ);
        }
    }
    
    private void executeSocializeBehavior(ServerWorld world) {
        // Find nearby villagers to socialize with
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class, 
            new Box(villager.getBlockPos()).expand(5), 
            v -> v != villager
        );
        
        if (!nearbyVillagers.isEmpty()) {
            // Look at the nearest villager
            VillagerEntity nearest = nearbyVillagers.get(0);
            villager.getLookControl().lookAt(nearest, 30f, 30f);
            
            // Occasional chat particle effects
            if (world.getRandom().nextInt(30) == 0) {
                VillagerFeedbackHelper.showSpeakingEffect(villager);
            }
        } else {
            // If no villagers nearby, look for players
            List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                PlayerEntity.class, 
                new Box(villager.getBlockPos()).expand(8), 
                player -> true
            );
            
            if (!nearbyPlayers.isEmpty()) {
                // Look at the nearest player
                PlayerEntity nearest = nearbyPlayers.get(0);
                villager.getLookControl().lookAt(nearest, 30f, 30f);
            }
        }
    }
    
    private void executeTradeBehavior(ServerWorld world) {
        // Trading behavior is mostly reactive to player interactions
        // Here we'll just make the villager look more attentive
        
        // Look for nearby players
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
            PlayerEntity.class, 
            new Box(villager.getBlockPos()).expand(5), 
            player -> true
        );
        
        if (!nearbyPlayers.isEmpty()) {
            // Look at the nearest player
            PlayerEntity nearest = nearbyPlayers.get(0);
            villager.getLookControl().lookAt(nearest, 30f, 30f);
            
            // Occasionally show interest particles
            if (world.getRandom().nextInt(40) == 0) {
                world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 1, villager.getZ(),
                    1, 0.5, 0.5, 0.5, 0.01
                );
            }
        }
    }
    
    private void executeCelebrationBehavior(ServerWorld world) {
        // Celebration effects - particles and animations
        if (world.getRandom().nextInt(10) == 0) {
            world.spawnParticles(
                ParticleTypes.NOTE,
                villager.getX(), villager.getY() + 1, villager.getZ(),
                1, 0.5, 0.5, 0.5, 0
            );
        }
        
        // Occasionally jump with happiness
        if (world.getRandom().nextInt(40) == 0 && villager.isOnGround()) {
            villager.setVelocity(villager.getVelocity().add(0, 0.42, 0));
            villager.velocityModified = true;
        }
        
        // Look at other celebrating villagers or players
        List<Entity> nearbyEntities = world.getOtherEntities(
            villager, 
            new Box(villager.getBlockPos()).expand(8), 
            e -> e instanceof VillagerEntity || e instanceof PlayerEntity
        );
        
        if (!nearbyEntities.isEmpty()) {
            Entity target = nearbyEntities.get(world.getRandom().nextInt(nearbyEntities.size()));
            villager.getLookControl().lookAt(target, 30f, 30f);
        }
    }
    
    private void executeFleeingBehavior(ServerWorld world) {
        // Get the danger source from parameters if available
        BlockPos dangerPos = null;
        if (behaviorParams.containsKey("dangerPos")) {
            dangerPos = (BlockPos) behaviorParams.get("dangerPos");
        }
        
        // If we have a danger position, ensure we're moving away from it
        if (dangerPos != null) {
            // Calculate direction away from danger
            double dx = villager.getX() - dangerPos.getX();
            double dz = villager.getZ() - dangerPos.getZ();
            
            // Normalize and scale
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 0.1) {
                // If too close to danger, pick a random direction
                dx = world.getRandom().nextDouble() * 2 - 1;
                dz = world.getRandom().nextDouble() * 2 - 1;
                length = Math.sqrt(dx * dx + dz * dz);
            }
            
            dx = dx / length * 10; // Scale to 10 blocks away
            dz = dz / length * 10;
            
            // Set flee destination
            BlockPos fleePos = new BlockPos(
                dangerPos.getX() + (int)dx,
                dangerPos.getY(),
                dangerPos.getZ() + (int)dz
            );
            
            // Move to the flee position if we're not already moving
            if (!villager.getNavigation().isFollowingPath()) {
                moveToPosition(fleePos, 1.0, 1);
            }
        }
        
        // Occasional panic particles
        if (world.getRandom().nextInt(15) == 0) {
            world.spawnParticles(
                ParticleTypes.SMOKE,
                villager.getX(), villager.getY() + 1, villager.getZ(),
                3, 0.3, 0.3, 0.3, 0.02
            );
        }
    }
    
    private void executeSleepingBehavior(ServerWorld world) {
        // Check time of day - should only sleep at night
        long timeOfDay = world.getTimeOfDay() % 24000;
        if (timeOfDay >= 1000 && timeOfDay <= 13000) {
            // It's daytime, end sleep behavior
            completeBehavior();
            return;
        }
        
        // Sleeping behaviors - reduced movement and occasional Z particles
        if (world.getRandom().nextInt(50) == 0) {
            world.spawnParticles(
                ParticleTypes.CLOUD,
                villager.getX(), villager.getY() + 0.8, villager.getZ(),
                1, 0.1, 0.1, 0.1, 0.01
            );
        }
        
        // If we're not at a bed, try to find one occasionally
        if (world.getRandom().nextInt(100) == 0) {
            BlockPos bedPos = findNearestBed(world);
            if (bedPos != null) {
                moveToPosition(bedPos, 0.5, 1);
            }
        }
    }
    
    private void executeIdleBehavior(ServerWorld world) {
        // Basic idle behavior - occasional looking around
        if (world.getRandom().nextInt(60) == 0) {
            // Look at random position
            double lookX = villager.getX() + world.getRandom().nextDouble() * 16 - 8;
            double lookY = villager.getY() + world.getRandom().nextDouble() * 3 - 1;
            double lookZ = villager.getZ() + world.getRandom().nextDouble() * 16 - 8;
            villager.getLookControl().lookAt(lookX, lookY, lookZ);
        }
        
        // Occasionally wander if stationary for too long
        if (world.getRandom().nextInt(120) == 0 && !villager.getNavigation().isFollowingPath()) {
            BlockPos randomPos = getRandomPositionAround(villager.getBlockPos(), 8);
            moveToPosition(randomPos, 0.4, 3);
        }
    }
    
    /**
     * Find the nearest bed block
     * @param world The server world
     * @return The position of the nearest bed, or null if none found
     */
    private BlockPos findNearestBed(ServerWorld world) {
        BlockPos villagerPos = villager.getBlockPos();
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        
        // Check for a remembered home bed first
        Optional<net.minecraft.util.math.GlobalPos> homeMem = villager.getBrain().getOptionalMemory(
            net.minecraft.entity.ai.brain.MemoryModuleType.HOME);
        
        if (homeMem.isPresent()) {
            net.minecraft.util.math.GlobalPos homeGlobalPos = homeMem.get();
            // Check if the home is in the same dimension
            if (homeGlobalPos.dimension().equals(world.getRegistryKey())) {
                BlockPos homePos = homeGlobalPos.pos();
                if (world.getBlockState(homePos).getBlock() instanceof net.minecraft.block.BedBlock) {
                    return homePos;
                }
            }
        }
        
        // Search in a radius
        int radius = 15;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = villagerPos.add(x, y, z);
                    if (world.getBlockState(checkPos).getBlock() instanceof net.minecraft.block.BedBlock) {
                        double distSq = checkPos.getSquaredDistance(villagerPos);
                        if (distSq < nearestDistSq) {
                            nearest = checkPos;
                            nearestDistSq = distSq;
                        }
                    }
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * Get a random position around a center point
     * @param center The center position
     * @param radius The radius to search within
     * @return A random position
     */
    private BlockPos getRandomPositionAround(BlockPos center, int radius) {
        Random random = new Random();
        return center.add(
            random.nextInt(radius * 2) - radius,
             0,
            random.nextInt(radius * 2) - radius
        );
    }
    
    /**
     * Get a villager's skill level in a specific profession
     * 
     * @param skill The skill name to get (e.g., "crafting", "farming", "trading")
     * @return The skill level (0.0f to 10.0f)
     */
    public float getProfessionSkill(String skill) {
        // Return the skill value if it exists, or 0 if it doesn't
        return professionSkills.getOrDefault(skill, 0.0f);
    }
    
    /**
     * Update a villager's skill level in a specific profession
     * 
     * @param skill The skill name to update
     * @param newValue The new skill value
     */
    public void updateProfessionSkill(String skill, float newValue) {
        // Keep skill values between 0 and 10
        float clampedValue = Math.max(0.0f, Math.min(10.0f, newValue));
        professionSkills.put(skill, clampedValue);
        LOGGER.debug("Updated {} skill for villager {} to {}", 
            skill, villager.getName().getString(), clampedValue);
    }
}
