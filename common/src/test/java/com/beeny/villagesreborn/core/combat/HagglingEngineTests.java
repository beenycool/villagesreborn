package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test-driven development for the HagglingEngine
 * Tests price calculation with relationship and reputation modifiers
 */
public class HagglingEngineTests {
    
    private HagglingEngine hagglingEngine;
    private RelationshipData baseRelationship;
    private ReputationData baseReputation;
    
    @BeforeEach
    void setUp() {
        hagglingEngine = new HagglingEngine();
        baseRelationship = new RelationshipData();
        baseReputation = new ReputationData();
    }
    
    @Test
    void calculatePrice_withNeutralRelationship_returnsBasePrice() {
        // Given: Base price and neutral relationships
        double basePrice = 100.0;
        
        // When: Calculate price with neutral modifiers
        double calculatedPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Should return base price (no modifiers)
        assertEquals(100.0, calculatedPrice, 0.01);
    }
    
    @Test
    void calculatePrice_withHighTrust_reducesPrice() {
        // Given: High trust relationship
        baseRelationship.adjustTrust(0.8f);
        double basePrice = 100.0;
        
        // When: Calculate price with high trust
        double calculatedPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should be reduced due to trust
        assertTrue(calculatedPrice < basePrice, "High trust should reduce price");
        assertTrue(calculatedPrice >= basePrice * 0.7, "Price reduction should not exceed 30%");
    }
    
    @Test
    void calculatePrice_withLowTrust_increasesPrice() {
        // Given: Low trust relationship
        baseRelationship.adjustTrust(-0.8f);
        double basePrice = 100.0;
        
        // When: Calculate price with low trust
        double calculatedPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should be increased due to low trust
        assertTrue(calculatedPrice > basePrice, "Low trust should increase price");
        assertTrue(calculatedPrice <= basePrice * 1.5, "Price increase should not exceed 50%");
    }
    
    @Test
    void calculatePrice_withHighReputation_reducesPrice() {
        // Given: High reputation
        baseReputation.adjustReputation(0.9f);
        double basePrice = 100.0;
        
        // When: Calculate price with high reputation
        double calculatedPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should be reduced due to reputation
        assertTrue(calculatedPrice < basePrice, "High reputation should reduce price");
    }
    
    @Test
    void calculatePrice_enforcesMinimumFloor() {
        // Given: Extremely high trust and reputation that would make price negative
        baseRelationship.adjustTrust(1.0f);
        baseRelationship.adjustFriendship(1.0f);
        baseReputation.adjustReputation(1.0f);
        double basePrice = 10.0;
        
        // When: Calculate price with maximum modifiers
        double calculatedPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should never be zero or negative
        assertTrue(calculatedPrice > 0, "Price should never be zero or negative");
        assertTrue(calculatedPrice >= basePrice * 0.1, "Price should maintain minimum 10% floor");
    }
    
    @Test
    void calculatePrice_enforcesCeiling() {
        // Given: Very poor relationship and reputation
        baseRelationship.adjustTrust(-1.0f);
        baseRelationship.adjustFriendship(-1.0f);
        baseReputation.adjustReputation(-1.0f);
        double basePrice = 100.0;
        
        // When: Calculate price with worst modifiers
        double calculatedPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should not exceed reasonable ceiling
        assertTrue(calculatedPrice <= basePrice * 2.0, "Price should not exceed 200% of base price");
    }
    
    @Test
    void multiRoundNegotiation_updatesRapportAndPrice() {
        // Given: Initial conditions
        double basePrice = 100.0;
        
        // When: First round of negotiation
        double firstPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        hagglingEngine.updateRapportAfterRound(baseRelationship, true); // Successful round
        
        // And: Second round after rapport improvement
        double secondPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should improve after successful negotiation
        assertTrue(secondPrice <= firstPrice, "Price should improve or stay same after successful negotiation");
    }
    
    @Test
    void multiRoundNegotiation_degradesRapportOnFailure() {
        // Given: Initial high trust
        baseRelationship.adjustTrust(0.5f);
        double basePrice = 100.0;
        
        // When: Failed negotiation round
        double firstPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        hagglingEngine.updateRapportAfterRound(baseRelationship, false); // Failed round
        
        // And: Second round after rapport degradation
        double secondPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should worsen after failed negotiation
        assertTrue(secondPrice >= firstPrice, "Price should worsen or stay same after failed negotiation");
    }
    
    @Test
    void calculatePrice_combinesMultipleFactors() {
        // Given: Mixed relationship factors
        baseRelationship.adjustTrust(0.3f);
        baseRelationship.adjustFriendship(-0.2f);
        baseReputation.adjustReputation(0.5f);
        double basePrice = 100.0;
        
        // When: Calculate price with mixed factors
        double calculatedPrice = hagglingEngine.calculatePrice(basePrice, baseRelationship, baseReputation);
        
        // Then: Price should reflect combined impact
        assertNotEquals(basePrice, calculatedPrice, "Mixed factors should modify price");
        assertTrue(calculatedPrice > 0, "Price should remain positive");
    }
}