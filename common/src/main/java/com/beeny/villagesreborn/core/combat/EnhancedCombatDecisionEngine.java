package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Enhanced combat decision engine that integrates AI-powered decision making
 * with robust fallback to rule-based systems.
 */
public class EnhancedCombatDecisionEngine extends CombatDecisionEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedCombatDecisionEngine.class);
    private static final long DEFAULT_AI_TIMEOUT_MS = 150;
    
    private final CombatAIEngine combatAI;
    private final CombatDecisionEngine ruleBased;
    
    public EnhancedCombatDecisionEngine(CombatAIEngine combatAI) {
        this.combatAI = combatAI;
        this.ruleBased = new CombatDecisionEngine();
    }
    
    /**
     * Enhanced combat decision that tries AI first with timeout fallback
     */
    public CombatDecision decideCombatAction(CombatSituation situation,
                                           CombatPersonalityTraits traits,
                                           CombatCapableVillager villager,
                                           MultiThreatAssessment threatData,
                                           RelationshipData relationships) {
        return decideCombatAction(situation, traits, villager, threatData, relationships, DEFAULT_AI_TIMEOUT_MS);
    }
    
    /**
     * Enhanced combat decision with configurable timeout
     */
    public CombatDecision decideCombatAction(CombatSituation situation,
                                           CombatPersonalityTraits traits,
                                           CombatCapableVillager villager,
                                           MultiThreatAssessment threatData,
                                           RelationshipData relationships,
                                           long timeoutMs) {
        
        // Determine if we should use AI for this decision
        if (combatAI != null && combatAI.shouldUseAI(villager, situation)) {
            try {
                // Convert MultiThreatAssessment to single ThreatAssessment for AI
                ThreatAssessment singleThreat = convertToSingleThreat(threatData);
                
                // Attempt AI decision with timeout
                CompletableFuture<CombatDecision> aiDecisionFuture = combatAI.analyzeAndDecide(
                    situation, traits, singleThreat, relationships, timeoutMs
                );
                
                CombatDecision aiDecision = aiDecisionFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                
                // Validate AI decision
                if (aiDecision != null && combatAI.validateAIDecision(aiDecision, situation)) {
                    LOGGER.debug("AI combat decision successful: {}", aiDecision);
                    
                    // Update combat memory if villager supports it
                    updateCombatMemory(villager, situation, aiDecision, true);
                    
                    return aiDecision;
                } else {
                    LOGGER.warn("AI combat decision validation failed, falling back to rule-based");
                }
                
            } catch (TimeoutException e) {
                LOGGER.warn("AI combat decision timed out after {}ms, falling back to rule-based", timeoutMs);
            } catch (Exception e) {
                LOGGER.error("AI combat decision failed with exception, falling back to rule-based", e);
            }
        }
        
        // Fallback to rule-based decision
        CombatDecision fallbackDecision = generateRuleBasedDecision(situation, traits, villager, threatData, relationships);
        updateCombatMemory(villager, situation, fallbackDecision, false);
        
        return fallbackDecision;
    }
    
    /**
     * Converts MultiThreatAssessment to single ThreatAssessment for AI compatibility
     */
    private ThreatAssessment convertToSingleThreat(MultiThreatAssessment multiThreat) {
        if (!multiThreat.hasThreats()) {
            // Create a dummy threat assessment
            return new ThreatAssessment(ThreatLevel.NONE, null, 0.0f);
        }
        
        // Use the first threat source as representative
        LivingEntity primaryThreat = multiThreat.getThreatSources().get(0);
        return new ThreatAssessment(multiThreat.getThreatLevel(), primaryThreat, multiThreat.getThreatScore());
    }
    
    /**
     * Generates rule-based combat decision using existing logic
     */
    private CombatDecision generateRuleBasedDecision(CombatSituation situation,
                                                   CombatPersonalityTraits traits,
                                                   CombatCapableVillager villager,
                                                   MultiThreatAssessment threatData,
                                                   RelationshipData relationships) {
        
        // Convert to single threat for existing logic compatibility
        ThreatAssessment singleThreat = convertToSingleThreat(threatData);
        
        // Use existing rule-based logic
        if (shouldFlee(singleThreat, traits, villager)) {
            return new CombatDecision(CombatDecision.CombatAction.FLEE, null, null);
        }
        
        if (shouldAttack(singleThreat, traits, villager)) {
            List<LivingEntity> validTargets = getValidCombatTargets(
                situation.getEnemies(), relationships, villager
            );
            
            if (!validTargets.isEmpty()) {
                LivingEntity target = selectTargetWithRelationships(
                    validTargets, villager, traits, relationships
                );
                
                List<ItemStack> weapons = villager.getAvailableWeapons();
                ItemStack bestWeapon = selectBestWeapon(weapons, traits, villager);
                
                return new CombatDecision(
                    CombatDecision.CombatAction.ATTACK,
                    target != null ? List.of(target.getUUID()) : null,
                    bestWeapon
                );
            }
        }
        
        // Default to defend
        return new CombatDecision(CombatDecision.CombatAction.DEFEND, null, null);
    }
    
    /**
     * Updates combat memory if the villager supports it
     */
    private void updateCombatMemory(CombatCapableVillager villager,
                                  CombatSituation situation,
                                  CombatDecision decision,
                                  boolean usedAI) {
        try {
            if (villager.hasCombatMemory()) {
                CombatMemoryData memory = villager.getCombatMemory();
                
                // Record encounter
                for (LivingEntity enemy : situation.getEnemies()) {
                    memory.recordEnemyEncounter(enemy.getUUID());
                }
                
                // Update AI decision timestamp if AI was used
                if (usedAI) {
                    memory.updateLastAIDecision();
                }
                
                // Learn strategy for this situation type
                String situationType = categorizeSquadTactics(situation);
                memory.learnStrategy(situationType, decision);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to update combat memory", e);
        }
    }
    
    /**
     * Categorizes combat situation for learning purposes
     */
    private String categorizeSquadTactics(CombatSituation situation) {
        int enemyCount = situation.getEnemies().size();
        ThreatLevel threatLevel = situation.getOverallThreatLevel();
        
        return String.format("%s_%d_enemies", threatLevel.name().toLowerCase(), enemyCount);
    }
    
    /**
     * Processes a complete combat tick with AI integration
     */
    public CombatTickResult processCombatTick(CombatCapableVillager villager, CombatSituation situation) {
        // Gather contextual data
        MultiThreatAssessment threatData = assessThreats(villager, situation.getEnemies());
        RelationshipData relationships = villager.getRelationshipData();
        CombatPersonalityTraits traits = villager.getCombatTraits();
        
        // Make decision
        CombatDecision decision = decideCombatAction(situation, traits, villager, threatData, relationships);
        
        // Execute decision
        boolean executed = executeDecision(villager, decision, situation);
        
        return new CombatTickResult(decision, executed, decision != null && !decision.isFallbackDecision());
    }
    
    /**
     * Assesses threats from a list of enemies
     */
    private MultiThreatAssessment assessThreats(CombatCapableVillager villager, List<LivingEntity> enemies) {
        if (enemies.isEmpty()) {
            return new MultiThreatAssessment(ThreatLevel.NONE, 0.0f, List.of(), List.of());
        }
        
        float totalThreat = 0.0f;
        ThreatLevel maxLevel = ThreatLevel.NONE;
        List<ThreatAssessment> individualThreats = new ArrayList<>();
        
        for (LivingEntity enemy : enemies) {
            float threatScore = calculateThreatScore(enemy, villager, villager.getCombatTraits());
            totalThreat += threatScore;
            
            ThreatLevel enemyLevel = ThreatLevel.NONE;
            if (threatScore > 0.8f) {
                enemyLevel = ThreatLevel.EXTREME;
                maxLevel = ThreatLevel.EXTREME;
            } else if (threatScore > 0.6f) {
                enemyLevel = ThreatLevel.HIGH;
                if (maxLevel.ordinal() < ThreatLevel.HIGH.ordinal()) {
                    maxLevel = ThreatLevel.HIGH;
                }
            } else if (threatScore > 0.4f) {
                enemyLevel = ThreatLevel.MODERATE;
                if (maxLevel.ordinal() < ThreatLevel.MODERATE.ordinal()) {
                    maxLevel = ThreatLevel.MODERATE;
                }
            } else if (threatScore > 0.2f) {
                enemyLevel = ThreatLevel.LOW;
                if (maxLevel.ordinal() < ThreatLevel.LOW.ordinal()) {
                    maxLevel = ThreatLevel.LOW;
                }
            }
            
            individualThreats.add(new ThreatAssessment(enemyLevel, enemy, threatScore));
        }
        
        return new MultiThreatAssessment(maxLevel, totalThreat / enemies.size(), enemies, individualThreats);
    }
    
    /**
     * Executes a combat decision
     */
    private boolean executeDecision(CombatCapableVillager villager, CombatDecision decision, CombatSituation situation) {
        if (decision == null) return false;
        
        try {
            switch (decision.getAction()) {
                case ATTACK:
                    return executeAttack(villager, decision, situation);
                case DEFEND:
                    return executeDefend(villager, decision);
                case FLEE:
                    return executeFlee(villager, decision);
                case NEGOTIATE:
                    return executeNegotiate(villager, decision, situation);
                default:
                    return false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute combat decision: {}", decision, e);
            return false;
        }
    }
    
    private boolean executeAttack(CombatCapableVillager villager, CombatDecision decision, CombatSituation situation) {
        if (decision.getTargetPriority() == null || decision.getTargetPriority().isEmpty()) {
            return false;
        }
        
        // Equip preferred weapon if available
        if (decision.getPreferredWeapon() != null) {
            villager.equipWeapon(decision.getPreferredWeapon());
        }
        
        // Attack primary target
        return villager.attackTarget(decision.getTargetPriority().get(0));
    }
    
    private boolean executeDefend(CombatCapableVillager villager, CombatDecision decision) {
        return villager.enterDefensiveStance();
    }
    
    private boolean executeFlee(CombatCapableVillager villager, CombatDecision decision) {
        return villager.flee();
    }
    
    private boolean executeNegotiate(CombatCapableVillager villager, CombatDecision decision, CombatSituation situation) {
        // Placeholder for negotiation logic
        return villager.attemptNegotiation();
    }
    
    /**
     * Result of processing a combat tick
     */
    public static class CombatTickResult {
        private final CombatDecision decision;
        private final boolean executed;
        private final boolean usedAI;
        
        public CombatTickResult(CombatDecision decision, boolean executed, boolean usedAI) {
            this.decision = decision;
            this.executed = executed;
            this.usedAI = usedAI;
        }
        
        public CombatDecision getDecision() { return decision; }
        public boolean wasExecuted() { return executed; }
        public boolean usedAI() { return usedAI; }
        public CombatDecision.CombatAction getAction() { 
            return decision != null ? decision.getAction() : null; 
        }
    }
}