package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for AI-powered combat decision making.
 * Provides intelligent analysis of combat situations with fallback mechanisms.
 */
public interface CombatAIEngine {
    
    /**
     * Analyzes a combat situation and makes an AI-powered decision
     * 
     * @param situation the current combat situation
     * @param traits the villager's combat personality traits
     * @param threatData assessment of current threats
     * @param relationships relationship data for targeting decisions
     * @param timeoutMs maximum time to wait for AI response
     * @return future containing the AI combat decision
     */
    CompletableFuture<CombatDecision> analyzeAndDecide(
        CombatSituation situation, 
        CombatPersonalityTraits traits,
        ThreatAssessment threatData,
        RelationshipData relationships,
        long timeoutMs
    );
    
    /**
     * Determines if AI should be used for this combat decision
     * 
     * @param villager the villager making the decision
     * @param situation the combat situation
     * @return true if AI should be used, false for rule-based fallback
     */
    boolean shouldUseAI(VillagerEntity villager, CombatSituation situation);
    
    /**
     * Provides rule-based fallback decision when AI is unavailable
     * 
     * @param situation the combat situation
     * @param traits the villager's combat personality traits
     * @return fallback combat decision
     */
    CombatDecision fallbackDecision(CombatSituation situation, CombatPersonalityTraits traits);
    
    /**
     * Validates an AI-generated combat decision for safety and game constraints
     * 
     * @param decision the AI-generated decision
     * @param situation the current combat situation
     * @return true if decision is valid and safe to execute
     */
    boolean validateAIDecision(CombatDecision decision, CombatSituation situation);
}