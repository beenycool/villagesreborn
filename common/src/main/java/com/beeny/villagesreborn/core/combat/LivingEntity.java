package com.beeny.villagesreborn.core.combat;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Represents a living entity that can participate in combat
 */
public class LivingEntity {
    
    private final String entityId;
    private final UUID uuid;
    private final String entityType;
    private float health;
    private final float maxHealth;
    private final List<ItemStack> equipment;
    private final CombatPersonalityTraits personalityTraits;
    private final List<String> activeEffects;
    private boolean isAlive;
    private float positionX;
    private float positionY;
    private float positionZ;
    private boolean isHostile;
    private boolean isBeingAttacked;
    private LivingEntity attacker;
    
    public LivingEntity(String entityId, String entityType, float maxHealth,
                       CombatPersonalityTraits personalityTraits) {
        this.entityId = entityId;
        this.uuid = UUID.randomUUID();
        this.entityType = entityType;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.personalityTraits = personalityTraits != null ? personalityTraits : CombatPersonalityTraits.createDefault();
        this.equipment = new ArrayList<>();
        this.activeEffects = new ArrayList<>();
        this.isAlive = true;
        this.positionX = 0.0f;
        this.positionY = 0.0f;
        this.positionZ = 0.0f;
        this.isHostile = false;
        this.isBeingAttacked = false;
        this.attacker = null;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public float getHealth() {
        return health;
    }
    
    public void setHealth(float health) {
        this.health = Math.max(0.0f, Math.min(maxHealth, health));
        this.isAlive = this.health > 0.0f;
    }
    
    public float getMaxHealth() {
        return maxHealth;
    }
    
    public float getHealthPercentage() {
        return maxHealth > 0 ? health / maxHealth : 0.0f;
    }
    
    public boolean isAlive() {
        return isAlive;
    }
    
    public void damage(float amount) {
        setHealth(health - amount);
    }
    
    public void heal(float amount) {
        setHealth(health + amount);
    }
    
    public CombatPersonalityTraits getPersonalityTraits() {
        return personalityTraits;
    }
    
    public List<ItemStack> getEquipment() {
        return new ArrayList<>(equipment);
    }
    
    public ItemStack getEquippedItem(EquipmentSlot slot) {
        return equipment.stream()
                .filter(item -> item.getEquipmentSlot() == slot)
                .findFirst()
                .orElse(null);
    }
    
    public void equipItem(ItemStack item) {
        if (item == null || item.getEquipmentSlot() == null) return;
        
        // Remove any existing item in the same slot
        equipment.removeIf(existing -> existing.getEquipmentSlot() == item.getEquipmentSlot());
        
        // Add the new item
        equipment.add(item);
    }
    
    public void unequipItem(EquipmentSlot slot) {
        equipment.removeIf(item -> item.getEquipmentSlot() == slot);
    }
    
    public boolean hasWeapon() {
        return getEquippedItem(EquipmentSlot.MAIN_HAND) != null;
    }
    
    public ItemStack getWeapon() {
        return getEquippedItem(EquipmentSlot.MAIN_HAND);
    }
    
    public boolean hasShield() {
        return getEquippedItem(EquipmentSlot.OFF_HAND) != null;
    }
    
    public ItemStack getShield() {
        return getEquippedItem(EquipmentSlot.OFF_HAND);
    }
    
    public boolean hasArmor() {
        return getEquippedItem(EquipmentSlot.CHEST) != null ||
               getEquippedItem(EquipmentSlot.HEAD) != null ||
               getEquippedItem(EquipmentSlot.LEGS) != null ||
               getEquippedItem(EquipmentSlot.FEET) != null;
    }
    
    public List<String> getActiveEffects() {
        return new ArrayList<>(activeEffects);
    }
    
    public void addEffect(String effect) {
        if (!activeEffects.contains(effect)) {
            activeEffects.add(effect);
        }
    }
    
    public void removeEffect(String effect) {
        activeEffects.remove(effect);
    }
    
    public boolean hasEffect(String effect) {
        return activeEffects.contains(effect);
    }
    
    public float getPositionX() {
        return positionX;
    }
    
    public float getPositionY() {
        return positionY;
    }
    
    public float getPositionZ() {
        return positionZ;
    }
    
    public void setPosition(float x, float y, float z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }
    
    public boolean isHostile() {
        return isHostile;
    }
    
    public void setHostile(boolean hostile) {
        this.isHostile = hostile;
    }
    
    public boolean isBeingAttacked() {
        return isBeingAttacked;
    }
    
    public void setBeingAttacked(boolean beingAttacked) {
        this.isBeingAttacked = beingAttacked;
        if (!beingAttacked) {
            this.attacker = null;
        }
    }
    
    public LivingEntity getAttacker() {
        return attacker;
    }
    
    public void setAttacker(LivingEntity attacker) {
        this.attacker = attacker;
        this.isBeingAttacked = (attacker != null);
    }
    
    /**
     * Gets the attack damage this entity can deal
     */
    public float getAttackDamage() {
        ItemStack weapon = getWeapon();
        if (weapon != null) {
            return weapon.getDamage();
        }
        // Base unarmed damage
        return 2.0f;
    }
    
    public double getDistanceTo(LivingEntity other) {
        if (other == null) return Double.MAX_VALUE;
        
        double dx = this.positionX - other.positionX;
        double dy = this.positionY - other.positionY;
        double dz = this.positionZ - other.positionZ;
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    // Overloaded method for compatibility with any object that has position
    public double getDistanceTo(Object other) {
        if (other instanceof LivingEntity) {
            return getDistanceTo((LivingEntity) other);
        }
        return Double.MAX_VALUE;
    }
    
    /**
     * Gets the combat effectiveness of this entity
     */
    public float getCombatEffectiveness() {
        float baseEffectiveness = getHealthPercentage();
        
        // Factor in equipment
        ItemStack weapon = getWeapon();
        if (weapon != null) {
            baseEffectiveness += weapon.getCombatValue() * 0.3f;
        }
        
        if (hasArmor()) {
            baseEffectiveness += 0.2f; // Basic armor bonus
        }
        
        if (hasShield()) {
            baseEffectiveness += 0.1f; // Shield defensive bonus
        }
        
        // Factor in personality traits
        baseEffectiveness *= (personalityTraits.getCourage() * 0.5f + 0.5f);
        
        return Math.max(0.0f, Math.min(2.0f, baseEffectiveness));
    }
    
    /**
     * Determines if this entity can engage another entity in combat
     */
    public boolean canEngageInCombat() {
        return isAlive && health > (maxHealth * 0.1f); // Must have at least 10% health
    }
    
    /**
     * Gets the entity's preferred combat range
     */
    public float getPreferredCombatRange() {
        ItemStack weapon = getWeapon();
        if (weapon != null) {
            return switch (weapon.getItemType()) {
                case SWORD, AXE -> 2.0f;      // Melee range
                case BOW, CROSSBOW -> 15.0f;  // Ranged combat
                case STAFF -> 8.0f;           // Magic range
                default -> 3.0f;              // Default melee
            };
        }
        return 1.5f; // Unarmed combat range
    }
    
    @Override
    public String toString() {
        return String.format("LivingEntity{id='%s', type='%s', health=%.1f/%.1f, alive=%s}", 
                           entityId, entityType, health, maxHealth, isAlive);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LivingEntity that = (LivingEntity) obj;
        return entityId.equals(that.entityId);
    }
    
    @Override
    public int hashCode() {
        return entityId.hashCode();
    }
}