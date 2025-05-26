package com.beeny.villagesreborn.core.combat;

import java.util.List;
import java.util.UUID;

/**
 * Represents a combat decision made by AI or rule-based systems.
 * Contains action type, target priorities, weapon selection, and reasoning.
 */
public class CombatDecision {
    
    /**
     * The primary combat action to take
     */
    public enum CombatAction {
        ATTACK,
        DEFEND, 
        FLEE,
        NEGOTIATE
    }
    
    private final CombatAction action;
    private final List<UUID> targetPriority;
    private final ItemStack preferredWeapon;
    private final String tacticalReasoning;
    private final float confidenceScore;
    private final boolean isFallbackDecision;
    
    /**
     * Constructor for AI-generated decisions
     */
    public CombatDecision(CombatAction action, List<UUID> targetPriority, 
                         ItemStack preferredWeapon, String tacticalReasoning, 
                         float confidenceScore) {
        this.action = action;
        this.targetPriority = targetPriority;
        this.preferredWeapon = preferredWeapon;
        this.tacticalReasoning = tacticalReasoning;
        this.confidenceScore = Math.max(0.0f, Math.min(1.0f, confidenceScore));
        this.isFallbackDecision = false;
    }
    
    /**
     * Constructor for fallback decisions
     */
    public CombatDecision(CombatAction action, List<UUID> targetPriority, 
                         ItemStack preferredWeapon) {
        this.action = action;
        this.targetPriority = targetPriority;
        this.preferredWeapon = preferredWeapon;
        this.tacticalReasoning = "Rule-based decision";
        this.confidenceScore = 0.5f;
        this.isFallbackDecision = true;
    }
    
    public CombatAction getAction() {
        return action;
    }
    
    public List<UUID> getTargetPriority() {
        return targetPriority;
    }
    
    public ItemStack getPreferredWeapon() {
        return preferredWeapon;
    }
    
    public String getTacticalReasoning() {
        return tacticalReasoning;
    }
    
    public float getConfidenceScore() {
        return confidenceScore;
    }
    
    public boolean isFallbackDecision() {
        return isFallbackDecision;
    }
    
    /**
     * Validates that this decision is coherent and executable
     */
    public boolean isValid() {
        if (action == null) return false;
        if (action == CombatAction.ATTACK && (targetPriority == null || targetPriority.isEmpty())) {
            return false;
        }
        return confidenceScore >= 0.0f && confidenceScore <= 1.0f;
    }
    
    @Override
    public String toString() {
        return String.format("CombatDecision{action=%s, targets=%d, weapon=%s, confidence=%.2f, fallback=%s}", 
                           action, 
                           targetPriority != null ? targetPriority.size() : 0,
                           preferredWeapon != null ? preferredWeapon.getItemType() : "none",
                           confidenceScore,
                           isFallbackDecision);
    }
}