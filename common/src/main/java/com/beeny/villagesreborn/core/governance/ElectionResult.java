package com.beeny.villagesreborn.core.governance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the results of a village election
 */
public class ElectionResult {
    private final UUID winner;
    private final Map<UUID, List<Vote>> candidateVotes;
    private final List<VotingDecision> votingDecisions;
    private final ElectionType electionType;
    private final long timestamp;
    
    public ElectionResult(UUID winner, Map<UUID, List<Vote>> candidateVotes, 
                         List<VotingDecision> votingDecisions, ElectionType electionType) {
        this.winner = winner;
        this.candidateVotes = candidateVotes;
        this.votingDecisions = votingDecisions;
        this.electionType = electionType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public UUID getWinner() { return winner; }
    public Map<UUID, List<Vote>> getCandidateVotes() { return candidateVotes; }
    public List<VotingDecision> getVotingDecisions() { return votingDecisions; }
    public ElectionType getElectionType() { return electionType; }
    public long getTimestamp() { return timestamp; }
    
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