package com.beeny.village.event;

import com.beeny.village.VillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;

import java.util.*;

/**
 * Handles the interaction of players with cultural event activities
 */
public class CulturalEventActivityHandler {
    private static final CulturalEventActivityHandler INSTANCE = new CulturalEventActivityHandler();
    
    // Map of activity types to their required items
    private final Map<String, List<Item>> activityRequiredItems = new HashMap<>();
    
    // Map of activity types to blocks that can trigger them
    private final Map<String, List<Block>> activityTriggerBlocks = new HashMap<>();
    
    // Map of activity types to descriptions
    private final Map<String, String> activityDescriptions = new HashMap<>();
    
    // Random for chance-based events
    private final Random random = new Random();
    
    private CulturalEventActivityHandler() {
        initializeActivityData();
    }
    
    public static CulturalEventActivityHandler getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize all activity data
     */
    private void initializeActivityData() {
        // Trading activity
        List<Item> tradingItems = Arrays.asList(
            Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT, Items.DIAMOND
        );
        activityRequiredItems.put("trade", tradingItems);
        activityTriggerBlocks.put("trade", Arrays.asList(
            Blocks.BARREL, Blocks.CHEST, Blocks.CRAFTING_TABLE, Blocks.LECTERN
        ));
        activityDescriptions.put("trade", "Exchange valuable items with villagers");
        
        // Dance/Performance activity
        List<Item> performItems = Arrays.asList(
            Items.NOTE_BLOCK, Items.JUKEBOX, Items.MUSIC_DISC_13, Items.BELL
        );
        activityRequiredItems.put("dance", performItems);
        activityTriggerBlocks.put("dance", Arrays.asList(
            Blocks.NOTE_BLOCK, Blocks.JUKEBOX, Blocks.BELL
        ));
        activityDescriptions.put("dance", "Perform music or dance for the villagers");
        
        // Cooking activity
        List<Item> cookingItems = Arrays.asList(
            Items.BREAD, Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.CAKE, 
            Items.COOKED_PORKCHOP, Items.COOKED_MUTTON, Items.HONEY_BOTTLE
        );
        activityRequiredItems.put("cook", cookingItems);
        activityTriggerBlocks.put("cook", Arrays.asList(
            Blocks.CAMPFIRE, Blocks.SMOKER, Blocks.FURNACE, Blocks.CAULDRON
        ));
        activityDescriptions.put("cook", "Prepare food for the cultural celebration");
        
        // Crafting activity
        List<Item> craftingItems = Arrays.asList(
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD,
            Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ
        );
        activityRequiredItems.put("craft", craftingItems);
        activityTriggerBlocks.put("craft", Arrays.asList(
            Blocks.CRAFTING_TABLE, Blocks.ANVIL, Blocks.SMITHING_TABLE, Blocks.STONECUTTER, 
            Blocks.LOOM, Blocks.CARTOGRAPHY_TABLE
        ));
        activityDescriptions.put("craft", "Create cultural items or decorations");
        
        // Roman bath activity
        List<Item> bathItems = Arrays.asList(
            Items.WATER_BUCKET, Items.POTION, Items.HONEY_BOTTLE, Items.GLASS_BOTTLE
        );
        activityRequiredItems.put("bathhouse", bathItems);
        activityTriggerBlocks.put("bathhouse", Arrays.asList(
            Blocks.WATER, Blocks.CAULDRON, Blocks.SOUL_CAMPFIRE
        ));
        activityDescriptions.put("bathhouse", "Participate in Roman bathing rituals");
        
        // Egyptian ritual activity
        List<Item> ritualItems = Arrays.asList(
            Items.GOLD_INGOT, Items.EMERALD, Items.REDSTONE_TORCH, Items.TORCH,
            Items.BLAZE_POWDER, Items.GLOWSTONE_DUST
        );
        activityRequiredItems.put("ritual", ritualItems);
        activityTriggerBlocks.put("ritual", Arrays.asList(
            Blocks.GOLD_BLOCK, Blocks.EMERALD_BLOCK, Blocks.REDSTONE_TORCH, 
            Blocks.ENCHANTING_TABLE, Blocks.CANDLE
        ));
        activityDescriptions.put("ritual", "Perform an ancient Egyptian ceremony");
        
        // Competition activity
        List<Item> gameItems = Arrays.asList(
            Items.BOW, Items.CROSSBOW, Items.SNOWBALL, Items.EGG,
            Items.FISHING_ROD, Items.TRIDENT
        );
        activityRequiredItems.put("game", gameItems);
        activityTriggerBlocks.put("game", Arrays.asList(
            Blocks.TARGET, Blocks.HAY_BLOCK
        ));
        activityDescriptions.put("game", "Participate in a cultural game or competition");
    }
    
