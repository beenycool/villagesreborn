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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Objects;

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
}
