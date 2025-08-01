package com.beeny.commands;

import com.beeny.client.gui.DialogueConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class ClientDialogueCommands {
    
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientDialogueCommands::registerCommands);
    }
    
    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("dialogue-config")
            .executes(context -> {
                MinecraftClient client = context.getSource().getClient();
                
                // Schedule to run on next client tick to avoid threading issues
                client.execute(() -> {
                    client.setScreen(new DialogueConfigScreen(client.currentScreen));
                });
                
                context.getSource().sendFeedback(
                    Text.literal("Opening dialogue configuration...").formatted(Formatting.GREEN)
                );
                
                return 1;
            }));
        
        dispatcher.register(literal("dialogue-help")
            .executes(context -> {
                sendHelpMessage(context.getSource());
                return 1;
            }));
    }
    
    private static void sendHelpMessage(FabricClientCommandSource source) {
        source.sendFeedback(Text.literal("=== Dynamic Dialogue Help ===").formatted(Formatting.GOLD, Formatting.BOLD));
        
        source.sendFeedback(Text.literal("\nClient Commands (Single Player):").formatted(Formatting.AQUA));
        source.sendFeedback(Text.literal("• /dialogue-config").formatted(Formatting.WHITE)
            .append(Text.literal(" - Open configuration GUI").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /dialogue-help").formatted(Formatting.WHITE)
            .append(Text.literal(" - Show this help").formatted(Formatting.GRAY)));
        
        source.sendFeedback(Text.literal("\nServer Commands (Multiplayer/OP):").formatted(Formatting.AQUA));
        source.sendFeedback(Text.literal("• /dialogue-setup").formatted(Formatting.WHITE)
            .append(Text.literal(" - Guided setup wizard").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /dialogue-setup quick <provider> <apikey>").formatted(Formatting.WHITE)
            .append(Text.literal(" - Quick setup").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /dialogue test").formatted(Formatting.WHITE)
            .append(Text.literal(" - Test with nearest villager").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /dialogue status").formatted(Formatting.WHITE)
            .append(Text.literal(" - Show current settings").formatted(Formatting.GRAY)));
        source.sendFeedback(Text.literal("• /dialogue toggle").formatted(Formatting.WHITE)
            .append(Text.literal(" - Enable/disable").formatted(Formatting.GRAY)));
        
        source.sendFeedback(Text.literal("\nAPI Key Sources:").formatted(Formatting.AQUA));
        source.sendFeedback(Text.literal("• Gemini (Free): ").formatted(Formatting.WHITE)
            .append(Text.literal("https://ai.google.dev").formatted(Formatting.BLUE)));
        source.sendFeedback(Text.literal("• OpenRouter (Paid): ").formatted(Formatting.WHITE)
            .append(Text.literal("https://openrouter.ai").formatted(Formatting.BLUE)));
        
        source.sendFeedback(Text.literal("\nFor the best experience, start with the guided setup: ").formatted(Formatting.GREEN)
            .append(Text.literal("/dialogue-setup").formatted(Formatting.YELLOW, Formatting.BOLD)));
    }
}