package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Core engine for making combat decisions based on personality traits,
 * relationships, and threat assessment
 */
public class CombatDecisionEngine {
    
    /**
     * Select the best weapon from available options based on personality traits
     */
    public ItemStack selectBestWeapon(List<ItemStack> availableWeapons, 
                                    CombatPersonalityTraits traits, 
                                    VillagerEntity villager) {
        if (availableWeapons.isEmpty()) {
            return null;
        }
        
        ItemStack bestWeapon = null;
        float bestScore = -1.0f;
        
        for (ItemStack weapon : availableWeapons) {
            float score = calculateWeaponScore(weapon, traits);
            if (score > bestScore) {
                bestScore = score;
                bestWeapon = weapon;
            }
        }
        
        return bestWeapon;
    }
    
    /**
     * Select the best armor for a specific slot
     */
    public ItemStack selectBestArmor(List<ItemStack> availableArmor, 
                                   EquipmentSlot slot, 
                                   CombatPersonalityTraits traits) {
        if (availableArmor.isEmpty()) {
            return null;
        }
        
        return availableArmor.stream()
            .filter(armor -> armor.getEquipmentSlot() == slot)
            .max((a1, a2) -> Integer.compare(a1.getDefenseValue(), a2.getDefenseValue()))
            .orElse(null);
    }
    
    /**
     * Select the primary target from a list of enemies
     */
    public LivingEntity selectTarget(List<LivingEntity> enemies, 
                                   VillagerEntity villager, 
                                   CombatPersonalityTraits traits) {
        if (enemies.isEmpty()) {
            return null;
        }
        
        LivingEntity bestTarget = null;
        float highestThreat = -1.0f;
        
        for (LivingEntity enemy : enemies) {
            float threatScore = calculateThreatScore(enemy, villager, traits);
            if (threatScore > highestThreat) {
                highestThreat = threatScore;
                bestTarget = enemy;
            }
        }
        
        return bestTarget;
    }
    
    /**
     * Select target considering relationships
     */
    public LivingEntity selectTargetWithRelationships(List<LivingEntity> potentialTargets, 
                                                    VillagerEntity villager, 
                                                    CombatPersonalityTraits traits, 
                                                    RelationshipData relationships) {
        // Filter out friends first
        List<LivingEntity> validTargets = potentialTargets.stream()
            .filter(entity -> !relationships.isFriend(entity.getUUID()))
            .collect(Collectors.toList());
        
        return selectTarget(validTargets, villager, traits);
    }
    
    /**
     * Determine if villager should attack based on threat and personality
     */
    public boolean shouldAttack(ThreatAssessment threat, 
                              CombatPersonalityTraits traits, 
                              VillagerEntity villager) {
        // Calculate aggression threshold - higher aggression/courage = more likely to attack
        float attackThreshold = 0.8f - (traits.getAggression() * 0.4f) - (traits.getCourage() * 0.3f);
        
        // Attack if threat score exceeds our threshold
        return threat.getThreatScore() > attackThreshold;
    }
    
    /**
     * Determine if villager should defend an ally
     */
    public boolean shouldDefend(LivingEntity ally, 
                              CombatPersonalityTraits traits, 
                              RelationshipData relationships) {
        if (!ally.isBeingAttacked()) {
            return false;
        }
        
        RelationshipLevel relationship = relationships.getRelationshipLevel(ally.getUUID());
        float loyaltyThreshold = 0.5f;
        
        // Adjust threshold based on relationship
        switch (relationship) {
            case CLOSE_FRIEND:
            case FAMILY:
                loyaltyThreshold = 0.3f;
                break;
            case FRIEND:
                loyaltyThreshold = 0.4f;
                break;
            default:
                loyaltyThreshold = 0.7f;
                break;
        }
        
        return traits.getLoyalty() > loyaltyThreshold;
    }
    
    /**
     * Determine if villager should flee from threat
     */
    public boolean shouldFlee(ThreatAssessment threat, 
                            CombatPersonalityTraits traits, 
                            VillagerEntity villager) {
        // Calculate flee threshold based on personality
        // Very high courage villagers (>0.8) should almost never flee even from extreme threats
        if (traits.getCourage() > 0.8f) {
            return false; // Extremely courageous villagers never flee
        }
        
        // For others, calculate based on the balance of self-preservation vs courage
        float fleeThreshold = traits.getSelfPreservation() - (traits.getCourage() * 0.8f);
        fleeThreshold = Math.max(0.1f, fleeThreshold); // Minimum threshold
        
        return threat.getThreatScore() > (0.7f + fleeThreshold * 0.3f);
    }
    
    /**
     * Get valid combat targets excluding friends
     */
    public List<LivingEntity> getValidCombatTargets(List<LivingEntity> allTargets, 
                                                  RelationshipData relationships, 
                                                  VillagerEntity villager) {
        return allTargets.stream()
            .filter(entity -> !relationships.isFriend(entity.getUUID()))
            .filter(LivingEntity::isAlive)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate threat score for an enemy
     */
    public float calculateThreatScore(LivingEntity enemy, 
                                    VillagerEntity villager, 
                                    CombatPersonalityTraits traits) {
        if (!enemy.isAlive()) {
            return 0.0f;
        }
        
        float healthFactor = enemy.getHealth() / 20.0f; // Assume max health of 20
        float damageFactor = enemy.getAttackDamage() / 10.0f; // Normalize damage
        float distanceFactor = Math.max(0.1f, 1.0f - (enemy.getDistanceTo(villager) / 20.0f));
        
        float threatScore = (healthFactor * 0.3f) + (damageFactor * 0.5f) + (distanceFactor * 0.2f);
        
        return Math.max(0.0f, Math.min(1.0f, threatScore));
    }
    
    /**
     * Calculate weapon score based on personality preferences
     */
    private float calculateWeaponScore(ItemStack weapon, CombatPersonalityTraits traits) {
        float baseScore = weapon.getAttackDamage() / 10.0f; // Normalize damage
        
        // Personality-based preferences
        switch (weapon.getItemType()) {
            case SWORD:
                // Aggressive personalities prefer swords
                baseScore += traits.getAggression() * 0.3f;
                baseScore += traits.getCourage() * 0.2f;
                break;
            case BOW:
                // Self-preserving personalities prefer ranged weapons
                baseScore += traits.getSelfPreservation() * 0.4f;
                baseScore -= traits.getAggression() * 0.1f; // Less aggressive prefer ranged
                break;
            case AXE:
                // Very aggressive personalities like axes
                baseScore += traits.getAggression() * 0.4f;
                baseScore += traits.getVengefulness() * 0.2f;
                break;
            default:
                break;
        }
        
        return Math.max(0.0f, Math.min(1.0f, baseScore));
    }
}