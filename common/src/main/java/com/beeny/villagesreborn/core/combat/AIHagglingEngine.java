package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered haggling engine that uses LLM to generate dynamic negotiation strategies
 * and realistic trading behavior based on villager personality and relationships
 */
public class AIHagglingEngine extends HagglingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIHagglingEngine.class);
    
    private final LLMApiClient llmClient;
    private final HagglingEngine fallbackEngine;
    
    // Patterns for parsing AI responses
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)price:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern STRATEGY_PATTERN = Pattern.compile("(?i)strategy:\\s*(.+)");
    private static final Pattern ACCEPT_PATTERN = Pattern.compile("(?i)decision:\\s*(accept|reject|counter)");
    
    public AIHagglingEngine(LLMApiClient llmClient) {
        this.llmClient = llmClient;
        this.fallbackEngine = new HagglingEngine();
    }
    
    /**
     * AI-enhanced price calculation with dynamic personality-driven adjustments
     */
    public double calculateAIPrice(double basePrice, RelationshipData relationship, 
                                 ReputationData reputation, CombatPersonalityTraits traderPersonality,
                                 String itemName, int currentRound) {
        
        if (!shouldUseAI(relationship, reputation, currentRound)) {
            return super.calculatePrice(basePrice, relationship, reputation);
        }
        
        try {
            String prompt = buildPricingPrompt(basePrice, relationship, reputation, 
                                             traderPersonality, itemName, currentRound);
            
            ConversationRequest request = ConversationRequest.builder()
                .prompt(prompt)
                .maxTokens(100)
                .temperature(0.7f)
                .timeout(Duration.ofSeconds(5))
                .build();
            
            CompletableFuture<ConversationResponse> responseFuture = llmClient.generateConversationResponse(request);
            ConversationResponse response = responseFuture.get(5, TimeUnit.SECONDS);
            
            if (response != null && response.isSuccess()) {
                double aiPrice = parseAIPrice(response.getResponse(), basePrice);
                if (aiPrice > 0) {
                    return validateAndClampPrice(aiPrice, basePrice);
                }
            }
            
            LOGGER.warn("AI pricing failed, using fallback for item: {}", itemName);
            return fallbackEngine.calculatePrice(basePrice, relationship, reputation);
            
        } catch (Exception e) {
            LOGGER.error("Error in AI price calculation", e);
            return fallbackEngine.calculatePrice(basePrice, relationship, reputation);
        }
    }
    
    /**
     * AI-powered negotiation strategy that determines whether to accept, reject, or counter an offer
     */
    public NegotiationDecision generateNegotiationStrategy(double offer, double basePrice,
                                                         RelationshipData relationship,
                                                         ReputationData reputation,
                                                         CombatPersonalityTraits traderPersonality,
                                                         int currentRound, int maxRounds) {
        
        if (!shouldUseAI(relationship, reputation, currentRound)) {
            return generateFallbackStrategy(offer, basePrice, traderPersonality, currentRound, maxRounds);
        }
        
        try {
            String prompt = buildNegotiationPrompt(offer, basePrice, relationship, reputation,
                                                 traderPersonality, currentRound, maxRounds);
            
            ConversationRequest request = ConversationRequest.builder()
                .prompt(prompt)
                .maxTokens(120)
                .temperature(0.8f)
                .timeout(Duration.ofSeconds(5))
                .build();
            
            CompletableFuture<ConversationResponse> responseFuture = llmClient.generateConversationResponse(request);
            ConversationResponse response = responseFuture.get(5, TimeUnit.SECONDS);
            
            if (response != null && response.isSuccess()) {
                return parseNegotiationDecision(response.getResponse(), offer, basePrice);
            }
            
            LOGGER.warn("AI negotiation strategy failed, using fallback");
            return generateFallbackStrategy(offer, basePrice, traderPersonality, currentRound, maxRounds);
            
        } catch (Exception e) {
            LOGGER.error("Error in AI negotiation strategy", e);
            return generateFallbackStrategy(offer, basePrice, traderPersonality, currentRound, maxRounds);
        }
    }
    
    /**
     * Determines if AI should be used for this trading scenario
     */
    private boolean shouldUseAI(RelationshipData relationship, ReputationData reputation, int currentRound) {
        // Use AI for complex scenarios
        if (currentRound > 2) return true; // Multi-round negotiations
        if (relationship != null && (relationship.getTrustLevel() < 0.3f || relationship.getFriendshipLevel() < 0.3f)) return true;
        if (reputation != null && Math.abs(reputation.getReputation()) > 0.5f) return true;
        
        return false; // Use simple logic for basic scenarios
    }
    
    /**
     * Builds AI prompt for dynamic pricing
     */
    private String buildPricingPrompt(double basePrice, RelationshipData relationship,
                                    ReputationData reputation, CombatPersonalityTraits traderPersonality,
                                    String itemName, int currentRound) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a villager trader setting prices. Consider:\n\n");
        
        // Item context
        prompt.append("Item: ").append(itemName != null ? itemName : "trade goods").append("\n");
        prompt.append("Base Market Price: ").append(String.format("%.2f", basePrice)).append(" emeralds\n");
        prompt.append("Negotiation Round: ").append(currentRound).append("\n\n");
        
        // Personality factors
        prompt.append("Your Personality:\n");
        prompt.append("- Aggression: ").append(String.format("%.1f", traderPersonality.getAggression())).append("/1.0\n");
        prompt.append("- Self-Preservation: ").append(String.format("%.1f", traderPersonality.getSelfPreservation())).append("/1.0\n");
        prompt.append("- Courage: ").append(String.format("%.1f", traderPersonality.getCourage())).append("/1.0\n");
        prompt.append("- Loyalty: ").append(String.format("%.1f", traderPersonality.getLoyalty())).append("/1.0\n\n");
        
        // Relationship context
        if (relationship != null) {
            prompt.append("Relationship with Customer:\n");
            prompt.append("- Trust Level: ").append(String.format("%.1f", relationship.getTrustLevel())).append("/1.0\n");
            prompt.append("- Friendship: ").append(String.format("%.1f", relationship.getFriendshipLevel())).append("/1.0\n\n");
        }
        
        // Reputation context
        if (reputation != null) {
            prompt.append("Customer Reputation: ").append(String.format("%.2f", reputation.getReputation())).append("/1.0\n\n");
        }
        
        prompt.append("Set your asking price considering your personality and relationship.\n");
        prompt.append("Price: [your price in emeralds]\n");
        prompt.append("Strategy: [brief reasoning]\n\n");
        prompt.append("Keep response under 80 tokens.");
        
        return prompt.toString();
    }
    
    /**
     * Builds AI prompt for negotiation decisions
     */
    private String buildNegotiationPrompt(double offer, double basePrice, RelationshipData relationship,
                                        ReputationData reputation, CombatPersonalityTraits traderPersonality,
                                        int currentRound, int maxRounds) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Customer offers ").append(String.format("%.2f", offer)).append(" emeralds.\n");
        prompt.append("Your asking price: ").append(String.format("%.2f", basePrice)).append(" emeralds.\n");
        prompt.append("Round ").append(currentRound).append(" of ").append(maxRounds).append(" max.\n\n");
        
        // Personality influence
        if (traderPersonality.getAggression() > 0.7f) {
            prompt.append("You're an aggressive trader who drives hard bargains.\n");
        } else if (traderPersonality.getSelfPreservation() > 0.7f) {
            prompt.append("You're cautious and want to ensure profitable deals.\n");
        } else if (traderPersonality.getLoyalty() > 0.7f) {
            prompt.append("You value long-term customer relationships.\n");
        }
        
        // Relationship context
        if (relationship != null && relationship.getFriendshipLevel() > 0.6f) {
            prompt.append("This customer is a good friend - consider being flexible.\n");
        } else if (relationship != null && relationship.getTrustLevel() < 0.3f) {
            prompt.append("You don't trust this customer much.\n");
        }
        
        prompt.append("\nDecision: ACCEPT/REJECT/COUNTER\n");
        prompt.append("If COUNTER, Price: [your counter-offer]\n");
        prompt.append("Strategy: [brief reasoning]\n\n");
        prompt.append("Keep response under 100 tokens.");
        
        return prompt.toString();
    }
    
    /**
     * Parses AI price response
     */
    private double parseAIPrice(String response, double basePrice) {
        Matcher matcher = PRICE_PATTERN.matcher(response);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid price format in AI response: {}", matcher.group(1));
            }
        }
        return -1; // Indicates parsing failure
    }
    
    /**
     * Parses AI negotiation decision
     */
    private NegotiationDecision parseNegotiationDecision(String response, double offer, double basePrice) {
        Matcher acceptMatcher = ACCEPT_PATTERN.matcher(response);
        if (acceptMatcher.find()) {
            String decision = acceptMatcher.group(1).toLowerCase();
            
            switch (decision) {
                case "accept":
                    return new NegotiationDecision(NegotiationOutcome.ACCEPTED, offer, "AI accepted offer");
                    
                case "reject":
                    return new NegotiationDecision(NegotiationOutcome.REJECTED, 0, "AI rejected offer");
                    
                case "counter":
                    double counterPrice = parseAIPrice(response, basePrice);
                    if (counterPrice > 0) {
                        return new NegotiationDecision(NegotiationOutcome.COUNTER_OFFERED, counterPrice, "AI counter-offer");
                    }
                    // Fall through to default counter if parsing failed
                    break;
            }
        }
        
        // Default counter-offer if parsing failed
        double defaultCounter = (offer + basePrice) / 2.0;
        return new NegotiationDecision(NegotiationOutcome.COUNTER_OFFERED, defaultCounter, "Default AI counter");
    }
    
    /**
     * Validates and clamps AI-generated prices to reasonable bounds
     */
    private double validateAndClampPrice(double aiPrice, double basePrice) {
        // Clamp to reasonable bounds (50% to 300% of base price)
        double minPrice = basePrice * 0.5;
        double maxPrice = basePrice * 3.0;
        
        return Math.max(minPrice, Math.min(maxPrice, aiPrice));
    }
    
    /**
     * Fallback strategy using rule-based logic
     */
    private NegotiationDecision generateFallbackStrategy(double offer, double basePrice,
                                                       CombatPersonalityTraits traderPersonality,
                                                       int currentRound, int maxRounds) {
        
        double offerRatio = offer / basePrice;
        
        // Personality-driven thresholds
        double acceptThreshold = 0.85 - (traderPersonality.getAggression() * 0.1);
        double rejectThreshold = 0.6 + (traderPersonality.getSelfPreservation() * 0.2);
        
        if (offerRatio >= acceptThreshold) {
            return new NegotiationDecision(NegotiationOutcome.ACCEPTED, offer, "Fallback accept");
        } else if (offerRatio <= rejectThreshold && currentRound < maxRounds - 1) {
            return new NegotiationDecision(NegotiationOutcome.REJECTED, 0, "Fallback reject");
        } else {
            // Counter-offer
            double counterOffer = basePrice * (0.9 - traderPersonality.getAggression() * 0.1);
            return new NegotiationDecision(NegotiationOutcome.COUNTER_OFFERED, counterOffer, "Fallback counter");
        }
    }
    
    /**
     * Wrapper class for negotiation decisions
     */
    public static class NegotiationDecision {
        private final NegotiationOutcome outcome;
        private final double price;
        private final String reasoning;
        
        public NegotiationDecision(NegotiationOutcome outcome, double price, String reasoning) {
            this.outcome = outcome;
            this.price = price;
            this.reasoning = reasoning;
        }
        
        public NegotiationOutcome getOutcome() { return outcome; }
        public double getPrice() { return price; }
        public String getReasoning() { return reasoning; }
    }
}