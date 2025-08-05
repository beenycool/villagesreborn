package com.beeny.commands;

import com.beeny.commands.base.BaseVillagerCommand;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.data.VillagerData;
import com.beeny.dialogue.DialogueCache;
import com.beeny.dialogue.LLMDialogueManager;
import com.beeny.dialogue.VillagerMemoryManager;
import com.beeny.system.VillagerDialogueSystem;
import com.beeny.util.CommandMessageUtils;
import com.beeny.util.VillagerDataUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

/**
 * Commands for managing villager AI and dialogue systems
 */
public class VillagerAICommands extends BaseVillagerCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("villager")
            .then(CommandManager.literal("ai")
                .then(CommandManager.literal("setup")
                    .then(CommandManager.argument("provider", StringArgumentType.string())
                        .suggests(VillagerAICommands::suggestProviders)
                        .executes(VillagerAICommands::setupAIWithoutKey)
                        .then(CommandManager.argument("apikey", StringArgumentType.string())
                            .executes(VillagerAICommands::setupAI))))
                .then(CommandManager.literal("model")
                    .then(CommandManager.argument("model_name", StringArgumentType.string())
                        .executes(VillagerAICommands::setModel)))
                .then(CommandManager.literal("test")
                    .executes(VillagerAICommands::testAI)
                    .then(CommandManager.argument("category", StringArgumentType.string())
                        .suggests(VillagerAICommands::suggestDialogueCategories)
                        .executes(VillagerAICommands::testAIWithCategory)))
                .then(CommandManager.literal("toggle")
                    .executes(VillagerAICommands::toggleAI))
                .then(CommandManager.literal("status")
                    .executes(VillagerAICommands::showAIStatus))
                .then(CommandManager.literal("cache")
                    .then(CommandManager.literal("clear")
                        .executes(VillagerAICommands::clearAICache))
                    .then(CommandManager.literal("size")
                        .executes(VillagerAICommands::showCacheSize)))
                .then(CommandManager.literal("memory")
                    .then(CommandManager.literal("clear")
                        .executes(VillagerAICommands::clearAIMemory))
                    .then(CommandManager.literal("size")
                        .executes(VillagerAICommands::showMemorySize))))
        );
    }
    
    private static int setupAI(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String provider = StringArgumentType.getString(context, "provider").toLowerCase();
        String apiKey = StringArgumentType.getString(context, "apikey");
        ServerCommandSource source = context.getSource();
        
        if (!isValidProvider(provider)) {
            CommandMessageUtils.sendError(source, "Invalid provider. Use: gemini, openrouter, or local");
            return 0;
        }
        
        // Set configuration
        VillagersRebornConfig.LLM_PROVIDER = provider;
        VillagersRebornConfig.LLM_API_KEY = apiKey;
        
        // Set default model based on provider
        setDefaultModelForProvider(provider);
        
        // Reinitialize the dialogue manager
        LLMDialogueManager.initialize();
        
        // Save configuration
        return saveConfigurationAndNotify(source, provider);
    }
    
    private static int setupAIWithoutKey(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String provider = StringArgumentType.getString(context, "provider").toLowerCase();
        ServerCommandSource source = context.getSource();
        
        if (!isValidProvider(provider)) {
            CommandMessageUtils.sendError(source, "Invalid provider. Use: gemini, openrouter, or local");
            return 0;
        }
        
        CommandMessageUtils.sendInfo(source, "Setting up " + provider + " provider...");
        CommandMessageUtils.sendInfo(source, "You'll need to set your API key as an environment variable:");
        CommandMessageUtils.sendInfo(source, "export VILLAGERS_REBORN_API_KEY=your_key_here");
        
        provideProviderInfo(source, provider);
        
        VillagersRebornConfig.LLM_PROVIDER = provider;
        setDefaultModelForProvider(provider);
        
        LLMDialogueManager.initialize();
        
        return saveConfigurationAndNotify(source, provider);
    }
    
    private static int setModel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String model = StringArgumentType.getString(context, "model_name");
        ServerCommandSource source = context.getSource();
        
        VillagersRebornConfig.LLM_MODEL = model;
        LLMDialogueManager.initialize();
        
        try {
            com.beeny.config.ConfigManager.saveConfig();
            CommandMessageUtils.sendSuccess(source, "Model set to: " + model);
            return 1;
        } catch (Exception e) {
            CommandMessageUtils.sendError(source, "Failed to save configuration: " + e.getMessage());
            return 0;
        }
    }
    
    private static int testAI(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return testAIWithCategory(context, VillagerDialogueSystem.DialogueCategory.GREETING);
    }
    
    private static int testAIWithCategory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String categoryName = StringArgumentType.getString(context, "category").toUpperCase();
        VillagerDialogueSystem.DialogueCategory category;
        
        try {
            category = VillagerDialogueSystem.DialogueCategory.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            CommandMessageUtils.sendError(context.getSource(), "Invalid category: " + categoryName);
            return 0;
        }
        
        return testAIWithCategory(context, category);
    }
    
    private static int testAIWithCategory(CommandContext<ServerCommandSource> context, VillagerDialogueSystem.DialogueCategory category) {
        ServerCommandSource source = context.getSource();
        
        VillagerEntity villager = getTargetVillager(source);
        if (villager == null) {
            CommandMessageUtils.sendError(source, "Look at a villager to test AI dialogue!");
            return 0;
        }
        
        VillagerData villagerData = VillagerDataUtils.getVillagerDataOrNull(villager);
        if (villagerData == null) {
            CommandMessageUtils.sendError(source, "This villager has no data!");
            return 0;
        }
        
        VillagerDialogueSystem.DialogueContext dialogueContext = 
            new VillagerDialogueSystem.DialogueContext(villager, source.getPlayer());
        
        CommandMessageUtils.sendInfo(source, "Testing " + category.name().toLowerCase() + " dialogue with " + villagerData.getName() + "...");
        
        // Test async LLM dialogue if enabled
        if (VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE) {
            VillagerDialogueSystem.generateDialogueAsync(dialogueContext, category)
                .thenAccept(dialogue -> {
                    context.getSource().getServer().execute(() -> {
                        CommandMessageUtils.sendSuccess(source, "AI Result: " + dialogue.getString());
                    });
                })
                .exceptionally(throwable -> {
                    context.getSource().getServer().execute(() -> {
                        CommandMessageUtils.sendError(source, "AI Error: " + throwable.getMessage());
                    });
                    return null;
                });
        }
        
        // Test static fallback
        net.minecraft.text.Text staticDialogue = VillagerDialogueSystem.generateDialogue(dialogueContext, category, t -> {});
        CommandMessageUtils.sendInfo(source, "Static Fallback: " + staticDialogue.getString());
        
        return 1;
    }
    
    private static int toggleAI(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = !VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE;
        
        String status = VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE ? "ENABLED" : "DISABLED";
        CommandMessageUtils.sendSuccess(context.getSource(), "Dynamic AI dialogue " + status);
        
        return saveConfiguration(context.getSource());
    }
    
    private static int showAIStatus(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        CommandMessageUtils.sendInfo(source, "=== AI System Status ===");
        CommandMessageUtils.sendInfo(source, "Dynamic Dialogue: " + (VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE ? "ENABLED" : "DISABLED"));
        CommandMessageUtils.sendInfo(source, "Provider: " + VillagersRebornConfig.LLM_PROVIDER);
        CommandMessageUtils.sendInfo(source, "Model: " + VillagersRebornConfig.LLM_MODEL);
        CommandMessageUtils.sendInfo(source, "Configured: " + (LLMDialogueManager.isConfigured() ? "YES" : "NO"));
        CommandMessageUtils.sendInfo(source, "Cache Size: " + DialogueCache.size());
        CommandMessageUtils.sendInfo(source, "Memory Size: " + VillagerMemoryManager.getMemorySize());
        CommandMessageUtils.sendInfo(source, "Fallback to Static: " + (VillagersRebornConfig.FALLBACK_TO_STATIC ? "YES" : "NO"));
        
        return 1;
    }
    
    private static int clearAICache(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        LLMDialogueManager.clearCache();
        CommandMessageUtils.sendSuccess(context.getSource(), "AI dialogue cache cleared!");
        return 1;
    }
    
    private static int showCacheSize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = LLMDialogueManager.getCacheSize();
        CommandMessageUtils.sendInfo(context.getSource(), "AI dialogue cache: " + size + " entries");
        return 1;
    }
    
    private static int clearAIMemory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        VillagerEntity villager = getTargetVillager(context.getSource());
        if (villager == null) {
            CommandMessageUtils.sendError(context.getSource(), "Look at a villager to clear their memory!");
            return 0;
        }
        
        VillagerMemoryManager.clearMemory(villager.getUuidAsString());
        CommandMessageUtils.sendSuccess(context.getSource(), "Cleared AI memory for villager!");
        return 1;
    }
    
    private static int showMemorySize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int size = VillagerMemoryManager.getMemorySize();
        CommandMessageUtils.sendInfo(context.getSource(), "Total AI memory: " + size + " conversations");
        return 1;
    }
    
    // Helper methods
    
    private static boolean isValidProvider(String provider) {
        return provider.equals("gemini") || provider.equals("openrouter") || provider.equals("local");
    }
    
    private static void setDefaultModelForProvider(String provider) {
        switch (provider) {
            case "gemini" -> VillagersRebornConfig.LLM_MODEL = "gemini-1.5-flash";
            case "openrouter" -> VillagersRebornConfig.LLM_MODEL = "openai/gpt-3.5-turbo";
            case "local" -> VillagersRebornConfig.LLM_MODEL = "";
        }
    }
    
    private static void provideProviderInfo(ServerCommandSource source, String provider) {
        switch (provider) {
            case "gemini" -> CommandMessageUtils.sendInfo(source, "Get your free API key from: https://ai.google.dev");
            case "openrouter" -> CommandMessageUtils.sendInfo(source, "Get your API key from: https://openrouter.ai");
            case "local" -> CommandMessageUtils.sendInfo(source, "Make sure your local LLM server is running on " + VillagersRebornConfig.LLM_LOCAL_URL);
        }
    }
    
    private static int saveConfigurationAndNotify(ServerCommandSource source, String provider) {
        try {
            com.beeny.config.ConfigManager.saveConfig();
            CommandMessageUtils.sendSuccess(source, "AI configured with " + provider + " provider");
            CommandMessageUtils.sendInfo(source, "Model set to: " + VillagersRebornConfig.LLM_MODEL);
            CommandMessageUtils.sendInfo(source, "Configured: " + (LLMDialogueManager.isConfigured() ? "YES" : "NO"));
            return 1;
        } catch (Exception e) {
            CommandMessageUtils.sendError(source, "Failed to save configuration: " + e.getMessage());
            return 0;
        }
    }
    
    private static int saveConfiguration(ServerCommandSource source) {
        try {
            com.beeny.config.ConfigManager.saveConfig();
            return 1;
        } catch (Exception e) {
            CommandMessageUtils.sendError(source, "Failed to save configuration: " + e.getMessage());
            return 0;
        }
    }
    
    // Suggestion providers
    
    private static CompletableFuture<Suggestions> suggestProviders(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("gemini");
        builder.suggest("openrouter");
        builder.suggest("local");
        return builder.buildFuture();
    }
    
    private static CompletableFuture<Suggestions> suggestDialogueCategories(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (VillagerDialogueSystem.DialogueCategory category : VillagerDialogueSystem.DialogueCategory.values()) {
            builder.suggest(category.name().toLowerCase());
        }
        return builder.buildFuture();
    }
}