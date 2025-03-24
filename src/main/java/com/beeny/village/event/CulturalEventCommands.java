package com.beeny.village.event;

import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.artifacts.CulturalArtifactSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Handles commands for interacting with the cultural events system
 */
public class CulturalEventCommands {
    
    /**
     * Register all cultural event commands
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, 
                             CommandRegistryAccess registryAccess,
                             CommandManager.RegistrationEnvironment environment) {
        
        // Event management commands
        dispatcher.register(CommandManager.literal("culturalevent")
            .requires(source -> source.hasPermissionLevel(2)) // Require operator permission
            .then(CommandManager.literal("create")
                .then(CommandManager.argument("culture", StringArgumentType.word())
                .then(CommandManager.argument("type", StringArgumentType.word())
                .then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(5, 100))
                .then(CommandManager.argument("durationMinutes", IntegerArgumentType.integer(5, 1440))
                .executes(context -> createEvent(
                    context,
                    StringArgumentType.getString(context, "culture"),
                    StringArgumentType.getString(context, "type"),
                    BlockPosArgumentType.getBlockPos(context, "position"),
                    IntegerArgumentType.getInteger(context, "radius"),
                    IntegerArgumentType.getInteger(context, "durationMinutes")
                ))))))
        );
        
        dispatcher.register(CommandManager.literal("culturalevent")
            .requires(source -> source.hasPermissionLevel(2)) // Require operator permission
            .then(CommandManager.literal("list")
                .executes(CulturalEventCommands::listEvents))
        );
        
        dispatcher.register(CommandManager.literal("culturalevent")
            .requires(source -> source.hasPermissionLevel(2)) // Require operator permission
            .then(CommandManager.literal("registerVillage")
                .then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
                .then(CommandManager.argument("culture", StringArgumentType.word())
                .then(CommandManager.argument("frequency", IntegerArgumentType.integer(30, 1440))
                .then(CommandManager.argument("radius", IntegerArgumentType.integer(5, 100))
                .executes(context -> registerVillage(
                    context,
                    BlockPosArgumentType.getBlockPos(context, "position"),
                    StringArgumentType.getString(context, "culture"),
                    IntegerArgumentType.getInteger(context, "frequency"),
                    IntegerArgumentType.getInteger(context, "radius")
                )))))))
        );
        
        dispatcher.register(CommandManager.literal("culturalevent")
            .requires(source -> source.hasPermissionLevel(2)) // Require operator permission
            .then(CommandManager.literal("giveArtifact")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                .then(CommandManager.argument("culture", StringArgumentType.word())
                .then(CommandManager.argument("rarity", StringArgumentType.word())
                .executes(context -> giveArtifact(
                    context,
                    EntityArgumentType.getPlayer(context, "player"),
                    StringArgumentType.getString(context, "culture"),
                    StringArgumentType.getString(context, "rarity")
                ))))))
        );
        
        // Player commands - available to all players
        dispatcher.register(CommandManager.literal("culturalevent")
            .then(CommandManager.literal("join")
                .then(CommandManager.argument("eventId", StringArgumentType.string())
                .executes(context -> joinEvent(
                    context,
                    StringArgumentType.getString(context, "eventId")
                ))))
        );
        
        dispatcher.register(CommandManager.literal("culturalevent")
            .then(CommandManager.literal("nearby")
                .executes(CulturalEventCommands::findNearbyEvents))
        );
        
        dispatcher.register(CommandManager.literal("reputation")
            .executes(CulturalEventCommands::showReputation)
            .then(CommandManager.literal("check")
                .then(CommandManager.argument("culture", StringArgumentType.word())
                .executes(context -> checkSpecificReputation(
                    context,
                    StringArgumentType.getString(context, "culture")
                ))))
        );
    }
    
    /**
     * Command to create a new cultural event
     */
    private static int createEvent(CommandContext<ServerCommandSource> context, String culture, 
                                 String type, BlockPos pos, int radius, int durationMinutes) {
        ServerCommandSource source = context.getSource();
        
        // Validate culture
        if (!isValidCulture(culture)) {
            source.sendError(Text.of("Invalid culture. Valid options are: roman, egyptian, victorian, nyc"));
            return 0;
        }
        
        // Create description
        String description = capitalizeFirstLetter(culture) + " " + type;
        
        // Create the event
        VillageEvent event = VillageEvent.createEvent(
            type,
            culture,
            description,
            pos,
            radius,
            durationMinutes
        );
        
        // Send success message
        source.sendFeedback(() -> Text.of("Created a new " + culture + " " + type + 
            " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + 
            " with radius " + radius + " and duration " + durationMinutes + " minutes."), true);
        
        // Return event ID for reference
        source.sendFeedback(() -> Text.of("Event ID: " + event.getId()), false);
        
        return 1;
    }
    
