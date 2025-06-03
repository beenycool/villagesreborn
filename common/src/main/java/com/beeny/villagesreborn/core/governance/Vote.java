package com.beeny.villagesreborn.core.governance;

import java.util.UUID;

/**
 * Represents a single vote in an election
 */
public class Vote {
    private final UUID voterUUID;
    private final UUID candidateUUID;
    private final float supportLevel;
    private final String reasoning;
    
    public Vote(UUID voterUUID, UUID candidateUUID, float supportLevel, String reasoning) {
        this.voterUUID = voterUUID;
        this.candidateUUID = candidateUUID;
        this.supportLevel = Math.max(0.0f, Math.min(1.0f, supportLevel));
        this.reasoning = reasoning;
    }
    
    public UUID getVoterUUID() { return voterUUID; }
    public UUID getCandidateUUID() { return candidateUUID; }
    public float getSupportLevel() { return supportLevel; }
    public String getReasoning() { return reasoning; }
}