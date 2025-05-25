package com.beeny.villagesreborn.core.governance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElectionManagerTests {
    
    @Mock
    private VillageResidents mockResidents;
    
    private ElectionManager electionManager;
    private ElectionConfig electionConfig;
    
    private final UUID voterId1 = UUID.randomUUID();
    private final UUID voterId2 = UUID.randomUUID();
    private final UUID candidateId1 = UUID.randomUUID();
    private final UUID candidateId2 = UUID.randomUUID();
    private final UUID candidateId3 = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        ElectionManager.clearTestElections(); // Clear any previous test data
        electionConfig = new ElectionConfig(30, 3); // 30 day interval, 3 day voting period
        electionManager = new ElectionManager(electionConfig, mockResidents);
    }
    
    @Test
    void scheduleElection_WithValidConfig_ShouldCreateElectionAtCorrectTime() {
        // Given
        long currentTime = 1000L;
        
        // When
        ElectionData election = electionManager.scheduleElection(currentTime);
        
        // Then
        assertNotNull(election);
        assertEquals(currentTime + (30 * 24 * 60 * 60 * 1000), election.getScheduledStartTime());
        assertEquals(currentTime + (33 * 24 * 60 * 60 * 1000), election.getScheduledEndTime());
        assertEquals(ElectionStatus.SCHEDULED, election.getStatus());
    }
    
    @Test
    void collectVote_WithValidVoterAndCandidate_ShouldRecordVote() {
        // Given
        ElectionData election = createActiveElection();
        ResidentData voter = createResident(voterId1, 100, 50); // reputation 100, age 50
        
        when(mockResidents.getResident(voterId1)).thenReturn(voter);
        
        // When
        boolean voteRecorded = electionManager.collectVote(election.getId(), voterId1, candidateId1);
        
        // Then
        assertTrue(voteRecorded);
        assertEquals(1, election.getVoteCount(candidateId1));
        assertTrue(election.hasVoted(voterId1));
    }
    
    @Test
    void collectVote_WithDuplicateVoter_ShouldRejectSecondVote() {
        // Given
        ElectionData election = createActiveElection();
        ResidentData voter = createResident(voterId1, 100, 50);
        
        when(mockResidents.getResident(voterId1)).thenReturn(voter);
        
        // First vote should succeed
        electionManager.collectVote(election.getId(), voterId1, candidateId1);
        
        // When - second vote attempt
        boolean secondVoteRecorded = electionManager.collectVote(election.getId(), voterId1, candidateId1);
        
        // Then
        assertFalse(secondVoteRecorded);
        assertEquals(1, election.getVoteCount(candidateId1)); // Should still be 1
    }
    
    @Test
    void tallyVotes_WithClearWinner_ShouldReturnCorrectWinner() {
        // Given
        ElectionData election = createActiveElection();
        
        // candidate1 gets 2 votes, candidate2 gets 1 vote
        election.recordVote(voterId1, candidateId1, 1.0);
        election.recordVote(voterId2, candidateId1, 1.0);
        election.recordVote(UUID.randomUUID(), candidateId2, 1.0);
        
        // When
        ElectionResult result = electionManager.tallyVotes(election.getId());
        
        // Then
        assertNotNull(result);
        assertEquals(candidateId1, result.getWinnerId());
        assertEquals(2.0, result.getWinningVoteCount(), 0.01);
        assertEquals(ElectionOutcome.WINNER_DETERMINED, result.getOutcome());
    }
    
    @Test
    void tallyVotes_WithTie_ShouldUseReputationTieBreaker() {
        // Given
        ElectionData election = createActiveElection();
        ResidentData candidate1 = createResident(candidateId1, 100, 30); // higher reputation
        ResidentData candidate2 = createResident(candidateId2, 80, 30);
        
        when(mockResidents.getResident(candidateId1)).thenReturn(candidate1);
        when(mockResidents.getResident(candidateId2)).thenReturn(candidate2);
        
        // Both candidates get 1 vote each
        election.recordVote(voterId1, candidateId1, 1.0);
        election.recordVote(voterId2, candidateId2, 1.0);
        
        // When
        ElectionResult result = electionManager.tallyVotes(election.getId());
        
        // Then
        assertNotNull(result);
        assertEquals(candidateId1, result.getWinnerId()); // Higher reputation should win
        assertEquals(ElectionOutcome.TIE_BROKEN_BY_RANDOM, result.getOutcome());
    }
    
    @Test
    void tallyVotes_WithTieAndEqualReputation_ShouldUseRandomTieBreaker() {
        // Given
        ElectionData election = createActiveElection();
        ResidentData candidate1 = createResident(candidateId1, 80, 30); // same reputation
        ResidentData candidate2 = createResident(candidateId2, 80, 30); // same reputation
        
        when(mockResidents.getResident(candidateId1)).thenReturn(candidate1);
        when(mockResidents.getResident(candidateId2)).thenReturn(candidate2);
        
        // Both candidates get 1 vote each
        election.recordVote(voterId1, candidateId1, 1.0);
        election.recordVote(voterId2, candidateId2, 1.0);
        
        // When
        ElectionResult result = electionManager.tallyVotes(election.getId());
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getWinnerId()); // Should have a winner
        assertEquals(ElectionOutcome.TIE_BROKEN_BY_RANDOM, result.getOutcome());
    }
    
    @Test
    void scheduleElection_WithInvalidConfig_ShouldThrowException() {
        // Given
        ElectionConfig invalidConfig = new ElectionConfig(-1, 3);
        ElectionManager invalidElectionManager = new ElectionManager(invalidConfig, mockResidents);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            invalidElectionManager.scheduleElection(1000L);
        });
    }
    
    private ElectionData createActiveElection() {
        ElectionData election = new ElectionData(UUID.randomUUID());
        election.setStatus(ElectionStatus.ACTIVE);
        election.addCandidate(candidateId1);
        election.addCandidate(candidateId2);
        election.addCandidate(candidateId3);
        
        // Register the election for testing
        ElectionManager.registerElectionForTesting(election);
        
        return election;
    }
    
    private ResidentData createResident(UUID id, int reputation, int age) {
        ResidentData resident = new ResidentData(id);
        resident.setReputation(reputation);
        resident.setAge(age);
        return resident;
    }
}