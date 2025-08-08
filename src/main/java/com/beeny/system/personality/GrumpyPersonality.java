package com.beeny.system.personality;

import com.beeny.data.VillagerData;
import com.beeny.util.VillagerUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

import java.util.List;

/**
 * Strategy implementation for grumpy personality behavior.
 */
public class GrumpyPersonality implements PersonalityStrategy {
    
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 6);
        
        // Grumpy villagers don't like crowds
        if (nearbyVillagers.size() > 2) {
            data.adjustHappiness(-1);
            
            if (VillagerUtils.shouldPerformRandomAction(0.02f)) {
                VillagerUtils.playVillagerSound(world, villager.getBlockPos(), 
                    SoundEvents.ENTITY_VILLAGER_NO, 0.8f, 0.8f);
            }
        }
        
        // Gradually decrease happiness if too happy
        if (data.getHappiness() > 60 && VillagerUtils.shouldPerformRandomAction(0.1f)) {
            data.adjustHappiness(-1);
        }
        
        // Being alone makes them slightly happier
        if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(1);
        }
    }
    
    @Override
    public float getHappinessModifier() {
        return 0.8f;
    }
    
    @Override
    public int getTradeBonus(int playerReputation) {
        return playerReputation > 50 ? 1 : -1;
    }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return switch (getReputationLevel(reputation)) {
            case HIGH -> "Oh, it's you. I suppose you're not too bad.";
            case NEUTRAL -> "What do you want now?";
            case LOW -> "What do you want now?";
        };
    }
    
    @Override
    public String getPersonalityName() {
        return "GRUMPY";
    }
    
    private ReputationLevel getReputationLevel(int reputation) {
        if (reputation > 60) return ReputationLevel.HIGH;
        if (reputation > -10) return ReputationLevel.NEUTRAL;
        return ReputationLevel.LOW;
    }
    
    private enum ReputationLevel {
        HIGH, NEUTRAL, LOW
    }
}