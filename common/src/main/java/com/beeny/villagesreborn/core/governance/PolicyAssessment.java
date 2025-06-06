package com.beeny.villagesreborn.core.governance;

import java.util.UUID;

/**
 * Represents an individual's assessment of a policy proposal
 */
public class PolicyAssessment {
    private final UUID assessorUUID;
    private final PolicyDecision.Decision decision;
    private final float supportLevel;
    private final String reasoning;
    
    public PolicyAssessment(UUID assessorUUID, PolicyDecision.Decision decision, 
                           float supportLevel, String reasoning) {
        this.assessorUUID = assessorUUID;
        this.decision = decision;
        this.supportLevel = Math.max(0.0f, Math.min(1.0f, supportLevel));
        this.reasoning = reasoning;
    }
    
    public UUID getAssessorUUID() { return assessorUUID; }
    public PolicyDecision.Decision getDecision() { return decision; }
    public float getSupportLevel() { return supportLevel; }
    public String getReasoning() { return reasoning; }
}