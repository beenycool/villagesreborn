package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;

/**
 * Routes conversation messages between players and villagers
 */
public interface ConversationRouter {
    
    /**
     * Routes a directed message from a player to a specific villager
     * @param player the player sending the message
     * @param villager the target villager
     * @param message the message content
     */
    void routeMessage(Player player, VillagerEntity villager, String message);
    
    /**
     * Routes a conversation with full context to nearby villagers
     * @param context the conversation context including sender, message, and environment data
     */
    void routeConversation(ConversationContext context);
}