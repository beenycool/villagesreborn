package com.beeny.villagesreborn.core.expansion;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI-powered village expansion planning.
 * Provides intelligent analysis of expansion opportunities with fallback mechanisms.
 */
public interface ExpansionAIEngine {
    
    /**
     * Generates an AI-powered expansion plan
     * 
     * @param resources current village resources
     * @param terrainData terrain analysis data
     * @param biomeData biome characteristics
     * @param growthTrends population and economic trends
     * @param timeoutMs maximum time to wait for AI response
     * @return future containing the expansion proposal
     */
    CompletableFuture<ExpansionProposal> generateExpansionPlan(
        VillageResources resources,
        TerrainData terrainData,
        BiomeData biomeData,
        GrowthTrends growthTrends,
        long timeoutMs
    );
    
    /**
     * Determines if AI should be used for this expansion decision
     * 
     * @param resources current village state
     * @param complexity expansion complexity factors
     * @return true if AI should be used, false for procedural fallback
     */
    boolean shouldUseAI(VillageResources resources, ExpansionComplexity complexity);
    
    /**
     * Provides procedural fallback expansion plan
     * 
     * @param resources current village resources
     * @param biomeData biome characteristics
     * @return procedural expansion proposal
     */
    ExpansionProposal proceduralFallback(VillageResources resources, BiomeData biomeData);
    
    /**
     * Validates an AI-generated expansion plan for feasibility
     * 
     * @param proposal the AI-generated proposal
     * @param resources current village resources
     * @return true if proposal is valid and feasible
     */
    boolean validateExpansionPlan(ExpansionProposal proposal, VillageResources resources);
}