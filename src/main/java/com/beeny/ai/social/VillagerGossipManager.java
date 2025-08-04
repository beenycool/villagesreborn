package com.beeny.ai.social;

import com.beeny.ai.AIWorldManager;
import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Instance-based gossip and reputation network where villagers share information and opinions
 * Creates emergent social dynamics and reputation consequences
 */
public class VillagerGossipManager {
    private final AIWorldManager worldManager;
    
    public enum GossipType {
        POSITIVE_INTERACTION("positive interaction", 5, 0.8f, true),
        NEGATIVE_INTERACTION("negative interaction", -8, 0.9f, true),
        TRADE_SUCCESS("successful trade", 3, 0.6f, true),
        TRADE_EXPLOIT("trade exploitation", -12, 0.95f, true),
        KINDNESS("act of kindness", 8, 0.7f, true),
        AGGRESSION("aggressive behavior", -15, 0.85f, true),
        HELPFULNESS("being helpful", 6, 0.65f, true),
        ROMANCE("romantic situation", 2, 0.9f, false), // High spread but neutral impact
        SCANDAL("village scandal", -5, 0.95f, false),
        ACHIEVEMENT("notable achievement", 10, 0.6f, true),
        MYSTERIOUS_BEHAVIOR("mysterious behavior", 0, 0.8f, false);
        
        public final String description;
        public final int reputationImpact;
        public final float spreadChance;
        public final boolean affectsReputation;
        
        GossipType(String description, int reputationImpact, float spreadChance, boolean affectsReputation) {
            this.description = description;
            this.reputationImpact = reputationImpact;
            this.spreadChance = spreadChance;
            this.affectsReputation = affectsReputation;
        }
    }
    
    public static class GossipPiece {
        public final String subject; // Usually player UUID
        public final String gossiper; // Villager UUID who started the gossip
        public final GossipType type;
        public final String details;
        public final long timestamp;
        public final Set<String> knownBy; // UUIDs of villagers who know this gossip
        public float credibility; // 0.0 to 1.0, decreases over time and with retellings
        
        public GossipPiece(String subject, String gossiper, GossipType type, String details) {
            this.subject = subject;
            this.gossiper = gossiper;
            this.type = type;
            this.details = details;
            this.timestamp = System.currentTimeMillis();
            this.knownBy = ConcurrentHashMap.newKeySet();
            this.credibility = 1.0f;
            this.knownBy.add(gossiper);
        }
        
        public boolean isRecent() {
            return (System.currentTimeMillis() - timestamp) < 600000; // 10 minutes
        }
        
        public void spreadTo(String villagerUuid) {
            knownBy.add(villagerUuid);
            // Credibility decreases slightly with each retelling
            credibility *= 0.95f;
        }
        
        public int getSpreadCount() {
            return knownBy.size();
        }
    }
    
    // Per-server gossip storage
    private final Map<String, List<GossipPiece>> villageGossip = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> globalReputation = new ConcurrentHashMap<>();
    
    public VillagerGossipManager(AIWorldManager worldManager) {
        this.worldManager = worldManager;
    }
    
    /**
     * Create and spread a new piece of gossip
     */
    public void createGossip(VillagerEntity gossiper, String subject, GossipType type, String details) {
        if (gossiper == null || subject == null) return;
        
        String villageName = getVillageName(gossiper);
        GossipPiece gossip = new GossipPiece(subject, gossiper.getUuidAsString(), type, details);
        
        // Add to village gossip
        villageGossip.computeIfAbsent(villageName, k -> new ArrayList<>()).add(gossip);
        
        // Update the gossiper's emotional state
        worldManager.processVillagerEmotionalEvent(gossiper, 
            new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.EXCITEMENT, 10.0f, "sharing_gossip", false));
        
        // Immediately try to spread to nearby villagers
        spreadGossip(gossiper, gossip);
        
