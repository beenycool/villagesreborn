package com.beeny.village;

import com.beeny.ai.LLMService;
import com.beeny.config.ModConfig;        // Import new config
import com.beeny.config.ModConfigManager; // Import new config manager
import com.beeny.config.VillagesConfig; // Keep old config for LLM settings for now
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
    
    private static final long MIN_TICKS_BETWEEN_ACTIVITIES = 100; // Base cooldown (5 seconds)
    private static final double BASE_TRADE_ATTEMPT_CHANCE = 0.1; // Base chance (e.g., 10%) per execution cycle when 'trading' activity is active
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
        
        // Apply activity frequency multiplier
        // Apply activity frequency multiplier from ModConfig
        double activityMultiplier = ModConfigManager.getConfig().gameplay.activityFrequency;
        if (activityMultiplier <= 0) activityMultiplier = 1.0; // Prevent division by zero or negative time
        long requiredTicksBetweenActivities = (long) (MIN_TICKS_BETWEEN_ACTIVITIES / activityMultiplier);

        // Check cooldown before allowing any activity change logic (except LLM override)
        boolean canTransition = timeSinceLastTransition >= requiredTicksBetweenActivities;

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
        if (canTransition && !newActivity.equals(currentActivity)) {
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
        context.put("timeout", "60"); // Keep timeout context

        // Get AI detail level from config
        String detailLevel = VillagesConfig.getInstance().getLLMSettings().getAiDetailLevel();
        String prompt;

        // Adjust prompt based on detail level
        switch (detailLevel.toUpperCase()) {
            case "MINIMAL":
                prompt = String.format("Brief action for villager. Personality: %s. Situation: %s.",
                                       personality, situation);
                // Optionally reduce max tokens if LLMService supports it per-request
                // context.put("max_tokens", "50");
                break;
            case "DETAILED":
                // Add more context, e.g., recent memories or relationships
                String recentMemory = getRecentMemoryDescription(); // Need to implement this helper
                prompt = String.format("Detailed behavior for villager. Personality: %s. Situation: %s. Recent memory: %s. Respond with ACTION: and TARGET:",
                                       personality, situation, recentMemory);
                // Optionally increase max tokens
                // context.put("max_tokens", "200");
                break;
            case "CUSTOM":
                 // For CUSTOM, maybe use temperature/other settings more directly?
                 // For now, treat as BALANCED.
            case "BALANCED":
            default:
                prompt = String.format("Villager behavior. Personality: %s. Situation: %s. Respond with ACTION: and TARGET:",
                                       personality, situation);
                // context.put("max_tokens", "100"); // Default max tokens
                break;
        }
        LOGGER.debug("Generating behavior with detail level '{}'. Prompt: {}", detailLevel, prompt);
        return LLMService.getInstance().generateResponse(prompt, context)
            .thenApply(this::parseBehaviorAndUpdateGoals)
            .exceptionally(ex -> {
                LOGGER.error("Error generating behavior", ex);
                return "Default behavior";
            });
    }

    // Helper method to get a description of a recent memory
    @Unique
    private String getRecentMemoryDescription() {
        // Check if memory system is enabled in config
        if (!VillagesConfig.getInstance().getLLMSettings().isUseMemory()) {
            return "Memory disabled";
        }
        // Retrieve and format a recent, relevant memory
        VillagerMemory memory = VillagerManager.getInstance().getVillagerMemory(this.villager.getUuid());
        if (memory != null) {
            List<VillagerMemory.Memory> recent = memory.getMemories(EnumSet.allOf(VillagerMemory.Memory.MemoryType.class), 0.3f); // Get recent important memories
            if (!recent.isEmpty()) {
                // Return the description of the most important recent memory
                return recent.get(0).getDescription();
            }
        }
        return "None";
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
                LOGGER.warn("Unknown action '{}', applying wander goals", action);
                applyWanderGoals("around");
        }
    }
    
    /**
     * Applies goals related to working
     * @param target The target of the work (e.g., "job", "farm")
     */
    private void applyWorkGoals(String target) {
        boolean resourceGathering = VillagesConfig.getInstance().getGameplaySettings().isResourceGatheringEnabled();

        updateActivity("working");
        
        if (target.contains("job") || target.contains("workstation")) {
            // Move to workstation (always allowed regardless of resource gathering setting)
            moveToWorkstation();
        } else if (target.contains("farm")) {
            if (!resourceGathering) {
                LOGGER.debug("Resource gathering disabled, skipping farm goal for {}", villager.getName().getString());
                // Optionally, add a default wander near workstation instead
            } else {
            // Find and move to farmland
            findAndMoveToBlockType(Blocks.FARMLAND, 10, 1);
            }
        } else {
            // Default work behavior: wander near workstation
            moveToWorkstation(); // Try to get close first
            Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.5D);
            addGoal(wanderGoal, 3, "work_wander");
        }
        
        // Look at nearby players/villagers while working
        Goal lookPlayerGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 6.0F, 0.5F);
        addGoal(lookPlayerGoal, 4, "work_look_player");
        Goal lookVillagerGoal = new LookAtEntityGoal(villager, VillagerEntity.class, 6.0F, 0.5F);
        addGoal(lookVillagerGoal, 5, "work_look_villager");
    }
    
    /**
     * Applies goals related to socializing
     * @param target Who to socialize with (e.g., "villager", "player")
     */
    private void applySocializeGoals(String target) {
        updateActivity("socializing");
        
        if (target.contains("villager")) {
            // Look at other villagers
            Goal lookGoal = new LookAtEntityGoal(villager, VillagerEntity.class, 8.0F, 0.8F);
            addGoal(lookGoal, 1, "socialize_look");
        } else if (target.contains("player")) {
            // Look at players
            Goal lookGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 8.0F, 0.8F);
            addGoal(lookGoal, 1, "socialize_look");
        }
        
        // Wander around while socializing
        Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.6D);
        addGoal(wanderGoal, 2, "socialize_wander");
    }
    
    /**
     * Applies goals related to fleeing
     * @param target What to flee from (e.g., "danger", "monster")
     */
    private void applyFleeGoals(String target) {
        // Check if village defense is enabled
        if (!VillagesConfig.getInstance().getGameplaySettings().isVillageDefenseEnabled()) {
            LOGGER.debug("Village defense disabled, skipping flee goal application for {}", villager.getName().getString());
            return; // Don't apply flee goals if defense is off
        }

        updateActivity("fleeing");
        
        if (target.contains("monster") || target.contains("zombie")) {
            // Flee from monsters
            Goal fleeMonsterGoal = new FleeEntityGoal<>((PathAwareEntity)villager, Monster.class, 8.0F, 1.0D, 1.2D);
            addGoal(fleeMonsterGoal, 1, "flee_monster");
        } else if (target.contains("danger")) {
            // Escape general danger
            Goal escapeGoal = new EscapeDangerGoal((PathAwareEntity)villager, 1.0D);
            addGoal(escapeGoal, 1, "flee_danger");
        } else if (target.contains("player")) {
            // Avoid players (if needed)
            Goal avoidPlayerGoal = new AvoidEntityGoal<>((PathAwareEntity)villager, PlayerEntity.class, 6.0F, 1.0D, 1.2D);
            addGoal(avoidPlayerGoal, 1, "flee_player");
        } else {
            // Default flee: Escape general danger
            Goal escapeGoal = new EscapeDangerGoal((PathAwareEntity)villager, 1.0D);
            addGoal(escapeGoal, 1, "flee_default");
        }
        
        // Add a faster wander goal while fleeing
        Goal wanderGoal = new WanderAroundFarGoal((PathAwareEntity)villager, 1.0D);
        addGoal(wanderGoal, 2, "flee_wander");
    }
    
    /**
     * Applies goals related to wandering
     * @param target Where to wander (e.g., "around", "far")
     */
    private void applyWanderGoals(String target) {
        updateActivity("wandering");
        
        if (target.contains("far")) {
            // Wander far
            Goal wanderFarGoal = new WanderAroundFarGoal((PathAwareEntity)villager, 0.7D);
            addGoal(wanderFarGoal, 1, "wander_far");
        } else {
            // Wander nearby
            Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.6D);
            addGoal(wanderGoal, 1, "wander_around");
        }
        
        // Look at nearby entities while wandering
        Goal lookPlayerGoal = new LookAtEntityGoal(villager, PlayerEntity.class, 6.0F, 0.3F);
        addGoal(lookPlayerGoal, 2, "wander_look_player");
        Goal lookVillagerGoal = new LookAtEntityGoal(villager, VillagerEntity.class, 6.0F, 0.3F);
        addGoal(lookVillagerGoal, 3, "wander_look_villager");
    }
    
    /**
     * Applies goals related to resting/sleeping
     * @param target Where to rest (e.g., "bed", "home")
     */
    private void applyRestGoals(String target) {
        updateActivity("sleeping");
        
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
            for (int y = -2; y <= 2; y++) { // Check slightly above/below
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = villagerPos.add(x, y, z);
                    if (serverWorld.getBlockState(checkPos).getBlock() == blockType) {
                        // Found the block, move to it
                        moveToPosition(checkPos, 0.6D, priority);
                        return; // Move to the first one found
                    }
                }
            }
        }
        LOGGER.debug("Could not find block {} within radius {} for villager {}", 
            blockType.toString(), searchRadius, villager.getName().getString());
    }
    
    /**
     * Moves the villager towards their workstation
     */
    private void moveToWorkstation() {
        villager.getBrain().getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.JOB_SITE)
            .ifPresent(globalPos -> {
                // Check if the job site is in the same dimension
                if (globalPos.dimension().equals(villager.getWorld().getRegistryKey())) {
                    moveToPosition(globalPos.pos(), 0.6D, 1);
                } else {
                    LOGGER.warn("Villager {} cannot path to workstation in different dimension: {}", 
                        villager.getName().getString(), globalPos.dimension().getValue());
                }
            });
    }
    
    /**
     * Adds a goal to move the villager to a specific position
     * @param pos The target position
     * @param speed The movement speed modifier
     * @param priority The goal priority
     */
    private void moveToPosition(BlockPos pos, double speed, int priority) {
        // Custom goal to move to a specific position
        class SimpleMoveToGoal extends MoveToTargetPosGoal {
            public SimpleMoveToGoal(PathAwareEntity mob, BlockPos target, double speed, int range) {
                // The 'range' parameter here is the search range, not the acceptance range
                // We set 'targetExpected' to false as we don't require a specific block state
                super(mob, speed, range, 1); // Use range 1 for acceptance distance
                this.targetPos = target;
            }
            
            @Override
            protected boolean isTargetPos(net.minecraft.world.WorldView world, BlockPos pos) {
                // We consider the target reached if we are close enough
                return pos.getSquaredDistance(this.targetPos) <= 2.0; // Within ~1 block distance
            }
            
            @Override
            public double getDesiredSquaredDistanceToTarget() {
                return 1.5; // Try to get within 1.5 blocks squared
            }
            
            @Override
            public void start() {
                super.start();
                LOGGER.debug("Villager {} starting move to {}", mob.getName().getString(), targetPos);
            }
        }
        
        Goal moveToGoal = new SimpleMoveToGoal((PathAwareEntity)villager, pos, speed, 8); // Search range 8
        addGoal(moveToGoal, priority, "move_to_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
    }
    
    /**
     * Adds a goal to move the villager to a specific global position (handles dimension checks)
     * @param globalPos The target global position
     * @param speed The movement speed modifier
     * @param priority The goal priority
     */
    private void moveToPosition(net.minecraft.util.math.GlobalPos globalPos, double speed, int priority) {
        if (globalPos.dimension().equals(villager.getWorld().getRegistryKey())) {
            moveToPosition(globalPos.pos(), speed, priority);
        } else {
            LOGGER.warn("Cannot move villager {} to different dimension: {}", 
                villager.getName().getString(), globalPos.dimension().getValue());
        }
    }
    
    /**
     * Adds a goal to the villager's goal selector if it's not already present
     * @param goal The goal to add
     * @param priority The priority of the goal
     * @param id A unique ID for the goal (used for tracking)
     */
    private void addGoal(Goal goal, int priority, String id) {
        if (villager instanceof MobEntity) {
            MobEntity mobEntity = (MobEntity) villager;
            GoalSelector goalSelector = mobEntity.goalSelector;
            
            // Check if a goal with this ID is already active
            if (activeGoals.containsKey(id)) {
                // If priorities differ, remove the old one first
                if (goalPriorities.getOrDefault(id, -1) != priority) {
                    removeGoal(activeGoals.get(id));
                } else {
                    // Goal with same ID and priority already exists, do nothing
                    return;
                }
            }
            
            // Check if the exact goal instance is already added (less reliable)
            boolean alreadyAdded = goalSelector.getGoals().stream()
                .anyMatch(prioritizedGoal -> prioritizedGoal.getGoal().equals(goal));
                
            if (!alreadyAdded) {
                try {
                    goalSelector.add(priority, goal);
                    activeGoals.put(id, goal);
                    goalPriorities.put(id, priority);
                    LOGGER.debug("Added goal '{}' with priority {} to villager {}", id, priority, villager.getName().getString());
                } catch (Exception e) {
                    LOGGER.error("Failed to add goal '{}' to villager {}", id, villager.getName().getString(), e);
                }
            }
        } else {
            LOGGER.warn("Cannot add goal: Villager is not a MobEntity");
        }
    }
    
    /**
     * Removes a goal from the villager's goal selector
     * @param goal The goal to remove
     */
    private void removeGoal(Goal goal) {
        if (villager instanceof MobEntity) {
            MobEntity mobEntity = (MobEntity) villager;
            GoalSelector goalSelector = mobEntity.goalSelector;
            
            try {
                // Find the ID associated with this goal instance
                String goalIdToRemove = null;
                for (Map.Entry<String, Goal> entry : activeGoals.entrySet()) {
                    if (entry.getValue().equals(goal)) {
                        goalIdToRemove = entry.getKey();
                        break;
                    }
                }
                
                goalSelector.remove(goal);
                
                // Remove from tracking maps if found
                if (goalIdToRemove != null) {
                    activeGoals.remove(goalIdToRemove);
                    goalPriorities.remove(goalIdToRemove);
                    LOGGER.debug("Removed goal '{}' from villager {}", goalIdToRemove, villager.getName().getString());
                } else {
                    LOGGER.debug("Removed untracked goal instance from villager {}", villager.getName().getString());
                }
                
            } catch (Exception e) {
                LOGGER.error("Failed to remove goal from villager {}", villager.getName().getString(), e);
            }
        } else {
            LOGGER.warn("Cannot remove goal: Villager is not a MobEntity");
        }
    }
    
    // --- Relationship and Memory Methods ---
    
    public void addRelationship(UUID targetVillager, RelationshipType type) {
        // Implementation depends on how relationships are stored (e.g., in VillagerManager)
        VillagerManager.getInstance().addRelationship(villager.getUuid(), targetVillager, type);
    }
    
    public void recordTheft(UUID thiefId, String itemDescription, BlockPos location) {
        // Implementation depends on memory system
        // Example: getVillagerMemory().addMemory("theft", Map.of("thief", thiefId, "item", itemDescription, "location", location));
    }
    
    public int getFriendshipLevel() {
        // Placeholder - needs implementation based on relationship storage
        return 50; // Neutral default
    }
    
    public void updateProfessionSkill(String skill) {
        // Placeholder - needs implementation
    }
    
    // --- Dialogue and Learning ---
    
    public CompletableFuture<String> generateDialogue(String input, Object context) {
        // Implementation depends on dialogue system
        VillagerDialogue dialogue = VillagerManager.getInstance().getVillagerDialogue(villager.getUuid());
        if (dialogue != null) {
            return dialogue.generatePlayerInteraction(null, input); // Context needs mapping
        }
        return CompletableFuture.completedFuture("Hmm?");
    }
    
    public void learnFromEvent(VillageEvent event, String reflection) {
        // Implementation depends on memory system
        // Example: getVillagerMemory().addMemory("event_reflection", Map.of("event", event.getName(), "reflection", reflection));
    }
    
    public String getSocialSummary() {
        // Placeholder - needs implementation based on relationship storage
        int friendCount = VillagerManager.getInstance().getRelationshipCount(villager.getUuid()); // Example
        return String.format("Has %d known associates.", friendCount);
    }
    
    // --- PvP and Combat ---
    
    /**
     * Adds PvP-related goals to the villager if PvP is enabled.
     */
    public void addPvPGoals() {
        if (!(villager instanceof MobEntity mobEntity)) {
            LOGGER.warn("Cannot add PvP goals: Villager is not a MobEntity");
            return;
        }
        
        if (!VillagesConfig.getInstance().getGameplaySettings().isVillagerPvPEnabled()) {
            LOGGER.debug("PvP is disabled, not adding PvP goals for {}", villager.getName().getString());
            return;
        }
        
        if (pvpEnabled) return; // Already added
        
        LOGGER.info("Adding PvP goals for villager {}", villager.getName().getString());
        
        // Target other villagers who are enemies or rivals
        class VillagerTargetGoal extends ActiveTargetGoal<VillagerEntity> {
            private final VillagerAI attackerAI;
            
            public VillagerTargetGoal(MobEntity mob, VillagerAI attackerAI) {
                super(mob, VillagerEntity.class, true, false); // Check sight, don't check visibility
                this.attackerAI = attackerAI;
            }
            
            @Override
            public boolean canStart() {
                if (!VillagesConfig.getInstance().getGameplaySettings().isVillagerPvPEnabled()) return false;
                return super.canStart();
            }
            
            @Override
            protected boolean shouldContinue(LivingEntity target, Box box) {
                if (!VillagesConfig.getInstance().getGameplaySettings().isVillagerPvPEnabled()) return false;
                
                RelationshipType relationship = attackerAI.getRelationshipWith(target.getUuid());
                // Only target enemies or rivals
                return (relationship == RelationshipType.ENEMY || relationship == RelationshipType.RIVAL) &&
                       super.shouldContinue(target, box);
            }
        }
        
        ActiveTargetGoal<VillagerEntity> targetGoal = new VillagerTargetGoal(mobEntity, this);
        addGoal(targetGoal, 1, "pvp_target_villager");
        
        // Add revenge goal
        Goal revengeGoal = new RevengeGoal((PathAwareEntity)villager);
        addGoal(revengeGoal, 2, "pvp_revenge");
        
        pvpEnabled = true;
    }
    
    /**
     * Checks if a location is considered safe (e.g., well-lit, no monsters nearby).
     * @param world The world
     * @param pos The position to check
     * @return True if the location is safe, false otherwise
     */
    private boolean isSafeLocation(ServerWorld world, BlockPos pos) {
        // Check light level
        if (world.getLightLevel(pos) < 8 && !world.isDay()) {
            return false; // Too dark at night
        }
        
        // Check for nearby monsters
        Box checkBox = new Box(pos).expand(8.0); // Check within 8 blocks
        List<Monster> nearbyMonsters = world.getEntitiesByClass(Monster.class, checkBox, monster -> monster.isAlive());
        if (!nearbyMonsters.isEmpty()) {
            return false; // Monsters nearby
        }
        
        // Add more safety checks if needed (e.g., hazards like lava)
        
        return true;
    }
    
    // --- Behavior Execution ---
    
    /**
     * Ticks the current behavior, checking for completion or interruption.
     * @param world The server world
     */
    public void tickBehavior(ServerWorld world) {
        if (activeBehavior == null) {
            // If no specific behavior, maybe generate a random one occasionally?
            if (world.getTime() % 600 == 0 && Math.random() < 0.1) { // Every 30 seconds, 10% chance
                generateRandomBehavior(world);
            }
            return;
        }
        
        long currentTime = world.getTime();
        
        // Check if behavior duration has expired
        if (currentTime >= behaviorStartTime + behaviorDuration) {
            LOGGER.debug("Behavior '{}' completed for villager {}", activeBehavior, villager.getName().getString());
            completeBehavior();
            return;
        }
        
        // Execute the logic for the current behavior
        executeBehavior(world);
        
        // TODO: Add conditions for interrupting behavior (e.g., danger, player interaction)
    }
    
    /**
     * Executes the logic associated with the currently active behavior.
     * @param world The server world
     */
    private void executeBehavior(ServerWorld world) {
        if (activeBehavior == null) return;
        
        // Use a switch or if-else chain to call specific execution methods
        switch (activeBehavior) {
            case "work":
                executeWorkBehavior(world);
                break;
            case "socialize":
                executeSocializeBehavior(world);
                break;
            case "trade":
                executeTradeBehavior(world);
                break;
            case "celebrate":
                executeCelebrationBehavior(world);
                break;
            case "flee":
                executeFleeingBehavior(world);
                break;
            case "sleep":
                executeSleepingBehavior(world);
                break;
            case "idle":
            default:
                executeIdleBehavior(world);
                break;
        }
    }
    
    /**
     * Starts a new behavior, clearing old goals and applying new ones.
     * @param behaviorName The name of the behavior (e.g., "work", "socialize")
     * @param durationTicks The duration in game ticks
     * @param params Additional parameters for the behavior
     */
    public void startBehavior(String behaviorName, long durationTicks, Map<String, Object> params) {
        if (busy && !behaviorName.equals("flee")) { // Allow fleeing even if busy
            LOGGER.debug("Villager {} is busy, cannot start behavior '{}'", villager.getName().getString(), behaviorName);
            return;
        }
        
        LOGGER.info("Villager {} starting behavior: '{}' for {} ticks", villager.getName().getString(), behaviorName, durationTicks);
        
        // Clear previous behavior goals
        clearGoalsForNewBehavior(behaviorName); // Use action name to clear relevant goals
        
        this.activeBehavior = behaviorName;
        this.behaviorStartTime = villager.getWorld().getTime();
        this.behaviorDuration = durationTicks;
        this.behaviorParams = params != null ? new HashMap<>(params) : new HashMap<>();
        
        // Apply initial goals for the new behavior
        applyInitialBehaviorGoals(behaviorName);
        
        // Set busy state if appropriate for the behavior
        if (!behaviorName.equals("idle") && !behaviorName.equals("wander")) {
            setBusy(true);
        }
    }
    
    /**
     * Completes the current behavior, clearing its goals and busy state.
     */
    private void completeBehavior() {
        if (activeBehavior != null) {
            LOGGER.debug("Villager {} completing behavior: {}", villager.getName().getString(), activeBehavior);
            clearGoalsForNewBehavior(activeBehavior); // Clear goals associated with the completed behavior
        }
        
        this.activeBehavior = null;
        this.behaviorStartTime = 0;
        this.behaviorDuration = 0;
        this.behaviorParams.clear();
        setBusy(false); // No longer busy
        
        // Trigger a re-evaluation of time-based activity
        if (villager.getWorld() instanceof ServerWorld sw) {
            updateActivityBasedOnTime(sw);
        }
    }
    
    /**
     * Applies the initial set of goals when a behavior starts.
     * @param behaviorName The name of the behavior that started.
     */
    private void applyInitialBehaviorGoals(String behaviorName) {
        // This is similar to applyBehaviorGoals, but might set up longer-term goals
        // or initial movement targets based on behaviorParams.
        
        switch (behaviorName) {
            case "work":
                applyWorkGoals(behaviorParams.getOrDefault("target", "job").toString());
                break;
            case "socialize":
                applySocializeGoals(behaviorParams.getOrDefault("target", "villager").toString());
                break;
            case "trade":
                applyTradeGoals(behaviorParams.getOrDefault("target", "player").toString());
                break;
            case "celebrate":
                // Maybe wander near a specific celebration point?
                applyWanderGoals("around"); // Placeholder
                break;
            case "flee":
                applyFleeGoals(behaviorParams.getOrDefault("target", "danger").toString());
                break;
            case "sleep":
                applyRestGoals(behaviorParams.getOrDefault("target", "bed").toString());
                break;
            case "idle":
            default:
                applyWanderGoals("around");
                break;
        }
    }
    
    /**
     * Generates a random, short-term behavior based on the current situation.
     * @param world The server world
     */
    private void generateRandomBehavior(ServerWorld world) {
        if (busy) return; // Don't generate if already busy
        
        String situation = String.format("Villager %s (%s) is currently %s near %s. Weather is %s. Time is %s.",
            villager.getName().getString(),
            personality,
            currentActivity,
            getNearbyDescription(world),
            world.isRaining() ? "rainy" : (world.isThundering() ? "stormy" : "clear"),
            getTimeDescription(world.getTimeOfDay() % 24000)
        );
        
        String prompt = String.format(
            "Based on the situation: \"%s\"\nSuggest a brief, simple action (10-30 seconds) for the villager. Format: ACTION: [action_verb] TARGET: [target_noun]",
            situation
        );
        
        generateBehavior(prompt)
            .thenAccept(behavior -> {
                // Behavior is parsed and goals applied within parseBehaviorAndUpdateGoals
                LOGGER.debug("Generated random behavior for {}: {}", villager.getName().getString(), behavior);
                // Set a short duration for this random behavior
                this.behaviorDuration = 200 + world.random.nextInt(400); // 10-30 seconds
                this.behaviorStartTime = world.getTime();
                // Extract action for setting busy state
                Map<String, String> parsed = parseBehaviorString(behavior);
                this.activeBehavior = parsed.getOrDefault("ACTION", "idle").toLowerCase();
                if (!activeBehavior.equals("idle") && !activeBehavior.equals("wander")) {
                    setBusy(true);
                }
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating random behavior", e);
                return null;
            });
    }
    
    /**
     * Gets a brief description of the villager's surroundings.
     * @param world The server world
     * @return A string describing nearby blocks or entities.
     */
    private String getNearbyDescription(ServerWorld world) {
        BlockPos pos = villager.getBlockPos();
        
        // Check nearby blocks
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockState state = world.getBlockState(pos.add(dx, dy, dz));
                    if (!state.isAir()) {
                        // Simple description based on block type
                        if (state.getBlock() == Blocks.CRAFTING_TABLE) return "a crafting table";
                        if (state.getBlock() == Blocks.FURNACE) return "a furnace";
                        if (state.getBlock() == Blocks.CHEST) return "a chest";
                        if (state.getBlock().toString().contains("door")) return "a door";
                        if (state.getBlock().toString().contains("bed")) return "a bed";
                        // Add more common blocks if needed
                    }
                }
            }
        }
        
        // Check nearby entities
        Box checkBox = villager.getBoundingBox().expand(5.0);
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(VillagerEntity.class, checkBox, v -> v != villager);
        if (!nearbyVillagers.isEmpty()) {
            return "other villagers";
        }
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(PlayerEntity.class, checkBox, p -> true);
        if (!nearbyPlayers.isEmpty()) {
            return "a player";
        }
        
        return "open space"; // Default
    }
    
    // --- Specific Behavior Execution Methods ---
    
    private void executeWorkBehavior(ServerWorld world) {
        // Example: If at workstation, occasionally interact with it
        if (villager.getBrain().getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.JOB_SITE).isPresent()) {
            BlockPos jobSitePos = villager.getBrain().getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.JOB_SITE).get().pos();
            if (villager.getBlockPos().getSquaredDistance(jobSitePos) < 4.0) { // If close to job site
                if (world.random.nextInt(100) == 0) { // Occasionally
                    villager.swingHand(Hand.MAIN_HAND);
                    // Play work sound based on profession?
                    LOGGER.trace("Villager {} working at {}", villager.getName().getString(), jobSitePos);
                }
            }
        }
        // Add more profession-specific work actions here
    }
    
    private void executeSocializeBehavior(ServerWorld world) {
        // Example: Look for nearby villagers/players and path towards them slightly
        Box checkBox = villager.getBoundingBox().expand(10.0);
        List<LivingEntity> potentialTargets = world.getEntitiesByClass(LivingEntity.class, checkBox, e ->
            (e instanceof VillagerEntity && e != villager) || e instanceof PlayerEntity);
            
        if (!potentialTargets.isEmpty()) {
            LivingEntity target = potentialTargets.get(world.random.nextInt(potentialTargets.size()));
            villager.getLookControl().lookAt(target, 30f, 30f);
            
            // Slightly move towards target if far, but don't chase aggressively
            if (villager.distanceTo(target) > 4.0 && world.random.nextInt(5) == 0) {
                villager.getNavigation().startMovingTo(target, 0.6D);
            }
            LOGGER.trace("Villager {} socializing near {}", villager.getName().getString(), target.getName().getString());
        } else {
            // If no one nearby, just continue wandering slowly
            if (!activeGoals.containsKey("socialize_wander")) {
                 Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.5D);
                 addGoal(wanderGoal, 2, "socialize_wander");
            }
        }
    }
    
    private void executeTradeBehavior(ServerWorld world) {
        // Check if villager attempts to trade based on frequency setting
        ModConfig config = ModConfigManager.getConfig();
        double adjustedTradeChance = BASE_TRADE_ATTEMPT_CHANCE * config.gameplay.tradingFrequency;
        // Use villager's world random instance for consistency
        if (villager.getWorld().random.nextDouble() >= adjustedTradeChance) {
            // Didn't attempt trade this cycle, maybe wander instead
            applyWanderGoals("trade_wander_failed_chance");
            // Ensure activity reflects not actively trading if chance fails
            if (getCurrentActivity().equals("trading")) {
                 updateActivity("wandering"); // Or another suitable fallback
            }
            return;
        }

        // Check trading frequency multiplier
        float tradeMultiplier = VillagesConfig.getInstance().getGameplaySettings().getTradingFrequencyMultiplier();
        if (tradeMultiplier <= 0) tradeMultiplier = 1.0f;

        // Use the multiplier to determine the probability of executing the trade logic this tick
        // Multiplier > 1 decreases frequency (lower probability)
        // Multiplier < 1 increases frequency (higher probability)
        // Example: If multiplier is 2.0, probability is 1/2 = 0.5. If 0.5, probability is 1/0.5 = 2.0 (clamped to 1.0)
        double executionProbability = 1.0 / tradeMultiplier;

        // Only execute trade logic based on probability
        if (world.random.nextDouble() > executionProbability) {
            // Skip trading logic this tick due to frequency setting
            // Optionally, add some idle behavior here if needed, or just return
            return;
        }

        // --- Original Trading Logic ---
        // Example: Look for players, maybe display emerald particles?
        Box checkBox = villager.getBoundingBox().expand(8.0);
        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(PlayerEntity.class, checkBox, p -> true);
        
        if (!nearbyPlayers.isEmpty()) {
            PlayerEntity targetPlayer = nearbyPlayers.get(0); // Look at the first player found
            villager.getLookControl().lookAt(targetPlayer, 30f, 30f);
            
            // Show emerald particles occasionally
            if (world.random.nextInt(40) == 0) {
                world.spawnParticles(ParticleTypes.EMERALD,
                    villager.getX(), villager.getY() + 1.5, villager.getZ(),
                    3, 0.2, 0.2, 0.2, 0.0);
                LOGGER.trace("Villager {} ready to trade near {}", villager.getName().getString(), targetPlayer.getName().getString());
            }
        }
        // Villager should mostly stay put while trading (handled by low wander speed in applyTradeGoals)
    }
    
    private void executeCelebrationBehavior(ServerWorld world) {
        // Example: Wander around happily, occasionally jump or show happy particles
        if (!activeGoals.containsKey("celebrate_wander")) {
            Goal wanderGoal = new WanderAroundGoal((PathAwareEntity)villager, 0.7D);
            addGoal(wanderGoal, 1, "celebrate_wander");
        }
        
        if (world.random.nextInt(60) == 0) { // Occasionally jump
            villager.getJumpControl().setActive();
            LOGGER.trace("Villager {} jumping during celebration", villager.getName().getString());
        }
        
        if (world.random.nextInt(30) == 0) { // Occasionally show happy particles
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                villager.getX(), villager.getY() + 1.0, villager.getZ(),
                4, 0.3, 0.3, 0.3, 0.0);
        }
    }
    
    private void executeFleeingBehavior(ServerWorld world) {
        // Goals for fleeing are already applied in applyFleeGoals.
        // This method could add extra effects, like panic particles.
        if (world.random.nextInt(10) == 0) {
            world.spawnParticles(ParticleTypes.SMOKE, // Or another particle?
                villager.getX(), villager.getY() + 0.5, villager.getZ(),
                2, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Check if the danger source is gone
        String target = behaviorParams.getOrDefault("target", "danger").toString();
        boolean dangerPresent = false;
        if (target.contains("monster")) {
             Box checkBox = villager.getBoundingBox().expand(10.0);
             dangerPresent = !world.getEntitiesByClass(Monster.class, checkBox, m -> m.isAlive()).isEmpty();
        } else if (target.contains("player")) {
             Box checkBox = villager.getBoundingBox().expand(8.0);
             dangerPresent = !world.getEntitiesByClass(PlayerEntity.class, checkBox, p -> true).isEmpty(); // Simple check if any player is nearby
        } else {
            // General danger check - maybe based on memory?
             dangerPresent = villager.getBrain().getOptionalMemory(MemoryModuleType.HURT_BY).isPresent() ||
                             villager.getBrain().getOptionalMemory(MemoryModuleType.HURT_BY_ENTITY).isPresent();
        }
        
        // If danger is gone, complete the behavior early
        if (!dangerPresent && world.getTime() > behaviorStartTime + 40) { // Minimum flee time 2 seconds
             LOGGER.debug("Danger seems gone, villager {} stopping flee behavior", villager.getName().getString());
             completeBehavior();
        }
    }
    
    private void executeSleepingBehavior(ServerWorld world) {
        // Villager should be pathing towards bed via goals applied in applyRestGoals.
        // Check if villager has reached a bed.
        BlockPos currentPos = villager.getBlockPos();
        BlockState blockState = world.getBlockState(currentPos);
        
        // Crude check if the block is a bed
        if (blockState.getBlock().toString().contains("bed")) {
            // If on a bed, stop moving and maybe play sleeping particles/sounds?
            if (villager.getNavigation().isIdle()) {
                 // Already stopped moving
                 if (world.random.nextInt(100) == 0) {
                     // Play Zzz particle? (No standard particle for this)
                     LOGGER.trace("Villager {} sleeping at {}", villager.getName().getString(), currentPos);
                 }
            } else {
                 villager.getNavigation().stop();
                 LOGGER.debug("Villager {} reached bed at {}, stopping navigation", villager.getName().getString(), currentPos);
            }
        } else {
            // If not near a bed, ensure the move goal is still active
            if (!activeGoals.containsKey("move_to_bed")) { // Assuming the move goal ID is consistent
                 applyRestGoals("bed"); // Re-apply goals if needed
                 LOGGER.trace("Villager {} re-applying sleep goals", villager.getName().getString());
            }
        }
    }
    
    private void executeIdleBehavior(ServerWorld world) {
        // Default behavior when nothing else is happening.
        // Mostly relies on the wander/look goals applied.
        // Could add very occasional small actions.
        if (world.random.nextInt(500) == 0) {
            // Briefly look around or scratch head? (No standard animation)
            villager.swingHand(Hand.MAIN_HAND); // Simple visual cue
            LOGGER.trace("Villager {} idling", villager.getName().getString());
        }
    }
    
    // --- Utility Methods ---
    
    /**
     * Finds the nearest bed block within a certain radius.
     * @param world The server world
     * @return Optional containing the BlockPos of the nearest bed, or empty if none found.
     */
    private Optional<BlockPos> findNearestBed(ServerWorld world) {
        BlockPos villagerPos = villager.getBlockPos();
        int searchRadius = 16; // Search radius for beds
        
        return BlockPos.streamOutwards(villagerPos, searchRadius, searchRadius, searchRadius)
            .filter(pos -> {
                BlockState state = world.getBlockState(pos);
                // Basic check for any block containing "bed" in its name
                return state.getBlock().toString().toLowerCase().contains("bed");
            })
            .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(villagerPos)))
            .map(BlockPos::toImmutable); // Make sure the returned pos is immutable
    }
    
    /**
     * Gets a random position within a certain radius around a center point.
     * @param center The center position
     * @param radius The radius
     * @return A random BlockPos within the radius
     */
    private BlockPos getRandomPositionAround(BlockPos center, int radius) {
        Random random = villager.getWorld().random;
        int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
        int y = center.getY(); // Keep Y level the same for simplicity, or add small variation
        int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
        return new BlockPos(x, y, z);
    }
    
    // --- Getters/Setters for internal state ---
    
    public float getProfessionSkill(String skill) {
        return professionSkills.getOrDefault(skill.toLowerCase(), 0.0f);
    }
    
    public boolean isPvPEnabled() {
        return pvpEnabled;
    }
    
    public RelationshipType getRelationshipWith(UUID otherVillagerId) {
        // Needs implementation based on how relationships are stored
        // Example: return VillagerManager.getInstance().getRelationship(villager.getUuid(), otherVillagerId);
        return RelationshipType.NEUTRAL; // Placeholder
    }
    
    public void updateProfessionSkill(String skill, float newValue) {
        professionSkills.put(skill.toLowerCase(), Math.max(0.0f, Math.min(1.0f, newValue)));
    }
    
    // Add other necessary getters/setters if needed

    /**
     * Updates relationships based on interaction sentiment, applying intensity and cultural bias.
     * @param v1 The first villager AI
     * @param v2 The second villager AI
     * @param interaction The parsed interaction details from the LLM
     */
    private void updateRelationships(VillagerAI v1, VillagerAI v2, Map<String, String> interaction) {
        // Get multipliers from config
        // Get multipliers from the new ModConfig
        ModConfig config = ModConfigManager.getConfig();
        double intensityMultiplier = config.gameplay.relationshipIntensity;
        boolean culturalBias = config.gameplay.culturalBiasMultiplier > 0; // Check if bias multiplier is enabled (>0)
        if (intensityMultiplier <= 0) intensityMultiplier = 1.0; // Safety check

        String sentiment = interaction.getOrDefault("SENTIMENT", "neutral").toLowerCase();
        RelationshipType newType = RelationshipType.NEUTRAL;
        int baseChange = 0; // Base amount relationship changes per interaction

        if (sentiment.contains("positive")) {
            newType = RelationshipType.FRIEND;
            baseChange = 5; // Example base positive change
        } else if (sentiment.contains("negative")) {
            newType = RelationshipType.RIVAL;
            baseChange = -5; // Example base negative change
        }

        // Apply intensity multiplier
        int adjustedChange = (int) (baseChange * intensityMultiplier);

        // Apply cultural bias if enabled
        // Apply cultural bias if enabled and multiplier is not 1.0
        if (culturalBias && config.gameplay.culturalBiasMultiplier != 1.0) {
            VillagerManager vm = VillagerManager.getInstance();
            SpawnRegion region1 = vm.getNearestSpawnRegion(v1.getVillager().getBlockPos());
            SpawnRegion region2 = vm.getNearestSpawnRegion(v2.getVillager().getBlockPos());
            String culture1 = (region1 != null) ? region1.getCultureAsString() : null;
            String culture2 = (region2 != null) ? region2.getCultureAsString() : null;

            double biasEffect = 0;
            final double BASE_CULTURAL_BIAS_EFFECT = 2.0; // Base effect amount

            if (culture1 != null && culture2 != null) {
                if (culture1.equals(culture2)) {
                    // Positive bias for same culture, scaled by multiplier
                    biasEffect = BASE_CULTURAL_BIAS_EFFECT * config.gameplay.culturalBiasMultiplier;
                } else {
                    // Negative bias for different cultures, scaled by multiplier
                    biasEffect = -BASE_CULTURAL_BIAS_EFFECT * config.gameplay.culturalBiasMultiplier;
                }
            }
            // Add the calculated bias effect to the change
            adjustedChange += (int) biasEffect;
            LOGGER.trace("Applied cultural bias effect: {} (Multiplier: {}, Culture1: {}, Culture2: {})",
                         (int) biasEffect, config.gameplay.culturalBiasMultiplier, culture1, culture2);
        }

// Apply adjustedChange to the numerical relationship score
VillagerManager manager = VillagerManager.getInstance();
UUID uuid1 = v1.getVillager().getUuid();
UUID uuid2 = v2.getVillager().getUuid();
Relationship relationship = manager.getRelationship(uuid1, uuid2);

if (relationship != null) {
    relationship.modifyScore(adjustedChange);
    LOGGER.debug("Updated relationship score between {} and {}: {} (Change: {})",
                 v1.getVillager().getName().getString(), v2.getVillager().getName().getString(),
                 relationship.getScore(), adjustedChange);
} else {
     // If no relationship exists yet, the code below (addRelationship) will create it.
     // The initial score will be 0, and this change won't be applied until the next interaction.
     // Alternatively, could modify addRelationship to take an initial score. For now, log this.
     LOGGER.debug("No existing relationship found to apply score change between {} and {}. Will be created.",
                  v1.getVillager().getName().getString(), v2.getVillager().getName().getString());
}

LOGGER.debug("Calculated relationship change: {} (Base: {}, Intensity Multiplier: {}, Bias Applied: {}) between {} and {}",
    adjustedChange, baseChange, intensityMultiplier, culturalBias, v1.getVillager().getName().getString(), v2.getVillager().getName().getString());
            adjustedChange, baseChange, intensityMultiplier, culturalBias, v1.getVillager().getName().getString(), v2.getVillager().getName().getString());

        // Update the categorical relationship type (Friend/Rival/Neutral) based on the sentiment
        // This part might need refinement based on how numerical scores translate to types
        // For now, we still use the sentiment-based type, but the underlying score is adjusted.
        v1.addRelationship(v2.getVillager().getUuid(), newType);
        v2.addRelationship(v1.getVillager().getUuid(), newType);
    }
}
