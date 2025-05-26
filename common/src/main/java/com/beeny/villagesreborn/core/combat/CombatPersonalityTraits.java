package com.beeny.villagesreborn.core.combat;

/**
 * Defines personality traits that affect combat behavior
 */
public class CombatPersonalityTraits {
    
    public enum AggressionLevel {
        PACIFIST,     // Avoids combat whenever possible
        DEFENSIVE,    // Only fights when threatened
        BALANCED,     // Normal combat behavior
        AGGRESSIVE,   // Seeks out conflicts
        HOSTILE       // Attacks on sight
    }
    
    public enum CombatStyle {
        CAUTIOUS,     // Careful, methodical fighting
        BALANCED,     // Standard combat approach
        RECKLESS,     // High-risk, high-reward tactics
        STRATEGIC,    // Focuses on tactics and positioning
        BERSERKER     // All-out aggressive attacks
    }
    
    public enum TeamworkTendency {
        LONE_WOLF,    // Prefers to fight alone
        RELUCTANT,    // Will work with others if necessary
        COOPERATIVE,  // Works well in groups
        LEADER,       // Takes charge in group combat
        FOLLOWER      // Follows others' lead
    }
    
    private AggressionLevel aggressionLevel;
    private CombatStyle combatStyle;
    private TeamworkTendency teamworkTendency;
    private float courage; // 0.0 to 1.0, affects willingness to engage
    private float loyalty; // 0.0 to 1.0, affects protection of allies
    private float aggression; // 0.0 to 1.0, direct aggression value
    private float selfPreservation; // 0.0 to 1.0, self-preservation instinct
    private float vengefulness; // 0.0 to 1.0, tendency for revenge
    
    // Default constructor for compatibility
    public CombatPersonalityTraits() {
        this.aggressionLevel = AggressionLevel.BALANCED;
        this.combatStyle = CombatStyle.BALANCED;
        this.teamworkTendency = TeamworkTendency.COOPERATIVE;
        this.courage = 0.5f;
        this.loyalty = 0.7f;
        this.aggression = 0.5f;
        this.selfPreservation = 0.5f;
        this.vengefulness = 0.5f;
    }
    
    public CombatPersonalityTraits(AggressionLevel aggressionLevel, CombatStyle combatStyle, 
                                 TeamworkTendency teamworkTendency, float courage, float loyalty) {
        this.aggressionLevel = aggressionLevel;
        this.combatStyle = combatStyle;
        this.teamworkTendency = teamworkTendency;
        this.courage = Math.max(0.0f, Math.min(1.0f, courage));
        this.loyalty = Math.max(0.0f, Math.min(1.0f, loyalty));
        this.aggression = convertAggressionLevelToFloat(aggressionLevel);
        this.selfPreservation = 0.5f;
        this.vengefulness = 0.5f;
    }
    
    public AggressionLevel getAggressionLevel() {
        return aggressionLevel;
    }
    
    public CombatStyle getCombatStyle() {
        return combatStyle;
    }
    
    public TeamworkTendency getTeamworkTendency() {
        return teamworkTendency;
    }
    
    public float getCourage() {
        return courage;
    }
    
    public void setCourage(float courage) {
        this.courage = Math.max(0.0f, Math.min(1.0f, courage));
    }
    
    public float getLoyalty() {
        return loyalty;
    }
    
    public void setLoyalty(float loyalty) {
        this.loyalty = Math.max(0.0f, Math.min(1.0f, loyalty));
    }
    
    public float getAggression() {
        return aggression;
    }
    
    public void setAggression(float aggression) {
        this.aggression = Math.max(0.0f, Math.min(1.0f, aggression));
        this.aggressionLevel = convertFloatToAggressionLevel(aggression);
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
    
    private float convertAggressionLevelToFloat(AggressionLevel level) {
        return switch (level) {
            case PACIFIST -> 0.1f;
            case DEFENSIVE -> 0.3f;
            case BALANCED -> 0.5f;
            case AGGRESSIVE -> 0.7f;
            case HOSTILE -> 0.9f;
        };
    }
    
    private AggressionLevel convertFloatToAggressionLevel(float aggression) {
        if (aggression <= 0.2f) return AggressionLevel.PACIFIST;
        if (aggression <= 0.4f) return AggressionLevel.DEFENSIVE;
        if (aggression <= 0.6f) return AggressionLevel.BALANCED;
        if (aggression <= 0.8f) return AggressionLevel.AGGRESSIVE;
        return AggressionLevel.HOSTILE;
    }
    
    /**
     * Determines if this personality would engage in combat given the threat level
     */
    public boolean wouldEngage(ThreatLevel threatLevel) {
        float engagementThreshold = switch (aggressionLevel) {
            case PACIFIST -> 0.9f;
            case DEFENSIVE -> 0.7f;
            case BALANCED -> 0.5f;
            case AGGRESSIVE -> 0.3f;
            case HOSTILE -> 0.1f;
        };
        
        float threatValue = switch (threatLevel) {
            case NONE -> 0.0f;
            case LOW -> 0.3f;
            case MODERATE -> 0.6f;
            case HIGH -> 0.8f;
            case EXTREME -> 1.0f;
        };
        
        return (threatValue * courage) >= engagementThreshold;
    }
    
    /**
     * Gets the combat effectiveness modifier based on style and situation
     */
    public float getCombatModifier(boolean hasAllies, boolean isOutnumbered) {
        float styleModifier = switch (combatStyle) {
            case CAUTIOUS -> isOutnumbered ? 1.2f : 0.9f;
            case BALANCED -> 1.0f;
            case RECKLESS -> isOutnumbered ? 0.8f : 1.3f;
            case STRATEGIC -> hasAllies ? 1.2f : 1.0f;
            case BERSERKER -> isOutnumbered ? 1.4f : 1.1f;
        };
        
        float teamworkModifier = switch (teamworkTendency) {
            case LONE_WOLF -> hasAllies ? 0.9f : 1.1f;
            case RELUCTANT -> hasAllies ? 0.95f : 1.0f;
            case COOPERATIVE -> hasAllies ? 1.1f : 0.9f;
            case LEADER -> hasAllies ? 1.2f : 1.0f;
            case FOLLOWER -> hasAllies ? 1.05f : 0.8f;
        };
        
        return styleModifier * teamworkModifier;
    }
    
    @Override
    public String toString() {
        return String.format("CombatPersonality{aggression=%s, style=%s, teamwork=%s, courage=%.2f, loyalty=%.2f}", 
                           aggressionLevel, combatStyle, teamworkTendency, courage, loyalty);
    }
    
    /**
     * Creates a default balanced personality
     */
    public static CombatPersonalityTraits createDefault() {
        return new CombatPersonalityTraits(
            AggressionLevel.BALANCED, 
            CombatStyle.BALANCED, 
            TeamworkTendency.COOPERATIVE, 
            0.5f, 
            0.7f
        );
    }
    
    /**
     * Creates a peaceful personality that avoids combat
     */
    public static CombatPersonalityTraits createPacifist() {
        return new CombatPersonalityTraits(
            AggressionLevel.PACIFIST, 
            CombatStyle.CAUTIOUS, 
            TeamworkTendency.FOLLOWER, 
            0.2f, 
            0.8f
        );
    }
    
    /**
     * Creates an aggressive warrior personality
     */
    public static CombatPersonalityTraits createWarrior() {
        return new CombatPersonalityTraits(
            AggressionLevel.AGGRESSIVE, 
            CombatStyle.STRATEGIC, 
            TeamworkTendency.LEADER, 
            0.8f, 
            0.6f
        );
    }
}