package com.beeny.village;

import com.beeny.ai.LLMService;
import com.beeny.config.VillagesConfig;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
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

import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.*;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.*;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class VillagerAI {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;
    private static final Map<String, CachedBehavior> behaviorCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRY_MS = 1800000; // 30 minutes
    // Store the PvP goal references to allow removal
    private static final Map<UUID, ActiveTargetGoal<VillagerEntity>> pvpGoals = new ConcurrentHashMap<>();

    private static class CachedBehavior {
        final String behavior;
        final long timestamp;

        CachedBehavior(String behavior) {
            this.behavior = behavior;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }

    private final VillagerEntity villager;
    private final String personality;
    private BlockPos homePos;
    private String currentActivity;
    private Map<Integer, ActivitySchedule> schedule;
    private int happiness;
    private Map<UUID, RelationshipType> relationships = new ConcurrentHashMap<>();
    private Map<String, Integer> professionSkills = new ConcurrentHashMap<>();
    private final Map<String, List<Activity>> dailySchedule = new HashMap<>();
    private Activity currentActivityDetail;
    private long lastActivityUpdate;
    private final Map<String, Object> behaviorMemory = new ConcurrentHashMap<>();
    private final Queue<Activity> plannedActivities = new ConcurrentLinkedQueue<>();
    private final Map<String, Integer> activityPriorities = new ConcurrentHashMap<>();
    private BehaviorState currentState = BehaviorState.IDLE;
    // Add a busy flag for the villager
    private boolean busy = false;

    // Store theft records for this villager's memory
    private final Map<UUID, List<TheftRecord>> theftMemory = new ConcurrentHashMap<>();
    // Track if this villager has PvP goals added
    private boolean pvpEnabled = false;

    public static class TheftRecord {
        public final UUID playerID;
        public final long timestamp;
        public final String stolenItem;
        public final BlockPos location;
        
        public TheftRecord(UUID playerID, String stolenItem, BlockPos location) {
            this.playerID = playerID;
            this.timestamp = System.currentTimeMillis();
            this.stolenItem = stolenItem;
            this.location = location;
        }
        
        public boolean isRecent() {
            // Consider thefts within the last 24 hours (real time) as recent
            return System.currentTimeMillis() - timestamp < 86400000;
        }
    }

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

    public enum RelationshipType {
        FRIEND(10),
        FAMILY(20),
        RIVAL(-10),
        NEUTRAL(0), // Added NEUTRAL relationship type
        ENEMY(-20); // Added ENEMY relationship type for serious conflicts

        public final int moodModifier;

        RelationshipType(int moodModifier) {
            this.moodModifier = moodModifier;
        }
    }

    // Addition: Add PvP goals to villager
    /**
     * Add PvP goals to this villager if PvP is enabled in config
     * This allows villagers to target and attack other villagers they have negative relationships with
     */
    public void addPvPGoals() {
        // Check if villager PvP is enabled in config
        if (!VillagesConfig.getInstance().isVillagerPvPEnabled()) {
            return;
        }
        
        if (pvpEnabled) {
            return; // Already has PvP goals
        }

        // Set up revenge goal - villager will target entities that attack it
        RevengeGoal revengeGoal = new RevengeGoal((MobEntity)(Object)villager, VillagerEntity.class);
        ((MobEntity)(Object)villager).targetSelector.add(1, revengeGoal);
        
        // Setup attack goal for enemy villagers
        ActiveTargetGoal<VillagerEntity> targetGoal = new ActiveTargetGoal<>((MobEntity)(Object)villager, 
            VillagerEntity.class, 10, true, false, (potentialTarget) -> {
                if (!(potentialTarget instanceof VillagerEntity otherVillager)) {
                    return false;
                }
                RelationshipType relationship = getRelationshipWith(otherVillager.getUuid());
                // Only attack enemies or rivals who are too close
                return relationship == RelationshipType.ENEMY || 
                       (relationship == RelationshipType.RIVAL && 
                        otherVillager.squaredDistanceTo(villager) < 5);
            });
            
        ((MobEntity)(Object)villager).targetSelector.add(2, targetGoal);
        pvpGoals.put(villager.getUuid(), targetGoal);
        pvpEnabled = true;
        
        // Modify villager's movement speed when angry for better combat
        if (((MobEntity)(Object)villager).getTarget() != null) {
            villager.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED, 100, 1, false, false));
        }
        
        LOGGER.debug("Added PvP goals to villager: {}", villager.getName().getString());
    }
    
    /**
     * Remove PvP goals from this villager
     */
    public void removePvPGoals() {
        if (!pvpEnabled) {
            return;
        }
        
        ActiveTargetGoal<VillagerEntity> targetGoal = pvpGoals.remove(villager.getUuid());
        if (targetGoal != null) {
            ((MobEntity)(Object)villager).targetSelector.remove(targetGoal);
        }
        pvpEnabled = false;
        LOGGER.debug("Removed PvP goals from villager: {}", villager.getName().getString());
    }
    
    /**
     * Record a theft committed by a player against this villager's property
     */
    public void recordTheft(UUID playerID, String stolenItem, BlockPos location) {
        theftMemory.computeIfAbsent(playerID, k -> new ArrayList<>())
                  .add(new TheftRecord(playerID, stolenItem, location));
        
        // Deteriorate relationship with thief
        relationships.put(playerID, RelationshipType.ENEMY);
        adjustHappiness(-10); // Stealing makes villagers unhappy
        
        // If PvP is enabled, consider the thief an enemy
        if (VillagesConfig.getInstance().isVillagerPvPEnabled() && 
            VillagesConfig.getInstance().isTheftDetectionEnabled()) {
            addPvPGoals(); // Make sure PvP goals are added
        }
        
        // Alert nearby villagers about theft
        World world = villager.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            alertNearbyVillagersAboutTheft(serverWorld, playerID, location);
        }
    }
    
    /**
     * Alert nearby villagers about a theft
     */
    private void alertNearbyVillagersAboutTheft(ServerWorld world, UUID thiefID, BlockPos location) {
        // Find nearby villagers within 32 blocks
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class, 
            new Box(location).expand(32),
            v -> v != villager
        );
        
        for (VillagerEntity otherVillager : nearbyVillagers) {
            VillagerAI otherAI = VillagerManager.getInstance().getVillagerAI(otherVillager.getUuid());
            if (otherAI != null) {
                // Make them aware of the theft with a reduced severity
                otherAI.onTheftWitnessed(thiefID, location);
            }
        }
    }
    
    /**
     * Called when this villager witnesses a theft but wasn't the direct victim
     */
    public void onTheftWitnessed(UUID thiefID, BlockPos location) {
        // Deteriorate relationship but not as severely as the direct victim
        RelationshipType currentRelationship = relationships.getOrDefault(thiefID, RelationshipType.NEUTRAL);
        
        if (currentRelationship != RelationshipType.ENEMY) {
            relationships.put(thiefID, RelationshipType.RIVAL);
            adjustHappiness(-5); // Witnessing theft causes unhappiness
        }
        
        // Add memory of witnessing the theft
        theftMemory.computeIfAbsent(thiefID, k -> new ArrayList<>())
                  .add(new TheftRecord(thiefID, "witnessed theft", location));
    }

    /**
     * Check if a player is known to have stolen from this villager
     * @return true if player has stolen from this villager recently
     */
    public boolean hasPlayerStolen(UUID playerID) {
        List<TheftRecord> records = theftMemory.get(playerID);
        if (records == null || records.isEmpty()) {
            return false;
        }
        // Only consider recent thefts
        return records.stream().anyMatch(TheftRecord::isRecent);
    }

    public VillagerAI(VillagerEntity villager, String personality) {
            this.villager = villager;
            this.personality = personality;
            this.homePos = villager.getBlockPos();
            this.currentActivity = "idle";
            this.schedule = createSchedule(villager.getVillagerData().getProfession().toString());
            this.happiness = 50; // Default happiness level
            applyDimensionTraits();
        }
    
        public void applyDimensionTraits() {
            String culture = getCurrentCulturalContext();
            if (culture.startsWith("infernal_") || culture.startsWith("obsidian_") ||
                culture.startsWith("crimson_") || culture.startsWith("warped_")) {
                villager.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
            }
        }

    /**
     * Check if the villager is currently busy with an activity that shouldn't be interrupted
     * @return true if the villager is busy
     */
    public boolean isBusy() {
        return busy;
    }

    /**
     * Set the busy state of the villager
     * @param busy true if the villager is busy with an important activity
     */
    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public int getHappiness() {
        return happiness;
    }

    public void setHappiness(int happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }

    public void adjustHappiness(int amount) {
        setHappiness(this.happiness + amount);
    }

    public int getFriendshipLevel() {
        // Calculate friendship level (1-10) based on happiness and relationships
        int baseLevel = Math.max(1, Math.min(10, (happiness / 10)));
        
        // Boost level based on positive relationships
        long friendCount = relationships.values().stream()
            .filter(r -> r == RelationshipType.FRIEND || r == RelationshipType.FAMILY)
            .count();
        
        int relationshipBonus = (int) Math.min(3, friendCount / 2);
        return Math.min(10, baseLevel + relationshipBonus);
    }

    private Map<Integer, ActivitySchedule> createSchedule(String profession) {
        StringBuilder prompt = new StringBuilder()
            .append(String.format(
                "Create a realistic daily schedule for a %s villager with %s personality in a %s village.\n" +
                "Their current happiness is %d/100.\n\n" +
                "Consider profession-specific activities:\n",
                profession, personality, getCurrentCulturalContext(), happiness
            ));

        // Add profession-specific activity suggestions
        switch(profession.toLowerCase()) {
            case "farmer":
                prompt.append("- Tending crops (planting, harvesting)\n")
                      .append("- Checking farmland and irrigation\n")
                      .append("- Storing harvested goods\n");
                break;
            case "librarian":
                prompt.append("- Reading and organizing books\n")
                      .append("- Teaching villagers at lectern\n")
                      .append("- Studying and research\n");
                break;
            case "blacksmith":
                prompt.append("- Forging tools and weapons\n")
                      .append("- Repairing equipment\n")
                      .append("- Working at anvil\n");
                break;
            case "cleric":
                prompt.append("- Brewing potions\n")
                      .append("- Gathering brewing ingredients\n")
                      .append("- Offering blessings\n");
                break;
            default:
                prompt.append("- Work-related activities\n")
                      .append("- Trading with customers\n")
                      .append("- Organizing inventory\n");
        }

        prompt.append("\nFormat schedule entries as:\n")
              .append("TIME: (0-24000 minecraft ticks)\n")
              .append("ACTIVITY: (specific activity name)\n")
              .append("LOCATION: (work/home/social)\n")
              .append("DURATION: (ticks)\n")
              .append("\nProvide 6-8 detailed schedule entries that show a full day's routine.");

        Map<Integer, ActivitySchedule> schedule = new ConcurrentHashMap<>();

        LLMService.getInstance()
            .generateResponse(prompt.toString())
            .thenApply(response -> Arrays.stream(response.split("\n\n"))
                .map(entry -> parseScheduleEntry(entry))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                    entry -> entry.time,
                    entry -> new ActivitySchedule(
                        entry.activity,
                        getLocationForType(entry.locationType),
                        entry.duration
                    ),
                    (a, b) -> b // In case of duplicate times, keep the later entry
                )))
            .thenAccept(newSchedule -> {
                schedule.putAll(newSchedule);
                LOGGER.debug("Generated schedule for {} ({}): {} entries",
                    villager.getName().getString(),
                    profession,
                    newSchedule.size());
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating schedule for {}, using fallback", profession, e);
                // Fallback schedule with more variety
                int workStart = 1000; // Early morning
                schedule.put(0, new ActivitySchedule("rest", homePos, workStart));
                schedule.put(workStart, new ActivitySchedule("work", findNearestWorkstation(profession), 8000));
                schedule.put(9000, new ActivitySchedule("socializing", findVillageCenter(), 2000));
                schedule.put(11000, new ActivitySchedule("work", findNearestWorkstation(profession), 6000));
                schedule.put(17000, new ActivitySchedule("rest", homePos, 7000));
                return null;
            });

        return schedule;
    }

    private static class ScheduleEntry {
        final int time;
        final String activity;
        final String locationType;
        final int duration;

        ScheduleEntry(int time, String activity, String locationType, int duration) {
            this.time = time;
            this.activity = activity;
            this.locationType = locationType;
            this.duration = duration;
        }
    }

    private ScheduleEntry parseScheduleEntry(String entry) {
        try {
            Map<String, String> params = Arrays.stream(entry.split("\n"))
                .map(line -> line.split(": ", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                    parts -> parts[0],
                    parts -> parts[1].trim()
                ));

            return new ScheduleEntry(
                Integer.parseInt(params.getOrDefault("TIME", "0")),
                params.getOrDefault("ACTIVITY", "idle"),
                params.getOrDefault("LOCATION", "home"),
                Integer.parseInt(params.getOrDefault("DURATION", "6000"))
            );
        } catch (Exception e) {
            LOGGER.error("Error parsing schedule entry", e);
            return null;
        }
    }

    private BlockPos getLocationForType(String locationType) {
        return switch(locationType.toLowerCase()) {
            case "work" -> findNearestWorkstation(villager.getVillagerData().getProfession().toString());
            case "social" -> findVillageCenter();
            default -> homePos;
        };
    }

    public void updateActivityBasedOnTime(ServerWorld world) {
        long timeOfDay = world.getTimeOfDay() % 24000;

        // Find the appropriate schedule entry
        ActivitySchedule currentSchedule = null;
        int lastTime = 0;

        for (Map.Entry<Integer, ActivitySchedule> entry : schedule.entrySet()) {
            if (timeOfDay >= entry.getKey() && entry.getKey() >= lastTime) {
                currentSchedule = entry.getValue();
                lastTime = entry.getKey();
            }
        }

        if (currentSchedule != null) {
            updateActivity(currentSchedule.activity);
            // Move villager towards activity location
            if (!villager.getBlockPos().equals(currentSchedule.location)) {
                villager.getNavigation().startMovingTo(
                    currentSchedule.location.getX(),
                    currentSchedule.location.getY(),
                    currentSchedule.location.getZ(),
                    0.5D
                );
            }
        }
    }

    private BlockPos findNearestWorkstation(String blockType) {
        ServerWorld world = (ServerWorld) villager.getWorld();
        if (world == null) return homePos;

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos closestPos = null;
        double closestDistance = Double.MAX_VALUE;

        for (int x = -16; x <= 16; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -16; z <= 16; z++) {
                    mutable.set(
                        villager.getBlockX() + x,
                        villager.getBlockY() + y,
                        villager.getBlockZ() + z
                    );
                    
                    if (isMatchingWorkstation(world, mutable, blockType)) {
                        double distance = villager.getBlockPos().getSquaredDistance(mutable);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestPos = mutable.toImmutable();
                        }
                    }
                }
            }
        }

        return closestPos != null ? closestPos : homePos;
    }

    private boolean isMatchingWorkstation(ServerWorld world, BlockPos pos, String type) {
        Block block = world.getBlockState(pos).getBlock();
        return switch(type.toLowerCase()) {
            case "farmland" -> block == Blocks.FARMLAND;
            case "lectern" -> block == Blocks.LECTERN;
            case "smithing_table" -> block == Blocks.SMITHING_TABLE;
            case "brewing_stand" -> block == Blocks.BREWING_STAND;
            case "composter" -> block == Blocks.COMPOSTER;
            case "barrel" -> block == Blocks.BARREL;
            case "fletching_table" -> block == Blocks.FLETCHING_TABLE;
            case "cartography_table" -> block == Blocks.CARTOGRAPHY_TABLE;
            case "loom" -> block == Blocks.LOOM;
            default -> false;
        };
    }

    private BlockPos findVillageCenter() {
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        return region != null ? region.getCenter() : homePos;
    }

    public CompletableFuture<String> generateBehavior(String situation) {
        return generateBehavior(situation, DEFAULT_TIMEOUT_SECONDS);
    }

    public CompletableFuture<String> generateBehavior(String situation, int timeoutSeconds) {
        // Create a more generalized cache key focusing on essential context
        String cacheKey = String.format("%s|%s|%s",
            personality,
            situation.replaceAll("\\s+", "_").toLowerCase(),
            currentActivity
        );
        
        // Check cache and remove if expired
        CachedBehavior cached = behaviorCache.get(cacheKey);
        if (cached != null) {
            if (!cached.isExpired()) {
                LOGGER.debug("Using cached behavior for {}: {}", villager.getName().getString(), cached.behavior);
                return CompletableFuture.completedFuture(cached.behavior);
            } else {
                behaviorCache.remove(cacheKey);
            }
        }

        // Periodically prune expired cache entries
        if (Math.random() < 0.1) { // 10% chance to trigger cleanup
            pruneExpiredCache();
        }

        // Build rich context for the LLM
        StringBuilder context = new StringBuilder()
            .append("As a Minecraft villager with:\n")
            .append("- ").append(personality).append(" personality\n")
            .append("- Happiness: ").append(happiness).append("/100\n")
            .append("- Current activity: ").append(currentActivity).append("\n")
            .append("- Profession skills: ").append(getSkillSummary()).append("\n")
            .append("- Cultural background: ").append(getCurrentCulturalContext()).append("\n")
            .append("- Social status: ").append(getSocialSummary()).append("\n");

        String prompt = context.toString() + 
            "\nWhat would you do in this situation: " + situation + "?\n" +
            "Respond with a brief action description in 10 words or less.";

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(response -> {
                String behavior = response.trim();
                LOGGER.debug("Generated behavior for {}: {}", villager.getName().getString(), behavior);

                if (behaviorCache.size() < MAX_CACHE_SIZE || removeOldestExpiredEntry()) {
                    behaviorCache.put(cacheKey, new CachedBehavior(behavior));
                }
                return behavior;
            })
            .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .exceptionally(e -> {
                if (e.getCause() instanceof TimeoutException) {
                    LOGGER.warn("Behavior generation timed out after {} seconds for {}",
                        timeoutSeconds, villager.getName().getString());
                    return "Continue " + currentActivity;
                } else {
                    LOGGER.error("Error generating behavior for {}: {}",
                        villager.getName().getString(), e.getMessage());
                    return "Stay in current location";
                }
            });
    }

    private String getCurrentCulturalContext() {
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        if (region == null) return "unknown culture";
        
        StringBuilder context = new StringBuilder();
        context.append(region.getCulture().toString());
        
        // Add active cultural events
        List<VillagerManager.CulturalEvent> events = vm.getCurrentEvents(villager.getBlockPos(), (ServerWorld)villager.getWorld());
        if (!events.isEmpty()) {
            context.append(" (").append(events.get(0).name).append(" ongoing)");
        }
        
        return context.toString();
    }

    private String getSkillSummary() {
        return professionSkills.entrySet().stream()
            .map(e -> String.format("%s:%d", e.getKey(), e.getValue()))
            .collect(Collectors.joining(", "));
    }

    public String getSocialSummary() {
        long friends = relationships.values().stream().filter(r -> r == RelationshipType.FRIEND).count();
        long family = relationships.values().stream().filter(r -> r == RelationshipType.FAMILY).count();
        long rivals = relationships.values().stream().filter(r -> r == RelationshipType.RIVAL).count();
        
        return String.format("%d friends, %d family, %d rivals", friends, family, rivals);
    }

    public void updateActivity(String newActivity) {
        updateActivityInternal(newActivity);
    }

    public void updateActivity(Activity activity) {
        this.currentActivityDetail = activity;
        updateActivityInternal(activity.type());
    }

    private void updateActivityInternal(String newActivity) {
        if (newActivity.equals("welcoming")) {
            // Move towards player and wave
            VillagerEntity villager = getVillager();
            List<ServerPlayerEntity> nearbyPlayers = ((ServerWorld)villager.getWorld()).getPlayers()
                .stream()
                .filter(player -> player.squaredDistanceTo(villager) < 100)
                .collect(Collectors.toList());

            if (!nearbyPlayers.isEmpty()) {
                ServerPlayerEntity player = nearbyPlayers.get(0);
                villager.getLookControl().lookAt(player);
                
                // Wave animation (using villager's work animation)
                villager.setHeadRollingTimeLeft(40);
                villager.swingHand(Hand.MAIN_HAND);
            }
        }
        if (!newActivity.equals(currentActivity)) {
            // Generate contextual thoughts about activity change
            String situation = String.format("Switching from %s to %s", currentActivity, newActivity);
            generateBehavior(situation)
                .thenAccept(thought -> {
                    LOGGER.debug("{} thinks: {}", villager.getName().getString(), thought);
                });
                
            // Add visual effects for activity change
            ServerWorld world = (ServerWorld) villager.getWorld();
            addActivityEffects(world, newActivity);
        }
        
        this.currentActivity = newActivity;
        villager.setCustomName(Text.of(String.format("%s (%s)", 
            villager.getName().getString(), currentActivity)));

        // Notify activity listeners
        VillagerActivityCallback.EVENT.invoker().onActivity(
            villager,
            (ServerWorld) villager.getWorld(),
            newActivity,
            villager.getBlockPos()
        );

        // Update profession skills
        updateProfessionSkill(newActivity);
    }

    private void addActivityEffects(ServerWorld world, String activity) {
        double x = villager.getX();
        double y = villager.getY();
        double z = villager.getZ();

        switch(activity.toLowerCase()) {
            case "farming":
                world.spawnParticles(ParticleTypes.COMPOSTER, x, y, z, 10, 0.5, 0.1, 0.5, 0.1);
                break;
            case "reading", "studying":
                world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.5, z, 5, 0.2, 0.2, 0.2, 0.1);
                break;
            case "smithing", "crafting":
                world.spawnParticles(ParticleTypes.CRIT, x, y, z, 8, 0.3, 0.3, 0.3, 0.1);
                break;
            case "trading":
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1, z, 5, 0.2, 0.2, 0.2, 0);
                break;
            case "socializing":
                world.spawnParticles(ParticleTypes.HEART, x, y + 1, z, 1, 0, 0, 0, 0);
                break;
        }
    }

    public String getCurrentActivity() {
        return currentActivity;
    }

    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(BlockPos pos) {
        this.homePos = pos;
    }

    public String getPersonality() {
        return personality;
    }

    public VillagerEntity getVillager() {
        return villager;
    }

    public CompletableFuture<RelationshipType> determineRelationshipDynamically(VillagerEntity other) {
        StringBuilder context = new StringBuilder()
            .append("You are a Minecraft villager with:\n")
            .append("- ").append(personality).append(" personality\n")
            .append("- Happiness: ").append(happiness).append("/100\n")
            .append("- Cultural background: ").append(getCurrentCulturalContext()).append("\n")
            .append("\nYou meet another villager who is a ").append(other.getVillagerData().getProfession())
            .append(". Based on your personality and their profession, what kind of relationship would you form?\n")
            .append("Respond with exactly one word: FRIEND, FAMILY, or RIVAL");

        return LLMService.getInstance()
            .generateResponse(context.toString())
            .thenApply(response -> {
                String type = response.trim().toUpperCase();
                return RelationshipType.valueOf(type);
            })
            .exceptionally(e -> {
                LOGGER.error("Error determining relationship, defaulting to FRIEND", e);
                return RelationshipType.FRIEND;
            });
    }

    public CompletableFuture<String> generateDialogue(String situation, VillagerEntity other) {
        StringBuilder context = new StringBuilder()
            .append("You are a Minecraft villager with:\n")
            .append("- ").append(personality).append(" personality\n")
            .append("- Current activity: ").append(currentActivity).append("\n")
            .append("- Cultural background: ").append(getCurrentCulturalContext()).append("\n");

        if (other != null) {
            RelationshipType relationship = relationships.get(other.getUuid());
            context.append("- Speaking to: ").append(other.getName().getString())
                  .append(" (").append(relationship != null ? relationship : "stranger").append(")\n");
        }

        context.append("\nSituation: ").append(situation)
               .append("\nRespond with a brief, natural dialogue line under 15 words.");

        return LLMService.getInstance()
            .generateResponse(context.toString())
            .exceptionally(e -> {
                LOGGER.error("Error generating dialogue", e);
                return "...";
            });
    }

    public void addRelationship(UUID otherVillager, RelationshipType type) {
        relationships.put(otherVillager, type);
        adjustHappiness(type.moodModifier);
        
        // Generate and cache initial dialogue for this relationship
        Entity entity = ((ServerWorld)villager.getWorld()).getEntity(otherVillager);
        if (entity instanceof VillagerEntity other) {
            generateDialogue("Meeting new " + type.toString().toLowerCase(), other)
                .thenAccept(dialogue -> {
                    String cacheKey = "meeting|" + otherVillager.toString();
                    behaviorCache.put(cacheKey, new CachedBehavior(dialogue));
                });
        }
    }

    public RelationshipType getRelationshipWith(UUID otherVillager) {
        return relationships.getOrDefault(otherVillager, null);
    }

    public Map<UUID, RelationshipType> getRelationships() {
        return Collections.unmodifiableMap(relationships);
    }

    public void updateProfessionSkill(String activity) {
        professionSkills.merge(activity, 1, Integer::sum);
        if (professionSkills.get(activity) % 100 == 0) {
            adjustHappiness(5); // Happiness boost from mastering skills
        }
    }

    public double getSkillLevel(String activity) {
        return professionSkills.getOrDefault(activity, 0) / 100.0;
    }

    // Get displayable relationship status for chat/UI
    public String getRelationshipDisplay(VillagerEntity other) {
        RelationshipType type = relationships.get(other.getUuid());
        if (type == null) return "stranger";
        
        return switch(type) {
            case FRIEND -> "friend";
            case FAMILY -> "family";
            case RIVAL -> "rival";
            case NEUTRAL -> "neutral";
            case ENEMY -> "enemy";
        };
    }

    public void learnFromEvent(VillageEvent event, String reflection) {
        // Update behavior memory with event reflection
        behaviorMemory.put("last_event_" + event.getType(), event);
        behaviorMemory.put("last_event_reflection", reflection);

        // Adjust happiness based on event outcome
        if (event.getParticipants().contains(villager.getUuid())) {
            String outcome = event.getOutcome();
            if (outcome != null) {
                if (outcome.toLowerCase().contains("success") || 
                    outcome.toLowerCase().contains("happy") || 
                    outcome.toLowerCase().contains("positive")) {
                    adjustHappiness(5);
                } else if (outcome.toLowerCase().contains("fail") || 
                         outcome.toLowerCase().contains("sad") || 
                         outcome.toLowerCase().contains("negative")) {
                    adjustHappiness(-3);
                }
            }
        }

        // Update profession skills if event was work-related
        if (event.getType().toLowerCase().contains("work") || 
            event.getType().toLowerCase().contains("craft") || 
            event.getType().toLowerCase().contains("trade")) {
            updateProfessionSkill(event.getType());
        }

        // Update relationships based on event participants
        event.getParticipants().forEach(participantId -> {
            if (!participantId.equals(villager.getUuid()) && 
                !relationships.containsKey(participantId)) {
                determineRelationshipDynamically((VillagerEntity)
                    ((ServerWorld)villager.getWorld()).getEntity(participantId))
                    .thenAccept(type -> addRelationship(participantId, type));
            }
        });
    }

    private static void pruneExpiredCache() {
        behaviorCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        LOGGER.debug("Pruned expired entries from behavior cache, new size: {}", behaviorCache.size());
    }

    private static boolean removeOldestExpiredEntry() {
        Optional<Map.Entry<String, CachedBehavior>> oldestExpired = behaviorCache.entrySet().stream()
            .filter(e -> e.getValue().isExpired())
            .min((a, b) -> Long.compare(a.getValue().timestamp, b.getValue().timestamp));
            
        if (oldestExpired.isPresent()) {
            behaviorCache.remove(oldestExpired.get().getKey());
            LOGGER.debug("Removed oldest expired entry from behavior cache");
            return true;
        }
        return false;
    }


    public static void clearBehaviorCache() {
        behaviorCache.clear();
        LOGGER.debug("Cleared villager behavior cache");
    }

    public static int getBehaviorCacheSize() {
        return behaviorCache.size();
    }

    private record Activity(String type, BlockPos target, int duration, String detail, int priority) {
        public Activity {
            if (priority < 0) priority = 0;
            if (duration < 0) duration = 1200;
        }
        
        public Activity(String type, BlockPos target, int duration, String detail) {
            this(type, target, duration, detail, 1);
        }
        
        public boolean isHighPriority() {
            return priority > 2;
        }
    }
    
    private enum BehaviorState {
        IDLE,
        EXECUTING,
        INTERRUPTED,
        WAITING,
        PLANNING
    }

    public void updateSchedule(ServerWorld world) {
        long timeOfDay = world.getTimeOfDay() % 24000;
        
        // Handle interruptions and high-priority activities
        if (currentState == BehaviorState.EXECUTING && shouldInterrupt(world)) {
            currentState = BehaviorState.INTERRUPTED;
            handleInterruption(world);
            return;
        }

        // Process planned activities or generate new ones
        if (plannedActivities.isEmpty() ||
            (currentActivityDetail == null && currentState != BehaviorState.PLANNING)) {
            currentState = BehaviorState.PLANNING;
            
            Map<String, Object> context = new HashMap<>(behaviorMemory);
            context.put("time_of_day", timeOfDay);
            context.put("weather", world.isRaining() ? "raining" : "clear");
            context.put("location", villager.getBlockPos());
            context.put("profession", villager.getVillagerData().getProfession().toString());
            context.put("last_activity", currentActivity);
            context.put("happiness", happiness);
            context.put("current_goals", getCurrentGoals());
            
            String planningPrompt = String.format(
                "As a %s villager with %s personality, plan the next activities.\n" +
                "Current time: %d, Weather: %s\n" +
                "Goals: %s\n\n" +
                "Respond with 2-3 sequential activities in format:\n" +
                "ACTION: (activity type)\n" +
                "TARGET: (location)\n" +
                "DURATION: (ticks)\n" +
                "PRIORITY: (1-5)\n" +
                "---",
                villager.getVillagerData().getProfession(),
                personality,
                timeOfDay,
                world.isRaining() ? "raining" : "clear",
                String.join(", ", getCurrentGoals())
            );

            LLMService.getInstance()
                .generateResponse(planningPrompt)
                .thenApply(response -> {
                    String[] activities = response.split("---");
                    for (String activityStr : activities) {
                        if (!activityStr.trim().isEmpty()) {
                            Activity activity = parseActivity(activityStr);
                            if (activity != null) {
                                plannedActivities.offer(activity);
                            }
                        }
                    }
                    currentState = BehaviorState.IDLE;
                    return null;
                })
                .exceptionally(e -> {
                    LOGGER.error("Error planning activities", e);
                    currentState = BehaviorState.IDLE;
                    return null;
                });
        }

        // Execute or update current activity
        if (currentActivityDetail == null ||
            world.getTime() > lastActivityUpdate + currentActivityDetail.duration()) {
            
            Activity nextActivity = plannedActivities.poll();
            if (nextActivity != null) {
                updateActivity(nextActivity);
                lastActivityUpdate = world.getTime();
                currentState = BehaviorState.EXECUTING;
            }
        }
        
        if (currentActivityDetail != null && currentState == BehaviorState.EXECUTING) {
            executeActivity(world);
            updateBehaviorMemory(world);
        }
    }

    private boolean shouldInterrupt(ServerWorld world) {
        if (currentActivityDetail == null) return false;
        
        // Check for dangers
        List<Entity> nearbyHostiles = world.getEntitiesByClass(
            Entity.class,
            new Box(villager.getBlockPos()).expand(8),
            e -> e.isAttackable() && e.distanceTo(villager) < 5
        );
        if (!nearbyHostiles.isEmpty()) return true;

        // Check for important events
        VillagerManager vm = VillagerManager.getInstance();
        List<VillagerManager.CulturalEvent> events = vm.getCurrentEvents(villager.getBlockPos(), world);
        int currentPriority = currentActivityDetail.priority();
        // Consider ongoing cultural events as high priority
        if (currentPriority < 3 && !events.isEmpty()) {
            return true;
        }

        // Check for emergency needs (low happiness, etc)
        return happiness < 20 && currentActivityDetail.priority() < 3;
    }

    private void handleInterruption(ServerWorld world) {
        Activity currentActivity = currentActivityDetail;
        Map<String, Object> context = new HashMap<>(behaviorMemory);
        context.put("interrupted_activity", currentActivity);
        context.put("time", world.getTimeOfDay() % 24000);
        context.put("happiness", happiness);
        context.put("threats", getNearbyThreats(world));

        String prompt = String.format(
            "Emergency situation for %s villager!\n" +
            "Current activity: %s\n" +
            "Happiness: %d\n" +
            "Threats nearby: %s\n\n" +
            "Respond with immediate action in standard format.",
            villager.getVillagerData().getProfession(),
            currentActivity.detail(),
            happiness,
            context.get("threats")
        );

        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(response -> {
                Activity emergencyActivity = parseActivity(response);
                if (emergencyActivity != null) {
                    plannedActivities.clear();
                    updateActivity(emergencyActivity);
                    currentState = BehaviorState.EXECUTING;
                }
            });
    }

    private List<String> getNearbyThreats(ServerWorld world) {
        List<String> threats = new ArrayList<>();
        world.getEntitiesByClass(Entity.class,
            new Box(villager.getBlockPos()).expand(8),
            e -> e.isAttackable())
            .forEach(e -> threats.add(e.getType().toString()));
        return threats;
    }

    private void updateBehaviorMemory(ServerWorld world) {
        behaviorMemory.put("last_activity", currentActivityDetail);
        behaviorMemory.put("last_location", villager.getBlockPos());
        behaviorMemory.put("last_update", world.getTime());
        
        // Track activity success
        boolean successful = isCurrentActivitySuccessful(world);
        behaviorMemory.put("last_activity_successful", successful);
        
        // Update skill progress and track performance
        if (successful) {
            professionSkills.merge(currentActivityDetail.type(), 1, Integer::sum);
            int currentStreak = (Integer)behaviorMemory.getOrDefault("success_streak", 0);
            behaviorMemory.put("success_streak", currentStreak + 1);
            adjustHappiness(1); // Small happiness boost for successful activities
        } else {
            behaviorMemory.put("success_streak", 0);
            if (currentActivityDetail.priority() > 2) {
                adjustHappiness(-1); // Small penalty for failing important activities
            }
        }
        
        // Track location preferences
        String locationKey = "preferred_" + currentActivityDetail.type() + "_location";
        if (successful) {
            behaviorMemory.put(locationKey, currentActivityDetail.target());
        }
        
        // Update time-based patterns
        long timeOfDay = world.getTimeOfDay() % 24000;
        String timeKey = "success_time_" + (timeOfDay / 1000) * 1000;
        if (successful) {
            int currentCount = (Integer)behaviorMemory.getOrDefault(timeKey, 0);
            behaviorMemory.put(timeKey, currentCount + 1);
        }

        // Maintain memory size with priority retention
        while (behaviorMemory.size() > 100) {
            // Keep success records and location preferences
            String oldestKey = behaviorMemory.keySet().stream()
                .filter(k -> !k.startsWith("success_") && !k.startsWith("preferred_"))
                .findFirst()
                .orElse(behaviorMemory.keySet().iterator().next());
            behaviorMemory.remove(oldestKey);
        }
    }

    private boolean isCurrentActivitySuccessful(ServerWorld world) {
        if (currentActivityDetail == null) return false;
        
        String activityType = currentActivityDetail.type().toLowerCase();
        
        if ("walk".equals(activityType)) {
            double distance = villager.getBlockPos().getSquaredDistance(currentActivityDetail.target());
            return distance < 4.0; // Within 2 blocks
        }
        
        if ("work".equals(activityType)) {
            if (villager.getVillagerData().getProfession() == VillagerProfession.FARMER) {
                return isFarmingSuccessful(world);
            }
            BlockPos workstation = findNearestWorkstation(
                villager.getVillagerData().getProfession().toString()
            );
            return villager.getBlockPos().isWithinDistance(workstation, 3);
        }
        
        if ("trade".equals(activityType)) {
            return hasActiveTrader();
        }
        
        if ("rest".equals(activityType)) {
            boolean isNight = world.getTimeOfDay() % 24000 > 12000;
            boolean needsRest = happiness < 50;
            return (isNight || needsRest) && villager.getBlockPos().isWithinDistance(homePos, 5);
        }
        
        if ("socialize".equals(activityType)) {
            List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                new Box(villager.getBlockPos()).expand(8),
                otherVillager -> otherVillager != villager && relationships.containsKey(otherVillager.getUuid())
            );
            return !nearbyVillagers.isEmpty();
        }
        
        return true;
    }
    
    private boolean hasActiveTrader() {
        return villager.getVillagerData().getProfession() != VillagerProfession.NONE &&
               !villager.getOffers().isEmpty();
    }

    private boolean isFarmingSuccessful(ServerWorld world) {
        BlockPos pos = villager.getBlockPos();
        int farmlandCount = 0;
        int matureCropCount = 0;

        // Check surrounding blocks for farmland and crops
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = pos.add(x, -1, z);
                Block block = world.getBlockState(checkPos).getBlock();
                if (block == Blocks.FARMLAND) {
                    farmlandCount++;
                    BlockState above = world.getBlockState(checkPos.up());
                    if (above.getBlock() instanceof CropBlock crop &&
                        crop.isMature(above)) {
                        matureCropCount++;
                    }
                }
            }
        }

        // Success if near farmland and either tending crops or harvesting
        return farmlandCount > 0 && (matureCropCount > 0 || farmlandCount > 5);
    }

    private Activity parseActivity(String response) {
        try {
            Map<String, String> parts = Arrays.stream(response.split("\n"))
                .map(inputLine -> inputLine.split(": ", 2))
                .filter(splitParts -> splitParts.length == 2)
                .collect(Collectors.toMap(
                    splitParts -> splitParts[0].trim(),
                    splitParts -> splitParts[1].trim(),
                    (existing, replacement) -> replacement
                ));

            String type = parts.getOrDefault("ACTION", "walk");
            String targetStr = parts.get("TARGET");
            BlockPos target;
            
            if (targetStr == null || targetStr.isEmpty()) {
                target = villager.getBlockPos();
            } else if (targetStr.equalsIgnoreCase("home")) {
                target = homePos;
            } else if (targetStr.equalsIgnoreCase("work")) {
                target = findNearestWorkstation(villager.getVillagerData().getProfession().toString());
            } else if (targetStr.equalsIgnoreCase("village")) {
                target = findVillageCenter();
            } else {
                // Try to parse coordinates if provided
                String[] coords = targetStr.split("\\s+");
                if (coords.length == 3) {
                    try {
                        int x = Integer.parseInt(coords[0]);
                        int y = Integer.parseInt(coords[1]);
                        int z = Integer.parseInt(coords[2]);
                        target = new BlockPos(x, y, z);
                    } catch (NumberFormatException e) {
                        target = villager.getBlockPos();
                    }
                } else {
                    target = villager.getBlockPos();
                }
            }
            
            int duration = Integer.parseInt(parts.getOrDefault("DURATION", "1200"));
            String detail = parts.getOrDefault("DETAIL", type);
            int priority = Integer.parseInt(parts.getOrDefault("PRIORITY", "1"));
            
            return new Activity(type, target, duration, detail, priority);
        } catch (Exception e) {
            LOGGER.error("Error parsing activity: {}", e.getMessage());
            return new Activity("idle", villager.getBlockPos(), 1200, "default activity", 1);
        }
    }

    private List<String> getCurrentGoals() {
        List<String> goals = new ArrayList<>();
        
        // Add goals based on current state
        if (happiness < 50) {
            goals.add("improve happiness");
        }
        
        // Add profession-based goals
        String profession = villager.getVillagerData().getProfession().toString().toLowerCase();
        switch (profession) {
            case "farmer":
                goals.add("tend crops");
                goals.add("harvest mature plants");
                break;
            case "librarian":
                goals.add("organize books");
                goals.add("read and learn");
                break;
            case "blacksmith":
                goals.add("forge new tools");
                goals.add("repair damaged equipment");
                break;
            case "cleric":
                goals.add("brew potions");
                goals.add("gather ingredients");
                break;
            default:
                goals.add("work at profession station");
        }
        
        // Time-based goals
        if (villager.getWorld().getTimeOfDay() % 24000 > 12000) {
            goals.add("rest for the night");
        } else {
            goals.add("be productive during day");
        }
        
        // Social goals if relationships exist
        if (!relationships.isEmpty()) {
            goals.add("socialize with friends");
        }
        
        return goals;
    }

    private void executeActivity(ServerWorld world) {
        if (currentActivityDetail == null) return;
        
        String activityType = currentActivityDetail.type().toLowerCase();
        
        switch (activityType) {
            case "walk":
                moveToTarget(currentActivityDetail.target());
                break;
            case "work":
                handleWorking(world);
                break;
            case "trade":
                handleTrading();
                break;
            case "rest":
                handleResting(world);
                break;
            case "socialize":
                handleSocializing(world);
                break;
            case "hide":
                // Find safe location and move there
                BlockPos safeSpot = findSafeLocation(world);
                moveToTarget(safeSpot);
                break;
            default:
                // Default behavior - move towards target location
                moveToTarget(currentActivityDetail.target());
        }
    }
    
    private BlockPos findSafeLocation(ServerWorld world) {
        // Find nearby safe location (indoors or away from threats)
        BlockPos pos = villager.getBlockPos();
        for (int y = 0; y <= 2; y++) {
            for (int r = 1; r <= 10; r++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r) continue; // Only check the perimeter
                        
                        BlockPos checkPos = pos.add(x, y, z);
                        if (isSafeLocation(world, checkPos)) {
                            return checkPos;
                        }
                    }
                }
            }
        }
        return homePos; // Default to home if no safe spot found
    }
    
    private boolean isSafeLocation(ServerWorld world, BlockPos pos) {
        // Check if position is safe (has roof overhead, no monsters nearby)
        boolean hasRoof = false;
        for (int y = 1; y <= 4; y++) {
            if (!world.getBlockState(pos.up(y)).isAir()) {
                hasRoof = true;
                break;
            }
        }
        
        if (!hasRoof) return false;
        
        // Check for nearby threats
        List<Monster> threats = world.getEntitiesByClass(
            Monster.class,
            new Box(pos).expand(8),
            e -> true
        );
        
        return threats.isEmpty();
    }
    
    private void moveToTarget(BlockPos target) {
        if (target == null) return;
        
        // Only navigate if not already at target
        if (villager.getBlockPos().getSquaredDistance(target) > 2) {
            villager.getNavigation().startMovingTo(
                target.getX(), target.getY(), target.getZ(), 0.5D);
        }
    }

    private BlockPos findNearestStructure(String structureType) {
        // Simple implementation - return home position for now
        return homePos;
    }
    
    private void handleWorking(ServerWorld world) {
        String profession = villager.getVillagerData().getProfession().toString().toLowerCase();
        BlockPos workstation = findNearestWorkstation(profession);
        
        moveToTarget(workstation);
        
        // Work animations and particles
        if (villager.getBlockPos().isWithinDistance(workstation, 2)) {
            villager.swingHand(Hand.MAIN_HAND);
            world.spawnParticles(
                ParticleTypes.CRIT,
                workstation.getX(), workstation.getY() + 1, workstation.getZ(),
                3, 0.2, 0.2, 0.2, 0.01
            );
        }
    }

    private void handleTrading() {
        // Check for nearby players to trade with
        List<PlayerEntity> nearbyPlayers = villager.getWorld().getEntitiesByClass(
            PlayerEntity.class,
            new Box(villager.getBlockPos()).expand(5),
            p -> true
        );
        
        if (!nearbyPlayers.isEmpty()) {
            PlayerEntity player = nearbyPlayers.get(0);
            villager.getLookControl().lookAt(player, 30.0F, 30.0F);
            
            // Generate trade offers if none exist
            if (villager.getOffers().isEmpty()) {
                generateTrades();
            }
        }
    }
    
    private void generateTrades() {
        // Simple method to ensure villager has some basic trades
        if (villager.getOffers() == null || villager.getOffers().isEmpty()) {
            TradeOfferList offers = new TradeOfferList();
            // Add fallback trades based on profession
            villager.setOffers(offers);
        }
    }

    private void handleResting(ServerWorld world) {
        if (world.getTimeOfDay() % 24000 > 12000) {
            BlockPos bed = findNearestStructure("house");
            moveToTarget(bed);
        }
    }

    private void handleSocializing(ServerWorld world) {
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            new Box(villager.getBlockPos()).expand(8),
            v -> v != villager
        );
        
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity target = nearbyVillagers.get(0);
            moveToTarget(target.getBlockPos());
            world.spawnParticles(
                ParticleTypes.HEART,
                villager.getX(), villager.getY() + 2, villager.getZ(),
                1, 0.5, 0.5, 0.5, 0.1
            );
        }
    }
}
