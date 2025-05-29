package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages delivery of AI responses with rate limiting and formatting
 * Routes responses via chat or UI based on configuration
 */
public class ResponseDeliveryManager {
    
    private static final long RATE_LIMIT_MS = 3000; // 3 seconds between responses per villager
    private static final int MAX_RESPONSE_LENGTH = 200;
    
    private final ConcurrentHashMap<String, AtomicLong> lastResponseTimes = new ConcurrentHashMap<>();
    
    /**
     * Delivers a villager response to a player with rate limiting
     */
    public void deliverResponse(VillagerEntity villager, Player player, String response) {
        String villagerId = getVillagerId(villager);
        
        // Check rate limiting
        if (isRateLimited(villagerId)) {
            return;
        }
        
        // Format and deliver response
        String formattedResponse = formatResponse(villager, response);
        sendToPlayer(player, formattedResponse);
        
        // Update rate limiting
        updateLastResponseTime(villagerId);
    }
    
    /**
     * Checks if a villager is rate limited
     */
    private boolean isRateLimited(String villagerId) {
        AtomicLong lastTime = lastResponseTimes.get(villagerId);
        if (lastTime == null) {
            return false;
        }
        
        long timeSinceLastResponse = System.currentTimeMillis() - lastTime.get();
        return timeSinceLastResponse < RATE_LIMIT_MS;
    }
    
    /**
     * Updates the last response time for a villager
     */
    private void updateLastResponseTime(String villagerId) {
        lastResponseTimes.computeIfAbsent(villagerId, k -> new AtomicLong())
            .set(System.currentTimeMillis());
    }
    
    /**
     * Formats a response with villager name and appropriate styling
     */
    private String formatResponse(VillagerEntity villager, String response) {
        // Truncate if too long
        if (response.length() > MAX_RESPONSE_LENGTH) {
            response = response.substring(0, MAX_RESPONSE_LENGTH - 3) + "...";
        }
        
        String villagerName = getVillagerDisplayName(villager);
        return String.format("[%s] %s", villagerName, response);
    }
    
    /**
     * Sends the formatted response to the player
     * In a real implementation, this would use the appropriate chat/UI system
     */
    private void sendToPlayer(Player player, String message) {
        // Use the platform-agnostic sendMessage method
        try {
            player.sendMessage(message);
        } catch (Exception e) {
            // Fallback to console logging if platform implementation fails
            System.err.println("Failed to send message to " + player.getName() + ": " + e.getMessage());
            System.out.println("To " + player.getName() + ": " + message);
        }
    }
    
    /**
     * Gets a unique identifier for a villager
     */
    private String getVillagerId(VillagerEntity villager) {
        // Use UUID if available, otherwise position-based ID
        return villager.getUUID() != null ?
            villager.getUUID().toString() :
            "villager_" + villager.getBlockPos().getX() + "_" + villager.getBlockPos().getZ();
    }
    
    /**
     * Gets display name for a villager
     */
    private String getVillagerDisplayName(VillagerEntity villager) {
        String name = villager.getName();
        String profession = villager.getProfession();
        
        if (name != null && !name.isEmpty()) {
            return name;
        } else if (profession != null && !profession.isEmpty()) {
            return profession;
        } else {
            return "Villager";
        }
    }
    
    /**
     * Clears rate limiting data (for testing)
     */
    public void clearRateLimiting() {
        lastResponseTimes.clear();
    }
    
    /**
     * Sets custom rate limit for testing
     */
    public void setRateLimit(String villagerId, long timeMs) {
        lastResponseTimes.computeIfAbsent(villagerId, k -> new AtomicLong())
            .set(System.currentTimeMillis() - RATE_LIMIT_MS + timeMs);
    }
}
