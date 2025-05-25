package com.beeny.villagesreborn.platform.fabric.gui.governance;

import com.beeny.villagesreborn.core.governance.ResidentData;

/**
 * Widget representing a candidate in the election screen
 */
public class CandidateWidget {
    private final ResidentData candidate;
    private boolean selected;
    private boolean hasTooltip;
    private String tooltipText;
    private MayorElectionScreen parentScreen;
    
    public CandidateWidget(ResidentData candidate) {
        this.candidate = candidate;
        this.selected = false;
        this.hasTooltip = false;
    }
    
    public void setParentScreen(MayorElectionScreen parentScreen) {
        this.parentScreen = parentScreen;
    }
    
    public String getDisplayText() {
        return candidate.getName() + " (Reputation: " + candidate.getReputation() + ")";
    }
    
    public void onClick(int mouseX, int mouseY) {
        this.selected = true;
        // Communicate selection back to parent screen
        if (parentScreen != null) {
            parentScreen.selectCandidate(candidate.getId());
        }
    }
    
    public void onHover(int mouseX, int mouseY) {
        this.hasTooltip = true;
        this.tooltipText = "Click to select " + candidate.getName() + " as your vote";
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean hasTooltip() {
        return hasTooltip;
    }
    
    public String getTooltipText() {
        return tooltipText;
    }
    
    public ResidentData getCandidate() {
        return candidate;
    }
}