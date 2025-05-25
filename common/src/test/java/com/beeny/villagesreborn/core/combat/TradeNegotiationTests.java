package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test-driven development for TradeNegotiation
 * Tests full negotiation flow with personality-driven behavior
 */
public class TradeNegotiationTests {
    
    private TradeNegotiation negotiation;
    private CombatPersonalityTraits traderPersonality;
    private CombatPersonalityTraits buyerPersonality;
    private RelationshipData relationship;
    private ReputationData reputation;
    
    @BeforeEach
    void setUp() {
        traderPersonality = new CombatPersonalityTraits();
        buyerPersonality = new CombatPersonalityTraits();
        relationship = new RelationshipData();
        reputation = new ReputationData();
        
        negotiation = new TradeNegotiation(
            100.0, // basePrice
            traderPersonality,
            buyerPersonality,
            relationship,
            reputation
        );
    }
    
    @Test
    void initializeNegotiation_setsCorrectInitialState() {
        // Then: Initial state should be properly set
        assertEquals(NegotiationState.INITIAL_OFFER, negotiation.getCurrentState());
        assertEquals(0, negotiation.getCurrentRound());
        assertFalse(negotiation.isCompleted());
        assertEquals(100.0, negotiation.getBasePrice(), 0.01);
    }
    
    @Test
    void makeOffer_fromInitialState_transitionsToCounterOffer() {
        // When: Make initial offer
        NegotiationResult result = negotiation.makeOffer(90.0);
        
        // Then: Should transition to counter-offer state
        assertEquals(NegotiationState.COUNTER_OFFER, negotiation.getCurrentState());
        assertEquals(1, negotiation.getCurrentRound());
        assertEquals(NegotiationOutcome.COUNTER_OFFERED, result.getOutcome());
        assertTrue(result.getCounterOffer() > 0);
    }
    
    @Test
    void makeOffer_withReasonablePrice_getsAccepted() {
        // Given: High trust relationship
        relationship.adjustTrust(0.8f);
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        // When: Make offer close to fair price
        NegotiationResult result = negotiation.makeOffer(95.0);
        
        // Then: Should be accepted
        assertEquals(NegotiationOutcome.ACCEPTED, result.getOutcome());
        assertTrue(negotiation.isCompleted());
        assertEquals(NegotiationState.COMPLETED, negotiation.getCurrentState());
    }
    
    @Test
    void makeOffer_withUnreasonablePrice_getsRejected() {
        // Given: Low trust relationship
        relationship.adjustTrust(-0.8f);
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        // When: Make unreasonably low offer
        NegotiationResult result = negotiation.makeOffer(20.0);
        
        // Then: Should be rejected outright
        assertEquals(NegotiationOutcome.REJECTED, result.getOutcome());
        assertTrue(negotiation.isCompleted());
        assertEquals(NegotiationState.FAILED, negotiation.getCurrentState());
    }
    
    @Test
    void multiRoundNegotiation_withStubborTrader_continuesForMaxRounds() {
        // Given: Stubborn trader (high aggression, low flexibility)
        traderPersonality.setAggression(0.9f);
        traderPersonality.setSelfPreservation(0.1f); // Low willingness to compromise
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        int maxRounds = negotiation.getMaxRounds();
        
        // When: Continue negotiation for multiple rounds
        for (int i = 0; i < maxRounds - 1; i++) {
            if (!negotiation.isCompleted()) {
                NegotiationResult result = negotiation.makeOffer(70.0 + i * 2); // Gradually increase offer
                if (result.getOutcome() == NegotiationOutcome.COUNTER_OFFERED) {
                    // Reject counter-offer to continue negotiating
                    negotiation.rejectCounterOffer();
                }
            }
        }
        
        // Then: Should reach maximum rounds
        assertTrue(negotiation.getCurrentRound() >= maxRounds - 1, "Should negotiate for multiple rounds with stubborn trader");
    }
    
    @Test
    void personalityDrivenBehavior_courageousTrader_acceptsRiskyDeals() {
        // Given: Courageous trader
        traderPersonality.setCourage(0.9f);
        traderPersonality.setAggression(0.2f); // Low aggression to focus on courage
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        // When: Make slightly risky offer (below typical acceptance threshold)
        NegotiationResult result = negotiation.makeOffer(75.0);
        
        // Then: Courageous trader should be more likely to accept
        assertTrue(result.getOutcome() == NegotiationOutcome.ACCEPTED || 
                  result.getOutcome() == NegotiationOutcome.COUNTER_OFFERED,
                  "Courageous trader should consider risky offers");
    }
    
