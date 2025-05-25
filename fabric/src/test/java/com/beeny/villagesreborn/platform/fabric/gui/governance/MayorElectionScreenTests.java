package com.beeny.villagesreborn.platform.fabric.gui.governance;

import com.beeny.villagesreborn.core.governance.ElectionData;
import com.beeny.villagesreborn.core.governance.ElectionStatus;
import com.beeny.villagesreborn.core.governance.ResidentData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;

/**
 * Tests for MayorElectionScreen GUI - candidate list, voting buttons, and election data updates
 */
@ExtendWith(MockitoExtension.class)
public class MayorElectionScreenTests {

    private MayorElectionScreen mayorElectionScreen;
    
    @Mock
    private ElectionData mockElectionData;
    
    @Mock
    private ButtonWidget mockVoteButton;
    
    @Mock
    private ButtonWidget mockConfirmButton;
    
    @Mock
    private TextWidget mockCandidateLabel;
    
    private UUID candidateId1;
    private UUID candidateId2;
    private UUID candidateId3;
    private UUID playerId;
    
    @BeforeEach
    void setUp() {
        candidateId1 = UUID.randomUUID();
        candidateId2 = UUID.randomUUID();
        candidateId3 = UUID.randomUUID();
        playerId = UUID.randomUUID();
        
        mayorElectionScreen = new MayorElectionScreen(mockElectionData, playerId);
    }
    
    @Test
    void init_WithActiveCandidates_ShouldPopulateCandidateList() {
        // Given
        List<ResidentData> candidates = Arrays.asList(
            createCandidate(candidateId1, "John Smith", 85),
            createCandidate(candidateId2, "Mary Johnson", 70),
            createCandidate(candidateId3, "Bob Wilson", 60)
        );
        
        when(mockElectionData.getCandidates()).thenReturn(candidates);
        when(mockElectionData.getStatus()).thenReturn(ElectionStatus.ACTIVE);
        
        // When
        mayorElectionScreen.init();
        
        // Then
        List<CandidateWidget> candidateWidgets = mayorElectionScreen.getCandidateWidgets();
        assertEquals(3, candidateWidgets.size());
        assertEquals("John Smith (Reputation: 85)", candidateWidgets.get(0).getDisplayText());
        assertEquals("Mary Johnson (Reputation: 70)", candidateWidgets.get(1).getDisplayText());
        assertEquals("Bob Wilson (Reputation: 60)", candidateWidgets.get(2).getDisplayText());
    }
    
    @Test
    void init_WithNoActiveCandidates_ShouldShowNoCandidatesMessage() {
        // Given
        when(mockElectionData.getCandidates()).thenReturn(Arrays.asList());
        when(mockElectionData.getStatus()).thenReturn(ElectionStatus.ACTIVE);
        when(mockElectionData.hasVoted(playerId)).thenReturn(false);
        
        // When
        mayorElectionScreen.init();
        
        // Then
        assertTrue(mayorElectionScreen.hasWidget("no_candidates_label"));
        assertEquals("No candidates available", mayorElectionScreen.getWidgetText("no_candidates_label"));
    }
    
    @Test
    void selectCandidate_WithValidCandidate_ShouldEnableVoteButton() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        
        // When
        mayorElectionScreen.selectCandidate(candidateId1);
        
