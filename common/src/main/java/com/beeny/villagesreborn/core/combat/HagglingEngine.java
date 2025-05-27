package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;

/**
 * Engine for calculating prices during trade negotiations
 * Factors in relationship, reputation, and haggling dynamics
 */
public class HagglingEngine {
    
    private static final double MIN_PRICE_FLOOR = 0.1; // 10% minimum price
    private static final double MAX_PRICE_CEILING = 2.0; // 200% maximum price
    private static final double TRUST_MODIFIER_WEIGHT = 0.3;
    private static final double FRIENDSHIP_MODIFIER_WEIGHT = 0.2;
    private static final double REPUTATION_MODIFIER_WEIGHT = 0.25;
    
    /**
     * Calculates the final price based on relationship and reputation modifiers
     */
    public double calculatePrice(double basePrice, RelationshipData relationship, ReputationData reputation) {
        if (basePrice <= 0) {
            throw new IllegalArgumentException("Base price must be positive");
        }
        
        double modifier = 1.0;
        
        // Apply trust modifier (negative trust increases price, positive decreases)
        if (relationship != null) {
            modifier -= relationship.getTrustLevel() * TRUST_MODIFIER_WEIGHT;
            modifier -= relationship.getFriendshipLevel() * FRIENDSHIP_MODIFIER_WEIGHT;
        }
        
        // Apply reputation modifier (positive reputation decreases price)
        if (reputation != null) {
            modifier -= reputation.getReputation() * REPUTATION_MODIFIER_WEIGHT;
        }
        
        double finalPrice = basePrice * modifier;
        
        // Apply floor and ceiling constraints
        finalPrice = Math.max(finalPrice, basePrice * MIN_PRICE_FLOOR);
        finalPrice = Math.min(finalPrice, basePrice * MAX_PRICE_CEILING);
        
        return finalPrice;
    }
    
    /**
     * Updates relationship rapport after a negotiation round
     */
    public void updateRapportAfterRound(RelationshipData relationship, boolean successful) {
        if (relationship == null) return;
        
        if (successful) {
            // Improve trust and friendship slightly on successful negotiation
            relationship.adjustTrust(0.05f);
            relationship.adjustFriendship(0.03f);
        } else {
            // Decrease trust slightly on failed negotiation
            relationship.adjustTrust(-0.03f);
        }
    }
}