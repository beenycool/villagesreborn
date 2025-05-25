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
     * Cancels the chat event, preventing it from being broadcast
     */
    void cancel();
    
    /**
     * Checks if the event has been cancelled
     * @return true if cancelled
     */
    boolean isCancelled();
}