        // Then
        assertTrue(mayorElectionScreen.isVoteButtonEnabled());
        assertEquals(candidateId1, mayorElectionScreen.getSelectedCandidateId());
    }
    
    @Test
    void selectCandidate_WithNoSelection_ShouldDisableVoteButton() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        
        // When
        mayorElectionScreen.selectCandidate(null);
        
        // Then
        assertFalse(mayorElectionScreen.isVoteButtonEnabled());
        assertNull(mayorElectionScreen.getSelectedCandidateId());
    }
    
    @Test
    void onVoteButtonClick_WithSelectedCandidate_ShouldEnableConfirmButton() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        mayorElectionScreen.selectCandidate(candidateId1);
        
        // When
        mayorElectionScreen.onVoteButtonClick();
        
        // Then
        assertTrue(mayorElectionScreen.isConfirmButtonEnabled());
        assertEquals("Confirm Vote for John Smith", mayorElectionScreen.getConfirmButtonText());
    }
    
    @Test
    void onConfirmButtonClick_WithValidVote_ShouldWriteBackToElectionData() {
        // Given
        setupActiveElection();
        when(mockElectionData.hasVoted(playerId)).thenReturn(false);
        mayorElectionScreen.init();
        mayorElectionScreen.selectCandidate(candidateId1);
        mayorElectionScreen.onVoteButtonClick();
        
        // When
        mayorElectionScreen.onConfirmButtonClick();
        
        // Then
        verify(mockElectionData).recordVote(eq(playerId), eq(candidateId1), anyDouble());
        assertTrue(mayorElectionScreen.isVoteSubmitted());
        assertEquals("Vote submitted successfully!", mayorElectionScreen.getStatusMessage());
    }
    
    @Test
    void init_WhenPlayerAlreadyVoted_ShouldDisableVotingInterface() {
        // Given
        setupActiveElection();
        when(mockElectionData.hasVoted(playerId)).thenReturn(true);
        when(mockElectionData.getPlayerVote(playerId)).thenReturn(candidateId2);
        
        // When
        mayorElectionScreen.init();
        
        // Then
        assertFalse(mayorElectionScreen.isVoteButtonEnabled());
        assertFalse(mayorElectionScreen.isConfirmButtonEnabled());
        assertEquals("You have already voted for Mary Johnson", mayorElectionScreen.getStatusMessage());
    }
    
    @Test
    void init_WhenElectionNotActive_ShouldDisableVotingInterface() {
        // Given
        when(mockElectionData.getStatus()).thenReturn(ElectionStatus.COMPLETED);
        
        // When
        mayorElectionScreen.init();
        
        // Then
        assertFalse(mayorElectionScreen.isVoteButtonEnabled());
        assertFalse(mayorElectionScreen.isConfirmButtonEnabled());
        assertEquals("Election has ended", mayorElectionScreen.getStatusMessage());
    }
    
    @Test
    void candidateWidget_OnClick_ShouldSelectCandidate() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        CandidateWidget candidateWidget = mayorElectionScreen.getCandidateWidgets().get(0);
        
        // When
        candidateWidget.onClick(100, 50); // mock click coordinates
        
        // Then
        assertEquals(candidateId1, mayorElectionScreen.getSelectedCandidateId());
        assertTrue(candidateWidget.isSelected());
    }
    
    @Test
    void candidateWidget_OnHover_ShouldShowTooltip() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        CandidateWidget candidateWidget = mayorElectionScreen.getCandidateWidgets().get(0);
        
        // When
        candidateWidget.onHover(100, 50);
        
        // Then
        assertTrue(candidateWidget.hasTooltip());
        assertEquals("Click to select John Smith as your vote", candidateWidget.getTooltipText());
    }
    
    @Test
    void render_ShouldDisplayAllUIElements() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        
        // When
        mayorElectionScreen.render(null, 0, 0, 0f); // mock render call
        
        // Then
        assertTrue(mayorElectionScreen.isRendered());
        assertEquals(3, mayorElectionScreen.getRenderedCandidateCount());
        assertTrue(mayorElectionScreen.hasRenderedWidget("vote_button"));
        assertTrue(mayorElectionScreen.hasRenderedWidget("confirm_button"));
    }
    
    @Test
    void onClose_ShouldCleanupResources() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        
        // When
        mayorElectionScreen.onClose();
        
        // Then
        assertTrue(mayorElectionScreen.isClosed());
        assertEquals(0, mayorElectionScreen.getCandidateWidgets().size());
    }
    
    @Test
    void getVoteWeight_ShouldReflectPlayerReputation() {
        // Given
        setupActiveElection();
        mayorElectionScreen.init();
        
        // When
        double voteWeight = mayorElectionScreen.calculatePlayerVoteWeight();
        
        // Then
        assertEquals(1.25, voteWeight, 0.01); // based on reputation 75
    }
    
    private void setupActiveElection() {
        List<ResidentData> candidates = Arrays.asList(
            createCandidate(candidateId1, "John Smith", 85),
            createCandidate(candidateId2, "Mary Johnson", 70),
            createCandidate(candidateId3, "Bob Wilson", 60)
        );
        
        when(mockElectionData.getCandidates()).thenReturn(candidates);
        when(mockElectionData.getStatus()).thenReturn(ElectionStatus.ACTIVE);
        when(mockElectionData.hasVoted(playerId)).thenReturn(false);
    }
    
    private ResidentData createCandidate(UUID id, String name, int reputation) {
        ResidentData candidate = new ResidentData(id);
        candidate.setName(name);
        candidate.setReputation(reputation);
        candidate.setAge(30);
        return candidate;
    }
}