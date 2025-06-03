package com.beeny.villagesreborn.core.governance;

import java.util.UUID;

/**
 * Represents a villager's voting decision including their chosen candidate and reasoning
 */
public class VotingDecision {
    private final UUID voterUUID;
    private final UUID chosenCandidate;
    private final float supportLevel;
    private final String reasoning;
    
    public VotingDecision(UUID voterUUID, UUID chosenCandidate, float supportLevel, String reasoning) {
        this.voterUUID = voterUUID;
        this.chosenCandidate = chosenCandidate;
        this.supportLevel = Math.max(0.0f, Math.min(1.0f, supportLevel));
        this.reasoning = reasoning;
    }
    
    public UUID getVoterUUID() { return voterUUID; }
    public UUID getChosenCandidate() { return chosenCandidate; }
    public float getSupportLevel() { return supportLevel; }
    public String getReasoning() { return reasoning; }
}