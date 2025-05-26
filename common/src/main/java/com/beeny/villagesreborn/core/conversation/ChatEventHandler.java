package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.ai.VillagerBrainManager;
import com.beeny.villagesreborn.core.ai.VillagerProximityDetector;
import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.ServerChatEvent;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced ChatEventHandler with configurable regex patterns and asynchronous routing
 * Filters AI-triggering chat via configurable regex and routes matching messages asynchronously
 */
public class ChatEventHandler {
    
    public static final int CHAT_RADIUS = 16;
    public static final double CONVERSATION_RANGE = 8.0;
    
    // Default AI trigger patterns
    private static final List<String> DEFAULT_PATTERNS = List.of(
        "(?i)hello|hi|hey",
        "(?i)@villager",
        "\\?$",  // Questions ending with ?
        "(?i)help",
        "(?i)\\b(?:farmer|librarian|blacksmith|cleric|butcher|fletcher|leatherworker|stone)\\b"
    );
    
    private final VillagerProximityDetector proximityDetector;
    private final VillagerBrainManager brainManager;
    private final ConversationRouter conversationRouter;
    private List<Pattern> aiTriggerPatterns;
    
    public ChatEventHandler(VillagerProximityDetector proximityDetector,
                           VillagerBrainManager brainManager,
                           ConversationRouter conversationRouter) {
        this.proximityDetector = proximityDetector;
        this.brainManager = brainManager;
        this.conversationRouter = conversationRouter;
        this.aiTriggerPatterns = compilePatterns(DEFAULT_PATTERNS);
    }
    
    /**
     * Compiles a list of regex patterns
     */
    private List<Pattern> compilePatterns(List<String> patterns) {
        List<Pattern> compiledPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            try {
                compiledPatterns.add(Pattern.compile(pattern));
            } catch (Exception e) {
                // Log error and skip invalid pattern
                System.err.println("Invalid regex pattern: " + pattern + " - " + e.getMessage());
            }
        }
        return compiledPatterns;
    }
    
    /**
     * Updates AI trigger patterns dynamically
     */
    public void updateTriggerPatterns(List<String> patterns) {
        this.aiTriggerPatterns = compilePatterns(patterns);
    }
    
    /**
     * Enhanced onServerChatEvent with async routing and improved filtering
     */
    public void onServerChatEvent(ServerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        
        if (shouldTriggerAI(message)) {
            ConversationContext context = new ConversationContext(
                sender, message, event.getTimestamp(), event.getWorld()
            );
            
            // Route conversation asynchronously to prevent blocking
            CompletableFuture.runAsync(() -> {
                try {
                    conversationRouter.routeConversation(context);
                } catch (Exception e) {
                    System.err.println("Error routing conversation: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public void onServerChatReceived(ServerChatEvent event) {
        onServerChatEvent(event);
    }
    
    /**
     * Checks if a message should trigger AI based on configured patterns
     */
    public boolean shouldTriggerAI(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        return aiTriggerPatterns.stream()
            .anyMatch(pattern -> pattern.matcher(message).find());
    }
    
    /**
     * Checks if a message is directed at villagers (legacy method)
     */
    public boolean isDirectedAtVillager(String message) {
        return shouldTriggerAI(message);
    }
    
    /**
     * Finds villagers near the player within chat radius
     */
    public List<VillagerEntity> findNearbyVillagers(Player player) {
        return findNearbyVillagers(player, CHAT_RADIUS);
    }
    
    /**
     * Finds villagers near the player within specified radius
     */
    public List<VillagerEntity> findNearbyVillagers(Player player, int radius) {
        BlockPos playerPos = player.getBlockPos();
        return proximityDetector.findNearbyVillagers(playerPos, radius);
    }
    
    /**
     * Selects the target villager for a directed message
     * Currently selects the closest villager
     */
    public VillagerEntity selectTargetVillager(List<VillagerEntity> villagers, Player player, String message) {
        if (villagers.isEmpty()) {
            return null;
        }
        
        // For now, select the closest villager
        BlockPos playerPos = player.getBlockPos();
        VillagerEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (VillagerEntity villager : villagers) {
            BlockPos villagerPos = villager.getBlockPos();
            double distance = calculateDistance(playerPos, villagerPos);
            
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = villager;
            }
        }
        
        return closest;
    }
    
    /**
     * Calculates the distance between two block positions
     */
    private double calculateDistance(BlockPos pos1, BlockPos pos2) {
        int dx = pos1.getX() - pos2.getX();
        int dy = pos1.getY() - pos2.getY();
        int dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}