    /**
     * Command to list all active events
     */
    private static int listEvents(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Collection<VillageEvent> events = VillageEvent.getActiveEvents();
        
        if (events.isEmpty()) {
            source.sendFeedback(() -> Text.of("No active events found."), false);
            return 0;
        }
        
        source.sendFeedback(() -> Text.of("§6===== Active Cultural Events =====§r"), false);
        
        // Convert to array to access by index
        VillageEvent[] eventsArray = events.toArray(new VillageEvent[0]);
        for (int i = 0; i < eventsArray.length; i++) {
            final int eventNum = i + 1; // Make a final copy for lambda
            VillageEvent event = eventsArray[i];
            BlockPos pos = event.getLocation();
            long remainingMinutes = event.getTimeRemaining() / (1000 * 60);
            
            source.sendFeedback(() -> Text.of(
                eventNum + ". §b" + event.getDescription() + "§r" +
                "\n  §7ID:§r " + event.getId() +
                "\n  §7Location:§r " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                "\n  §7Radius:§r " + event.getRadius() + " blocks" +
                "\n  §7Remaining Time:§r " + remainingMinutes + " minutes" +
                "\n  §7Participants:§r " + event.getParticipants().size()
            ), false);
        }
        
        return events.size();
    }
    
    /**
     * Command to register a village for event scheduling
     */
    private static int registerVillage(CommandContext<ServerCommandSource> context, 
                                    BlockPos pos, String culture, int frequency, int radius) {
        ServerCommandSource source = context.getSource();
        
        // Validate culture
        if (!isValidCulture(culture)) {
            source.sendError(Text.of("Invalid culture. Valid options are: roman, egyptian, victorian, nyc"));
            return 0;
        }
        
        // Register village
        CulturalEventSystem.getInstance().registerVillageForEvents(pos, culture, frequency, radius);
        
        source.sendFeedback(() -> Text.of(
            "Registered " + capitalizeFirstLetter(culture) + " village at " + 
            pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + 
            " with event frequency " + frequency + " minutes and radius " + radius + " blocks."
        ), true);
        
        return 1;
    }
    
