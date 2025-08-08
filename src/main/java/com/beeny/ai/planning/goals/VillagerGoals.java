package com.beeny.ai.planning.goals;

import com.beeny.ai.planning.GOAPGoal;
import com.beeny.ai.planning.GOAPWorldState;
import com.beeny.data.VillagerData;
import com.beeny.constants.VillagerConstants;
import com.beeny.util.VillagerPersonalityUtils;
import com.beeny.system.VillagerScheduleManager;
import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Collection of villager-specific goals for GOAP planning.
 */
public class VillagerGoals {
    
    public static class IncreaseHappinessGoal extends GOAPGoal {
        public IncreaseHappinessGoal(VillagerEntity villager) {
            super("increase_happiness", 0.7f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("is_happy", true);
        }
        
        @Override
        public boolean isValid(@NotNull GOAPWorldState currentState) {
            return currentState.getFloat("happiness") < 70.0f;
        }
        
        @Override
        public float calculateDynamicPriority(@NotNull GOAPWorldState currentState) {
            float happiness = currentState.getFloat("happiness");
            return Math.max(0.1f, (100.0f - happiness) / 100.0f);
        }
    }
    
    public static class SocializeGoal extends GOAPGoal {
        public SocializeGoal(VillagerEntity villager) {
            super("socialize", 0.5f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("has_socialized", true);
        }
        
        @Override
        public boolean isValid(@NotNull GOAPWorldState currentState) {
            return currentState.getFloat("loneliness") > 20.0f || !currentState.getBool("has_socialized_today");
        }
        
        @Override
        public float calculateDynamicPriority(@NotNull GOAPWorldState currentState) {
            float loneliness = currentState.getFloat("loneliness");
            String personality = VillagerPersonalityUtils.getPersonality(villager);
            float personality_modifier = VillagerPersonalityUtils.getSocialModifier(personality);
            return Math.max(0.1f, (loneliness / 50.0f)) * personality_modifier;
        }
    }
    
    public static class WorkGoal extends GOAPGoal {
        public WorkGoal(VillagerEntity villager) {
            super("work", 0.6f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("has_worked", true);
        }
        
        @Override
        public boolean isValid(@NotNull GOAPWorldState currentState) {
            VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(villager.getWorld().getTimeOfDay());
            return (timeOfDay == VillagerScheduleManager.TimeOfDay.MORNING || 
                   timeOfDay == VillagerScheduleManager.TimeOfDay.AFTERNOON) &&
                   !currentState.getBool("has_worked_today");
        }
        
        @Override
        public float calculateDynamicPriority(@NotNull GOAPWorldState currentState) {
            String personality = VillagerPersonalityUtils.getPersonality(villager);
            return VillagerPersonalityUtils.getWorkModifier(personality);
        }
    }
    
    public static class FindLoveGoal extends GOAPGoal {
        public FindLoveGoal(VillagerEntity villager) {
            super("find_love", 0.3f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("has_spouse", true);
        }
        
        @Override
        public boolean isValid(@NotNull GOAPWorldState currentState) {
            return !currentState.getBool("has_spouse") && 
                   currentState.getInt("age") > 100 &&
                   currentState.getFloat("happiness") > 40.0f;
        }
        
        @Override
        public float calculateDynamicPriority(@NotNull GOAPWorldState currentState) {
            float loneliness = currentState.getFloat("loneliness");
            float love = currentState.getFloat("love");
            int age = currentState.getInt("age");
            
            float ageFactor = Math.max(0.1f, Math.min(1.0f, (age - 100) / 200.0f));
            float emotionalFactor = (loneliness + love) / 100.0f;
            
            return ageFactor * emotionalFactor;
        }
    }
    
    public static class LearnGossipGoal extends GOAPGoal {
        public LearnGossipGoal(VillagerEntity villager) {
            super("learn_gossip", 0.4f, villager);
        }
        
        @Override
        protected void defineGoalState() {
            desiredState.setBool("knows_latest_gossip", true);
        }
        
        @Override
        public boolean isValid(@NotNull GOAPWorldState currentState) {
            return currentState.getFloat("curiosity") > 30.0f;
        }
        
        @Override
        public float calculateDynamicPriority(@NotNull GOAPWorldState currentState) {
            String personality = VillagerPersonalityUtils.getPersonality(villager);
            float modifier = switch (personality) {
                case "Curious" -> 2.0f;
                case "Friendly", "Cheerful" -> 1.3f;
                case "Shy" -> 0.5f;
                default -> 1.0f;
            };
            return (currentState.getFloat("curiosity") / 100.0f) * modifier;
        }
    }
}