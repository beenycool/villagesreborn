package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.PromptBuilder;
import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Implementation of CombatAIEngine using LLM for intelligent combat decisions.
 * Optimized to reduce performance impact by caching responses and limiting AI calls.
 */
public class CombatAIEngineImpl implements CombatAIEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombatAIEngineImpl.class);
    private static final long AI_RESPONSE_CACHE_DURATION_MS = 3000; // Cache AI responses for 3 seconds
    private static final int MAX_CACHE_SIZE = 50;
    
    private final LLMApiClient llmClient;
    
    // Performance optimization caches
    private final Map<String, CachedAIResponse> responseCache = new ConcurrentHashMap<>();
    
    // Patterns for parsing AI responses
    private static final Pattern ACTION_PATTERN = Pattern.compile("(?i)decide:\\s*(ATTACK|DEFEND|FLEE|NEGOTIATE)");
    private static final Pattern WEAPON_PATTERN = Pattern.compile("(?i)weapon choice:\\s*(\\w+)");
    private static final Pattern TARGET_PATTERN = Pattern.compile("(?i)target priority:\\s*\\[([^\\]]+)\\]");
    private static final Pattern REASONING_PATTERN = Pattern.compile("(?i)tactical notes:\\s*(.+)");
    
    public CombatAIEngineImpl(LLMApiClient llmClient) {
        this.llmClient = llmClient;
    }
    
    @Override
    public CompletableFuture<CombatDecision> analyzeAndDecide(CombatSituation situation, 
                                                            CombatPersonalityTraits traits,
                                                            ThreatAssessment threatData,
                                                            RelationshipData relationships,
                                                            long timeoutMs) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Generate cache key based on situation characteristics
                String cacheKey = generateCacheKey(situation, traits, threatData);
                
                // Check for cached response
                CachedAIResponse cached = responseCache.get(cacheKey);
                if (cached != null && System.currentTimeMillis() - cached.timestamp < AI_RESPONSE_CACHE_DURATION_MS) {
                    LOGGER.debug("Using cached AI combat response");
                    return cached.decision;
                }
                
                // Build combat prompt
                String prompt = buildCombatPrompt(situation, traits, threatData, relationships);
                
                // Create conversation request using builder
                ConversationRequest request = ConversationRequest.builder()
                    .prompt(prompt)
                    .maxTokens(100) // Keep response concise
                    .timeout(java.time.Duration.ofMillis(timeoutMs))
                    .build();
                
                // Get AI response (this returns CompletableFuture)
                CompletableFuture<ConversationResponse> responseFuture = llmClient.generateConversationResponse(request);
                ConversationResponse response = responseFuture.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                if (response != null && response.isSuccess() && response.getResponse() != null) {
                    CombatDecision decision = parseAIResponse(response.getResponse(), situation, traits);
                    
                    // Cache the successful response
                    if (decision != null) {
                        cacheResponse(cacheKey, decision);
                    }
                    
                    return decision;
                } else {
                    LOGGER.warn("Empty or failed AI response, using fallback");
                    return fallbackDecision(situation, traits);
                }
                
            } catch (Exception e) {
                LOGGER.error("AI combat analysis failed", e);
                return fallbackDecision(situation, traits);
            }
        });
    }
    
    /**
     * Generates a cache key based on situation characteristics.
     */
    private String generateCacheKey(CombatSituation situation, CombatPersonalityTraits traits, ThreatAssessment threatData) {
        return String.format("%d_%d_%s_%.1f_%.1f", 
            situation.getEnemyCount(),
            situation.getAllyCount(),
            threatData.getThreatLevel().name(),
            traits.getAggression(),
            traits.getCourage());
    }
    
    /**
     * Caches an AI response to avoid repeated expensive API calls.
     */
    private void cacheResponse(String cacheKey, CombatDecision decision) {
        responseCache.put(cacheKey, new CachedAIResponse(decision, System.currentTimeMillis()));
        
        // Clean up cache periodically
        if (responseCache.size() > MAX_CACHE_SIZE) {
            cleanupResponseCache();
        }
    }
    
    /**
     * Removes expired entries from the response cache.
     */
    private void cleanupResponseCache() {
        long currentTime = System.currentTimeMillis();
        responseCache.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > AI_RESPONSE_CACHE_DURATION_MS);
    }
    
    @Override
    public boolean shouldUseAI(VillagerEntity villager, CombatSituation situation) {
        // More conservative AI usage to reduce performance impact
        
        // Don't use AI for very simple situations
        if (situation.getEnemyCount() == 1 && situation.getAllyCount() == 0 && 
            situation.getOverallThreatLevel().ordinal() <= ThreatLevel.LOW.ordinal()) {
            return false;
        }
        
        // Use AI for complex situations
        if (situation.getEnemyCount() > 3) return true;
        if (situation.getOverallThreatLevel().ordinal() >= ThreatLevel.HIGH.ordinal()) return true;
        if (situation.getAllyCount() > 2) return true; // Complex team situations
        
        // Use AI for moderate complexity
        return situation.getEnemyCount() > 1 && situation.getOverallThreatLevel() != ThreatLevel.NONE;
    }
    
    @Override
    public CombatDecision fallbackDecision(CombatSituation situation, CombatPersonalityTraits traits) {
        // Use rule-based logic as fallback
        if (situation.getEnemies().isEmpty()) {
            return new CombatDecision(CombatDecision.CombatAction.DEFEND, null, null);
        }
        
        // Simple threat-based decision
        if (situation.getOverallThreatLevel().ordinal() >= ThreatLevel.HIGH.ordinal() && traits.getSelfPreservation() > 0.7f) {
            return new CombatDecision(CombatDecision.CombatAction.FLEE, null, null);
        }
        
        // Default attack decision
        LivingEntity firstEnemy = situation.getEnemies().get(0);
        CombatDecision decision = new CombatDecision(
            CombatDecision.CombatAction.ATTACK,
            List.of(firstEnemy.getUUID()),
            null,
            "Rule-based attack decision",
            0.5f
        );
        // Mark as fallback by using the fallback constructor
        return new CombatDecision(CombatDecision.CombatAction.ATTACK, List.of(firstEnemy.getUUID()), null);
    }
    
    @Override
    public boolean validateAIDecision(CombatDecision decision, CombatSituation situation) {
        if (decision == null || !decision.isValid()) {
            return false;
        }
        
        // Validate action makes sense for situation
        switch (decision.getAction()) {
            case ATTACK:
                // Must have targets when attacking
                if (decision.getTargetPriority() == null || decision.getTargetPriority().isEmpty()) {
                    return false;
                }
                // Targets must be valid enemies
                return validateTargets(decision.getTargetPriority(), situation.getEnemies());
                
            case FLEE:
                // Flee is always valid
                return true;
                
            case DEFEND:
                // Defend is always valid
                return true;
                
            case NEGOTIATE:
                // Negotiation requires enemies present
                return !situation.getEnemies().isEmpty();
                
            default:
                return false;
        }
    }
    
    /**
     * Builds AI prompt for combat decision
     */
    private String buildCombatPrompt(CombatSituation situation,
                                   CombatPersonalityTraits traits,
                                   ThreatAssessment threatData,
                                   RelationshipData relationships) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a villager facing a combat situation. Analyze and decide:\n\n");
        
        // Personality context
        prompt.append("Personality:\n");
        prompt.append("- Aggression: ").append(String.format("%.1f", traits.getAggression())).append("\n");
        prompt.append("- Courage: ").append(String.format("%.1f", traits.getCourage())).append("\n");
        prompt.append("- Self-Preservation: ").append(String.format("%.1f", traits.getSelfPreservation())).append("\n");
        prompt.append("- Loyalty: ").append(String.format("%.1f", traits.getLoyalty())).append("\n\n");
        
        // Threat assessment
        prompt.append("Current Threat: ").append(threatData.getThreatLevel()).append("\n");
        prompt.append("Threat Score: ").append(String.format("%.2f", threatData.getThreatScore())).append("\n\n");
        
        // Enemy information
        prompt.append("Enemies: ");
        if (situation.getEnemies().isEmpty()) {
            prompt.append("None");
        } else {
            for (int i = 0; i < situation.getEnemies().size(); i++) {
                LivingEntity enemy = situation.getEnemies().get(i);
                if (i > 0) prompt.append(", ");
                prompt.append(enemy.getClass().getSimpleName()).append("(").append(enemy.getUUID().toString(), 0, 8).append(")");
            }
        }
        prompt.append("\n\n");
        
        // Ally information
        prompt.append("Allies Present: ");
        if (situation.getAllies().isEmpty()) {
            prompt.append("None");
        } else {
            prompt.append(situation.getAllyCount()).append(" allies nearby");
        }
        prompt.append("\n\n");
        
        // Available weapons (simplified)
        prompt.append("Available Weapons: sword, bow, axe\n\n");
        
        // Relationship context
        prompt.append("Relationship Context: ");
        if (relationships != null) {
            prompt.append("Has relationship data");
        } else {
            prompt.append("No specific relationships");
        }
        prompt.append("\n\n");
        
        // Decision format
        prompt.append("Decide: ATTACK/DEFEND/FLEE/NEGOTIATE\n");
        prompt.append("Target Priority: [enemy IDs in order]\n");
        prompt.append("Weapon Choice: sword/bow/axe\n");
        prompt.append("Tactical Notes: [brief reasoning]\n\n");
        prompt.append("Keep response under 100 tokens.");
        
        return prompt.toString();
    }
    
    /**
     * Parses AI response into CombatDecision
     */
    private CombatDecision parseAIResponse(String response, CombatSituation situation, CombatPersonalityTraits traits) {
        try {
            // Parse action
            CombatDecision.CombatAction action = parseAction(response);
            if (action == null) {
                LOGGER.warn("Could not parse action from AI response: {}", response);
                return fallbackDecision(situation, traits);
            }
            
            // Parse targets
            List<UUID> targets = parseTargets(response, situation);
            
            // Parse weapon choice
            ItemStack weapon = parseWeapon(response);
            
            // Parse reasoning
            String reasoning = parseReasoning(response);
            
            // Calculate confidence based on response quality
            float confidence = calculateConfidence(response, action, targets, situation);
            
            return new CombatDecision(action, targets, weapon, reasoning, confidence);
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse AI response: {}", response, e);
            return fallbackDecision(situation, traits);
        }
    }
    
    private CombatDecision.CombatAction parseAction(String response) {
        Matcher matcher = ACTION_PATTERN.matcher(response);
        if (matcher.find()) {
            try {
                return CombatDecision.CombatAction.valueOf(matcher.group(1).toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid action in AI response: {}", matcher.group(1));
            }
        }
        return null;
    }
    
    private List<UUID> parseTargets(String response, CombatSituation situation) {
        List<UUID> targets = new ArrayList<>();
        Matcher matcher = TARGET_PATTERN.matcher(response);
        
        if (matcher.find()) {
            String targetStr = matcher.group(1);
            String[] targetParts = targetStr.split(",");
            
            for (String part : targetParts) {
                String trimmed = part.trim();
                // Try to match partial UUIDs with available enemies
                for (LivingEntity enemy : situation.getEnemies()) {
                    if (enemy.getUUID().toString().startsWith(trimmed) || trimmed.contains(enemy.getUUID().toString().substring(0, 8))) {
                        targets.add(enemy.getUUID());
                        break;
                    }
                }
            }
        }
        
        // If no targets parsed but we have enemies, use first enemy
        if (targets.isEmpty() && !situation.getEnemies().isEmpty()) {
            targets.add(situation.getEnemies().get(0).getUUID());
        }
        
        return targets;
    }
    
    private ItemStack parseWeapon(String response) {
        Matcher matcher = WEAPON_PATTERN.matcher(response);
        if (matcher.find()) {
            String weaponStr = matcher.group(1).toLowerCase();
            switch (weaponStr) {
                case "sword":
                    return createWeapon(ItemType.SWORD);
                case "bow":
                    return createWeapon(ItemType.BOW);
                case "axe":
                    return createWeapon(ItemType.AXE);
                default:
                    return createWeapon(ItemType.SWORD); // Default
            }
        }
        return null;
    }
    
    private ItemStack createWeapon(ItemType type) {
        // Create a basic weapon item
        float damage = switch (type) {
            case SWORD -> 6.0f;
            case BOW -> 4.0f;
            case AXE -> 7.0f;
            default -> 5.0f;
        };
        return new SimpleItemStack(type, damage);
    }
    
    private String parseReasoning(String response) {
        Matcher matcher = REASONING_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "AI-generated decision";
    }
    
    private float calculateConfidence(String response, CombatDecision.CombatAction action, List<UUID> targets, CombatSituation situation) {
        float confidence = 0.5f; // Base confidence
        
        // Higher confidence if response contains expected keywords
        if (response.toLowerCase().contains("tactical") || response.toLowerCase().contains("strategy")) {
            confidence += 0.1f;
        }
        
        // Higher confidence if action matches situation
        if (action == CombatDecision.CombatAction.FLEE && situation.getOverallThreatLevel() == ThreatLevel.EXTREME) {
            confidence += 0.2f;
        }
        
        if (action == CombatDecision.CombatAction.ATTACK && !situation.getEnemies().isEmpty()) {
            confidence += 0.1f;
        }
        
        // Higher confidence if targets are specified for attack
        if (action == CombatDecision.CombatAction.ATTACK && !targets.isEmpty()) {
            confidence += 0.1f;
        }
        
        return Math.max(0.0f, Math.min(1.0f, confidence));
    }
    
    private boolean validateTargets(List<UUID> targets, List<LivingEntity> enemies) {
        for (UUID targetId : targets) {
            boolean found = false;
            for (LivingEntity enemy : enemies) {
                if (enemy.getUUID().equals(targetId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Clears all cached data (e.g., on world unload).
     */
    public void clearAllCaches() {
        responseCache.clear();
    }
    
    /**
     * Cache entry for AI responses.
     */
    private static class CachedAIResponse {
        final CombatDecision decision;
        final long timestamp;
        
        CachedAIResponse(CombatDecision decision, long timestamp) {
            this.decision = decision;
            this.timestamp = timestamp;
        }
    }
}