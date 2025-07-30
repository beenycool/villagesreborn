package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
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
        
        final long startTime;
        final long endTime;
        final String name;
        
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
            // Default schedule
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
        // Initialize profession-specific schedules
        initializeProfessionSchedules();
    }
    
    private static void initializeProfessionSchedules() {
        // Farmer schedule
        Schedule farmerSchedule = new Schedule();
        farmerSchedule.setActivity(TimeOfDay.DAWN, Activity.WAKE_UP);
        farmerSchedule.setActivity(TimeOfDay.MORNING, Activity.WORK);
        farmerSchedule.setActivity(TimeOfDay.NOON, Activity.EAT);
        farmerSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.WORK);
        farmerSchedule.setActivity(TimeOfDay.DUSK, Activity.RELAX);
        farmerSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:farmer", farmerSchedule);
        
        // Librarian schedule
        Schedule librarianSchedule = new Schedule();
        librarianSchedule.setActivity(TimeOfDay.DAWN, Activity.WAKE_UP);
        librarianSchedule.setActivity(TimeOfDay.MORNING, Activity.STUDY);
        librarianSchedule.setActivity(TimeOfDay.NOON, Activity.WORK);
        librarianSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.STUDY);
        librarianSchedule.setActivity(TimeOfDay.DUSK, Activity.SOCIALIZE);
        librarianSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:librarian", librarianSchedule);
        
        // Cleric schedule
        Schedule clericSchedule = new Schedule();
        clericSchedule.setActivity(TimeOfDay.DAWN, Activity.PRAY);
        clericSchedule.setActivity(TimeOfDay.MORNING, Activity.WORK);
        clericSchedule.setActivity(TimeOfDay.NOON, Activity.PRAY);
        clericSchedule.setActivity(TimeOfDay.AFTERNOON, Activity.SOCIALIZE);
        clericSchedule.setActivity(TimeOfDay.DUSK, Activity.PRAY);
        clericSchedule.setActivity(TimeOfDay.NIGHT, Activity.SLEEP);
        PROFESSION_SCHEDULES.put("minecraft:cleric", clericSchedule);
        
        // Blacksmith schedule
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
        
        // Nitwit schedule (lazy)
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
        
        String professionKey = villager.getVillagerData().profession().getKey()
            .map(key -> key.getValue().toString()).orElse("minecraft:none");
        
        Schedule schedule = PROFESSION_SCHEDULES.getOrDefault(professionKey, new Schedule());
        
        // Special cases based on villager state
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            // If very unhappy, might skip work
            if (data.getHappiness() < 20 && schedule.getActivity(timeOfDay) == Activity.WORK) {
                return Activity.WANDER;
            }
            
            // On special days (birthdays, anniversaries), different schedule
            if (isSpecialDay(data, worldTime)) {
                return Activity.SOCIALIZE;
            }
            
            // Babies have different schedule
            if (data.getAge() < 20) {
                return switch (timeOfDay) {
                    case DAWN, MORNING -> Activity.WAKE_UP;
                    case NOON -> Activity.EAT;
                    case AFTERNOON -> Activity.SOCIALIZE;
                    case DUSK -> Activity.RELAX;
                    default -> Activity.SLEEP;
                };
            }
        }
        
        return schedule.getActivity(timeOfDay);
    }
    
    public static void updateSchedules(ServerWorld world) {
        List<VillagerEntity> villagers = world.getEntitiesByClass(
            VillagerEntity.class,
            new Box(-30000000, -256, -30000000, 30000000, 256, 30000000),
            v -> true
        );
        
        for (VillagerEntity villager : villagers) {
            Activity currentActivity = getCurrentActivity(villager);
            updateVillagerBehavior(villager, currentActivity);
            
            // Update custom name to show activity
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                Text activityText = Text.literal(" [" + currentActivity.description + "]")
                    .formatted(getActivityFormatting(currentActivity));
                Text fullName = Text.literal(data.getName()).append(activityText);
                villager.setCustomName(fullName);
            }
        }
    }
    
    private static void updateVillagerBehavior(VillagerEntity villager, Activity activity) {
        // This would integrate with the villager's AI goals
        // For now, just update their state based on activity
        
        switch (activity) {
            case SLEEP -> {
                // Villager should go to bed - note: sleep behavior handled by Minecraft AI
            }
            case WORK -> {
                // Villager should go to workstation
            }
            case SOCIALIZE -> {
                // Villager should seek other villagers
            }
            case EAT -> {
                // Villager should seek food
            }
            default -> {
                // Default behavior
            }
        }
    }
    
    private static boolean isSpecialDay(VillagerData data, long worldTime) {
        // Check if it's the villager's birthday (every 365 Minecraft days)
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
        
        // Current activity
        Activity current = getCurrentActivity(villager);
        info.add(Text.empty());
        info.add(Text.literal("Currently: ")
            .formatted(Formatting.YELLOW)
            .append(Text.literal(current.description)
                .formatted(getActivityFormatting(current))));
        
        return info;
    }
    
    // Custom goal for following schedules
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