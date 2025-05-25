package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;

/**
 * Minimal HagglingEngine implementation for price calculation with relationship and reputation modifiers
 */
public class HagglingEngine {
    
    public HagglingEngine() {}
    
    public double calculatePrice(double basePrice, RelationshipData relationship, ReputationData reputation) {
        double modifier = 1.0;
        
        // Trust modifier: 0.0 is neutral, positive reduces price, negative increases price
        float trustLevel = relationship.getTrustLevel();
        modifier -= trustLevel * 0.3; // Max 30% reduction/increase from trust
        
        // Friendship modifier: 0.0 is neutral, positive reduces price, negative increases price
        float friendshipLevel = relationship.getFriendshipLevel();
        modifier -= friendshipLevel * 0.2; // Max 20% reduction/increase from friendship
        
        // Reputation modifier: 0.0 is neutral, positive reduces price, negative increases price
        float reputationLevel = reputation.getReputationLevel();
        modifier -= reputationLevel * 0.1; // Max 10% reduction from reputation
        
        double calculatedPrice = basePrice * modifier;
        
        // Enforce floor: minimum 10% of base price
        calculatedPrice = Math.max(calculatedPrice, basePrice * 0.1);
        
        // Enforce ceiling: maximum 200% of base price
        calculatedPrice = Math.min(calculatedPrice, basePrice * 2.0);
        
        return calculatedPrice;
    }
    
    public void updateRapportAfterRound(RelationshipData relationship, boolean successful) {
        if (successful) {
            relationship.adjustTrust(0.05f);
            relationship.adjustFriendship(0.02f);
        } else {
            relationship.adjustTrust(-0.03f);
            relationship.adjustFriendship(-0.01f);
        }
        relationship.incrementInteractionCount();
    }
}