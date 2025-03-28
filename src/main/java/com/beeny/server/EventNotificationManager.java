package com.beeny.server;

import com.beeny.network.VillagesNetwork;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Server-side manager for event notifications.
 * Provides methods to send notifications to players.
 * This class acts as the main API for triggering notifications from the server.
 */
public class EventNotificationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("EventNotificationManager");
    private static EventNotificationManager instance;
    
    /**
     * Gets the singleton instance of the EventNotificationManager
     */
    public static EventNotificationManager getInstance() {
        if (instance == null) {
            instance = new EventNotificationManager();
        }
        return instance;
    }
    
    private EventNotificationManager() {
        // Private constructor for singleton
    }
    
    /**
     * Sends a notification to a specific player
     * 
     * @param player The player to send the notification to
     * @param title The title of the notification
     * @param description The description text
     * @param durationTicks How long to display the notification (in ticks)
     */
    public void sendNotification(ServerPlayerEntity player, String title, String description, int durationTicks) {
        if (player == null) {
            LOGGER.warn("Attempted to send notification to null player");
            return;
        }
        
        VillagesNetwork.sendEventNotificationToClient(player, title, description, durationTicks);
        LOGGER.debug("Sent notification '{}' to player {}", title, player.getName().getString());
    }
    
    /**
     * Broadcasts a notification to all players in a server
     * 
     * @param world The server world
     * @param title The title of the notification
     * @param description The description text
     * @param durationTicks How long to display the notification (in ticks)
     */
    public void broadcastNotification(ServerWorld world, String title, String description, int durationTicks) {
        if (world == null) {
            LOGGER.warn("Attempted to broadcast notification to null world");
            return;
        }
        
        List<ServerPlayerEntity> players = world.getServer().getPlayerManager().getPlayerList();
        for (ServerPlayerEntity player : players) {
            if (player.getWorld() == world) {
                sendNotification(player, title, description, durationTicks);
            }
        }
        
        LOGGER.debug("Broadcast notification '{}' to {} players in world {}", 
                title, players.size(), world.getRegistryKey().getValue());
    }
    
    /**
     * Broadcasts a notification to all players within a certain radius of a position
     * 
     * @param world The server world
     * @param position The center position
     * @param radius The radius in blocks
     * @param title The title of the notification
     * @param description The description text
     * @param durationTicks How long to display the notification (in ticks)
     * @return The number of players that received the notification
     */
    public int broadcastNotificationInRadius(ServerWorld world, BlockPos position, int radius,
                                          String title, String description, int durationTicks) {
        if (world == null || position == null) {
            LOGGER.warn("Attempted to broadcast notification with null world or position");
            return 0;
        }
        
        int count = 0;
        Vec3d center = new Vec3d(position.getX(), position.getY(), position.getZ());
        double radiusSquared = radius * radius;
        
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            if (player.getWorld() == world && player.getPos().squaredDistanceTo(center) <= radiusSquared) {
                sendNotification(player, title, description, durationTicks);
                count++;
            }
        }
        
        LOGGER.debug("Broadcast notification '{}' to {} players within {} blocks of {}", 
                title, count, radius, position);
        return count;
    }
    
    /**
     * Broadcasts a notification to a collection of players
     * 
     * @param players The players to send the notification to
     * @param title The title of the notification
     * @param description The description text
     * @param durationTicks How long to display the notification (in ticks)
     * @return The number of players that received the notification
     */
    public int broadcastNotificationToPlayers(Collection<ServerPlayerEntity> players,
                                          String title, String description, int durationTicks) {
        if (players == null || players.isEmpty()) {
            LOGGER.warn("Attempted to broadcast notification to null or empty player collection");
            return 0;
        }
        
        int count = 0;
        for (ServerPlayerEntity player : players) {
            sendNotification(player, title, description, durationTicks);
            count++;
        }
        
        LOGGER.debug("Broadcast notification '{}' to {} specified players", title, count);
        return count;
    }
}