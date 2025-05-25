package com.beeny.villagesreborn.core.governance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

/**
 * Tests for ResidentReputation - reputation updates, decay, and vote weight calculations
 */
@ExtendWith(MockitoExtension.class)
public class ResidentReputationTests {

    private ResidentReputation residentReputation;
    private UUID residentId;
    private ResidentData residentData;
    
    @BeforeEach
    void setUp() {
        residentReputation = new ResidentReputation();
        residentId = UUID.randomUUID();
        residentData = new ResidentData(residentId);
        residentData.setReputation(50); // baseline reputation
        residentData.setAge(25); // baseline age
        residentData.setLastActivityRound(100); // current round
    }
    
    @Test
    void updateReputation_ForCivicContribution_ShouldIncreaseReputation() {
        // Given
        int initialReputation = residentData.getReputation();
        ContributionType contribution = ContributionType.CIVIC_PROJECT;
        int contributionValue = 200; // gold contributed
        
        // When
        residentReputation.updateReputation(residentData, contribution, contributionValue);
        
        // Then
        assertTrue(residentData.getReputation() > initialReputation);
        assertEquals(60, residentData.getReputation()); // +10 for civic contribution
    }
    
    @Test
    void updateReputation_ForTradeSuccess_ShouldIncreaseReputationBasedOnValue() {
        // Given
        int initialReputation = residentData.getReputation();
        ContributionType contribution = ContributionType.SUCCESSFUL_TRADE;
        int tradeValue = 100;
        
        // When
        residentReputation.updateReputation(residentData, contribution, tradeValue);
        
        // Then
        assertTrue(residentData.getReputation() > initialReputation);
        assertEquals(55, residentData.getReputation()); // +5 for successful trade
    }
    
    @Test
    void updateReputation_ForGuardService_ShouldProvideSignificantIncrease() {
        // Given
        int initialReputation = residentData.getReputation();
        ContributionType contribution = ContributionType.GUARD_SERVICE;
        int serviceDays = 30;
        
        // When
        residentReputation.updateReputation(residentData, contribution, serviceDays);
        
        // Then
        assertTrue(residentData.getReputation() > initialReputation);
        assertEquals(65, residentData.getReputation()); // +15 for guard service
    }
    
    @Test
    void updateReputation_ForCrimeCommitted_ShouldDecreaseReputation() {
        // Given
        int initialReputation = residentData.getReputation();
        ContributionType contribution = ContributionType.CRIME_COMMITTED;
        int severity = 2; // moderate crime
        
        // When
        residentReputation.updateReputation(residentData, contribution, severity);
        
        // Then
        assertTrue(residentData.getReputation() < initialReputation);
        assertEquals(30, residentData.getReputation()); // -20 for crime
    }
    
    @Test
    void applyReputationDecay_AfterInactiveRounds_ShouldReduceReputation() {
        // Given
        residentData.setReputation(80);
        residentData.setLastActivityRound(95); // 5 rounds ago
        int currentRound = 100;
        int inactivityThreshold = 3; // decay after 3 rounds
        
        // When
        residentReputation.applyReputationDecay(residentData, currentRound, inactivityThreshold);
        
        // Then
        assertTrue(residentData.getReputation() < 80);
        assertEquals(78, residentData.getReputation()); // -2 for 2 rounds of decay (5-3)
    }
    
    @Test
    void applyReputationDecay_WithinThreshold_ShouldNotDecayReputation() {
        // Given
        residentData.setReputation(80);
        residentData.setLastActivityRound(98); // 2 rounds ago
        int currentRound = 100;
        int inactivityThreshold = 3;
        
        // When
        residentReputation.applyReputationDecay(residentData, currentRound, inactivityThreshold);
        
        // Then
        assertEquals(80, residentData.getReputation()); // no decay
    }
    
