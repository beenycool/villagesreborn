package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;

/**
 * Manages villager AI brains and processes interactions
 */
public interface VillagerBrainManager {
    
    /**
     * Processes an overheard message by a villager
     * @param villager the villager who overheard the message
     * @param player the player who sent the message
     * @param message the message content
     */
    void processOverheardMessage(VillagerEntity villager, Player player, String message);
    
    /**
     * Gets or creates a brain for the specified villager
     * @param villager the villager entity
     * @return the villager's brain
     */
    VillagerBrain getBrain(VillagerEntity villager);
    
    /**
     * Saves a villager's brain data to persistent storage
     * @param villager the villager entity
     * @param brain the brain to save
     */
    void saveBrain(VillagerEntity villager, VillagerBrain brain);
}