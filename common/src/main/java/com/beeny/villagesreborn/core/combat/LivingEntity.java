package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import java.util.UUID;

/**
 * Interface representing any living entity that can participate in combat
 */
public interface LivingEntity {
    
    /**
     * Gets the unique identifier for this entity
     */
    UUID getUUID();
    
    /**
     * Checks if this entity is alive
     */
    boolean isAlive();
    
    /**
     * Checks if this entity is hostile to villagers
     */
    boolean isHostile();
    
    /**
     * Gets the current health of this entity
     */
    float getHealth();
    
    /**
     * Gets the attack damage this entity can deal
     */
    float getAttackDamage();
    
    /**
     * Gets the distance to another entity
     */
    float getDistanceTo(VillagerEntity target);
    
    /**
     * Checks if this entity is currently being attacked
     */
    boolean isBeingAttacked();
    
    /**
     * Gets the entity that is attacking this one (if any)
     */
    LivingEntity getAttacker();
}