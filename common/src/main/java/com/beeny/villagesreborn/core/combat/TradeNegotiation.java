package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;

/**
 * Manages full trade negotiation flow with personality-driven behavior
 */
public class TradeNegotiation {
    private final double basePrice;
    private final CombatPersonalityTraits traderPersonality;
    private final CombatPersonalityTraits buyerPersonality;
    private final RelationshipData relationship;
    private final ReputationData reputation;
    private final HagglingEngine hagglingEngine;
    
    private NegotiationState currentState;
    private int currentRound;
    private boolean completed;
    private double lastCounterOffer;
    private final int maxRounds;
    
    public TradeNegotiation(double basePrice, CombatPersonalityTraits traderPersonality, 
                           CombatPersonalityTraits buyerPersonality, RelationshipData relationship, 
                           ReputationData reputation) {
        this.basePrice = basePrice;
        this.traderPersonality = traderPersonality;
        this.buyerPersonality = buyerPersonality;
        this.relationship = relationship;
        this.reputation = reputation;
        this.hagglingEngine = new HagglingEngine();
        
        this.currentState = NegotiationState.INITIAL_OFFER;
        this.currentRound = 0;
        this.completed = false;
        this.lastCounterOffer = 0.0;
        this.maxRounds = calculateMaxRounds();
    }
    
    private int calculateMaxRounds() {
        // Stubborn traders (high aggression) negotiate longer
        float stubbornness = traderPersonality.getAggression() + (1.0f - traderPersonality.getSelfPreservation());
        return Math.max(3, (int)(3 + stubbornness * 4)); // 3-7 rounds based on personality
    }
    
    public NegotiationResult makeOffer(double offer) {
        if (completed) {
            return new NegotiationResult(NegotiationOutcome.REJECTED);
        }
        
        currentRound++;
        
        // Calculate what the trader considers fair
        double fairPrice = hagglingEngine.calculatePrice(basePrice, relationship, reputation);
        double offerRatio = offer / fairPrice;
        
        // Personality-driven decision making
        NegotiationOutcome outcome = evaluateOffer(offerRatio);
        
        switch (outcome) {
            case ACCEPTED:
                currentState = NegotiationState.COMPLETED;
                completed = true;
                hagglingEngine.updateRapportAfterRound(relationship, true);
                return new NegotiationResult(NegotiationOutcome.ACCEPTED);
                
            case REJECTED:
                currentState = NegotiationState.FAILED;
                completed = true;
                hagglingEngine.updateRapportAfterRound(relationship, false);
                return new NegotiationResult(NegotiationOutcome.REJECTED);
                
            case COUNTER_OFFERED:
                currentState = NegotiationState.COUNTER_OFFER;
                lastCounterOffer = generateCounterOffer(offer, fairPrice);
                return new NegotiationResult(NegotiationOutcome.COUNTER_OFFERED, lastCounterOffer);
                
            default:
                return new NegotiationResult(NegotiationOutcome.REJECTED);
        }
    }
    
    private NegotiationOutcome evaluateOffer(double offerRatio) {
        // Check if max rounds reached
        if (currentRound >= maxRounds) {
            return offerRatio >= 0.8 ? NegotiationOutcome.ACCEPTED : NegotiationOutcome.REJECTED;
        }
        
        // Personality-based thresholds
        float courage = traderPersonality.getCourage();
        float selfPreservation = traderPersonality.getSelfPreservation();
        float aggression = traderPersonality.getAggression();
        
        // Courageous traders accept riskier deals (lower offers)
        double acceptThreshold = 0.98 - (courage * 0.1);
        
        // Cautious traders reject more aggressively
        double rejectThreshold = 0.6 + (selfPreservation * 0.2);
        
        if (offerRatio >= acceptThreshold) {
            return NegotiationOutcome.ACCEPTED;
        } else if (offerRatio <= rejectThreshold) {
            return NegotiationOutcome.REJECTED;
        } else {
            return NegotiationOutcome.COUNTER_OFFERED;
        }
    }
    
    private double generateCounterOffer(double offer, double fairPrice) {
        // Generate counter-offer based on personality
        float aggression = traderPersonality.getAggression();
        
        // Aggressive traders make higher counter-offers
        double counterMultiplier = 0.9 + (aggression * 0.15);
        return Math.min(fairPrice * counterMultiplier, basePrice * 1.2);
    }
    
    public NegotiationResult acceptCounterOffer() {
        if (currentState != NegotiationState.COUNTER_OFFER) {
            return new NegotiationResult(NegotiationOutcome.REJECTED);
        }
        
        currentState = NegotiationState.COMPLETED;
        completed = true;
        hagglingEngine.updateRapportAfterRound(relationship, true);
        return new NegotiationResult(NegotiationOutcome.ACCEPTED);
    }
    
    public NegotiationResult rejectCounterOffer() {
        if (currentState != NegotiationState.COUNTER_OFFER) {
            return new NegotiationResult(NegotiationOutcome.REJECTED);
        }
        
        // Check if we should continue or end negotiation
        if (currentRound >= maxRounds - 1) {
            currentState = NegotiationState.FAILED;
            completed = true;
            return new NegotiationResult(NegotiationOutcome.REJECTED);
        }
        
        // Continue negotiation - generate new counter-offer
        double fairPrice = hagglingEngine.calculatePrice(basePrice, relationship, reputation);
        lastCounterOffer = generateCounterOffer(lastCounterOffer * 0.95, fairPrice);
        return new NegotiationResult(NegotiationOutcome.COUNTER_OFFERED, lastCounterOffer);
    }
    
    public void completeTradeSuccessfully() {
        reputation.adjustReputation(0.05f);
        hagglingEngine.updateRapportAfterRound(relationship, true);
    }
    
    public void completeTradeUnsuccessfully() {
        reputation.adjustReputation(-0.02f);
        hagglingEngine.updateRapportAfterRound(relationship, false);
    }
    
    // Getters
    public NegotiationState getCurrentState() { return currentState; }
    public int getCurrentRound() { return currentRound; }
    public boolean isCompleted() { return completed; }
    public double getBasePrice() { return basePrice; }
    public int getMaxRounds() { return maxRounds; }
}