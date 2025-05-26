package com.beeny.villagesreborn.core.combat;

/**
 * Interface representing an item stack for combat AI decision making
 */
public interface ItemStack {
    float getAttackDamage();
    ItemType getItemType();
    float getRange();
    int getDefenseValue();
    EquipmentSlot getEquipmentSlot();
    
    // Additional methods for compatibility
    default float getDamage() { return getAttackDamage(); }
    default float getCombatValue() { return getAttackDamage() + getDefenseValue(); }
}