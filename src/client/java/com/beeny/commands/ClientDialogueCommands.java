package com.beeny.commands;

import com.beeny.client.gui.SimpleConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import com.beeny.Villagersreborn;

/**
 * Unified client commands under the /villager namespace.
 * 
 * This replaces the confusing mix of /dialogue-config and /villager-config
 * with a single, consistent command structure.
 */
public class ClientDialogueCommands {
    
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientDialogueCommands::registerCommands);
    }
    
    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // Primary client command - unified under /villager namespace
        dispatcher.register(literal("villager")
            .then(literal("settings")
                .executes(context -> openConfigScreen(context)))
            .then(literal("help")
                .executes(context -> {
                    sendHelpMessage(context.getSource());
                    return 1;
                })));

        // Legacy aliases: point users to /villager settings
        dispatcher.register(literal("dialogue-config")
            .executes(context -> {
                context.getSource().sendFeedback(Text.literal("Use /villager settings for all villager configuration.").formatted(Formatting.YELLOW));
                return 1;
            }));

        dispatcher.register(literal("villager-config")
            .executes(context -> {
                context.getSource().sendFeedback(Text.literal("Use /villager settings for all villager configuration.").formatted(Formatting.YELLOW));
                return 1;
            }));
    }
    
    private static int openConfigScreen(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        
        // Schedule to run on next client tick to avoid threading issues
        client.execute(() -> {
            try {
                client.setScreen(new SimpleConfigScreen(client.currentScreen));
            } catch (Exception e) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Error opening settings: " + e.getMessage()).formatted(Formatting.RED), false);
                }
                com.beeny.Villagersreborn.LOGGER.error("Error opening settings screen", e);
            }
        });
        
        context.getSource().sendFeedback(
            Text.literal(com.beeny.constants.StringConstants.UI_OPENING_SETTINGS).formatted(Formatting.GREEN)
        );
        
        return 1;
    }
    
    private static void sendHelpMessage(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("=== Villages Reborn Commands ===").formatted(Formatting.GOLD, Formatting.BOLD));
        
        source.sendFeedback(Text.literal("\nClient Commands:").formatted(Formatting.AQUA));
        source.sendFeedback(Text.literal("• /villager settings").formatted(Formatting.WHITE)
            .append(Text.literal(" - Open configuration GUI").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /villager help").formatted(Formatting.WHITE)
            .append(Text.literal(" - Show this help").formatted(Formatting.GRAY)));
        
        source.sendFeedback(Text.literal("\nServer Commands (when OP):").formatted(Formatting.AQUA));
        source.sendFeedback(Text.literal("• /villager ai setup <provider>").formatted(Formatting.WHITE)
            .append(Text.literal(" - Setup AI system").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /villager ai test").formatted(Formatting.WHITE)
            .append(Text.literal(" - Test AI dialogue").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /villager ai status").formatted(Formatting.WHITE)
            .append(Text.literal(" - Show AI status").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /villager manage list").formatted(Formatting.WHITE)
            .append(Text.literal(" - List villagers").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /villager family tree <villager>").formatted(Formatting.WHITE)
            .append(Text.literal(" - Show family tree").formatted(Formatting.GRAY)));
        
        source.sendFeedback(Text.literal("\nFor more commands, use /villager help on the server").formatted(Formatting.YELLOW));
        
        source.sendFeedback(Text.literal("\nAPI Key Configuration:").formatted(Formatting.AQUA));
        source.sendFeedback(Text.literal("• Environment Variable: VILLAGERS_REBORN_API_KEY").formatted(Formatting.WHITE));
        source.sendFeedback(Text.literal("• Set this on the server for security").formatted(Formatting.GRAY));
    }
}