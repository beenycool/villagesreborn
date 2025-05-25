package com.beeny.villagesreborn.core.governance;

/**
 * Configuration for village elections
 */
public class ElectionConfig {
    private int electionIntervalDays;
    private int votingPeriodDays;
    
    public ElectionConfig(int electionIntervalDays, int votingPeriodDays) {
        this.electionIntervalDays = electionIntervalDays;
        this.votingPeriodDays = votingPeriodDays;
    }
    
    public int getElectionIntervalDays() {
        return electionIntervalDays;
    }
    
    public int getVotingPeriodDays() {
        return votingPeriodDays;
    }
}