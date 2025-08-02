package com.beeny.system;

import com.beeny.data.VillagerData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DailyActivityTracker {
    
    public static class ActivityEntry {
        public static final Codec<ActivityEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.fieldOf("activity").forGetter(ActivityEntry::getActivity),
                Codec.LONG.fieldOf("startTime").forGetter(ActivityEntry::getStartTime),
                Codec.LONG.fieldOf("endTime").forGetter(ActivityEntry::getEndTime),
                Codec.STRING.fieldOf("location").forGetter(ActivityEntry::getLocation),
                Codec.STRING.optionalFieldOf("details", "").forGetter(ActivityEntry::getDetails),
                Codec.INT.fieldOf("worldDay").forGetter(ActivityEntry::getWorldDay)
            ).apply(instance, ActivityEntry::new)
        );
        
        private final String activity;
        private final long startTime;
        private final long endTime;
        private final String location;
        private final String details;
        private final int worldDay;
        
        public ActivityEntry(String activity, long startTime, long endTime, String location, String details, int worldDay) {
            this.activity = activity;
            this.startTime = startTime;
            this.endTime = endTime;
            this.location = location;
            this.details = details;
            this.worldDay = worldDay;
        }
        
        public String getActivity() { return activity; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public String getLocation() { return location; }
        public String getDetails() { return details; }
        public int getWorldDay() { return worldDay; }
        public long getDuration() { return endTime - startTime; }
        
        public VillagerScheduleManager.TimeOfDay getTimeOfDay() {
            return VillagerScheduleManager.TimeOfDay.fromWorldTime(startTime);
        }
        
        @Override
        public String toString() {
            return String.format("%s at %s (%s) - %dmin", 
                activity, location, getTimeOfDay().name, getDuration() / 1000);
        }
    }
    
    public static class DailyLog {
        public static final Codec<DailyLog> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.INT.fieldOf("day").forGetter(DailyLog::getDay),
                Codec.list(ActivityEntry.CODEC).fieldOf("activities").forGetter(DailyLog::getActivities),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("activityDurations").forGetter(DailyLog::getActivityDurations),
                Codec.INT.fieldOf("happinessChange").forGetter(DailyLog::getHappinessChange),
                Codec.INT.fieldOf("socialInteractions").forGetter(DailyLog::getSocialInteractions),
                Codec.STRING.optionalFieldOf("notableEvent", "").forGetter(DailyLog::getNotableEvent)
            ).apply(instance, DailyLog::new)
        );
        
        private final int day;
        private final List<ActivityEntry> activities;
        private final Map<String, Long> activityDurations;
        private final int happinessChange;
        private final int socialInteractions;
        private final String notableEvent;
        
        public DailyLog(int day, List<ActivityEntry> activities, Map<String, Long> activityDurations, 
                       int happinessChange, int socialInteractions, String notableEvent) {
            this.day = day;
            this.activities = new ArrayList<>(activities);
            this.activityDurations = new HashMap<>(activityDurations);
            this.happinessChange = happinessChange;
            this.socialInteractions = socialInteractions;
            this.notableEvent = notableEvent;
        }
        
        public int getDay() { return day; }
        public List<ActivityEntry> getActivities() { return new ArrayList<>(activities); }
        public Map<String, Long> getActivityDurations() { return new HashMap<>(activityDurations); }
        public int getHappinessChange() { return happinessChange; }
        public int getSocialInteractions() { return socialInteractions; }
        public String getNotableEvent() { return notableEvent; }
        
        public long getTotalActivityTime(String activity) {
            return activityDurations.getOrDefault(activity, 0L);
        }
        
        public ActivityEntry getMostFrequentActivity() {
            return activities.stream()
                .max(Comparator.comparing(entry -> getTotalActivityTime(entry.getActivity())))
                .orElse(null);
        }
    }
    
    private static final Map<String, Map<Integer, DailyLog>> VILLAGER_DAILY_LOGS = new ConcurrentHashMap<>();
    private static final Map<String, ActivityEntry> CURRENT_ACTIVITIES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> DAILY_HAPPINESS_START = new ConcurrentHashMap<>();
    private static final Map<String, Integer> DAILY_SOCIAL_COUNT = new ConcurrentHashMap<>();
    
    private static final int MAX_DAYS_TO_KEEP = 30;
    
    public static void startActivity(VillagerEntity villager, VillagerScheduleManager.Activity activity, String details) {
        String villagerId = villager.getUuidAsString();
        long worldTime = villager.getWorld().getTimeOfDay();
        int worldDay = (int) (worldTime / 24000);
        
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        endCurrentActivity(villager);
        
        BlockPos pos = villager.getBlockPos();
        String location = String.format("%d,%d,%d", pos.getX(), pos.getY(), pos.getZ());
        
        ActivityEntry currentActivity = new ActivityEntry(
            activity.description,
            worldTime,
            worldTime,
            location,
            details,
            worldDay
        );
        
        CURRENT_ACTIVITIES.put(villagerId, currentActivity);
        
        if (!DAILY_HAPPINESS_START.containsKey(villagerId)) {
            DAILY_HAPPINESS_START.put(villagerId, data.getHappiness());
        }
        
        if (activity == VillagerScheduleManager.Activity.SOCIALIZE) {
            DAILY_SOCIAL_COUNT.merge(villagerId, 1, Integer::sum);
        }
    }
    
    public static void endCurrentActivity(VillagerEntity villager) {
        String villagerId = villager.getUuidAsString();
        ActivityEntry currentActivity = CURRENT_ACTIVITIES.remove(villagerId);
        
        if (currentActivity != null) {
            long worldTime = villager.getWorld().getTimeOfDay();
            int worldDay = (int) (worldTime / 24000);
            
            ActivityEntry completedActivity = new ActivityEntry(
                currentActivity.getActivity(),
                currentActivity.getStartTime(),
                worldTime,
                currentActivity.getLocation(),
                currentActivity.getDetails(),
                worldDay
            );
            
            addActivityToLog(villagerId, completedActivity);
        }
    }
    
    private static void addActivityToLog(String villagerId, ActivityEntry activity) {
        VILLAGER_DAILY_LOGS.computeIfAbsent(villagerId, k -> new ConcurrentHashMap<>());
        
        Map<Integer, DailyLog> villagerLogs = VILLAGER_DAILY_LOGS.get(villagerId);
        int day = activity.getWorldDay();
        
        if (!villagerLogs.containsKey(day)) {
            villagerLogs.put(day, createEmptyDailyLog(day));
        }
        
        DailyLog currentLog = villagerLogs.get(day);
        List<ActivityEntry> activities = new ArrayList<>(currentLog.getActivities());
        activities.add(activity);
        
        Map<String, Long> durations = new HashMap<>(currentLog.getActivityDurations());
        durations.merge(activity.getActivity(), activity.getDuration(), Long::sum);
        
        villagerLogs.put(day, new DailyLog(
            day,
            activities,
            durations,
            currentLog.getHappinessChange(),
            currentLog.getSocialInteractions(),
            currentLog.getNotableEvent()
        ));
        
        cleanupOldLogs(villagerId);
    }
    
    private static DailyLog createEmptyDailyLog(int day) {
        return new DailyLog(day, new ArrayList<>(), new HashMap<>(), 0, 0, "");
    }
    
    public static void finalizeDailyLog(VillagerEntity villager) {
        String villagerId = villager.getUuidAsString();
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        endCurrentActivity(villager);
        
        long worldTime = villager.getWorld().getTimeOfDay();
        int worldDay = (int) (worldTime / 24000);
        
        Map<Integer, DailyLog> villagerLogs = VILLAGER_DAILY_LOGS.get(villagerId);
        if (villagerLogs == null || !villagerLogs.containsKey(worldDay)) return;
        
        DailyLog currentLog = villagerLogs.get(worldDay);
        int startHappiness = DAILY_HAPPINESS_START.getOrDefault(villagerId, data.getHappiness());
        int happinessChange = data.getHappiness() - startHappiness;
        int socialCount = DAILY_SOCIAL_COUNT.getOrDefault(villagerId, 0);
        
        String notableEvent = determineNotableEvent(currentLog, happinessChange, socialCount);
        
        DailyLog finalizedLog = new DailyLog(
            worldDay,
            currentLog.getActivities(),
            currentLog.getActivityDurations(),
            happinessChange,
            socialCount,
            notableEvent
        );
        
        villagerLogs.put(worldDay, finalizedLog);
        
        DAILY_HAPPINESS_START.remove(villagerId);
        DAILY_SOCIAL_COUNT.remove(villagerId);
    }
    
    private static String determineNotableEvent(DailyLog log, int happinessChange, int socialCount) {
        if (happinessChange >= 20) return "Had a wonderful day";
        if (happinessChange <= -20) return "Had a terrible day";
        if (socialCount >= 5) return "Very social day";
        if (socialCount == 0) return "Spent day alone";
        
        long workTime = log.getTotalActivityTime("Working");
        long relaxTime = log.getTotalActivityTime("Relaxing");
        
        if (workTime > 8000) return "Worked extra hard";
        if (relaxTime > 6000) return "Had a relaxing day";
        
        return "";
    }
    
    private static void cleanupOldLogs(String villagerId) {
        Map<Integer, DailyLog> villagerLogs = VILLAGER_DAILY_LOGS.get(villagerId);
        if (villagerLogs == null) return;
        
        List<Integer> daysToRemove = villagerLogs.keySet().stream()
            .sorted()
            .limit(Math.max(0, villagerLogs.size() - MAX_DAYS_TO_KEEP))
            .toList();
        
        daysToRemove.forEach(villagerLogs::remove);
    }
    
    public static DailyLog getDailyLog(String villagerId, int day) {
        Map<Integer, DailyLog> villagerLogs = VILLAGER_DAILY_LOGS.get(villagerId);
        return villagerLogs != null ? villagerLogs.get(day) : null;
    }
    
    public static List<DailyLog> getRecentLogs(String villagerId, int days) {
        Map<Integer, DailyLog> villagerLogs = VILLAGER_DAILY_LOGS.get(villagerId);
        if (villagerLogs == null) return new ArrayList<>();
        
        return villagerLogs.values().stream()
            .sorted(Comparator.comparing(DailyLog::getDay).reversed())
            .limit(days)
            .toList();
    }
    
    public static Map<String, Long> getWeeklyActivitySummary(String villagerId) {
        List<DailyLog> recentLogs = getRecentLogs(villagerId, 7);
        Map<String, Long> summary = new HashMap<>();
        
        for (DailyLog log : recentLogs) {
            for (Map.Entry<String, Long> entry : log.getActivityDurations().entrySet()) {
                summary.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }
        
        return summary;
    }
    
    public static List<String> getActivityPatterns(String villagerId) {
        List<DailyLog> recentLogs = getRecentLogs(villagerId, 14);
        List<String> patterns = new ArrayList<>();
        
        Map<String, Integer> activityFrequency = new HashMap<>();
        Map<VillagerScheduleManager.TimeOfDay, String> preferredTimeActivities = new EnumMap<>(VillagerScheduleManager.TimeOfDay.class);
        
        for (DailyLog log : recentLogs) {
            for (ActivityEntry activity : log.getActivities()) {
                activityFrequency.merge(activity.getActivity(), 1, Integer::sum);
                
                VillagerScheduleManager.TimeOfDay timeOfDay = activity.getTimeOfDay();
                String currentPreferred = preferredTimeActivities.get(timeOfDay);
                if (currentPreferred == null || 
                    activityFrequency.getOrDefault(activity.getActivity(), 0) > 
                    activityFrequency.getOrDefault(currentPreferred, 0)) {
                    preferredTimeActivities.put(timeOfDay, activity.getActivity());
                }
            }
        }
        
        String mostFrequent = activityFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
        
        patterns.add("Most frequent activity: " + mostFrequent);
        
        int averageHappinessChange = recentLogs.stream()
            .mapToInt(DailyLog::getHappinessChange)
            .sum() / Math.max(1, recentLogs.size());
        
        if (averageHappinessChange > 5) {
            patterns.add("Generally becoming happier");
        } else if (averageHappinessChange < -5) {
            patterns.add("Generally becoming less happy");
        } else {
            patterns.add("Stable mood");
        }
        
        double averageSocial = recentLogs.stream()
            .mapToInt(DailyLog::getSocialInteractions)
            .average()
            .orElse(0.0);
        
        if (averageSocial > 3) {
            patterns.add("Very social villager");
        } else if (averageSocial < 1) {
            patterns.add("Prefers solitude");
        } else {
            patterns.add("Moderately social");
        }
        
        return patterns;
    }
    
    public static ActivityEntry getCurrentActivity(String villagerId) {
        return CURRENT_ACTIVITIES.get(villagerId);
    }
    
    public static void onDayChange(ServerWorld world) {
        for (VillagerEntity villager : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
            if (villager.getWorld() == world) {
                finalizeDailyLog(villager);
            }
        }
    }
    
    public static NbtCompound saveToNbt() {
        NbtCompound nbt = new NbtCompound();
        
        NbtCompound logsNbt = new NbtCompound();
        for (Map.Entry<String, Map<Integer, DailyLog>> villagerEntry : VILLAGER_DAILY_LOGS.entrySet()) {
            NbtCompound villagerNbt = new NbtCompound();
            for (Map.Entry<Integer, DailyLog> dayEntry : villagerEntry.getValue().entrySet()) {
                // Note: In a real implementation, you'd use proper NBT serialization with the codecs
                villagerNbt.putString(dayEntry.getKey().toString(), dayEntry.getValue().toString());
            }
            logsNbt.put(villagerEntry.getKey(), villagerNbt);
        }
        nbt.put("daily_logs", logsNbt);
        
        return nbt;
    }
    
    public static void loadFromNbt(NbtCompound nbt) {
        // Note: In a real implementation, you'd use proper NBT deserialization with the codecs
        VILLAGER_DAILY_LOGS.clear();
        CURRENT_ACTIVITIES.clear();
        DAILY_HAPPINESS_START.clear();
        DAILY_SOCIAL_COUNT.clear();
    }
}