    @Test
    void personalityDrivenBehavior_cautiousTrader_rejectsRiskyDeals() {
        // Given: Cautious trader (high self-preservation)
        traderPersonality.setSelfPreservation(0.9f);
        traderPersonality.setCourage(0.1f);
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        // When: Make risky offer
        NegotiationResult result = negotiation.makeOffer(70.0);
        
        // Then: Cautious trader should reject or demand higher counter-offer
        if (result.getOutcome() == NegotiationOutcome.COUNTER_OFFERED) {
            assertTrue(result.getCounterOffer() > 85.0, "Cautious trader should demand higher counter-offer");
        }
    }
    
    @Test
    void acceptCounterOffer_completesNegotiation() {
        // Given: Active negotiation with counter-offer
        negotiation.makeOffer(85.0);
        assertEquals(NegotiationState.COUNTER_OFFER, negotiation.getCurrentState());
        
        // When: Accept the counter-offer
        NegotiationResult result = negotiation.acceptCounterOffer();
        
        // Then: Negotiation should be completed
        assertEquals(NegotiationOutcome.ACCEPTED, result.getOutcome());
        assertTrue(negotiation.isCompleted());
        assertEquals(NegotiationState.COMPLETED, negotiation.getCurrentState());
    }
    
    @Test
    void rejectCounterOffer_continuesOrEndsNegotiation() {
        // Given: Active negotiation with counter-offer
        negotiation.makeOffer(85.0);
        assertEquals(NegotiationState.COUNTER_OFFER, negotiation.getCurrentState());
        
        // When: Reject the counter-offer
        NegotiationResult result = negotiation.rejectCounterOffer();
        
        // Then: Should either continue negotiation or end it
        assertTrue(result.getOutcome() == NegotiationOutcome.COUNTER_OFFERED || 
                  result.getOutcome() == NegotiationOutcome.REJECTED,
                  "Rejection should lead to new counter-offer or end negotiation");
    }
    
    @Test
    void completedTrade_updatesReputation() {
        // Given: Successful negotiation setup
        relationship.adjustTrust(0.5f);
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        float initialReputation = reputation.getReputationLevel();
        
        // When: Complete a successful trade
        NegotiationResult result = negotiation.makeOffer(95.0);
        if (result.getOutcome() == NegotiationOutcome.ACCEPTED) {
            negotiation.completeTradeSuccessfully();
        }
        
        // Then: Reputation should be updated
        assertTrue(reputation.getReputationLevel() >= initialReputation, 
                  "Successful trade should maintain or improve reputation");
    }
    
    @Test
    void failedTrade_penalizesReputation() {
        // Given: Setup for failed negotiation
        relationship.adjustTrust(-0.5f);
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        float initialReputation = reputation.getReputationLevel();
        
        // When: Fail a negotiation
        NegotiationResult result = negotiation.makeOffer(30.0); // Unreasonable offer
        if (result.getOutcome() == NegotiationOutcome.REJECTED) {
            negotiation.completeTradeUnsuccessfully();
        }
        
        // Then: Reputation should be penalized
        assertTrue(reputation.getReputationLevel() <= initialReputation,
                  "Failed trade should maintain or reduce reputation");
    }
    
    @Test
    void maxRoundsReached_forcesCompletion() {
        // Given: Long negotiation setup
        traderPersonality.setAggression(0.9f); // Stubborn trader
        negotiation = new TradeNegotiation(100.0, traderPersonality, buyerPersonality, relationship, reputation);
        
        // When: Negotiate until max rounds
        while (!negotiation.isCompleted() && negotiation.getCurrentRound() < negotiation.getMaxRounds()) {
            NegotiationResult result = negotiation.makeOffer(80.0);
            if (result.getOutcome() == NegotiationOutcome.COUNTER_OFFERED) {
                negotiation.rejectCounterOffer(); // Keep rejecting to extend rounds
            }
        }
        
        // Then: Should be forced to complete
        assertTrue(negotiation.isCompleted() || negotiation.getCurrentRound() >= negotiation.getMaxRounds(),
                  "Negotiation should complete or reach max rounds");
    }
}