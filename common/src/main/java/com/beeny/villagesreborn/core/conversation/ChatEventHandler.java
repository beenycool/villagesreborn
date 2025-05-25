package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.ai.VillagerBrainManager;
import com.beeny.villagesreborn.core.ai.VillagerProximityDetector;
import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.ServerChatEvent;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles chat events to intercept player messages and route them to villagers
 * when addressed with @villager prefix or overheard by proximity
 */
public class ChatEventHandler {
    
    public static final int CHAT_RADIUS = 16;
    
    private static final Pattern VILLAGER_ADDRESS_PATTERN = Pattern.compile(
        "(?i)^@villager\\b|\\b(?:farmer|librarian|blacksmith|cleric|butcher|fletcher|leatherworker|stone)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    private final VillagerProximityDetector proximityDetector;
    private final VillagerBrainManager brainManager;
    private final ConversationRouter conversationRouter;
    
    public ChatEventHandler(VillagerProximityDetector proximityDetector, 
                           VillagerBrainManager brainManager,
                           ConversationRouter conversationRouter) {
        this.proximityDetector = proximityDetector;
        this.brainManager = brainManager;
        this.conversationRouter = conversationRouter;
    }
    
    /**
     * Processes server chat events to detect villager interactions
     */
    public void onServerChatReceived(ServerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Find nearby villagers
        List<VillagerEntity> nearbyVillagers = findNearbyVillagers(player);
        
        if (nearbyVillagers.isEmpty()) {
            return; // No villagers nearby, let chat proceed normally
        }
        
        if (isDirectedAtVillager(message)) {
            // Message is directed at villagers
            VillagerEntity targetVillager = selectTargetVillager(nearbyVillagers, player, message);
            if (targetVillager != null) {
                conversationRouter.routeMessage(player, targetVillager, message);
                event.cancel(); // Cancel the chat event to prevent broadcast
            }
        } else {
            // Normal chat - process as overheard by nearby villagers
            for (VillagerEntity villager : nearbyVillagers) {
                brainManager.processOverheardMessage(villager, player, message);
            }
            // Don't cancel event - let normal chat proceed
        }
    }
    
    /**
     * Checks if a message is directed at villagers
     */
    public boolean isDirectedAtVillager(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        
        return VILLAGER_ADDRESS_PATTERN.matcher(message).find();
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