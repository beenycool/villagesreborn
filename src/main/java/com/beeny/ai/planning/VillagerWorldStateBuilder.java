package com.beeny.ai.planning;

import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.AIWorldManagerRefactored;
import com.beeny.data.VillagerData;
import com.beeny.constants.VillagerConstants;
import com.beeny.system.VillagerScheduleManager;
import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Builds world state representation from villager data and environment.
 */
public class VillagerWorldStateBuilder {
    
    /**
     * Build current world state for the given villager.
     */
    @NotNull
    public static GOAPWorldState buildCurrentWorldState(@NotNull VillagerEntity villager) {
        GOAPWorldState.Simple state = new GOAPWorldState.Simple();
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        VillagerEmotionSystem.EmotionalState emotions = AIWorldManagerRefactored.getInstance()
            .getEmotionSystem().getEmotionalState(villager);
        
        // Basic villager data
        if (data != null) {
            state.setInt("age", data.getAge());
            state.setFloat("happiness", data.getHappiness());
            state.setBool("has_spouse", !data.getSpouseId().isEmpty());
            state.setString("personality", VillagerConstants.PersonalityType.toString(data.getPersonality()));
            
            // Emotional state
            if (emotions != null) {
                state.setFloat("loneliness", emotions.getEmotion(VillagerEmotionSystem.EmotionType.LONELINESS));
                state.setFloat("curiosity", emotions.getEmotion(VillagerEmotionSystem.EmotionType.CURIOSITY));
                state.setFloat("love", emotions.getEmotion(VillagerEmotionSystem.EmotionType.LOVE));
                state.setFloat("boredom", emotions.getEmotion(VillagerEmotionSystem.EmotionType.BOREDOM));
            }
        }
        
        // Environmental state
        List<VillagerEntity> nearbyVillagers = villager.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            villager.getBoundingBox().expand(10.0),
            v -> v != villager
        );
        state.setBool("villagers_nearby", !nearbyVillagers.isEmpty());
        
        // Time-based state
        VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(villager.getWorld().getTimeOfDay());
        state.setString("time_of_day", timeOfDay.name());
        
        // Daily activity tracking with time-based reset
        setDailyActivityFlags(state, villager, data);
        
        return state;
    }
    
    private static void setDailyActivityFlags(@NotNull GOAPWorldState.Simple state, 
                                            @NotNull VillagerEntity villager, 
                                            VillagerData data) {
        long worldTime = villager.getWorld().getTimeOfDay();
        int currentDay = (int) (worldTime / 24000);

        // Use villager's lastConversationTime as a lightweight persisted "last day touched"
        long lastTouch = (data != null) ? data.getLastConversationTime() : 0L;
        int lastDay = (int) (lastTouch / 24000);
        boolean newDay = currentDay != lastDay;

        boolean hasWorkedToday = false, hasSocializedToday = false;

        if (!newDay) {
            // Infer from DailyActivityTracker if available for current day
            com.beeny.system.DailyActivityTracker.DailyLog todayLog =
                com.beeny.system.DailyActivityTracker.getDailyLog(villager.getUuidAsString(), currentDay);
            if (todayLog != null) {
                java.util.Map<String, Long> durations = todayLog.getActivityDurations();
                long workTime = durations.getOrDefault("Working", 0L);
                long socializeTime = durations.getOrDefault("Socializing", 0L);
                // Some places might label socialize activity differently; be defensive
                if (socializeTime == 0L) {
                    socializeTime = durations.getOrDefault("Socialize", 0L);
                }
                hasWorkedToday = workTime > 0;
                hasSocializedToday = socializeTime > 0;
            }
        } else {
            // Day changed: reset daily flags
            hasWorkedToday = false;
            hasSocializedToday = false;
            // Update last touch to the start of current day to avoid repeat resets within the day
            if (data != null) {
                // Store as a time within current day so (time/24000) == currentDay
                data.setLastConversationTime(currentDay * 24000L);
            }
        }

        state.setBool("has_worked_today", hasWorkedToday);
        state.setBool("has_socialized_today", hasSocializedToday);
    }
}