    /**
     * Command to give a cultural artifact to a player
     */
    private static int giveArtifact(CommandContext<ServerCommandSource> context, 
                                  ServerPlayerEntity player, String culture, String rarityStr) {
        ServerCommandSource source = context.getSource();
        
        // Validate culture
        if (!isValidCulture(culture)) {
            source.sendError(Text.of("Invalid culture. Valid options are: roman, egyptian, victorian, nyc"));
            return 0;
        }
        
        // Parse rarity
        CulturalArtifactSystem.Rarity rarity;
        try {
            rarity = CulturalArtifactSystem.Rarity.valueOf(rarityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendError(Text.of("Invalid rarity. Valid options are: COMMON, UNCOMMON, RARE, EPIC"));
            return 0;
        }
        
        // Generate artifact
        ItemStack artifact = CulturalArtifactSystem.getInstance().generateEventArtifact(player, culture, "admin");
        
        if (artifact == null) {
            source.sendError(Text.of("Failed to create artifact."));
            return 0;
        }
        
        // Give to player
        boolean success = player.giveItemStack(artifact);
        if (!success) {
            player.dropItem(artifact, false);
            source.sendFeedback(() -> Text.of("Dropped artifact at " + 
                player.getName().getString() + "'s feet (inventory full)."), true);
        } else {
            source.sendFeedback(() -> Text.of("Gave " + artifact.getName().getString() + 
                " to " + player.getName().getString()), true);
        }
        
        return 1;
    }
    
    /**
     * Command for a player to join an event
     */
    private static int joinEvent(CommandContext<ServerCommandSource> context, String eventId) {
        ServerCommandSource source = context.getSource();
        
        // Check if player
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.of("This command can only be used by players."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        VillageEvent event = VillageEvent.getEvent(eventId);
        
        if (event == null) {
            source.sendError(Text.of("Event with ID " + eventId + " not found."));
            return 0;
        }
        
        // Add player to event
        event.addParticipant(player);
        return 1;
    }
    
    /**
     * Command to find nearby events
     */
    private static int findNearbyEvents(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Check if player
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.of("This command can only be used by players."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        BlockPos playerPos = player.getBlockPos();
        
        List<VillageEvent> nearbyEvents = VillageEvent.findEventsNear(playerPos, 200);
        
        if (nearbyEvents.isEmpty()) {
            source.sendFeedback(() -> Text.of("No cultural events found nearby."), false);
            return 0;
        }
        
        source.sendFeedback(() -> Text.of("§6===== Nearby Cultural Events =====§r"), false);
        
        // Use indexed for loop with final counter for lambda
        for (int i = 0; i < nearbyEvents.size(); i++) {
            final int eventNum = i + 1; // Make a final copy for lambda
            VillageEvent event = nearbyEvents.get(i);
            BlockPos pos = event.getLocation();
            int distance = (int)Math.sqrt(playerPos.getSquaredDistance(pos));
            long remainingMinutes = event.getTimeRemaining() / (1000 * 60);
            
            source.sendFeedback(() -> Text.of(
                eventNum + ". §b" + event.getDescription() + "§r" +
                "\n  §7ID:§r " + event.getId() +
                "\n  §7Distance:§r " + distance + " blocks away" +
                "\n  §7Remaining Time:§r " + remainingMinutes + " minutes" +
                "\n  §7Use:§r /culturalevent join " + event.getId() + " to participate"
            ), false);
        }
        
        return nearbyEvents.size();
    }
    
    /**
     * Command to show player's reputation with all cultures
     */
    private static int showReputation(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Check if player
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.of("This command can only be used by players."));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        Map<String, Integer> allReputations = PlayerEventParticipation.getInstance().getAllReputations(player);
        
        if (allReputations.isEmpty()) {
            source.sendFeedback(() -> Text.of("You don't have any reputation with cultural groups yet."), false);
            return 0;
        }
        
        source.sendFeedback(() -> Text.of("§6===== Your Cultural Reputation =====§r"), false);
        
        for (Map.Entry<String, Integer> entry : allReputations.entrySet()) {
            String culture = entry.getKey();
            int reputation = entry.getValue();
            String title = PlayerEventParticipation.getInstance().getReputationTitle(culture, reputation);
            
            source.sendFeedback(() -> Text.of(
                "§b" + capitalizeFirstLetter(culture) + ":§r " + reputation + 
                " points (§e" + title + "§r)"
            ), false);
        }
        
        return allReputations.size();
    }
    
    /**
     * Command to check reputation with a specific culture
     */
    private static int checkSpecificReputation(CommandContext<ServerCommandSource> context, String culture) {
        ServerCommandSource source = context.getSource();
        
        // Check if player
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.of("This command can only be used by players."));
            return 0;
        }
        
        // Validate culture
        if (!isValidCulture(culture)) {
            source.sendError(Text.of("Invalid culture. Valid options are: roman, egyptian, victorian, nyc"));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        int reputation = PlayerEventParticipation.getInstance().getReputation(player, culture);
        String title = PlayerEventParticipation.getInstance().getReputationTitle(culture, reputation);
        
        source.sendFeedback(() -> Text.of(
            "§6Your reputation with " + capitalizeFirstLetter(culture) + ":§r " +
            reputation + " points (§e" + title + "§r)"
        ), false);
        
        return 1;
    }
    
    /**
     * Validate if a culture is supported
     */
    private static boolean isValidCulture(String culture) {
        String lowerCase = culture.toLowerCase();
        return lowerCase.equals("roman") || 
               lowerCase.equals("egyptian") || 
               lowerCase.equals("victorian") || 
               lowerCase.equals("nyc");
    }
    
    /**
     * Capitalize the first letter of a string
     */
    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}