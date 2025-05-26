package com.beeny.villagesreborn.core.expansion;

import java.util.List;
import java.util.Map;

/**
 * Represents an AI-generated or procedural expansion proposal for a village.
 * Contains building plans, resource requirements, and strategic information.
 */
public class ExpansionProposal {
    
    /**
     * Placement strategy for new buildings
     */
    public enum PlacementStrategy {
        COMPACT,      // Build close together for efficiency
        DEFENSIVE,    // Build with defensive considerations
        DISTRIBUTED,  // Spread out for resource access
        LINEAR        // Build in lines/rows
    }
    
    private final List<BuildingPlan> buildingQueue;
    private final ResourcePlan resourcePlan;
    private final PlacementStrategy strategy;
    private final Map<String, Object> aiMetadata;
    private final boolean isProceduralGenerated;
    private final float confidenceScore;
    
    /**
     * Constructor for AI-generated proposals
     */
    public ExpansionProposal(List<BuildingPlan> buildingQueue, ResourcePlan resourcePlan,
                           PlacementStrategy strategy, Map<String, Object> aiMetadata,
                           float confidenceScore) {
        this.buildingQueue = buildingQueue;
        this.resourcePlan = resourcePlan;
        this.strategy = strategy;
        this.aiMetadata = aiMetadata;
        this.confidenceScore = confidenceScore;
        this.isProceduralGenerated = false;
    }
    
    /**
     * Constructor for procedural proposals
     */
    public ExpansionProposal(List<BuildingPlan> buildingQueue, ResourcePlan resourcePlan,
                           PlacementStrategy strategy) {
        this.buildingQueue = buildingQueue;
        this.resourcePlan = resourcePlan;
        this.strategy = strategy;
        this.aiMetadata = Map.of();
        this.confidenceScore = 0.5f;
        this.isProceduralGenerated = true;
    }
    
    public List<BuildingPlan> getBuildingQueue() {
        return buildingQueue;
    }
    
    public ResourcePlan getResourcePlan() {
        return resourcePlan;
    }
    
    public PlacementStrategy getStrategy() {
        return strategy;
    }
    
    public Map<String, Object> getAiMetadata() {
        return aiMetadata;
    }
    
    public boolean isProceduralGenerated() {
        return isProceduralGenerated;
    }
    
    public float getConfidenceScore() {
        return confidenceScore;
    }
    
    /**
     * Validates that this proposal is feasible
     */
    public boolean isValid() {
        return buildingQueue != null && !buildingQueue.isEmpty() &&
               resourcePlan != null && strategy != null;
    }
    
    @Override
    public String toString() {
        return String.format("ExpansionProposal{buildings=%d, strategy=%s, procedural=%s, confidence=%.2f}",
                           buildingQueue.size(), strategy, isProceduralGenerated, confidenceScore);
    }
}