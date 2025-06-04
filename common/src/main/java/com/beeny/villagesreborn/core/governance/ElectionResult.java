package com.beeny.villagesreborn.core.governance;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Represents the results of a village election
 */
public class ElectionResult {
    private UUID winner;
    private Map<UUID, List<Vote>> candidateVotes;
    private List<VotingDecision> votingDecisions;
    private ElectionType electionType;
    private long timestamp;
    private double winningVoteCount;
    private ElectionOutcome outcome;
    
    public ElectionResult() {
        this.candidateVotes = new HashMap<>();
        this.votingDecisions = new ArrayList<>();
        this.electionType = ElectionType.MAYOR;
        this.timestamp = System.currentTimeMillis();
    }
    
    public ElectionResult(UUID winner, Map<UUID, List<Vote>> candidateVotes, 
                         List<VotingDecision> votingDecisions, ElectionType electionType) {
        this.winner = winner;
        this.candidateVotes = candidateVotes;
        this.votingDecisions = votingDecisions;
        this.electionType = electionType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public UUID getWinner() { return winner; }
    public UUID getWinnerId() { return winner; }
    public Map<UUID, List<Vote>> getCandidateVotes() { return candidateVotes; }
    public List<VotingDecision> getVotingDecisions() { return votingDecisions; }
    public ElectionType getElectionType() { return electionType; }
    public long getTimestamp() { return timestamp; }
    public double getWinningVoteCount() { return winningVoteCount; }
    public ElectionOutcome getOutcome() { return outcome; }
    
    public void setWinnerId(UUID winner) { this.winner = winner; }
    public void setWinningVoteCount(double count) { this.winningVoteCount = count; }
    public void setOutcome(ElectionOutcome outcome) { this.outcome = outcome; }
    
    public int getTotalVotes() {
        return candidateVotes.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    public float getWinnerSupport() {
        if (winner == null || !candidateVotes.containsKey(winner)) {
            return 0.0f;
        }
        
        List<Vote> winnerVotes = candidateVotes.get(winner);
        if (winnerVotes.isEmpty()) {
            return 0.0f;
        }
        
        return (float) winnerVotes.stream()
            .mapToDouble(Vote::getSupportLevel)
            .average()
            .orElse(0.0);
    }
}