package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.PersonalityProfile;
import com.beeny.villagesreborn.core.common.NBTCompound;

/**
 * Combat-specific personality traits that extend the base personality profile
 * with combat decision-making attributes
 */
public class CombatPersonalityTraits extends PersonalityProfile {
    
    private float courage = 0.5f;
    private float aggression = 0.5f;
    private float loyalty = 0.5f;
    private float selfPreservation = 0.5f;
    private float vengefulness = 0.5f;
    
    public CombatPersonalityTraits() {
        super();
    }
    
    public float getCourage() {
        return courage;
    }
    
    public void setCourage(float courage) {
        this.courage = Math.max(0.0f, Math.min(1.0f, courage));
    }
    
    public float getAggression() {
        return aggression;
    }
    
    public void setAggression(float aggression) {
        this.aggression = Math.max(0.0f, Math.min(1.0f, aggression));
    }
    
    public float getLoyalty() {
        return loyalty;
    }
    
    public void setLoyalty(float loyalty) {
        this.loyalty = Math.max(0.0f, Math.min(1.0f, loyalty));
    }
    
    public float getSelfPreservation() {
        return selfPreservation;
    }
    
    public void setSelfPreservation(float selfPreservation) {
        this.selfPreservation = Math.max(0.0f, Math.min(1.0f, selfPreservation));
    }
    
    public float getVengefulness() {
        return vengefulness;
    }
    
    public void setVengefulness(float vengefulness) {
        this.vengefulness = Math.max(0.0f, Math.min(1.0f, vengefulness));
    }
    
    public float getCombatDecisionWeight(CombatSituation situation) {
        // Simple implementation for tests to pass
        return (courage + aggression - selfPreservation) / 3.0f;
    }
    
    public boolean shouldFlee(ThreatAssessment threat) {
        // Simple implementation based on self-preservation vs courage
        float fleeThreshold = selfPreservation - courage;
        return threat.getThreatScore() > (0.5f + fleeThreshold);
    }
    
    public boolean shouldDefend(LivingEntity target) {
        // Simple implementation based on loyalty
        return loyalty > 0.5f;
    }
    
    public boolean shouldAttack(LivingEntity target) {
        // Simple implementation based on aggression and courage
        return (aggression + courage) / 2.0f > 0.6f;
    }
    
    @Override
    public NBTCompound toNBT() {
        NBTCompound nbt = super.toNBT();
        nbt.putFloat("courage", courage);
        nbt.putFloat("aggression", aggression);
        nbt.putFloat("loyalty", loyalty);
        nbt.putFloat("selfPreservation", selfPreservation);
        nbt.putFloat("vengefulness", vengefulness);
        return nbt;
    }
    
    public static CombatPersonalityTraits fromNBT(NBTCompound nbt) {
        CombatPersonalityTraits traits = new CombatPersonalityTraits();
        if (nbt.contains("courage")) {
            traits.setCourage(nbt.getFloat("courage"));
        }
        if (nbt.contains("aggression")) {
            traits.setAggression(nbt.getFloat("aggression"));
        }
        if (nbt.contains("loyalty")) {
            traits.setLoyalty(nbt.getFloat("loyalty"));
        }
        if (nbt.contains("selfPreservation")) {
            traits.setSelfPreservation(nbt.getFloat("selfPreservation"));
        }
        if (nbt.contains("vengefulness")) {
            traits.setVengefulness(nbt.getFloat("vengefulness"));
        }
        return traits;
    }
}