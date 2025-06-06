package com.beeny.villagesreborn.core.ai.social;

/**
 * Types of conflict resolution between villagers
 */
public enum ConflictResolution {
    PEACEFUL_COMPROMISE(0.3f, "Both parties found a mutually acceptable solution"),
    MEDIATED_AGREEMENT(0.2f, "A third party helped resolve the disagreement"),
    DOMINANCE_ASSERTED(-0.1f, "One party forced their will on the other"),
    MUTUAL_AVOIDANCE(-0.2f, "Both parties chose to avoid each other"),
    ESCALATED_CONFLICT(-0.5f, "The conflict worsened and spread to others"),
    FORGIVENESS(0.4f, "One party chose to forgive and move on"),
    GRUDGE_HELD(-0.3f, "One party holds a lasting resentment"),
    COMMUNITY_INTERVENTION(0.1f, "The village community stepped in to resolve it"),
    NATURAL_RESOLUTION(0.1f, "The conflict resolved itself over time"),
    AUTHORITY_RULING(0.0f, "A village leader made a binding decision");
    
    private final float impactStrength;
    private final String description;
    
    ConflictResolution(float impactStrength, String description) {
        this.impactStrength = impactStrength;
        this.description = description;
    }
    
    public float getImpactStrength() { return impactStrength; }
    public String getDescription() { return description; }
    
    /**
     * Determines if this resolution improves relationships
     */
    public boolean isPositiveResolution() {
        return impactStrength > 0.0f;
    }
    
    /**
     * Determines if this resolution causes lasting damage
     */
    public boolean causesLastingDamage() {
        return impactStrength <= -0.3f;
    }
}