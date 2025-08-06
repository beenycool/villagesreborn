package com.beeny.system.personality;

import com.beeny.data.VillagerData;
import com.beeny.util.VillagerUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Strategy implementation for friendly personality behavior.
 */
public class FriendlyPersonality implements PersonalityStrategy {
    
    @Override
    public void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Friendly villagers seek out social interactions more often
        if (VillagerUtils.shouldPerformRandomAction(0.05f)) {
            List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 12);
            
            if (!nearbyVillagers.isEmpty()) {
                VillagerEntity target = VillagerUtils.getRandomElement(nearbyVillagers.toArray(new VillagerEntity[0]));
                VillagerData targetData = VillagerUtils.getVillagerData(target);
                
                if (targetData != null) {
                    data.adjustHappiness(2);
                    targetData.adjustHappiness(1);
                    
                    VillagerUtils.spawnHeartParticles(world, villager.getPos(), target.getPos(), 3);
                }
            }
        }
    }
    
    @Override
    public float getHappinessModifier() {
        return 1.05f;
    }
    
    @Override
    public int getTradeBonus(int playerReputation) {
        return playerReputation > 30 ? 2 : 1;
    }
    
    @Override
    public String getGreeting(String villagerName, int reputation) {
        return switch (getReputationLevel(reputation)) {
            case HIGH -> reputation > 20 ? 
                "Hello there, friend! Wonderful to see you again!" :
                "Hello! Nice to meet you!";
            case NEUTRAL -> "Hello there!";
            case LOW -> "Oh... it's you again...";
        };
    }
    
    @Override
    public String getPersonalityName() {
        return "FRIENDLY";
    }
    
    private ReputationLevel getReputationLevel(int reputation) {
        if (reputation > 20) return ReputationLevel.HIGH;
        if (reputation > -10) return ReputationLevel.NEUTRAL;
        return ReputationLevel.LOW;
    }
    
    private enum ReputationLevel {
        HIGH, NEUTRAL, LOW
    }
}