        // Update global reputation if applicable
        if (type.affectsReputation) {
            updateGlobalReputation(subject, type.reputationImpact);
        }
    }
    
    /**
     * Spread gossip to nearby villagers based on personality and relationships
     */
    private void spreadGossip(VillagerEntity spreader, GossipPiece gossip) {
        List<VillagerEntity> nearbyVillagers = spreader.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            spreader.getBoundingBox().expand(15.0),
            v -> v != spreader && v.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA) != null
        );
        
        VillagerData spreaderData = spreader.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (spreaderData == null) return;
        
        for (VillagerEntity listener : nearbyVillagers) {
            VillagerData listenerData = listener.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (listenerData == null) continue;
            
            // Calculate spread probability
            float baseChance = gossip.type.spreadChance;
            float personalityModifier = getGossipPersonalityModifier(spreaderData.getPersonality());
            float relationshipModifier = getRelationshipModifier(spreaderData, listenerData);
            float listenerInterestModifier = getListenerInterest(listenerData, gossip);
            
            float spreadChance = baseChance * personalityModifier * relationshipModifier * listenerInterestModifier;
            
            if (ThreadLocalRandom.current().nextFloat() < spreadChance) {
                gossip.spreadTo(listener.getUuidAsString());
                
                // Listener gets emotional reaction to hearing gossip
                VillagerEmotionSystem.EmotionType reactionEmotion = getGossipEmotionalReaction(gossip.type);
                if (reactionEmotion != null) {
                    worldManager.processVillagerEmotionalEvent(listener,
                        new VillagerEmotionSystem.EmotionalEvent(reactionEmotion, 5.0f, "hearing_gossip", false));
                }
                
                // Update listener's opinion of the subject
                if (gossip.type.affectsReputation) {
                    listenerData.updatePlayerRelation(gossip.subject, gossip.type.reputationImpact / 2);
                }
            }
        }
    }
    
    private float getGossipPersonalityModifier(String personality) {
        return switch (personality) {
            case "Friendly", "Cheerful" -> 1.4f; // Love to gossip
            case "Curious" -> 1.6f; // Very interested in information
            case "Grumpy" -> 1.2f; // Enjoys negative gossip
            case "Shy", "Nervous" -> 0.6f; // Less likely to share
            case "Serious" -> 0.8f; // More reserved
            default -> 1.0f;
        };
    }
    
    private float getRelationshipModifier(VillagerData spreader, VillagerData listener) {
        // Check if they're family
        if (spreader.getFamilyMembers().contains(listener.getName()) ||
            listener.getFamilyMembers().contains(spreader.getName())) {
            return 1.5f; // Family shares more
        }
        
        // Check spouse relationship
        if (spreader.getSpouseName().equals(listener.getName()) ||
            listener.getSpouseName().equals(spreader.getName())) {
            return 1.8f; // Spouses share everything
        }
        
        return 1.0f;
    }
    
    private float getListenerInterest(VillagerData listener, GossipPiece gossip) {
        // More interested in gossip about players they know
        int playerReputation = listener.getPlayerReputation(gossip.subject);
        if (Math.abs(playerReputation) > 20) {
            return 1.3f; // Already has strong opinion
        }
        
        // Personality affects interest
        return switch (listener.getPersonality()) {
            case "Curious" -> 1.5f;
            case "Friendly", "Cheerful" -> 1.2f;
            case "Grumpy" -> gossip.type.reputationImpact < 0 ? 1.4f : 0.8f; // Prefers negative gossip
            case "Shy" -> 0.7f;
            default -> 1.0f;
        };
    }
    
    private VillagerEmotionSystem.EmotionType getGossipEmotionalReaction(GossipType gossipType) {
        return switch (gossipType) {
            case POSITIVE_INTERACTION, ACHIEVEMENT, KINDNESS -> VillagerEmotionSystem.EmotionType.HAPPINESS;
            case NEGATIVE_INTERACTION, AGGRESSION, TRADE_EXPLOIT -> VillagerEmotionSystem.EmotionType.ANGER;
            case ROMANCE -> VillagerEmotionSystem.EmotionType.EXCITEMENT;
            case SCANDAL -> VillagerEmotionSystem.EmotionType.CURIOSITY;
            case MYSTERIOUS_BEHAVIOR -> VillagerEmotionSystem.EmotionType.CURIOSITY;
            default -> null;
        };
    }
    
    /**
     * Update global reputation that affects all villagers' opinions
     */
    private void updateGlobalReputation(String playerUuid, int change) {
        Map<String, Integer> playerRep = globalReputation.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        playerRep.put("global", playerRep.getOrDefault("global", 0) + change);
        
        // Clamp between -200 and 200
        int newRep = Math.max(-200, Math.min(200, playerRep.get("global")));
        playerRep.put("global", newRep);
    }
    
    /**
     * Get player's global reputation
     */
    public int getGlobalReputation(String playerUuid) {
        return globalReputation.getOrDefault(playerUuid, new HashMap<>()).getOrDefault("global", 0);
    }
    
    /**
     * Apply global reputation to a villager's opinion when they first meet a player
     */
    public void applyGlobalReputation(VillagerEntity villager, PlayerEntity player) {
        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        String playerUuid = player.getUuidAsString();
        int currentReputation = data.getPlayerReputation(playerUuid);
        
        // Only apply global reputation if villager doesn't have strong personal opinion
        if (Math.abs(currentReputation) < 20) {
            int globalRep = getGlobalReputation(playerUuid);
            int reputationToApply = (int) (globalRep * 0.3f); // 30% of global reputation
            
            data.updatePlayerRelation(playerUuid, reputationToApply);
        }
    }
    
    /**
     * Get gossip about a specific player that a villager knows
     */
    public List<GossipPiece> getGossipAbout(VillagerEntity villager, String subject) {
        String villageName = getVillageName(villager);
        List<GossipPiece> villageGossipList = villageGossip.getOrDefault(villageName, new ArrayList<>());
        String villagerUuid = villager.getUuidAsString();
        
        return villageGossipList.stream()
            .filter(g -> g.subject.equals(subject) && g.knownBy.contains(villagerUuid))
            .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
            .toList();
    }
    
    /**
     * Generate gossip dialogue for a villager
     */
    public List<Text> generateGossipDialogue(VillagerEntity villager, PlayerEntity player) {
        List<Text> dialogue = new ArrayList<>();
        String playerUuid = player.getUuidAsString();
        String villageName = getVillageName(villager);
        
        List<GossipPiece> recentGossip = villageGossip.getOrDefault(villageName, new ArrayList<>())
            .stream()
            .filter(g -> g.knownBy.contains(villager.getUuidAsString()) && g.isRecent())
            .filter(g -> !g.subject.equals(playerUuid)) // Don't gossip about the player they're talking to
            .limit(3)
            .toList();
        
        if (recentGossip.isEmpty()) {
            dialogue.add(Text.literal("I don't have much news to share right now.").formatted(Formatting.GRAY));
            return dialogue;
        }
        
        VillagerData villagerData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        String personality = villagerData != null ? villagerData.getPersonality() : "Friendly";
        
        for (GossipPiece gossip : recentGossip) {
            String gossipText = formatGossipForPersonality(gossip, personality);
            Formatting color = gossip.type.reputationImpact > 0 ? Formatting.GREEN : 
                             gossip.type.reputationImpact < 0 ? Formatting.RED : Formatting.YELLOW;
            dialogue.add(Text.literal(gossipText).formatted(color));
        }
        
        return dialogue;
    }
    
    private String formatGossipForPersonality(GossipPiece gossip, String personality) {
        String baseText = switch (gossip.type) {
            case POSITIVE_INTERACTION -> "I heard someone was very kind recently...";
            case NEGATIVE_INTERACTION -> "There's been some troubling behavior around here...";
            case TRADE_SUCCESS -> "Word is, someone made an excellent trade!";
            case TRADE_EXPLOIT -> "I heard someone was taking advantage in trades...";
            case ROMANCE -> "There might be some romance blooming in the village...";
            case ACHIEVEMENT -> "Someone accomplished something remarkable!";
            case MYSTERIOUS_BEHAVIOR -> "Strange things have been happening lately...";
            default -> "I heard something interesting...";
        };
        
        // Modify based on personality
        return switch (personality) {
            case "Grumpy" -> baseText.replace("excellent", "decent").replace("remarkable", "noteworthy");
            case "Shy" -> "*whispers* " + baseText.toLowerCase();
            case "Energetic" -> baseText.replace(".", "!").toUpperCase();
            case "Curious" -> "Did you know? " + baseText;
            default -> baseText;
        };
    }
    
    /**
     * Cleanup old gossip to prevent memory leaks
     */
    public void cleanupOldGossip() {
        long cutoffTime = System.currentTimeMillis() - 3600000; // 1 hour
        
        for (List<GossipPiece> gossipList : villageGossip.values()) {
            gossipList.removeIf(g -> g.timestamp < cutoffTime);
        }
        
        // Remove empty village entries
        villageGossip.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Get village name based on villager's location
     */
    private String getVillageName(VillagerEntity villager) {
        // For now, use simple coordinate-based village naming
        // Could be enhanced to use actual village detection
        int chunkX = villager.getChunkPos().x;
        int chunkZ = villager.getChunkPos().z;
        return "village_" + (chunkX / 4) + "_" + (chunkZ / 4);
    }
    
    /**
     * Common gossip events for easy integration
     */
    public static class CommonGossipEvents {
        public static void playerTraded(VillagerGossipManager manager, VillagerEntity villager, PlayerEntity player, boolean fair) {
            GossipType type = fair ? GossipType.TRADE_SUCCESS : GossipType.TRADE_EXPLOIT;
            manager.createGossip(villager, player.getUuidAsString(), type, 
                "Player " + player.getName().getString() + " " + type.description);
        }
        
        public static void playerHelped(VillagerGossipManager manager, VillagerEntity villager, PlayerEntity player) {
            manager.createGossip(villager, player.getUuidAsString(), GossipType.KINDNESS,
                "Player " + player.getName().getString() + " was very helpful");
        }
        
        public static void playerAttacked(VillagerGossipManager manager, VillagerEntity villager, PlayerEntity player) {
            manager.createGossip(villager, player.getUuidAsString(), GossipType.AGGRESSION,
                "Player " + player.getName().getString() + " was aggressive");
        }
        
        public static void villagerMarried(VillagerGossipManager manager, VillagerEntity villager1, VillagerEntity villager2) {
            VillagerData data1 = villager1.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerData data2 = villager2.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            if (data1 != null && data2 != null) {
                manager.createGossip(villager1, villager2.getUuidAsString(), GossipType.ROMANCE,
                    data1.getName() + " and " + data2.getName() + " got married!");
            }
        }
        
        public static void mysteriousActivity(VillagerGossipManager manager, VillagerEntity villager, String subject, String details) {
            manager.createGossip(villager, subject, GossipType.MYSTERIOUS_BEHAVIOR, details);
        }
    }
    
    /**
     * Get comprehensive reputation report for a player
     */
    public Map<String, Object> getReputationReport(String playerUuid) {
        Map<String, Object> report = new HashMap<>();
        
        report.put("globalReputation", getGlobalReputation(playerUuid));
        
        // Count gossip by type
        Map<GossipType, Integer> gossipCounts = new HashMap<>();
        for (List<GossipPiece> gossipList : villageGossip.values()) {
            for (GossipPiece gossip : gossipList) {
                if (gossip.subject.equals(playerUuid)) {
                    gossipCounts.put(gossip.type, gossipCounts.getOrDefault(gossip.type, 0) + 1);
                }
            }
        }
        report.put("gossipCounts", gossipCounts);
        
        return report;
    }
    
    // Methods for AIWorldManager integration
    public void initializeVillagerGossip(VillagerEntity villager, VillagerData data) {
        // No specific initialization needed for gossip system
    }
    
    public void updateVillagerGossip(VillagerEntity villager) {
        // No regular updates needed for gossip system
    }
    
    public void cleanupVillager(String uuid) {
        // No specific cleanup needed for individual villagers in gossip system
    }
    
    public void performMaintenance() {
        // Cleanup old gossip periodically
        cleanupOldGossip();
    }
    
    public void shutdown() {
        // Clear all gossip data on shutdown
        villageGossip.clear();
        globalReputation.clear();
    }
    
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("village_gossip_count", villageGossip.size());
        analytics.put("global_reputation_count", globalReputation.size());
        return analytics;
    }
}