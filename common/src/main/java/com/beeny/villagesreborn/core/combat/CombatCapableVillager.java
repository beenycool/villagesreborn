package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.List;
import java.util.UUID;

/**
 * Extended villager interface that supports combat AI capabilities
 */
public interface CombatCapableVillager extends VillagerEntity {
    
    /**
     * Gets available weapons for this villager
     */
    List<ItemStack> getAvailableWeapons();
    
    /**
     * Gets the villager's combat personality traits
     */
    CombatPersonalityTraits getCombatTraits();
    
    /**
     * Gets the villager's relationship data
     */
    RelationshipData getRelationshipData();
    
    /**
     * Checks if this villager has combat memory capabilities
     */
    boolean hasCombatMemory();
    
    /**
     * Gets the villager's combat memory data
     */
    CombatMemoryData getCombatMemory();
    
    /**
     * Equips a weapon
     */
    boolean equipWeapon(ItemStack weapon);
    
    /**
     * Attacks a target by UUID
     */
    boolean attackTarget(UUID targetId);
    
    /**
     * Enters defensive stance
     */
    boolean enterDefensiveStance();
    
    /**
     * Attempts to flee from combat
     */
    boolean flee();
    
    /**
     * Attempts negotiation
     */
    boolean attemptNegotiation();
    
    /**
     * Gets distance to another entity
     */
    float getDistanceTo(LivingEntity entity);
}