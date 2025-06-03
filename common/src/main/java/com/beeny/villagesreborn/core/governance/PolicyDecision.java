package com.beeny.villagesreborn.core.governance;

import java.util.List;

/**
 * Represents a policy decision made by village leadership
 */
public class PolicyDecision {
    
    public enum Decision {
        APPROVE,
        REJECT,
        MODIFY
    }
    
    private final PolicyProposal proposal;
    private final Decision decision;
    private final float supportLevel;
    private final PolicyAssessment leaderAssessment;
    private final List<PolicyAssessment> councilAssessments;
    
    public PolicyDecision(PolicyProposal proposal, Decision decision, float supportLevel,
                         PolicyAssessment leaderAssessment, List<PolicyAssessment> councilAssessments) {
        this.proposal = proposal;
        this.decision = decision;
        this.supportLevel = Math.max(0.0f, Math.min(1.0f, supportLevel));
        this.leaderAssessment = leaderAssessment;
        this.councilAssessments = councilAssessments;
    }
    
    public PolicyProposal getProposal() { return proposal; }
    public Decision getDecision() { return decision; }
    public float getSupportLevel() { return supportLevel; }
    public PolicyAssessment getLeaderAssessment() { return leaderAssessment; }
    public List<PolicyAssessment> getCouncilAssessments() { return councilAssessments; }
}