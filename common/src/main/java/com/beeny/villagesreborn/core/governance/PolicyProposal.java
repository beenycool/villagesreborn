package com.beeny.villagesreborn.core.governance;

import java.util.UUID;

/**
 * Represents a policy proposal for village governance
 */
public class PolicyProposal {
    private final UUID proposalId;
    private final String title;
    private final String description;
    private final String expectedImpact;
    private final UUID proposerUUID;
    private final long timestamp;
    
    public PolicyProposal(UUID proposalId, String title, String description, 
                         String expectedImpact, UUID proposerUUID) {
        this.proposalId = proposalId;
        this.title = title;
        this.description = description;
        this.expectedImpact = expectedImpact;
        this.proposerUUID = proposerUUID;
        this.timestamp = System.currentTimeMillis();
    }
    
    public UUID getProposalId() { return proposalId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getExpectedImpact() { return expectedImpact; }
    public UUID getProposerUUID() { return proposerUUID; }
    public long getTimestamp() { return timestamp; }
}