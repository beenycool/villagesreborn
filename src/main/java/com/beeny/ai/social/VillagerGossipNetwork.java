package com.beeny.ai.social;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VillagerGossipNetwork {
    
    public enum GossipType {
        RELATIONSHIP_NEWS,
        WORK_PERFORMANCE,
        PERSONALITY_TRAITS,
        RECENT_INTERACTIONS,
        TRADE_EXPERIENCES,
        FAMILY_UPDATES,
        POSITIVE_INTERACTION,
        NEGATIVE_INTERACTION,
        ACHIEVEMENT,
        KINDNESS,
        AGGRESSION,
        MYSTERIOUS_BEHAVIOR
    }
    
    public static class GossipPiece {
        public final String content;
        public final String details;
        public final float reliability;
        public final GossipType type;
        
        public GossipPiece(@NotNull String content, float reliability, @NotNull GossipType type) {
            this.content = content;
            this.details = content;
            this.reliability = reliability;
            this.type = type;
        }
    }
    
    public static class GossipMessage {
        public final String fromVillager;
        public final String aboutVillager;
        public final GossipType type;
        public final String content;
        public final long timestamp;
        public final float reliability;
        
        public GossipMessage(@NotNull String fromVillager, @NotNull String aboutVillager, @NotNull GossipType type, @NotNull String content, float reliability) {
            this.fromVillager = fromVillager;
            this.aboutVillager = aboutVillager;
            this.type = type;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.reliability = reliability;
        }
    }
    
    private static final Map<String, List<GossipMessage>> villagerGossip = new HashMap<>();
    private static final long GOSSIP_EXPIRY_TIME = 24 * 60 * 60 * 1000; // 24 hours
    
    public static void shareGossip(@NotNull VillagerEntity from, @NotNull VillagerEntity about, @NotNull GossipType type, @NotNull String content, float reliability) {
        String fromUuid = from.getUuidAsString();
        String aboutUuid = about.getUuidAsString();
        
        GossipMessage message = new GossipMessage(fromUuid, aboutUuid, type, content, reliability);
        
        villagerGossip.computeIfAbsent(fromUuid, k -> new ArrayList<>()).add(message);
        
        // Spread gossip to nearby villagers
        spreadGossip(from.getWorld(), from.getBlockPos(), message);
    }
    
    public static List<GossipMessage> getGossipAbout(@NotNull String villagerUuid) {
        return villagerGossip.values().stream()
                .flatMap(List::stream)
                .filter(gossip -> gossip.aboutVillager.equals(villagerUuid))
                .filter(gossip -> System.currentTimeMillis() - gossip.timestamp < GOSSIP_EXPIRY_TIME)
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .toList();
    }
    
    public static List<GossipMessage> getGossipFrom(@NotNull String villagerUuid) {
        return villagerGossip.getOrDefault(villagerUuid, new ArrayList<>()).stream()
                .filter(gossip -> System.currentTimeMillis() - gossip.timestamp < GOSSIP_EXPIRY_TIME)
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .toList();
    }
    
    private static void spreadGossip(@NotNull net.minecraft.world.World world, @NotNull net.minecraft.util.math.BlockPos center, @NotNull GossipMessage message) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        
        // Find nearby villagers within 32 blocks
        List<VillagerEntity> nearbyVillagers = serverWorld.getEntitiesByClass(
            VillagerEntity.class,
            net.minecraft.util.math.Box.of(net.minecraft.util.math.Vec3d.ofCenter(center), 64, 64, 64),
            villager -> villager.squaredDistanceTo(net.minecraft.util.math.Vec3d.ofCenter(center)) <= 32 * 32
        );
        
        for (VillagerEntity villager : nearbyVillagers) {
            if (!villager.getUuidAsString().equals(message.fromVillager)) {
                // Reduce reliability as gossip spreads
                float newReliability = message.reliability * 0.8f;
                if (newReliability > 0.1f) {
                    GossipMessage spreadMessage = new GossipMessage(
                        villager.getUuidAsString(),
                        message.aboutVillager,
                        message.type,
                        message.content,
                        newReliability
                    );
                    villagerGossip.computeIfAbsent(villager.getUuidAsString(), k -> new ArrayList<>()).add(spreadMessage);
                }
            }
        }
    }
    
    public static float getReputationModifier(@NotNull String aboutVillager, @NotNull String fromPerspective) {
        List<GossipMessage> gossip = getGossipAbout(aboutVillager);
        float totalModifier = 0.0f;
        int count = 0;
        
        for (GossipMessage message : gossip) {
            if (message.fromVillager.equals(fromPerspective)) continue;
            
            float modifier = switch (message.type) {
                case RELATIONSHIP_NEWS -> message.reliability * 0.5f;
                case WORK_PERFORMANCE -> message.reliability * 0.7f;
                case PERSONALITY_TRAITS -> message.reliability * 0.3f;
                case RECENT_INTERACTIONS -> message.reliability * 0.6f;
                case TRADE_EXPERIENCES -> message.reliability * 0.8f;
                case FAMILY_UPDATES -> message.reliability * 0.4f;
                default -> 0.0f;
            };
            
            totalModifier += modifier;
            count++;
        }
        
        return count > 0 ? totalModifier / count : 0.0f;
    }
    
    public static void cleanupExpiredGossip() {
        long currentTime = System.currentTimeMillis();
        
        villagerGossip.values().forEach(gossipList -> 
            gossipList.removeIf(gossip -> currentTime - gossip.timestamp > GOSSIP_EXPIRY_TIME)
        );
        
        // Remove empty lists
        villagerGossip.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    public static int getGlobalReputation(@NotNull String villagerUuid) {
        List<GossipMessage> gossip = getGossipAbout(villagerUuid);
        float totalReputation = 0.0f;
        int count = 0;
        
        for (GossipMessage message : gossip) {
            float modifier = switch (message.type) {
                case POSITIVE_INTERACTION -> message.reliability * 2.0f;
                case NEGATIVE_INTERACTION -> -message.reliability * 2.0f;
                case TRADE_EXPERIENCES -> message.reliability * 1.5f;
                case WORK_PERFORMANCE -> message.reliability * 1.0f;
                default -> message.reliability * 0.5f;
            };
            
            totalReputation += modifier;
            count++;
        }
        
        return count > 0 ? Math.round(totalReputation / count * 10) : 0; // Scale to -100 to 100
    }
    
    public static java.util.List<GossipPiece> getGossipAbout(@NotNull VillagerEntity villager, @NotNull String category) {
        String villagerUuid = villager.getUuidAsString();
        List<GossipMessage> messages = getGossipAbout(villagerUuid);
        
        return messages.stream()
                .filter(msg -> category.equals("player") || msg.type.name().toLowerCase().contains(category.toLowerCase()))
                .map(msg -> new GossipPiece(msg.content, msg.reliability, msg.type))
                .toList();
    }

    public static void clearGossipFor(@NotNull String villagerUuid) {
        villagerGossip.remove(villagerUuid);
        
        // Remove gossip about this villager
        villagerGossip.values().forEach(gossipList -> 
            gossipList.removeIf(gossip -> gossip.aboutVillager.equals(villagerUuid))
        );
    }
    
    // Add missing static methods called by other classes
    public static void createGossip(@NotNull VillagerEntity villager, @NotNull String subject, @NotNull GossipType type, @NotNull String details) {
        // Delegate to instance method through a manager if available
        // For now, create a basic gossip message
        String fromUuid = villager.getUuidAsString();
        GossipMessage message = new GossipMessage(fromUuid, subject, type, details, 1.0f);
        villagerGossip.computeIfAbsent(fromUuid, k -> new ArrayList<>()).add(message);
    }
    
    public static List<Text> generateGossipDialogue(@NotNull VillagerEntity villager, @NotNull PlayerEntity player) {
        List<Text> dialogue = new ArrayList<>();
        String playerUuid = player.getUuidAsString();
        String villagerUuid = villager.getUuidAsString();
        
        List<GossipMessage> relevantGossip = villagerGossip.getOrDefault(villagerUuid, new ArrayList<>())
            .stream()
            .filter(gossip -> !gossip.aboutVillager.equals(playerUuid))
            .limit(3)
            .toList();
            
        if (relevantGossip.isEmpty()) {
            dialogue.add(Text.literal("I don't have much news to share right now."));
        } else {
            for (GossipMessage gossip : relevantGossip) {
                dialogue.add(Text.literal("Well, " + gossip.content + " Have you heard about that?"));
            }
        }
        
        return dialogue;
    }
    
    public static class CommonGossipEvents {
        public static void playerTraded(@NotNull VillagerEntity villager, @NotNull PlayerEntity player, boolean fair) {
            GossipType type = fair ? GossipType.POSITIVE_INTERACTION : GossipType.NEGATIVE_INTERACTION;
            createGossip(villager, player.getUuidAsString(), type,
                "Player " + player.getName().getString() + " traded " + (fair ? "fairly" : "unfairly"));
        }
        
        public static void mysteriousActivity(@NotNull VillagerEntity villager, @NotNull String subject, @NotNull String details) {
            createGossip(villager, subject, GossipType.MYSTERIOUS_BEHAVIOR, details);
        }
    }
}