    @Test
    void applyReputationDecay_CannotGoBelowMinimum_ShouldStopAtFloor() {
        // Given
        residentData.setReputation(5); // very low reputation
        residentData.setLastActivityRound(80); // 20 rounds ago
        int currentRound = 100;
        int inactivityThreshold = 3;
        
        // When
        residentReputation.applyReputationDecay(residentData, currentRound, inactivityThreshold);
        
        // Then
        assertEquals(0, residentData.getReputation()); // minimum reputation floor
    }
    
    @Test
    void calculateVoteWeight_WithHighReputationAndAge_ShouldReturnHighWeight() {
        // Given
        residentData.setReputation(100);
        residentData.setAge(50);
        
        // When
        double voteWeight = residentReputation.calculateVoteWeight(residentData);
        
        // Then
        assertTrue(voteWeight > 1.0);
        assertEquals(2.0, voteWeight, 0.01); // high reputation + age bonus
    }
    
    @Test
    void calculateVoteWeight_WithLowReputationAndAge_ShouldReturnLowWeight() {
        // Given
        residentData.setReputation(10);
        residentData.setAge(18);
        
        // When
        double voteWeight = residentReputation.calculateVoteWeight(residentData);
        
        // Then
        assertTrue(voteWeight < 1.0);
        assertEquals(0.3, voteWeight, 0.01); // low reputation + young age penalty
    }
    
    @Test
    void calculateVoteWeight_WithAverageStats_ShouldReturnBaseWeight() {
        // Given
        residentData.setReputation(50);
        residentData.setAge(30);
        
        // When
        double voteWeight = residentReputation.calculateVoteWeight(residentData);
        
        // Then
        assertEquals(1.0, voteWeight, 0.01); // baseline vote weight
    }
    
    @Test
    void calculateVoteWeight_WithNegativeReputation_ShouldReturnMinimumWeight() {
        // Given
        residentData.setReputation(-10); // criminal reputation
        residentData.setAge(25);
        
        // When
        double voteWeight = residentReputation.calculateVoteWeight(residentData);
        
        // Then
        assertTrue(voteWeight >= 0.1); // minimum vote weight floor
        assertEquals(0.1, voteWeight, 0.01);
    }
    
    @Test
    void updateReputation_WithMaxReputation_ShouldNotExceedCeiling() {
        // Given
        residentData.setReputation(95); // near maximum
        ContributionType contribution = ContributionType.CIVIC_PROJECT;
        int contributionValue = 500; // large contribution
        
        // When
        residentReputation.updateReputation(residentData, contribution, contributionValue);
        
        // Then
        assertEquals(100, residentData.getReputation()); // capped at maximum
    }
    
    @Test
    void calculateAgeBonus_ForElderlyResident_ShouldProvideWisdomBonus() {
        // Given
        residentData.setAge(60); // elderly resident
        
        // When
        double ageBonus = residentReputation.calculateAgeBonus(residentData.getAge());
        
        // Then
        assertTrue(ageBonus > 0);
        assertEquals(0.3, ageBonus, 0.01); // wisdom bonus for age
    }
    
    @Test
    void calculateAgeBonus_ForYoungResident_ShouldProvideNoPenalty() {
        // Given
        residentData.setAge(20); // young resident
        
        // When
        double ageBonus = residentReputation.calculateAgeBonus(residentData.getAge());
        
        // Then
        assertEquals(0.0, ageBonus, 0.01); // no bonus or penalty
    }
    
    @Test
    void getReputationLevel_ShouldReturnCorrectCategory() {
        // Given & When & Then
        residentData.setReputation(90);
        assertEquals(ReputationLevel.EXCELLENT, residentReputation.getReputationLevel(residentData));
        
        residentData.setReputation(70);
        assertEquals(ReputationLevel.GOOD, residentReputation.getReputationLevel(residentData));
        
        residentData.setReputation(50);
        assertEquals(ReputationLevel.AVERAGE, residentReputation.getReputationLevel(residentData));
        
        residentData.setReputation(30);
        assertEquals(ReputationLevel.POOR, residentReputation.getReputationLevel(residentData));
        
        residentData.setReputation(10);
        assertEquals(ReputationLevel.TERRIBLE, residentReputation.getReputationLevel(residentData));
    }
}