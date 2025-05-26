package com.beeny.villagesreborn.core.common;

/**
 * Represents a server chat event that can be intercepted and cancelled
 */
public interface ServerChatEvent {
    
    /**
     * Gets the player who sent the chat message
     * @return the player
     */
    Player getPlayer();
    
    /**
     * Gets the chat message content
     * @return the message string
     */
    String getMessage();
    
    /**
     * Gets the timestamp when the message was sent
     * @return timestamp in milliseconds
     */
    long getTimestamp();
    
    /**
     * Gets the world where the message was sent
     * @return world identifier string
     */
    String getWorld();
    
    /**
     * Cancels the chat event, preventing it from being broadcast
     */
    void cancel();
    
    /**
     * Checks if the event has been cancelled
     * @return true if cancelled
     */
    boolean isCancelled();
}