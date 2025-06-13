package com.beeny.villagesreborn.platform.fabric.trading;

import com.beeny.villagesreborn.core.VillagesRebornCommon;
import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.ai.PersonalityProfile;
import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.platform.fabric.ai.VillagerAIIntegration;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic trading system that adapts villager trades based on AI personality and relationships
 */
public class DynamicTradingSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTradingSystem.class);
    private static DynamicTradingSystem instance;
    private boolean enabled = false;
    
    // Cache for modified trade offers to avoid recalculating constantly
    private final Map<UUID, Map<Integer, TradeOffer>> modifiedTrades = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTradeUpdate = new ConcurrentHashMap<>();
    
    private DynamicTradingSystem() {}
    
    public static DynamicTradingSystem getInstance() {
        if (instance == null) {
            instance = new DynamicTradingSystem();
        }
        return instance;
    }
    
    public static void initialize() {
        LOGGER.info("Dynamic Trading System initialized");
        getInstance().enabled = true;
    }
    
    /**
     * Modifies trade offers based on villager AI and relationships
     */
    public void modifyTradeOffers(VillagerEntity villager, List<TradeOffer> trades, UUID playerUUID) {
        if (!enabled || trades.isEmpty()) {
            return;
        }
        
        try {
            UUID villagerUUID = villager.getUuid();
            long currentTime = System.currentTimeMillis();
            Long lastUpdate = lastTradeUpdate.get(villagerUUID);
            
            // Only update trades every 30 seconds to avoid performance issues
            if (lastUpdate != null && (currentTime - lastUpdate) < 30000) {
                // Use cached trades if available
                Map<Integer, TradeOffer> cached = modifiedTrades.get(villagerUUID);
                if (cached != null) {
                    for (int i = 0; i < trades.size() && i < cached.size(); i++) {
                        TradeOffer cachedTrade = cached.get(i);
                        if (cachedTrade != null) {
                            trades.set(i, cachedTrade);
                        }
                    }
                }
                return;
            }
            
            VillagerAIIntegration aiIntegration = VillagerAIIntegration.getInstance();
            VillagerBrain brain = aiIntegration.getVillagerBrain(villagerUUID);
            
            if (brain == null) {
                LOGGER.debug("No AI brain found for villager {}, using default trades", villagerUUID);
                return;
            }
            
            RelationshipData relationship = brain.getRelationship(playerUUID);
            PersonalityProfile personality = brain.getPersonalityTraits();
            
            // For now, just adjust pricing based on relationship
            float priceModifier = calculateSimplePriceModifier(relationship, brain);
            
            Map<Integer, TradeOffer> modifiedTradesMap = new ConcurrentHashMap<>();
            
            // For now, we'll just log the price adjustments since TradeOffer modification is complex
            for (int i = 0; i < trades.size(); i++) {
                TradeOffer originalTrade = trades.get(i);
                LOGGER.debug("Trade {}: Base price modifier {} for villager {} (relationship: trust={}, friendship={})", 
                    i, priceModifier, brain.getVillagerName(), 
                    relationship.getTrustLevel(), relationship.getFriendshipLevel());
                
                // For now, keep original trades but record the intended modifications
                modifiedTradesMap.put(i, originalTrade);
            }
            
            // Cache the results
            modifiedTrades.put(villagerUUID, modifiedTradesMap);
            lastTradeUpdate.put(villagerUUID, currentTime);
            
            LOGGER.debug("Processed {} trades for villager {} with modifier {}", 
                modifiedTradesMap.size(), brain.getVillagerName(), priceModifier);
            
        } catch (Exception e) {
            LOGGER.error("Failed to modify trade offers for villager {}", villager.getUuid(), e);
        }
    }
    
    /**
     * Calculates price modifier based on AI factors
     */
    private float calculatePriceModifier(RelationshipData relationship, PersonalityProfile personality, VillagerBrain brain) {
        float modifier = 1.0f;
        
        // Relationship affects pricing
        if (relationship != null) {
            float trustLevel = relationship.getTrustLevel();
            float friendshipLevel = relationship.getFriendshipLevel();
            
            // Good relationships get better prices
            float relationshipBonus = (trustLevel + friendshipLevel) / 2.0f;
            modifier -= relationshipBonus * 0.2f; // Up to 20% discount for great relationships
        }
        
        // Personality affects pricing using available trait system
        if (personality != null) {
            // Use available trait types from the personality system
            float helpfulness = personality.getTraitInfluence(com.beeny.villagesreborn.core.ai.TraitType.HELPFULNESS);
            float friendliness = personality.getTraitInfluence(com.beeny.villagesreborn.core.ai.TraitType.FRIENDLINESS);
            
            // Helpful and friendly villagers give better prices
            if (helpfulness > 0.7f) {
                modifier -= 0.05f; // 5% discount for helpful villagers
            }
            if (friendliness > 0.7f) {
                modifier -= 0.05f; // 5% discount for friendly villagers
            }
        }
        
        // Mood affects pricing slightly
        var mood = brain.getCurrentMood();
        if (mood != null) {
            float happiness = mood.getHappiness();
            if (happiness > 0.7f) {
                modifier -= 0.05f; // Small discount when happy
            } else if (happiness < 0.3f) {
                modifier += 0.05f; // Small markup when unhappy
            }
        }
        
        // Ensure modifier stays within reasonable bounds
        return Math.max(0.5f, Math.min(1.5f, modifier));
    }
    
    /**
     * Simple price modifier calculation
     */
    private float calculateSimplePriceModifier(RelationshipData relationship, VillagerBrain brain) {
        return calculatePriceModifier(relationship, brain.getPersonalityTraits(), brain);
    }
    
    /**
     * Modifies a traded item based on price modifier (simplified for now)
     */
    private String getTradeItemDescription(TradedItem item) {
        try {
            return item.item().value().toString();
        } catch (Exception e) {
            return "Unknown Item";
        }
    }
    
    /**
     * Records a trade interaction for AI learning
     */
    public void recordTradeInteraction(VillagerEntity villager, UUID playerUUID, boolean successful) {
        if (!enabled) {
            return;
        }
        
        try {
            VillagerAIIntegration aiIntegration = VillagerAIIntegration.getInstance();
            VillagerBrain brain = aiIntegration.getVillagerBrain(villager.getUuid());
            
            if (brain != null) {
                String tradeDescription = "Player trade interaction";
                brain.recordMemory(tradeDescription);
                
                if (successful) {
                    // Positive trade interaction improves relationship
                    aiIntegration.updateVillagerRelationship(villager.getUuid(), playerUUID, 0.1f, "successful_trade");
                }
                
                LOGGER.debug("Recorded trade interaction for villager {}: {}", brain.getVillagerName(), tradeDescription);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to record trade interaction", e);
        }
    }
    
    /**
     * Clears cached trades for a villager (useful when they level up or restock)
     */
    public void clearTradeCache(UUID villagerUUID) {
        modifiedTrades.remove(villagerUUID);
        lastTradeUpdate.remove(villagerUUID);
    }
    
    public String getTradingStats() {
        return String.format("Dynamic Trading: %s, Cached Trades: %d villagers", 
            enabled ? "Online" : "Offline", modifiedTrades.size());
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            // Clear all cached data when disabled
            modifiedTrades.clear();
            lastTradeUpdate.clear();
        }
        LOGGER.info("Dynamic Trading System {}", enabled ? "enabled" : "disabled");
    }
} 