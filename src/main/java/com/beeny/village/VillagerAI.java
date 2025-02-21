package com.beeny.village;

import com.beeny.ai.LLMService;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
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
import java.util.stream.Collectors;

import net.minecraft.entity.Entity;
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
    private static final Map<String, String> behaviorCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

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
        RIVAL(-10);

        public final int moodModifier;

        RelationshipType(int moodModifier) {
            this.moodModifier = moodModifier;
        }
    }

    public VillagerAI(VillagerEntity villager, String personality) {
        this.villager = villager;
        this.personality = personality;
        this.homePos = villager.getBlockPos();
        this.currentActivity = "idle";
        this.schedule = createSchedule(villager.getVillagerData().getProfession().toString());
        this.happiness = 50; // Default happiness level
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
        String cacheKey = String.format("%s|%s|%d|%s|%s", 
            personality,
            situation,
            happiness,
            currentActivity,
            getCurrentCulturalContext()
        );
        
        String cachedBehavior = behaviorCache.get(cacheKey);
        if (cachedBehavior != null) {
            LOGGER.debug("Using cached behavior for {}: {}", villager.getName().getString(), cachedBehavior);
            return CompletableFuture.completedFuture(cachedBehavior);
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

                if (behaviorCache.size() < MAX_CACHE_SIZE) {
                    behaviorCache.put(cacheKey, behavior);
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
        
        StringBuilder context = new StringBuilder(region.getCulture());
        
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
                    behaviorCache.put(cacheKey, dialogue);
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
        };
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
            int duration = Integer.parseInt(parts.getOrDefault("DURATION", "2400"));
            String detail = parts.getOrDefault("DETAIL", type);
            int priority = Integer.parseInt(parts.getOrDefault("PRIORITY", "1"));

            // Adjust priority based on context
            if (type.equalsIgnoreCase("rest") && happiness < 30) priority += 2;
            if (type.equalsIgnoreCase("work") && professionSkills.getOrDefault(type, 0) < 50) priority += 1;
            if (type.equalsIgnoreCase("socialize") && relationships.size() < 3) priority += 1;

            // Validate and adjust duration
            duration = Math.max(1200, Math.min(duration, 12000));

            // Find appropriate target location
            BlockPos target = targetStr != null ? findActivityTarget(targetStr) : villager.getBlockPos();

            return new Activity(type, target, duration, detail, priority);
        } catch (Exception e) {
            LOGGER.error("Error parsing activity from response: {}", response, e);
            return new Activity("idle", villager.getBlockPos(), 1200, "error recovery", 1);
        }
    }
    
    private List<String> getCurrentGoals() {
        List<String> goals = new ArrayList<>();
        String profession = villager.getVillagerData().getProfession().toString();
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        ServerWorld world = (ServerWorld) villager.getWorld();
        long timeOfDay = world.getTimeOfDay() % 24000;
        boolean isNight = timeOfDay > 12000;

        // Priority 1: Emergency needs
        if (happiness < 30) {
            goals.add("Find ways to improve mood quickly");
            goals.add("Seek social interaction for comfort");
        }

        // Priority 2: Basic needs
        if (isNight && !villager.getBlockPos().isWithinDistance(homePos, 20)) {
            goals.add("Return home for rest");
        }

        // Priority 3: Professional goals
        int skillLevel = professionSkills.getOrDefault(profession.toLowerCase(), 0);
        if (skillLevel < 100) {
            goals.add(String.format("Improve %s skills (current: %d%%)", profession, skillLevel));
        }

        // Priority 4: Social goals
        int friendCount = (int) relationships.values().stream()
            .filter(relation -> relation == RelationshipType.FRIEND).count();
        if (friendCount < 3) {
            goals.add(String.format("Make more friends (current: %d/3)", friendCount));
        }

        // Priority 5: Cultural participation
        if (region != null) {
            List<VillagerManager.CulturalEvent> events = vm.getCurrentEvents(villager.getBlockPos(), world);
            if (!events.isEmpty()) {
                goals.add("Participate in " + events.get(0).name);
            } else {
                goals.add("Maintain " + region.getCulture() + " traditions");
            }
        }

        // Priority 6: Location-based goals
        Object successLocation = behaviorMemory.get("preferred_" + profession.toLowerCase() + "_location");
        if (successLocation != null) {
            goals.add("Work at preferred location");
        }

        // Priority 7: Time-based goals
        String timeKey = "success_time_" + (timeOfDay / 1000) * 1000;
        Integer successCount = (Integer) behaviorMemory.get(timeKey);
        if (successCount != null && successCount > 5) {
            goals.add("Maintain successful time schedule");
        }

        // Priority 8: Relationship maintenance
        relationships.forEach((id, relationshipType) -> {
            if (relationshipType == RelationshipType.FAMILY) {
                goals.add("Spend time with family");
            }
        });

        // Sort goals by embedded priority (implicit in the order of addition)
        return goals.stream()
            .limit(5) // Keep the top 5 most important goals
            .collect(Collectors.toList());
    }

    private BlockPos findActivityTarget(String target) {
        if (target == null) return villager.getBlockPos();
        
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        
        return switch(target.toLowerCase()) {
            case "village_center" -> region.getCenter();
            case "nearest_farm" -> findNearestWorkstation(Blocks.COMPOSTER);
            case "nearest_house" -> findNearestStructure("house");
            default -> villager.getBlockPos();
        };
    }

    private void executeActivity(ServerWorld world) {
        switch(currentActivityDetail.type().toLowerCase()) {
            case "walk" -> moveToTarget(currentActivityDetail.target());
            case "work" -> performWork(world);
            case "trade" -> handleTrading();
            case "rest" -> handleResting(world);
            case "socialize" -> handleSocializing(world);
        }
    }

    private BlockPos findNearestWorkstation(Block block) {
        int searchRadius = 32;
        BlockPos pos = villager.getBlockPos();
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (villager.getWorld().getBlockState(checkPos).getBlock() == block) {
                        return checkPos;
                    }
                }
            }
        }
        return pos;
    }

    private BlockPos findNearestStructure(String type) {
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        if (region == null) return villager.getBlockPos();
        
        return region.getCulturalStructures().entrySet().stream()
            .filter(structMapping -> structMapping.getKey().toLowerCase().contains(type.toLowerCase()))
            .map(Map.Entry::getValue)
            .min(Comparator.comparingDouble(candidatePos -> candidatePos.getSquaredDistance(villager.getBlockPos())))
            .orElse(villager.getBlockPos());
    }

    private void moveToTarget(BlockPos target) {
        if (villager.getNavigation().isIdle()) {
            villager.getNavigation().startMovingTo(
                target.getX(), target.getY(), target.getZ(), 0.6);
        }
    }

    private void performWork(ServerWorld world) {
        if (villager.getVillagerData().getProfession() == VillagerProfession.FARMER) {
            handleFarming(world);
        }
        // Add other profession-specific work
    }

    private void handleFarming(ServerWorld world) {
        BlockPos pos = villager.getBlockPos();
        Block block = world.getBlockState(pos.down()).getBlock();
        
        if (block instanceof CropBlock crop) {
            if (crop.isMature(world.getBlockState(pos.down()))) {
                world.breakBlock(pos.down(), true);
                world.setBlockState(pos.down(), crop.getDefaultState());
            }
        }
    }

    private void handleTrading() {
        // Existing trading logic
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
