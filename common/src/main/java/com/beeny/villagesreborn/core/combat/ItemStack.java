package com.beeny.villagesreborn.core.combat;

/**
 * Represents an item stack that can be used in combat
 */
public interface ItemStack {
    
    /**
     * Gets the attack damage this item provides
     */
    float getAttackDamage();
    
    /**
     * Gets the type of this item
     */
    ItemType getItemType();
    
    /**
     * Gets the range of this item (for ranged weapons)
     */
    float getRange();
    
    /**
     * Gets the defense value this item provides (for armor)
     */
    int getDefenseValue();
    
    /**
     * Gets the equipment slot this item fits in
     */
    EquipmentSlot getEquipmentSlot();
}