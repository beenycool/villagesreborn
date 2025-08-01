package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.ServerVillagerManager;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.*;

public class VillagerScheduleManager {
    
    public enum TimeOfDay {
        DAWN(0, 1000, "Dawn"),
        MORNING(1000, 6000, "Morning"),
        NOON(6000, 7000, "Noon"),
        AFTERNOON(7000, 11000, "Afternoon"),
        DUSK(11000, 13000, "Dusk"),
        NIGHT(13000, 23000, "Night"),
        MIDNIGHT(23000, 24000, "Midnight");
        
        public final long startTime;
        public final long endTime;
        public final String name;
        
        TimeOfDay(long startTime, long endTime, String name) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.name = name;
        }
        
        public static TimeOfDay fromWorldTime(long worldTime) {
            long dayTime = worldTime % 24000;
            for (TimeOfDay time : values()) {
                if (dayTime >= time.startTime && dayTime < time.endTime) {
                    return time;
                }
            }
            return DAWN;
        }
    }
    
    public static class Schedule {
        private final Map<TimeOfDay, Activity> activities = new EnumMap<>(TimeOfDay.class);
        
        public Schedule() {
            
            activities.put(TimeOfDay.DAWN, Activity.WAKE_UP);
            activities.put(TimeOfDay.MORNING, Activity.WORK);
            activities.put(TimeOfDay.NOON, Activity.SOCIALIZE);
            activities.put(TimeOfDay.AFTERNOON, Activity.WORK);
            activities.put(TimeOfDay.DUSK, Activity.RELAX);
            activities.put(TimeOfDay.NIGHT, Activity.SLEEP);
            activities.put(TimeOfDay.MIDNIGHT, Activity.SLEEP);
        }
        
        public Activity getActivity(TimeOfDay time) {
            return activities.getOrDefault(time, Activity.WANDER);
        }
        
        public void setActivity(TimeOfDay time, Activity activity) {
            activities.put(time, activity);
        }
    }
    
    public enum Activity {
        WAKE_UP("Waking up", 0xFF87CEEB),
        WORK("Working", 0xFFFFD700),
        SOCIALIZE("Socializing", 0xFFFF69B4),
        EAT("Eating", 0xFF90EE90),
        RELAX("Relaxing", 0xFFDDA0DD),
        HOBBY("Enjoying hobby", 0xFF00CED1),
        SLEEP("Sleeping", 0xFF4B0082),
        WANDER("Wandering", 0xFFD3D3D3),
        PRAY("Praying", 0xFFFFFFE0),
        STUDY("Studying", 0xFF8B4513),
        EXERCISE("Exercising", 0xFF32CD32),
        SHOP("Shopping", 0xFFFFA500);
        
        public final String description;
        public final int color;
        
        Activity(String description, int color) {
            this.description = description;
            this.color = color;
        }
    }
    
    private static final Map<String, Schedule> PROFESSION_SCHEDULES = new HashMap<>();
    
    static {
        
        initializeProfessionSchedules();
    }
    
    private static void initializeProfessionSchedules() {
        
        Schedule farmerSchedule = new Schedule();
        farmerSchedule.setActivity(TimeOfDay.DAWN, Activity.WAKE_UP);
        farmerSchedule.setActivity(TimeOfDay.MORNING, Activity.WORK);
        farmerSchedule.setActivity(TimeOfDay.NOON, Activity.EAT);
        farmerSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.WORK);
        farmerSchedule.setActivity(TimeOfDay.DUSK, Activity.RELAX);
        farmerSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:farmer", farmerSchedule);
        
        
        Schedule librarianSchedule = new Schedule();
        librarianSchedule.setActivity(TimeOfDay.DAWN, Activity.WAKE_UP);
        librarianSchedule.setActivity(TimeOfDay.MORNING, Activity.STUDY);
        librarianSchedule.setActivity(TimeOfDay.NOON, Activity.WORK);
        librarianSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.STUDY);
        librarianSchedule.setActivity(TimeOfDay.DUSK, Activity.SOCIALIZE);
        librarianSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:librarian", librarianSchedule);
        
        
        Schedule clericSchedule = new Schedule();
        clericSchedule.setActivity(TimeOfDay.DAWN, Activity.PRAY);
        clericSchedule.setActivity(TimeOfDay.MORNING, Activity.WORK);
        clericSchedule.setActivity(TimeOfDay.NOON, Activity.PRAY);
        clericSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.SOCIALIZE);
        clericSchedule.setActivity(TimeOfDay.DUSK, Activity.PRAY);
        clericSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:cleric", clericSchedule);
        
        
        Schedule blacksmithSchedule = new Schedule();
        blacksmithSchedule.setActivity(TimeOfDay.DAWN, Activity.WAKE_UP);
        blacksmithSchedule.setActivity(TimeOfDay.MORNING, Activity.WORK);
        blacksmithSchedule.setActivity(TimeOfDay.NOON, Activity.EAT);
        blacksmithSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.WORK);
        blacksmithSchedule.setActivity(TimeOfDay.DUSK, Activity.EXERCISE);
        blacksmithSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:armorer", blacksmithSchedule);
        PROFESSION_SCHEDULES.put("minecraft:weaponsmith", blacksmithSchedule);
        PROFESSION_SCHEDULES.put("minecraft:toolsmith", blacksmithSchedule);
        
        
        Schedule nitwitSchedule = new Schedule();
        nitwitSchedule.setActivity(TimeOfDay.DAWN, Activity.SLEEP);
        nitwitSchedule.setActivity(TimeOfDay.MORNING, Activity.WAKE_UP);
        nitwitSchedule.setActivity(TimeOfDay.NOON, Activity.EAT);
        nitwitSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.WANDER);
        nitwitSchedule.setActivity(TimeOfDay.DUSK, Activity.SOCIALIZE);
        nitwitSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:nitwit", nitwitSchedule);
    }
    
    public static Activity getCurrentActivity(VillagerEntity villager) {
        long worldTime = villager.getWorld().getTimeOfDay();
        TimeOfDay timeOfDay = TimeOfDay.fromWorldTime(worldTime);
        
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            return Activity.WANDER;
        }
        
        // Get individual time preferences based on personality and villager ID
        int personalityOffset = getPersonalityTimeOffset(data);
        int individualOffset = Math.abs(villager.getUuid().hashCode()) % 4 - 2; // -2 to +1 hours variation
        long adjustedTime = (worldTime + (personalityOffset + individualOffset) * 1000) % 24000;
        TimeOfDay adjustedTimeOfDay = TimeOfDay.fromWorldTime(adjustedTime);
        
        // Get base activity probabilities for the time period
        Map<Activity, Float> activityWeights = getActivityWeights(adjustedTimeOfDay, data, villager);
        
        // Select activity based on weighted probability
        return selectWeightedActivity(activityWeights, villager);
    }
    
    private static int getPersonalityTimeOffset(VillagerData data) {
        return switch (data.getPersonality()) {
            case "Energetic" -> -2; // Early riser
            case "Lazy" -> 3; // Late sleeper
            case "Serious" -> -1; // Slightly early
            case "Cheerful" -> -1; // Morning person
            case "Grumpy" -> 2; // Not a morning person
            case "Nervous" -> 1; // Irregular schedule
            default -> 0; // Normal schedule
        };
    }
    
    private static Map<Activity, Float> getActivityWeights(TimeOfDay timeOfDay, VillagerData data, VillagerEntity villager) {
        Map<Activity, Float> weights = new EnumMap<>(Activity.class);
        
        // Base weights for time of day
        switch (timeOfDay) {
            case DAWN -> {
                weights.put(Activity.WAKE_UP, 0.3f);
                weights.put(Activity.SLEEP, 0.4f);
                weights.put(Activity.PRAY, 0.1f);
                weights.put(Activity.WANDER, 0.2f);
            }
            case MORNING -> {
                weights.put(Activity.WORK, 0.4f);
                weights.put(Activity.EAT, 0.2f);
                weights.put(Activity.SOCIALIZE, 0.1f);
                weights.put(Activity.EXERCISE, 0.1f);
                weights.put(Activity.STUDY, 0.1f);
                weights.put(Activity.WANDER, 0.1f);
            }
            case NOON -> {
                weights.put(Activity.EAT, 0.35f);
                weights.put(Activity.WORK, 0.25f);
                weights.put(Activity.SOCIALIZE, 0.2f);
                weights.put(Activity.RELAX, 0.1f);
                weights.put(Activity.SHOP, 0.1f);
            }
            case AFTERNOON -> {
                weights.put(Activity.WORK, 0.35f);
                weights.put(Activity.SOCIALIZE, 0.2f);
                weights.put(Activity.HOBBY, 0.15f);
                weights.put(Activity.STUDY, 0.1f);
                weights.put(Activity.SHOP, 0.1f);
                weights.put(Activity.WANDER, 0.1f);
            }
            case DUSK -> {
                weights.put(Activity.RELAX, 0.3f);
                weights.put(Activity.SOCIALIZE, 0.25f);
                weights.put(Activity.EAT, 0.2f);
                weights.put(Activity.HOBBY, 0.15f);
                weights.put(Activity.WANDER, 0.1f);
            }
            case NIGHT -> {
                weights.put(Activity.SLEEP, 0.6f);
                weights.put(Activity.RELAX, 0.15f);
                weights.put(Activity.SOCIALIZE, 0.1f);
                weights.put(Activity.STUDY, 0.1f);
                weights.put(Activity.WANDER, 0.05f);
            }
            case MIDNIGHT -> {
                weights.put(Activity.SLEEP, 0.85f);
                weights.put(Activity.WANDER, 0.1f);
                weights.put(Activity.STUDY, 0.05f);
            }
        }
        
        // Apply personality modifiers
        applyPersonalityModifiers(weights, data);
        
        // Apply profession modifiers
        applyProfessionModifiers(weights, villager);
        
        // Apply contextual modifiers
        applyContextualModifiers(weights, data, villager);
        
        return weights;
    }
    
    private static void applyPersonalityModifiers(Map<Activity, Float> weights, VillagerData data) {
        switch (data.getPersonality()) {
            case "Energetic" -> {
                weights.replaceAll((k, v) -> k == Activity.EXERCISE || k == Activity.WORK ? v * 1.5f : v);
                weights.replaceAll((k, v) -> k == Activity.SLEEP || k == Activity.RELAX ? v * 0.7f : v);
            }
            case "Lazy" -> {
                weights.replaceAll((k, v) -> k == Activity.RELAX || k == Activity.SLEEP ? v * 1.4f : v);
                weights.replaceAll((k, v) -> k == Activity.WORK || k == Activity.EXERCISE ? v * 0.6f : v);
            }
            case "Friendly" -> {
                weights.replaceAll((k, v) -> k == Activity.SOCIALIZE ? v * 1.6f : v);
            }
            case "Shy" -> {
                weights.replaceAll((k, v) -> k == Activity.SOCIALIZE ? v * 0.5f : v);
                weights.replaceAll((k, v) -> k == Activity.STUDY || k == Activity.HOBBY ? v * 1.3f : v);
            }
            case "Curious" -> {
                weights.replaceAll((k, v) -> k == Activity.STUDY || k == Activity.WANDER ? v * 1.4f : v);
            }
            case "Serious" -> {
                weights.replaceAll((k, v) -> k == Activity.WORK || k == Activity.STUDY ? v * 1.3f : v);
                weights.replaceAll((k, v) -> k == Activity.SOCIALIZE || k == Activity.HOBBY ? v * 0.8f : v);
            }
            case "Cheerful" -> {
                weights.replaceAll((k, v) -> k == Activity.SOCIALIZE || k == Activity.HOBBY ? v * 1.3f : v);
            }
            case "Grumpy" -> {
                weights.replaceAll((k, v) -> k == Activity.SOCIALIZE ? v * 0.6f : v);
                weights.replaceAll((k, v) -> k == Activity.WANDER ? v * 1.4f : v);
            }
        }
    }
    
    private static void applyProfessionModifiers(Map<Activity, Float> weights, VillagerEntity villager) {
        String professionKey = villager.getVillagerData().profession().getKey()
            .map(key -> key.getValue().toString()).orElse("minecraft:none");
            
        switch (professionKey) {
            case "minecraft:librarian" -> {
                weights.replaceAll((k, v) -> k == Activity.STUDY ? v * 2.0f : v);
            }
            case "minecraft:cleric" -> {
                weights.replaceAll((k, v) -> k == Activity.PRAY ? v * 3.0f : v);
            }
            case "minecraft:farmer" -> {
                weights.replaceAll((k, v) -> k == Activity.WORK ? v * 1.5f : v);
            }
            case "minecraft:nitwit" -> {
                weights.replaceAll((k, v) -> k == Activity.WANDER || k == Activity.RELAX ? v * 1.8f : v);
                weights.replaceAll((k, v) -> k == Activity.WORK ? v * 0.3f : v);
            }
        }
    }
    
    private static void applyContextualModifiers(Map<Activity, Float> weights, VillagerData data, VillagerEntity villager) {
        // Happiness affects activity preferences
        if (data.getHappiness() < 20) {
            weights.replaceAll((k, v) -> k == Activity.WANDER ? v * 2.0f : v);
            weights.replaceAll((k, v) -> k == Activity.WORK || k == Activity.SOCIALIZE ? v * 0.5f : v);
        } else if (data.getHappiness() > 80) {
            weights.replaceAll((k, v) -> k == Activity.SOCIALIZE || k == Activity.HOBBY ? v * 1.4f : v);
        }
        
        // Age affects activities
        if (data.getAge() < 20) { // Young villagers
            weights.replaceAll((k, v) -> k == Activity.SOCIALIZE || k == Activity.WANDER ? v * 1.5f : v);
            weights.replaceAll((k, v) -> k == Activity.WORK ? v * 0.3f : v);
        } else if (data.getAge() > 300) { // Elder villagers
            weights.replaceAll((k, v) -> k == Activity.RELAX || k == Activity.STUDY ? v * 1.3f : v);
            weights.replaceAll((k, v) -> k == Activity.EXERCISE ? v * 0.4f : v);
        }
        
        // Special day modifier
        if (isSpecialDay(data, villager.getWorld().getTimeOfDay())) {
            weights.replaceAll((k, v) -> k == Activity.SOCIALIZE ? v * 3.0f : v);
        }
    }
    
    private static Activity selectWeightedActivity(Map<Activity, Float> weights, VillagerEntity villager) {
        float totalWeight = weights.values().stream().reduce(0f, Float::sum);
        if (totalWeight <= 0) return Activity.WANDER;
        
        net.minecraft.util.math.random.Random random = villager.getWorld().getRandom();
        float randomValue = random.nextFloat() * totalWeight;
        
        float currentWeight = 0;
        for (Map.Entry<Activity, Float> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        return Activity.WANDER;
    }
    
    public static void updateSchedules(ServerWorld world) {
        // Use ServerVillagerManager instead of scanning the entire world
        for (VillagerEntity villager : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
            // Only update villagers in the same world
            if (villager.getWorld() != world) continue;
            
            Activity currentActivity = getCurrentActivity(villager);
            updateVillagerBehavior(villager, currentActivity);
            
            
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                if (currentActivity != Activity.WAKE_UP) {
                    Text activityText = Text.literal(" [" + currentActivity.description + "]")
                        .formatted(getActivityFormatting(currentActivity));
                    Text fullName = Text.literal(data.getName()).append(activityText);
                    villager.setCustomName(fullName);
                } else {
                    villager.setCustomName(Text.literal(data.getName()));
                }
            }
        }
    }
    
    private static void updateVillagerBehavior(VillagerEntity villager, Activity activity) {
        
        
        
        switch (activity) {
            case SLEEP -> {
                // Villager should go to bed - note: sleep behavior handled by Minecraft AI
            }
            case WORK -> {
                
            }
            case SOCIALIZE -> {
                
            }
            case EAT -> {
                
            }
            default -> {
                
            }
        }
    }
    
    private static boolean isSpecialDay(VillagerData data, long worldTime) {
        
        long daysSinceBirth = (worldTime - data.getBirthTime()) / 24000;
        return daysSinceBirth > 0 && daysSinceBirth % 365 == 0;
    }
    
    private static Formatting getActivityFormatting(Activity activity) {
        return switch (activity) {
            case WAKE_UP -> Formatting.AQUA;
            case WORK -> Formatting.GOLD;
            case SOCIALIZE -> Formatting.LIGHT_PURPLE;
            case EAT -> Formatting.GREEN;
            case RELAX -> Formatting.BLUE;
            case HOBBY -> Formatting.DARK_AQUA;
            case SLEEP -> Formatting.DARK_PURPLE;
            case PRAY -> Formatting.YELLOW;
            case STUDY -> Formatting.DARK_GREEN;
            case EXERCISE -> Formatting.GREEN;
            case SHOP -> Formatting.GOLD;
            default -> Formatting.GRAY;
        };
    }
    
    public static List<Text> getScheduleInfo(VillagerEntity villager) {
        List<Text> info = new ArrayList<>();
        
        String professionKey = villager.getVillagerData().profession().getKey()
            .map(key -> key.getValue().toString()).orElse("minecraft:none");
        Schedule schedule = PROFESSION_SCHEDULES.getOrDefault(professionKey, new Schedule());
        
        info.add(Text.literal("=== Daily Schedule ===").formatted(Formatting.GOLD));
        
        for (TimeOfDay time : TimeOfDay.values()) {
            Activity activity = schedule.getActivity(time);
            info.add(Text.literal(time.name + ": ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(activity.description)
                    .formatted(getActivityFormatting(activity))));
        }
        
        
        Activity current = getCurrentActivity(villager);
        info.add(Text.empty());
        info.add(Text.literal("Currently: ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(current.description)
                .formatted(getActivityFormatting(current))));
        
        return info;
    }
    
    
    public static class ScheduleGoal extends Goal {
        private final VillagerEntity villager;
        private Activity lastActivity;
        
        public ScheduleGoal(VillagerEntity villager) {
            this.villager = villager;
            this.lastActivity = Activity.WANDER;
        }
        
        @Override
        public boolean canStart() {
            return true;
        }
        
        @Override
        public boolean shouldContinue() {
            return true;
        }
        
        @Override
        public void tick() {
            Activity currentActivity = getCurrentActivity(villager);
            if (currentActivity != lastActivity) {
                updateVillagerBehavior(villager, currentActivity);
                lastActivity = currentActivity;
            }
        }
    }
}