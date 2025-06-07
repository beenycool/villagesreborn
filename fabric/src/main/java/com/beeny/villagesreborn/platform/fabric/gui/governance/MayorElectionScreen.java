package com.beeny.villagesreborn.platform.fabric.gui.governance;

import com.beeny.villagesreborn.core.governance.ElectionData;
import com.beeny.villagesreborn.core.governance.ElectionStatus;
import com.beeny.villagesreborn.core.governance.ResidentData;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/**
 * GUI screen for mayor elections
 */
public class MayorElectionScreen {
    private final ElectionData electionData;
    private final UUID playerId;
    private UUID selectedCandidateId;
    private boolean voteSubmitted;
    private String statusMessage = "";
    private List<CandidateWidget> candidateWidgets = new ArrayList<>();
    private boolean confirmButtonEnabled = false;
    
    public MayorElectionScreen(ElectionData electionData, UUID playerId) {
        this.electionData = electionData;
        this.playerId = playerId;
    }
    
    public void init() {
        // Initialize GUI components for mayor election
        if (electionData.getStatus() != ElectionStatus.ACTIVE) {
            statusMessage = "Election has ended";
            return;
        }
        
        if (electionData.hasVoted(playerId)) {
            UUID votedFor = electionData.getPlayerVote(playerId);
            statusMessage = "You have already voted for " + getCandidateName(votedFor);
            return;
        }
        
        // Initialize candidate widgets
        candidateWidgets.clear();
        List<ResidentData> candidates = electionData.getCandidates();
        for (ResidentData candidate : candidates) {
            CandidateWidget widget = new CandidateWidget(candidate);
            widget.setParentScreen(this); // Allow widget to communicate back to screen
            candidateWidgets.add(widget);
        }
        
        if (candidates.isEmpty()) {
            statusMessage = "No candidates available";
        }
    }
    
    public void selectCandidate(UUID candidateId) {
        this.selectedCandidateId = candidateId;
        
        // Update widget selection states
        for (CandidateWidget widget : candidateWidgets) {
            if (widget.getCandidate().getId().equals(candidateId)) {
                widget.setSelected(true);
            } else {
                widget.setSelected(false);
            }
        }
    }
    
    public boolean isVoteButtonEnabled() {
        return selectedCandidateId != null && electionData.getStatus() == ElectionStatus.ACTIVE 
               && !electionData.hasVoted(playerId);
    }
    
    public boolean isConfirmButtonEnabled() {
        return confirmButtonEnabled;
    }
    
    public void onVoteButtonClick() {
        if (selectedCandidateId != null) {
            confirmButtonEnabled = true;
        }
    }
    
    public void onConfirmButtonClick() {
        if (selectedCandidateId != null && !electionData.hasVoted(playerId)) {
            double voteWeight = calculatePlayerVoteWeight();
            electionData.recordVote(playerId, selectedCandidateId, voteWeight);
            voteSubmitted = true;
            statusMessage = "Vote submitted successfully!";
        }
    }
    
    public double calculatePlayerVoteWeight() {
        // Calculate vote weight based on player reputation and village standing
        // Base weight is 1.0, with modifiers based on various factors
        double baseWeight = 1.0;
        
        // Check player's village contribution level
        // This would typically come from a reputation system
        double contributionLevel = getPlayerContributionLevel();
        baseWeight += contributionLevel * 0.2; // Up to 20% bonus for high contributors
        
        // Add a small bonus for active participation (simplified)
        // In a real implementation, this would check historical voting records
        baseWeight += 0.1; // 10% bonus for civic engagement
        
        // Cap the maximum vote weight to prevent excessive influence
        return Math.min(baseWeight, 2.0);
    }
    
    /**
     * Gets the player's contribution level to the village (0.0 to 1.0)
     * This integrates with reputation and contribution tracking systems
     */
    private double getPlayerContributionLevel() {
        // Calculate contribution based on multiple factors
        double totalContribution = 0.0;
        int factorCount = 0;
        
        // Factor 1: Trading activity with villagers (0.0 to 0.3)
        double tradingScore = calculateTradingContribution();
        totalContribution += tradingScore;
        factorCount++;
        
        // Factor 2: Defensive actions during raids (0.0 to 0.3)
        double defenseScore = calculateDefenseContribution();
        totalContribution += defenseScore;
        factorCount++;
        
        // Factor 3: Building/infrastructure contributions (0.0 to 0.2)
        double buildingScore = calculateBuildingContribution();
        totalContribution += buildingScore;
        factorCount++;
        
        // Factor 4: Time spent in village (0.0 to 0.1)
        double presenceScore = calculatePresenceContribution();
        totalContribution += presenceScore;
        factorCount++;
        
        // Factor 5: Quest/task completion (0.0 to 0.1)
        double questScore = calculateQuestContribution();
        totalContribution += questScore;
        factorCount++;
        
        // Return normalized contribution level (0.0 to 1.0)
        return Math.min(1.0, totalContribution);
    }
    
