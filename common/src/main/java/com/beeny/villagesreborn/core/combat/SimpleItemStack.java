package com.beeny.villagesreborn.core.combat;

/**
 * Simple implementation of ItemStack for AI decision making
 */
public class SimpleItemStack implements ItemStack {
    private final ItemType itemType;
    private final float attackDamage;
    private final float range;
    private final int defenseValue;
    private final EquipmentSlot equipmentSlot;
    
    public SimpleItemStack(ItemType itemType, float attackDamage) {
        this(itemType, attackDamage, 1.0f, 0, getDefaultEquipmentSlot(itemType));
    }
    
    public SimpleItemStack(ItemType itemType, float attackDamage, float range, int defenseValue, EquipmentSlot equipmentSlot) {
        this.itemType = itemType;
        this.attackDamage = attackDamage;
        this.range = range;
        this.defenseValue = defenseValue;
        this.equipmentSlot = equipmentSlot;
    }
    
    private static EquipmentSlot getDefaultEquipmentSlot(ItemType itemType) {
        switch (itemType) {
            case SWORD:
            case BOW:
            case AXE:
                return EquipmentSlot.MAINHAND;
            case HELMET:
                return EquipmentSlot.HEAD;
            case CHESTPLATE:
                return EquipmentSlot.CHEST;
            case LEGGINGS:
                return EquipmentSlot.LEGS;
            case BOOTS:
                return EquipmentSlot.FEET;
            default:
                return EquipmentSlot.MAINHAND;
        }
    }
    
    @Override
    public float getAttackDamage() {
        return attackDamage;
    }
    
    @Override
    public ItemType getItemType() {
        return itemType;
    }
    
    @Override
    public float getRange() {
        return range;
    }
    
    @Override
    public int getDefenseValue() {
        return defenseValue;
    }
    
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }
    
    @Override
    public String toString() {
        return String.format("SimpleItemStack{type=%s, damage=%.1f, range=%.1f, defense=%d, slot=%s}", 
                           itemType, attackDamage, range, defenseValue, equipmentSlot);
    }
}