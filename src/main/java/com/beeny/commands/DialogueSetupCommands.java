package com.beeny.commands;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.dialogue.LLMDialogueManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DialogueSetupCommands {
    
    private static final Map<String, SetupSession> setupSessions = new ConcurrentHashMap<>();
    
    private static class SetupSession {
        String playerUuid;
        String provider;
        String step;
        long lastActivity;
        
        SetupSession(String playerUuid) {
            this.playerUuid = playerUuid;
            this.step = "start";
            this.lastActivity = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastActivity > 300000; // 5 minutes
        }
        
        void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
    }
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("dialogue-setup")
            .requires(source -> source.hasPermissionLevel(2))
            .executes(DialogueSetupCommands::startGuidedSetup)
            .then(CommandManager.literal("quick")
                .then(CommandManager.argument("provider", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("gemini");
                        builder.suggest("openrouter");
                        return builder.buildFuture();
                    })
                    .then(CommandManager.argument("apikey", StringArgumentType.string())
                        .executes(DialogueSetupCommands::quickSetup))))
            .then(CommandManager.literal("abort")
                .executes(DialogueSetupCommands::abortSetup))
        );
        
        // Simple response command for guided setup
        dispatcher.register(CommandManager.literal("setup-response")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("response", StringArgumentType.greedyString())
                .executes(DialogueSetupCommands::handleSetupResponse))
        );
    }
    
    private static int startGuidedSetup(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerUuid = source.getPlayer() != null ? source.getPlayer().getUuidAsString() : "console";
        
        // Clean up expired sessions
        setupSessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // Create new setup session
        SetupSession session = new SetupSession(playerUuid);
        setupSessions.put(playerUuid, session);
        
        sendWelcomeMessage(source);
        return 1;
    }
    
    private static int quickSetup(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String provider = StringArgumentType.getString(context, "provider").toLowerCase();
        String apiKey = StringArgumentType.getString(context, "apikey");
        
        if (!provider.equals("gemini") && !provider.equals("openrouter")) {
            source.sendFeedback(() -> 
                Text.literal("Invalid provider. Use 'gemini' or 'openrouter'").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Apply settings
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = true;
        VillagersRebornConfig.LLM_PROVIDER = provider;
        VillagersRebornConfig.LLM_API_KEY = apiKey;
        VillagersRebornConfig.LLM_API_ENDPOINT = "";
        VillagersRebornConfig.LLM_MODEL = provider.equals("gemini") ? "gemini-1.5-flash" : "openai/gpt-3.5-turbo";
        
        // Initialize and test
        LLMDialogueManager.initialize();
        
        source.sendFeedback(() -> 
            Text.literal("✓ Quick setup complete! Testing connection...").formatted(Formatting.GREEN), false);
        
        testConnectionAndReport(source);
        return 1;
    }
    
    private static int abortSetup(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerUuid = source.getPlayer() != null ? source.getPlayer().getUuidAsString() : "console";
        
        setupSessions.remove(playerUuid);
        source.sendFeedback(() -> 
            Text.literal("Setup cancelled.").formatted(Formatting.YELLOW), false);
        return 1;
    }
    
    private static int handleSetupResponse(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerUuid = source.getPlayer() != null ? source.getPlayer().getUuidAsString() : "console";
        String response = StringArgumentType.getString(context, "response");
        
        SetupSession session = setupSessions.get(playerUuid);
        if (session == null || session.isExpired()) {
            source.sendFeedback(() -> 
                Text.literal("No active setup session. Use /dialogue-setup to start.").formatted(Formatting.RED), false);
            return 0;
        }
        
        session.updateActivity();
        
        switch (session.step) {
            case "start" -> handleProviderSelection(source, session, response);
            case "provider_selected" -> handleApiKeyInput(source, session, response);
            case "api_key_entered" -> handleModelSelection(source, session, response);
            case "model_selected" -> completeSetup(source, session);
        }
        
        return 1;
    }
    
    private static void sendWelcomeMessage(ServerCommandSource source) {
        source.sendFeedback(() -> 
            Text.literal("=== Dynamic Dialogue Setup Wizard ===").formatted(Formatting.GOLD, Formatting.BOLD), false);
        
        source.sendFeedback(() -> 
            Text.literal("\nWelcome! This wizard will help you set up AI-powered villager conversations.").formatted(Formatting.WHITE), false);
        
        source.sendFeedback(() -> 
            Text.literal("Your villagers will be able to have unique, contextual conversations!").formatted(Formatting.GREEN), false);
        
        source.sendFeedback(() -> 
            Text.literal("\nStep 1: Choose your AI provider").formatted(Formatting.AQUA, Formatting.BOLD), false);
        
        source.sendFeedback(() -> 
            Text.literal("\n1. ").formatted(Formatting.WHITE)
                .append(Text.literal("Google Gemini").formatted(Formatting.GREEN))
                .append(Text.literal(" (Recommended - Free tier available)").formatted(Formatting.GRAY)), false);
        
        source.sendFeedback(() -> 
            Text.literal("2. ").formatted(Formatting.WHITE)
                .append(Text.literal("OpenRouter").formatted(Formatting.BLUE))
                .append(Text.literal(" (Many models available, pay-per-use)").formatted(Formatting.GRAY)), false);
        
        Text responseText = Text.literal("\n→ Type: ").formatted(Formatting.WHITE)
            .append(Text.literal("/setup-response gemini").formatted(Formatting.YELLOW)
                .styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/setup-response gemini"))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to select Gemini")))))
            .append(Text.literal(" or ").formatted(Formatting.WHITE))
            .append(Text.literal("/setup-response openrouter").formatted(Formatting.YELLOW)
                .styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/setup-response openrouter"))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to select OpenRouter")))));
        
        source.sendFeedback(() -> responseText, false);
        
        source.sendFeedback(() -> 
            Text.literal("\nType ").formatted(Formatting.GRAY)
                .append(Text.literal("/dialogue-setup abort").formatted(Formatting.RED))
                .append(Text.literal(" to cancel at any time.").formatted(Formatting.GRAY)), false);
    }
    
    private static void handleProviderSelection(ServerCommandSource source, SetupSession session, String response) {
        String provider = response.toLowerCase().trim();
        
        if (!provider.equals("gemini") && !provider.equals("openrouter")) {
            source.sendFeedback(() -> 
                Text.literal("Please choose 'gemini' or 'openrouter'").formatted(Formatting.RED), false);
            return;
        }
        
        session.provider = provider;
        session.step = "provider_selected";
        
        source.sendFeedback(() -> 
            Text.literal("✓ Selected: ").formatted(Formatting.GREEN)
                .append(Text.literal(provider.toUpperCase()).formatted(Formatting.BOLD)), false);
        
        // Provider-specific instructions
        if (provider.equals("gemini")) {
            source.sendFeedback(() -> 
                Text.literal("\nStep 2: Get your Gemini API key").formatted(Formatting.AQUA, Formatting.BOLD), false);
            
            source.sendFeedback(() -> 
                Text.literal("1. Visit: ").formatted(Formatting.WHITE)
                    .append(Text.literal("https://ai.google.dev").formatted(Formatting.BLUE, Formatting.UNDERLINE)
                        .styled(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://ai.google.dev"))))), false);
            
            source.sendFeedback(() -> 
                Text.literal("2. Click 'Get API key' and create a new key").formatted(Formatting.WHITE), false);
            
            source.sendFeedback(() -> 
                Text.literal("3. Copy your API key and paste it below").formatted(Formatting.WHITE), false);
        } else {
            source.sendFeedback(() -> 
                Text.literal("\nStep 2: Get your OpenRouter API key").formatted(Formatting.AQUA, Formatting.BOLD), false);
            
            source.sendFeedback(() -> 
                Text.literal("1. Visit: ").formatted(Formatting.WHITE)
                    .append(Text.literal("https://openrouter.ai").formatted(Formatting.BLUE, Formatting.UNDERLINE)
                        .styled(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://openrouter.ai"))))), false);
            
            source.sendFeedback(() -> 
                Text.literal("2. Sign up and add credits to your account").formatted(Formatting.WHITE), false);
            
            source.sendFeedback(() -> 
                Text.literal("3. Go to Keys section and create an API key").formatted(Formatting.WHITE), false);
        }
        
        source.sendFeedback(() -> 
            Text.literal("\n→ Type: ").formatted(Formatting.WHITE)
                .append(Text.literal("/setup-response YOUR_API_KEY_HERE").formatted(Formatting.YELLOW)
                    .styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/setup-response "))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to enter your API key"))))), false);
    }
    
    private static void handleApiKeyInput(ServerCommandSource source, SetupSession session, String response) {
        String apiKey = response.trim();
        
        if (apiKey.length() < 10) {
            source.sendFeedback(() -> 
                Text.literal("API key seems too short. Please check and try again.").formatted(Formatting.RED), false);
            return;
        }
        
        session.step = "api_key_entered";
        
        source.sendFeedback(() -> 
            Text.literal("✓ API key received!").formatted(Formatting.GREEN), false);
        
        // Set default model and complete setup
        String defaultModel = session.provider.equals("gemini") ? "gemini-1.5-flash" : "openai/gpt-3.5-turbo";
        
        source.sendFeedback(() -> 
            Text.literal("\nStep 3: Using default model: ").formatted(Formatting.AQUA)
                .append(Text.literal(defaultModel).formatted(Formatting.BOLD)), false);
        
        // Apply configuration
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = true;
        VillagersRebornConfig.LLM_PROVIDER = session.provider;
        VillagersRebornConfig.LLM_API_KEY = apiKey;
        VillagersRebornConfig.LLM_API_ENDPOINT = "";
        VillagersRebornConfig.LLM_MODEL = defaultModel;
        
        LLMDialogueManager.initialize();
        
        source.sendFeedback(() -> 
            Text.literal("✓ Configuration complete! Testing connection...").formatted(Formatting.GREEN), false);
        
        testConnectionAndReport(source);
        
        // Clean up session
        setupSessions.remove(session.playerUuid);
        
        source.sendFeedback(() -> 
            Text.literal("\n🎉 Setup complete! Your villagers now have AI-powered conversations!").formatted(Formatting.GREEN, Formatting.BOLD), false);
        
        source.sendFeedback(() -> 
            Text.literal("Try talking to a villager to see the difference!").formatted(Formatting.YELLOW), false);
        
        source.sendFeedback(() -> 
            Text.literal("\nUseful commands:").formatted(Formatting.AQUA), false);
        
        source.sendFeedback(() -> 
            Text.literal("• /dialogue test").formatted(Formatting.WHITE)
                .append(Text.literal(" - Test dialogue with a villager").formatted(Formatting.GRAY)), false);
        
        source.sendFeedback(() -> 
            Text.literal("• /dialogue toggle").formatted(Formatting.WHITE)
                .append(Text.literal(" - Enable/disable dynamic dialogue").formatted(Formatting.GRAY)), false);
        
        source.sendFeedback(() -> 
            Text.literal("• /dialogue status").formatted(Formatting.WHITE)
                .append(Text.literal(" - View current configuration").formatted(Formatting.GRAY)), false);
    }
    
    private static void handleModelSelection(ServerCommandSource source, SetupSession session, String response) {
        // This handles model selection if we add that step in the future
        // For now, we use default models based on provider
        session.step = "model_selected";
        completeSetup(source, session);
    }
    
    private static void completeSetup(ServerCommandSource source, SetupSession session) {
        // This method is called if we have additional setup steps in the future
        setupSessions.remove(session.playerUuid);
    }
    
    private static void testConnectionAndReport(ServerCommandSource source) {
        LLMDialogueManager.testConnection().thenAccept(success -> {
            if (success) {
                source.sendFeedback(() -> 
                    Text.literal("✓ Connection test successful!").formatted(Formatting.GREEN), false);
            } else {
                source.sendFeedback(() -> 
                    Text.literal("⚠ Connection test failed. Please check your API key and try again.").formatted(Formatting.RED), false);
                
                source.sendFeedback(() -> 
                    Text.literal("You can run the setup again with: /dialogue-setup").formatted(Formatting.GRAY), false);
            }
        }).exceptionally(throwable -> {
            source.sendFeedback(() -> 
                Text.literal("⚠ Connection error: " + throwable.getMessage()).formatted(Formatting.RED), false);
            return null;
        });
    }
}