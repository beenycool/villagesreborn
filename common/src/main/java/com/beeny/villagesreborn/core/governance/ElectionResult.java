package com.beeny.villagesreborn.core.governance;

import java.util.UUID;

/**
 * Result of an election including winner and outcome type
 */
public class ElectionResult {
    private UUID winnerId;
    private double winningVoteCount;
    private ElectionOutcome outcome;
    
    public ElectionResult() {
        // Default constructor for stub
    }
    
    public ElectionResult(UUID winnerId, double winningVoteCount, ElectionOutcome outcome) {
        this.winnerId = winnerId;
        this.winningVoteCount = winningVoteCount;
        this.outcome = outcome;
    }
    
    public UUID getWinnerId() {
        return winnerId;
    }
    
    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }
    
    public double getWinningVoteCount() {
        return winningVoteCount;
    }
    
    public void setWinningVoteCount(double winningVoteCount) {
        this.winningVoteCount = winningVoteCount;
    }
    
    public ElectionOutcome getOutcome() {
        return outcome;
    }
    
    public void setOutcome(ElectionOutcome outcome) {
        this.outcome = outcome;
    }
}