    /**
     * Handle when a player interacts with a block during an active event
     */
    public void handleBlockInteraction(ServerPlayerEntity player, BlockPos pos, Block block) {
        // Check if player is near an active event
        if (!PlayerEventParticipation.getInstance().isNearActiveEvent(player)) {
            return;
        }
        
        // Find matching activities for this block
        List<String> matchingActivities = new ArrayList<>();
        for (Map.Entry<String, List<Block>> entry : activityTriggerBlocks.entrySet()) {
            if (entry.getValue().contains(block)) {
                matchingActivities.add(entry.getKey());
            }
        }
        
        if (matchingActivities.isEmpty()) {
            return;
        }
        
        // Get player's active events
        Collection<PlayerEventParticipation.EventParticipationData> activeEvents = 
            PlayerEventParticipation.getInstance().getActivePlayerEvents(player);
            
        if (activeEvents.isEmpty()) {
            return;
        }
        
        // Check if this block interaction could complete an activity
        for (PlayerEventParticipation.EventParticipationData event : activeEvents) {
            for (String activity : matchingActivities) {
                if (!event.hasCompletedActivity(activity) && 
                    isActivityRelevantForEvent(event.eventType, activity)) {
                    
                    // Check if the player has the required item in hand
                    ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
                    List<Item> requiredItems = activityRequiredItems.get(activity);
                    
                    if (requiredItems != null && requiredItems.contains(mainHand.getItem())) {
                        // Activity can be completed!
                        completeActivity(player, event.eventId, activity);
                        return;
                    } else if (requiredItems != null) {
                        // Player has the right block but wrong item
                        suggestItems(player, activity);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Handle when a player interacts with a villager during an active event
     */
    public void handleVillagerInteraction(ServerPlayerEntity player, VillagerEntity villager) {
        // Check if player is near an active event
        if (!PlayerEventParticipation.getInstance().isNearActiveEvent(player)) {
            return;
        }
        
        // Get player's active events
        Collection<PlayerEventParticipation.EventParticipationData> activeEvents = 
            PlayerEventParticipation.getInstance().getActivePlayerEvents(player);
            
        if (activeEvents.isEmpty()) {
            return;
        }
        
        // Check which activity this might complete - prioritize trading
        String activity = "trade";
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        List<Item> tradingItems = activityRequiredItems.get(activity);
        
        for (PlayerEventParticipation.EventParticipationData event : activeEvents) {
            if (!event.hasCompletedActivity(activity) && 
                isActivityRelevantForEvent(event.eventType, activity)) {
                
                if (tradingItems != null && tradingItems.contains(mainHand.getItem())) {
                    // Trading activity can be completed
                    completeActivity(player, event.eventId, activity);
                    
                    // Simulate trade - consume one item
                    if (!player.isCreative()) {
                        mainHand.decrement(1);
                    }
                    
                    // Give random reward sometimes
                    if (random.nextFloat() < 0.3f) {
                        giveActivityReward(player, event.culture, activity);
                    }
                    return;
                } else {
                    // Player is interacting with villager but doesn't have trade items
                    suggestItems(player, activity);
                    return;
                }
            }
        }
        
        // Check for other social activities like dance or ritual if trading wasn't applicable
        List<String> socialActivities = Arrays.asList("dance", "ritual");
        for (String socialActivity : socialActivities) {
            for (PlayerEventParticipation.EventParticipationData event : activeEvents) {
                if (!event.hasCompletedActivity(socialActivity) && 
                    isActivityRelevantForEvent(event.eventType, socialActivity)) {
                    
                    // Chance-based completion for social activities
                    if (random.nextFloat() < 0.15f) {
                        completeActivity(player, event.eventId, socialActivity);
                        return;
                    } else {
                        player.sendMessage(Text.of("§eThe villager seems interested in your " + 
                            socialActivity + ". Try again or use the right items.§r"), false);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Handle when a player uses an item during an active event
     */
    public void handleItemUse(ServerPlayerEntity player, ItemStack itemStack) {
        // Check if player is near an active event
        if (!PlayerEventParticipation.getInstance().isNearActiveEvent(player)) {
            return;
        }
        
        // Get player's active events
        Collection<PlayerEventParticipation.EventParticipationData> activeEvents = 
            PlayerEventParticipation.getInstance().getActivePlayerEvents(player);
            
        if (activeEvents.isEmpty()) {
            return;
        }
        
        // Find potential activity matches based on item
        List<String> potentialActivities = new ArrayList<>();
        Item usedItem = itemStack.getItem();
        
        for (Map.Entry<String, List<Item>> entry : activityRequiredItems.entrySet()) {
            if (entry.getValue().contains(usedItem)) {
                potentialActivities.add(entry.getKey());
            }
        }
        
        if (potentialActivities.isEmpty()) {
            return;
        }
        
        // Check if this could complete an activity
        for (PlayerEventParticipation.EventParticipationData event : activeEvents) {
            for (String activity : potentialActivities) {
                if (!event.hasCompletedActivity(activity) && 
                    isActivityRelevantForEvent(event.eventType, activity)) {
                    
                    // Reduced chance of success for just using item without context
                    if (random.nextFloat() < 0.2f) {
                        completeActivity(player, event.eventId, activity);
                        return;
                    } else {
                        player.sendMessage(Text.of("§eTry using this item at the right location or with villagers.§r"), false);
                        return;
                    }
                }
            }
        }
    }
    
    /**
     * Complete an activity and record progress
     */
    private void completeActivity(ServerPlayerEntity player, String eventId, String activity) {
        // Record the activity completion
        PlayerEventParticipation.getInstance().recordActivityCompletion(player, eventId, activity);
        
        // Send success message
        player.sendMessage(Text.of("§a§lActivity Completed: " + activity + "§r"), false);
        
        // Visual effects
        World world = player.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                15, 0.5, 0.5, 0.5, 0.1
            );
        }
    }
    
    /**
     * Give feedback about items needed for an activity
     */
    private void suggestItems(ServerPlayerEntity player, String activity) {
        List<Item> items = activityRequiredItems.get(activity);
        if (items != null && !items.isEmpty()) {
            String itemNames = getItemNamesForDisplay(items);
            String description = activityDescriptions.getOrDefault(activity, activity);
            
            player.sendMessage(Text.of("§eActivity hint: " + description + 
                " using items like " + itemNames + "§r"), false);
        }
    }
    
    /**
     * Format item names for display
     */
    private String getItemNamesForDisplay(List<Item> items) {
        if (items.isEmpty()) {
            return "various items";
        }
        
        // Show up to 3 items
        int displayCount = Math.min(3, items.size());
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < displayCount; i++) {
            if (i > 0) {
                result.append(i == displayCount - 1 ? " or " : ", ");
            }
            result.append(items.get(i).getName().getString());
        }
        
        if (displayCount < items.size()) {
            result.append(" or others");
        }
        
        return result.toString();
    }
    
    /**
     * Check if an activity type is relevant for the event type
     */
    private boolean isActivityRelevantForEvent(String eventType, String activityType) {
        eventType = eventType.toLowerCase();
        
        // Universal activities relevant to all events
        if (activityType.equals("trade") || activityType.equals("craft")) {
            return true;
        }
        
        // Specific activity-event matches
        switch (eventType) {
            case "festival", "celebration", "feast" -> {
                return activityType.equals("dance") || 
                       activityType.equals("cook") || 
                       activityType.equals("game");
            }
            case "market", "fair", "trading" -> {
                return activityType.equals("trade");
            }
            case "ritual", "ceremony" -> {
                return activityType.equals("ritual") || 
                       activityType.equals("bathhouse");
            }
            case "competition", "game", "contest" -> {
                return activityType.equals("game");
            }
        }
        
        // Default - 50% chance that random activities work
        return random.nextBoolean();
    }
    
    /**
     * Give a cultural reward for completing an event activity
     */
    private void giveActivityReward(ServerPlayerEntity player, String culture, String activity) {
        ItemStack reward = null;
        
        // Determine reward based on culture and activity
        switch (culture.toLowerCase()) {
            case "roman" -> {
                if (activity.equals("bathhouse")) {
                    reward = new ItemStack(Items.HONEY_BOTTLE);
                    reward.setCustomName(Text.of("Roman Bath Essence"));
                } else if (activity.equals("ritual")) {
                    reward = new ItemStack(Items.GOLD_NUGGET, 3);
                    reward.setCustomName(Text.of("Roman Ceremonial Coin"));
                } else {
                    reward = new ItemStack(Items.BRICK, 4);
                    reward.setCustomName(Text.of("Roman Architectural Fragment"));
                }
            }
            case "egyptian" -> {
                if (activity.equals("ritual")) {
                    reward = new ItemStack(Items.GOLD_NUGGET, 3);
                    reward.setCustomName(Text.of("Egyptian Scarab Token"));
                } else if (activity.equals("craft")) {
                    reward = new ItemStack(Items.PAPER, 2);
                    reward.setCustomName(Text.of("Egyptian Papyrus"));
                } else {
                    reward = new ItemStack(Items.SAND, 4);
                    reward.setCustomName(Text.of("Egyptian Fine Sand"));
                }
            }
            case "victorian" -> {
                if (activity.equals("trade")) {
                    reward = new ItemStack(Items.IRON_NUGGET, 5);
                    reward.setCustomName(Text.of("Victorian Trade Coin"));
                } else if (activity.equals("craft")) {
                    reward = new ItemStack(Items.REDSTONE, 3);
                    reward.setCustomName(Text.of("Victorian Mechanical Parts"));
                } else {
                    reward = new ItemStack(Items.PAPER, 2);
                    reward.setCustomName(Text.of("Victorian Certificate"));
                }
            }
            case "nyc" -> {
                if (activity.equals("dance")) {
                    reward = new ItemStack(Items.NOTE_BLOCK);
                    reward.setCustomName(Text.of("NYC Music Box"));
                } else if (activity.equals("trade")) {
                    reward = new ItemStack(Items.EMERALD, 1);
                    reward.setCustomName(Text.of("NYC Dollar"));
                } else {
                    reward = new ItemStack(Items.COOKIE, 4);
                    reward.setCustomName(Text.of("NYC Cookie"));
                }
            }
            default -> {
                reward = new ItemStack(Items.GOLD_NUGGET);
                reward.setCustomName(Text.of("Cultural Token"));
            }
        }
        
        // Give the reward to player
        if (reward != null) {
            if (player.getInventory().insertStack(reward)) {
                player.sendMessage(Text.of("§6You received: " + reward.getName().getString() + "§r"), false);
            } else {
                // Drop at player's feet if inventory full
                player.dropItem(reward, false);
                player.sendMessage(Text.of("§6Your reward was dropped at your feet: " + 
                    reward.getName().getString() + "§r"), false);
            }
        }
    }
    
    /**
     * Get a list of all supported activities
     */
    public Collection<String> getAllActivities() {
        return Collections.unmodifiableSet(activityDescriptions.keySet());
    }
    
    /**
     * Get the description of an activity
     */
    public String getActivityDescription(String activity) {
        return activityDescriptions.getOrDefault(activity, activity);
    }
}