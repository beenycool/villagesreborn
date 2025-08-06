package com.beeny.system.personality;

import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Strategy interface for personality-specific behaviors.
 * Each personality type implements this interface to define its unique behavior patterns.
 */
public interface PersonalityStrategy {
    
    /**
     * Apply personality-specific effects to a villager
     * @param villager The villager entity
     * @param data The villager's data
     * @param world The server world
     */
    void applyPersonalityEffects(VillagerEntity villager, VillagerData data, ServerWorld world);
    
    /**
     * Get the happiness modifier for this personality type
     * @return Happiness multiplier (1.0 = normal, >1.0 = happier, <1.0 = less happy)
     */
    float getHappinessModifier();
    
    /**
     * Get trade bonus for this personality based on player reputation
     * @param playerReputation The player's reputation with this villager
     * @return Trade bonus/penalty
     */
    int getTradeBonus(int playerReputation);
    
    /**
     * Get personality-specific greeting based on reputation
     * @param villagerName The villager's name
     * @param reputation The player's reputation
     * @return Greeting message
     */
    String getGreeting(String villagerName, int reputation);
    
    /**
     * Get the name of this personality type
     * @return Personality type name
     */
    String getPersonalityName();
}