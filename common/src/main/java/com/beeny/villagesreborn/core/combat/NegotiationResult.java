package com.beeny.villagesreborn.core.combat;

/**
 * Result of a negotiation round containing outcome and potential counter-offer
 */
public class NegotiationResult {
    private final NegotiationOutcome outcome;
    private final double counterOffer;
    
    public NegotiationResult(NegotiationOutcome outcome) {
        this.outcome = outcome;
        this.counterOffer = 0.0;
    }
    
    public NegotiationResult(NegotiationOutcome outcome, double counterOffer) {
        this.outcome = outcome;
        this.counterOffer = counterOffer;
    }
    
    public NegotiationOutcome getOutcome() {
        return outcome;
    }
    
    public double getCounterOffer() {
        return counterOffer;
    }
}