package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerMemoryEnhancer {
    
    private static final List<String> CONVERSATION_TOPICS = Arrays.asList(
        "weather", "trading", "family", "work", "village", "food", "travel", 
        "crafting", "farming", "gossip", "books", "magic", "monsters", "building"
    );
    
    public static void updatePlayerMemory(VillagerEntity villager, PlayerEntity player, String interactionType) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        String playerUuid = player.getUuidAsString();
        String playerName = player.getName().getString();
        
        // Update conversation time
        data.setLastConversationTime(System.currentTimeMillis());
        
        // Generate contextual memory based on interaction type and current state
        String newMemory = generateContextualMemory(data, playerName, interactionType);
        
        // Update or append to existing memory
        String existingMemory = data.getPlayerMemory(playerUuid);
        if (existingMemory.isEmpty()) {
            data.setPlayerMemory(playerUuid, newMemory);
        } else {
            // Combine memories, keeping the most recent and relevant
            String combinedMemory = combineMemories(existingMemory, newMemory);
            data.setPlayerMemory(playerUuid, combinedMemory);
        }
        
        // Update topic frequency
        String inferredTopic = inferConversationTopic(interactionType, data);
        if (!inferredTopic.isEmpty()) {
            int currentCount = data.getTopicFrequency().getOrDefault(inferredTopic, 0);
            data.updateTopicFrequency(inferredTopic, currentCount + 1);
        }
        
        // Add recent event if significant
        if (isSignificantInteraction(data, playerUuid, interactionType)) {
            String eventDescription = generateEventDescription(data, playerName, interactionType);
            data.addRecentEvent(eventDescription);
        }
    }
    
    private static String generateContextualMemory(VillagerData data, String playerName, String interactionType) {
        StringBuilder memory = new StringBuilder();
        
        switch (interactionType.toLowerCase()) {
            case "trade" -> {
                memory.append(playerName).append(" traded with me");
                if (data.getHappiness() > 70) {
                    memory.append(" and I enjoyed our business");
                } else if (data.getHappiness() < 40) {
                    memory.append(" but I wasn't feeling my best");
                }
                memory.append(". They seem ");
                
                int reputation = data.getPlayerReputation(playerName);
                if (reputation > 50) {
                    memory.append("trustworthy and fair");
                } else if (reputation > 20) {
                    memory.append("decent enough");
                } else if (reputation < -10) {
                    memory.append("questionable in their dealings");
                } else {
                    memory.append("new to me");
                }
            }
            case "greeting" -> {
                memory.append(playerName).append(" greeted me");
                
                // Add personality-specific memory details
                switch (data.getPersonality()) {
                    case FRIENDLY -> memory.append(" warmly, and I was happy to see them");
                    case SHY -> memory.append(", though I felt a bit nervous");
                    case GRUMPY -> memory.append(", interrupting my thoughts");
                    case CHEERFUL -> memory.append(", brightening my day");
                    case MELANCHOLY -> memory.append(", though I wasn't in the mood for company");
                    default -> memory.append(" politely");
                }
            }
            case "gift" -> {
                memory.append(playerName).append(" gave me something");
                if (data.getHappiness() > 60) {
                    memory.append(" and I was delighted by their kindness");
                } else {
                    memory.append(", which was thoughtful of them");
                }
            }
            case "conversation" -> {
                memory.append("I had a conversation with ").append(playerName);
                
                // Add hobby-related context
                if (ThreadLocalRandom.current().nextFloat() < 0.3f) {
                    memory.append(". We talked about ").append(data.getHobby().name().toLowerCase());
                }
                
                // Add emotional context
                String dominantEmotion = data.getEmotionalState().getDominantEmotion().name();
                if (!dominantEmotion.isEmpty() && !dominantEmotion.equals("neutral")) {
                    memory.append(". I was feeling ").append(dominantEmotion.toLowerCase())
                        .append(" at the time");
                }
            }
            default -> {
                memory.append("I interacted with ").append(playerName);
                if (data.getHappiness() > 50) {
                    memory.append(" and it was pleasant");
                }
            }
        }
        
        return memory.toString();
    }
    
    private static String combineMemories(String existingMemory, String newMemory) {
        // Keep memories concise - if existing is too long, summarize or replace
        if (existingMemory.length() > 200) {
            // Replace with new memory and a summary of old
            return newMemory + ". (Previously: " + existingMemory.substring(0, 50) + "...)";
        } else {
            // Append new memory
            return existingMemory + " " + newMemory;
        }
    }
    
    private static String inferConversationTopic(String interactionType, VillagerData data) {
        switch (interactionType.toLowerCase()) {
            case "trade" -> { return "trading"; }
            case "greeting" -> { return "social"; }
            case "gift" -> { return "gifts"; }
            case "family" -> { return "family"; }
            case "work" -> { return "work"; }
            default -> {
                // Infer based on hobby or personality
                if (ThreadLocalRandom.current().nextFloat() < 0.5f) {
                    return data.getHobby().name().toLowerCase();
                } else {
                    // Random topic
                    return CONVERSATION_TOPICS.get(ThreadLocalRandom.current().nextInt(CONVERSATION_TOPICS.size()));
                }
            }
        }
    }
    
    private static boolean isSignificantInteraction(VillagerData data, String playerUuid, String interactionType) {
        // First interaction is always significant
        if (data.getPlayerMemory(playerUuid).isEmpty()) {
            return true;
        }
        
        // High/low reputation players are more significant
        int reputation = data.getPlayerReputation(playerUuid);
        if (Math.abs(reputation) > 40) {
            return ThreadLocalRandom.current().nextFloat() < 0.4f;
        }
        
        // Trades and gifts are often significant
        if (interactionType.equals("trade") || interactionType.equals("gift")) {
            return ThreadLocalRandom.current().nextFloat() < 0.3f;
        }
        
        // Random chance for other interactions
        return ThreadLocalRandom.current().nextFloat() < 0.1f;
    }
    
    private static String generateEventDescription(VillagerData data, String playerName, String interactionType) {
        switch (interactionType.toLowerCase()) {
            case "trade" -> {
                if (data.getTotalTrades() == 1) {
                    return "Completed my very first trade with " + playerName;
                } else if (data.getTotalTrades() % 25 == 0) {
                    return "Reached " + data.getTotalTrades() + " total trades, with " + playerName + " as my latest customer";
                } else {
                    return "Had a " + (data.getHappiness() > 60 ? "pleasant" : "challenging") + " trade with " + playerName;
                }
            }
            case "gift" -> {
                return "Received a thoughtful gift from " + playerName;
            }
            case "greeting" -> {
                if (data.getPlayerReputation(playerName) > 50) {
                    return "Was visited by my good friend " + playerName;
                } else {
                    return "Met " + playerName + " for the first time";
                }
            }
            default -> {
                return "Had an interesting encounter with " + playerName;
            }
        }
    }
    
    public static void updateMemoryBasedOnMood(VillagerData data) {
        // Update memories based on current emotional state
        String dominantEmotion = data.getEmotionalState().getDominantEmotion().name();
        
        if (dominantEmotion.equals("sadness") && ThreadLocalRandom.current().nextFloat() < 0.1f) {
            data.addRecentEvent("Felt melancholy today, remembering better times");
        } else if (dominantEmotion.equals("joy") && ThreadLocalRandom.current().nextFloat() < 0.1f) {
            data.addRecentEvent("Had a wonderful day, feeling grateful for my life");
        } else if (dominantEmotion.equals("anxiety") && ThreadLocalRandom.current().nextFloat() < 0.05f) {
            data.addRecentEvent("Felt worried about various things today");
        }
    }
    
    public static void clearOldMemories(VillagerData data) {
        // Occasionally clear very old events to keep memory fresh
        if (data.getRecentEvents().size() > 10 && ThreadLocalRandom.current().nextFloat() < 0.1f) {
            // Remove oldest event - but we need to be careful since getRecentEvents() returns unmodifiable list
            // This would be handled by the addRecentEvent method which already trims old events
        }
        
        // Clean up topic frequency that's too old
        if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
            // We can't directly modify the unmodifiable map, but we can create a new one
            // This method would need to be called from VillagerData itself for proper cleanup
        }
    }
}