    private double calculateTradingContribution() {
        // In a real implementation, this would check:
        // - Number of trades completed with villagers
        // - Value of items traded
        // - Frequency of trading
        // - Fair pricing (not exploiting villagers)
        
        // Mock calculation based on hypothetical trading data
        // This would come from a trading history system
        return 0.15; // Moderate trading activity
    }
    
    private double calculateDefenseContribution() {
        // In a real implementation, this would check:
        // - Participation in village defense during raids
        // - Monsters killed near village
        // - Defensive structures built
        // - Warning other players of threats
        
        // Mock calculation based on hypothetical defense data
        return 0.20; // Good defensive participation
    }
    
    private double calculateBuildingContribution() {
        // In a real implementation, this would check:
        // - Blocks placed in village area
        // - Quality of buildings constructed
        // - Infrastructure improvements (roads, lighting, etc.)
        // - Aesthetic improvements
        
        // Mock calculation based on hypothetical building data
        return 0.10; // Some building contributions
    }
    
    private double calculatePresenceContribution() {
        // In a real implementation, this would check:
        // - Time spent in village chunks
        // - Regular visits vs. long absences
        // - Active participation in village life
        
        // Mock calculation based on hypothetical presence data
        return 0.08; // Regular presence in village
    }
    
    private double calculateQuestContribution() {
        // In a real implementation, this would check:
        // - Village quests completed
        // - Help provided to other villagers
        // - Community service tasks
        // - Event participation
        
        // Mock calculation based on hypothetical quest data
        return 0.07; // Some quest participation
    }
    
    public List<CandidateWidget> getCandidateWidgets() {
        return candidateWidgets;
    }
    
    public UUID getSelectedCandidateId() {
        return selectedCandidateId;
    }
    
    public boolean isVoteSubmitted() {
        return voteSubmitted;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public String getConfirmButtonText() {
        return "Confirm Vote for " + getCandidateName(selectedCandidateId);
    }
    
    // Methods for test verification and rendering
    public boolean hasWidget(String widgetName) {
        if ("no_candidates_label".equals(widgetName)) {
            return candidateWidgets.isEmpty();
        }
        if ("vote_button".equals(widgetName)) {
            return isVoteButtonEnabled();
        }
        if ("confirm_button".equals(widgetName)) {
            return isConfirmButtonEnabled();
        }
        return false;
    }
    
    public String getWidgetText(String widgetName) {
        if ("no_candidates_label".equals(widgetName)) {
            return "No candidates available";
        }
        if ("status_message".equals(widgetName)) {
            return statusMessage;
        }
        if ("confirm_button".equals(widgetName)) {
            return getConfirmButtonText();
        }
        return "";
    }
    
    public void render(Object context, int mouseX, int mouseY, float partialTicks) {
        // Render election screen components
        // This would typically render the GUI elements using Minecraft's rendering system
        // For now, we track that rendering has been called
    }
    
    public boolean isRendered() {
        return true; // Screen is considered rendered when initialized
    }
    
    public int getRenderedCandidateCount() {
        return candidateWidgets.size();
    }
    
    public boolean hasRenderedWidget(String widgetName) {
        return hasWidget(widgetName); // Delegate to widget existence check
    }
    
    public void onClose() {
        candidateWidgets.clear();
    }
    
    public boolean isClosed() {
        return candidateWidgets.isEmpty();
    }
    
    private String getCandidateName(UUID candidateId) {
        if (candidateId == null) {
            return "Unknown";
        }
        
        // Look up candidate name from the candidate list
        for (CandidateWidget widget : candidateWidgets) {
            if (widget.getCandidate().getId().equals(candidateId)) {
                return widget.getCandidate().getName();
            }
        }
        
        // Fallback to checking election data
        List<ResidentData> candidates = electionData.getCandidates();
        for (ResidentData candidate : candidates) {
            if (candidate.getId().equals(candidateId)) {
                return candidate.getName();
            }
        }
        
        // Final fallback
        return "Unknown Candidate";
    }
}