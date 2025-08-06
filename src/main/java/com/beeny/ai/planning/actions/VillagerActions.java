package com.beeny.ai.planning.actions;

import com.beeny.ai.planning.GOAPAction;
import com.beeny.ai.planning.GOAPWorldState;
import com.beeny.util.VillagerPersonalityUtils;
import com.beeny.util.VillagerEmotionUtils;
import com.beeny.system.VillagerScheduleManager;
import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Collection of villager-specific actions for GOAP planning.
 */
public class VillagerActions {
    
    public static class SocializeAction extends GOAPAction {
        public SocializeAction(VillagerEntity villager) {
            super("socialize", 5.0f, villager);
        }
        
        @Override
        public boolean canPerform(@NotNull GOAPWorldState state) {
            return state.getBool("villagers_nearby");
        }
        
        @Override
        @NotNull
        public GOAPWorldState getEffects(@NotNull GOAPWorldState state) {
            GOAPWorldState effects = new GOAPWorldState.Simple();
            effects.setBool("has_socialized", true);
            effects.setBool("has_socialized_today", true);
            return effects;
        }
        
        @Override
        public float getCost(@NotNull GOAPWorldState state) {
            String personality = VillagerPersonalityUtils.getPersonality(villager);
            return baseCost * VillagerPersonalityUtils.getActionCostModifier(personality, "social");
        }
        
        @Override
        public boolean execute(@NotNull GOAPWorldState state) {
            // Find nearby villagers and socialize
            List<VillagerEntity> nearby = villager.getWorld().getEntitiesByClass(
                VillagerEntity.class,
                villager.getBoundingBox().expand(10.0),
                v -> v != villager
            );
            
            if (!nearby.isEmpty()) {
                // Emotional effects of socializing
                VillagerEmotionUtils.processSocializingEffects(villager);
                
                state.setBool("has_socialized", true);
                state.setBool("has_socialized_today", true);
                return true;
            }
            return false;
        }
        
        @Override
        @NotNull
        public GOAPWorldState getPreconditions() {
            GOAPWorldState preconditions = new GOAPWorldState.Simple();
            preconditions.setBool("villagers_nearby", true);
            return preconditions;
        }
    }
    
    public static class WorkAction extends GOAPAction {
        public WorkAction(VillagerEntity villager) {
            super("work", 8.0f, villager);
        }
        
        @Override
        public boolean canPerform(@NotNull GOAPWorldState state) {
            VillagerScheduleManager.TimeOfDay timeOfDay = VillagerScheduleManager.TimeOfDay.fromWorldTime(villager.getWorld().getTimeOfDay());
            return timeOfDay == VillagerScheduleManager.TimeOfDay.MORNING || 
                   timeOfDay == VillagerScheduleManager.TimeOfDay.AFTERNOON;
        }
        
        @Override
        @NotNull
        public GOAPWorldState getEffects(@NotNull GOAPWorldState state) {
            GOAPWorldState effects = new GOAPWorldState.Simple();
            effects.setBool("has_worked", true);
            effects.setBool("has_worked_today", true);
            return effects;
        }
        
        @Override
        public float getCost(@NotNull GOAPWorldState state) {
            String personality = VillagerPersonalityUtils.getPersonality(villager);
            return baseCost * VillagerPersonalityUtils.getActionCostModifier(personality, "work");
        }
        
        @Override
        public boolean execute(@NotNull GOAPWorldState state) {
            // Simulate work and its effects
            VillagerEmotionUtils.processWorkingEffects(villager);
            
            state.setBool("has_worked", true);
            state.setBool("has_worked_today", true);
            return true;
        }
        
        @Override
        @NotNull
        public GOAPWorldState getPreconditions() {
            return new GOAPWorldState.Simple(); // No specific preconditions
        }
    }
    
    public static class GossipAction extends GOAPAction {
        public GossipAction(VillagerEntity villager) {
            super("gossip", 3.0f, villager);
        }
        
        @Override
        public boolean canPerform(@NotNull GOAPWorldState state) {
            return state.getBool("villagers_nearby");
        }
        
        @Override
        @NotNull
        public GOAPWorldState getEffects(@NotNull GOAPWorldState state) {
            GOAPWorldState effects = new GOAPWorldState.Simple();
            effects.setBool("knows_latest_gossip", true);
            effects.setBool("has_socialized", true);
            return effects;
        }
        
        @Override
        public float getCost(@NotNull GOAPWorldState state) {
            String personality = VillagerPersonalityUtils.getPersonality(villager);
            return baseCost * VillagerPersonalityUtils.getActionCostModifier(personality, "gossip");
        }
        
        @Override
        public boolean execute(@NotNull GOAPWorldState state) {
            // Process emotional effects of learning gossip
            VillagerEmotionUtils.processGossipLearningEffects(villager);
            
            state.setBool("knows_latest_gossip", true);
            state.setBool("has_socialized", true);
            return true;
        }
        
        @Override
        @NotNull
        public GOAPWorldState getPreconditions() {
            GOAPWorldState preconditions = new GOAPWorldState.Simple();
            preconditions.setBool("villagers_nearby", true);
            return preconditions;
        }
    }
}