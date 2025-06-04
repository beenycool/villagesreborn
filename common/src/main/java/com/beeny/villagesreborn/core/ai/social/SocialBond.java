package com.beeny.villagesreborn.core.ai.social;

import com.beeny.villagesreborn.core.ai.memory.RelationshipType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a social bond between two villagers
 */
public class SocialBond {
    private final UUID villagerUUID;
    private final String villagerName;
    private RelationshipType bondType;
    private float bondStrength;
    private final LocalDateTime formationDate;
    private LocalDateTime lastInteraction;
    private final String formationContext;
    private int sharedExperienceCount = 0;
    private float emotionalInvestment = 0.0f;
    
    public SocialBond(UUID villagerUUID, String villagerName, RelationshipType bondType,
                     float bondStrength, LocalDateTime formationDate, String formationContext) {
        this.villagerUUID = villagerUUID;
        this.villagerName = villagerName;
        this.bondType = bondType;
        this.bondStrength = Math.max(-1.0f, Math.min(1.0f, bondStrength));
        this.formationDate = formationDate;
        this.lastInteraction = formationDate;
        this.formationContext = formationContext;
    }
    
    /**
     * Adjusts the strength of this bond
     */
    public void adjustStrength(float adjustment) {
        this.bondStrength = Math.max(-1.0f, Math.min(1.0f, bondStrength + adjustment));
        updateLastInteraction();
    }
    
    /**
     * Updates the bond type as the relationship evolves
     */
    public void evolveBondType(RelationshipType newType) {
        this.bondType = newType;
        updateLastInteraction();
    }
    
    /**
     * Records a shared experience that affects this bond
     */
    public void recordSharedExperience(float emotionalImpact) {
        this.sharedExperienceCount++;
        this.emotionalInvestment = Math.max(0.0f, Math.min(1.0f, emotionalInvestment + emotionalImpact * 0.1f));
        adjustStrength(emotionalImpact * 0.2f);
    }
    
    /**
     * Processes time-based decay of the bond
     */
    public void processTimeDecay(long daysPassed) {
        // Some bond types are exempt from decay
        if (bondType == RelationshipType.FAMILY || bondType == RelationshipType.SPOUSE) {
            // Family and spouse bonds do not decay
            return;
        }
        
        if (daysPassed > 14) {
            // Bonds weaken slightly over time without interaction
            float decayAmount = -0.01f * (daysPassed - 14);
            adjustStrength(decayAmount);
        }
    }
    
    /**
     * Calculates the overall relationship quality
     */
    public float getRelationshipQuality() {
        float base = Math.abs(bondStrength);
        float experienceBonus = Math.min(0.3f, sharedExperienceCount * 0.02f);
        float investmentBonus = emotionalInvestment * 0.2f;
        
        return Math.min(1.0f, base + experienceBonus + investmentBonus);
    }
    
    /**
     * Determines if this bond is positive or negative
     */
    public boolean isPositiveBond() {
        return bondStrength > 0.0f;
    }
    
    /**
     * Determines if this is a strong bond (high emotional investment)
     */
    public boolean isStrongBond() {
        return Math.abs(bondStrength) > 0.6f && emotionalInvestment > 0.4f;
    }
    
    private void updateLastInteraction() {
        this.lastInteraction = LocalDateTime.now();
    }
    
    // Getters
    public UUID getVillagerUUID() { return villagerUUID; }
    public String getVillagerName() { return villagerName; }
    public RelationshipType getBondType() { return bondType; }
    public float getBondStrength() { return bondStrength; }
    public LocalDateTime getFormationDate() { return formationDate; }
    public LocalDateTime getLastInteraction() { return lastInteraction; }
    public String getFormationContext() { return formationContext; }
    public int getSharedExperienceCount() { return sharedExperienceCount; }
    public float getEmotionalInvestment() { return emotionalInvestment; }
    
    @Override
    public String toString() {
        return String.format("SocialBond[%s(%s): %.2f strength, %s]", 
            villagerName, bondType.name(), bondStrength, 
            isPositiveBond() ? "positive" : "negative");
    }
}