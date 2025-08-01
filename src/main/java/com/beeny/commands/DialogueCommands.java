package com.beeny.commands;

import com.beeny.dialogue.LLMDialogueManager;
import com.beeny.dialogue.DialogueCache;
import com.beeny.dialogue.VillagerMemoryManager;
import com.beeny.system.VillagerDialogueSystem;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.data.VillagerData;
import com.beeny.Villagersreborn;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.concurrent.CompletableFuture;

public class DialogueCommands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("dialogue")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("setup")
                .executes(DialogueCommands::showSetupInstructions))
            .then(CommandManager.literal("test")
                .executes(DialogueCommands::testDialogue)
                .then(CommandManager.argument("category", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        for (VillagerDialogueSystem.DialogueCategory category : VillagerDialogueSystem.DialogueCategory.values()) {
                            builder.suggest(category.name().toLowerCase());
                        }
                        return builder.buildFuture();
                    })
                    .executes(DialogueCommands::testDialogueWithCategory)))
            .then(CommandManager.literal("status")
                .executes(DialogueCommands::showStatus))
            .then(CommandManager.literal("cache")
                .then(CommandManager.literal("clear")
                    .executes(DialogueCommands::clearCache))
                .then(CommandManager.literal("size")
                    .executes(DialogueCommands::cacheSize)))
            .then(CommandManager.literal("memory")
                .then(CommandManager.literal("clear")
                    .executes(DialogueCommands::clearMemory))
                .then(CommandManager.literal("size")
                    .executes(DialogueCommands::memorySize)))
            .then(CommandManager.literal("toggle")
                .executes(DialogueCommands::toggleDynamicDialogue))
            .then(CommandManager.literal("provider")
                .then(CommandManager.argument("provider", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("gemini");
                        builder.suggest("openrouter");
                        return builder.buildFuture();
                    })
                    .executes(DialogueCommands::setProvider)))
            .then(CommandManager.literal("connection")
                .executes(DialogueCommands::testConnection))
        );
    }
    
    private static int testDialogue(CommandContext<ServerCommandSource> context) {
        return testDialogueWithCategory(context, VillagerDialogueSystem.DialogueCategory.GREETING);
    }
    
    private static int testDialogueWithCategory(CommandContext<ServerCommandSource> context) {
        String categoryName = StringArgumentType.getString(context, "category").toUpperCase();
        VillagerDialogueSystem.DialogueCategory category;
        
        try {
            category = VillagerDialogueSystem.DialogueCategory.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFeedback(() -> 
                Text.literal("Invalid category: " + categoryName).formatted(Formatting.RED), false);
            return 0;
        }
        
        return testDialogueWithCategory(context, category);
    }
    
    private static int testDialogueWithCategory(CommandContext<ServerCommandSource> context, VillagerDialogueSystem.DialogueCategory category) {
        ServerCommandSource source = context.getSource();
        
        // Get the villager the player is looking at
        VillagerEntity villager = getTargetVillager(source);
        if (villager == null) {
            source.sendFeedback(() -> 
                Text.literal("Look at a villager to test dialogue!").formatted(Formatting.RED), false);
            return 0;
        }
        
        VillagerData villagerData = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (villagerData == null) {
            source.sendFeedback(() -> 
                Text.literal("This villager has no data!").formatted(Formatting.RED), false);
            return 0;
        }
        
        // Create dialogue context
        VillagerDialogueSystem.DialogueContext dialogueContext = 
            new VillagerDialogueSystem.DialogueContext(villager, source.getPlayer());
        
        source.sendFeedback(() -> 
            Text.literal("Testing " + category.name().toLowerCase() + " dialogue with " + villagerData.getName() + "...")
                .formatted(Formatting.YELLOW), false);
        
        // Test both sync and async
        if (VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE) {
            // Test async LLM dialogue
            VillagerDialogueSystem.generateDialogueAsync(dialogueContext, category)
                .thenAccept(dialogue -> {
                    source.sendFeedback(() -> 
                        Text.literal("LLM Result: ").formatted(Formatting.GREEN)
                            .append(dialogue), false);
                })
                .exceptionally(throwable -> {
                    source.sendFeedback(() -> 
                        Text.literal("LLM Error: " + throwable.getMessage()).formatted(Formatting.RED), false);
                    return null;
                });
        }
        
        // Test static fallback
        Text staticDialogue = VillagerDialogueSystem.generateDialogue(dialogueContext, category);
        source.sendFeedback(() -> 
            Text.literal("Static Result: ").formatted(Formatting.BLUE)
                .append(staticDialogue), false);
        
        return 1;
    }
    
    private static int showStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> 
            Text.literal("=== Dialogue System Status ===").formatted(Formatting.GOLD), false);
        
        source.sendFeedback(() -> 
            Text.literal("Dynamic Dialogue: " + (VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE ? "ENABLED" : "DISABLED"))
                .formatted(VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE ? Formatting.GREEN : Formatting.RED), false);
        
        source.sendFeedback(() -> 
            Text.literal("LLM Provider: " + LLMDialogueManager.getProviderName())
                .formatted(Formatting.YELLOW), false);
        
        source.sendFeedback(() -> 
            Text.literal("Provider Configured: " + (LLMDialogueManager.isConfigured() ? "YES" : "NO"))
                .formatted(LLMDialogueManager.isConfigured() ? Formatting.GREEN : Formatting.RED), false);
        
        source.sendFeedback(() -> 
            Text.literal("Cache Size: " + DialogueCache.size())
                .formatted(Formatting.AQUA), false);
        
        source.sendFeedback(() -> 
            Text.literal("Memory Size: " + VillagerMemoryManager.getMemorySize())
                .formatted(Formatting.AQUA), false);
        
        source.sendFeedback(() -> 
            Text.literal("Fallback to Static: " + (VillagersRebornConfig.FALLBACK_TO_STATIC ? "YES" : "NO"))
                .formatted(Formatting.YELLOW), false);
        
        return 1;
    }
    
    private static int clearCache(CommandContext<ServerCommandSource> context) {
        LLMDialogueManager.clearCache();
        context.getSource().sendFeedback(() -> 
            Text.literal("Dialogue cache cleared!").formatted(Formatting.GREEN), false);
        return 1;
    }
    
    private static int cacheSize(CommandContext<ServerCommandSource> context) {
        int size = LLMDialogueManager.getCacheSize();
        context.getSource().sendFeedback(() -> 
            Text.literal("Dialogue cache size: " + size + " entries").formatted(Formatting.AQUA), false);
        return 1;
    }
    
    private static int clearMemory(CommandContext<ServerCommandSource> context) {
        // Get the villager the player is looking at
        VillagerEntity villager = getTargetVillager(context.getSource());
        if (villager == null) {
            context.getSource().sendFeedback(() -> 
                Text.literal("Look at a villager to clear their memory!").formatted(Formatting.RED), false);
            return 0;
        }
        
        VillagerMemoryManager.clearMemory(villager.getUuidAsString());
        context.getSource().sendFeedback(() -> 
            Text.literal("Cleared memory for villager!").formatted(Formatting.GREEN), false);
        return 1;
    }
    
    private static int memorySize(CommandContext<ServerCommandSource> context) {
        int size = VillagerMemoryManager.getMemorySize();
        context.getSource().sendFeedback(() -> 
            Text.literal("Total memory size: " + size + " conversations").formatted(Formatting.AQUA), false);
        return 1;
    }
    
    private static int toggleDynamicDialogue(CommandContext<ServerCommandSource> context) {
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = !VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE;
        
        String status = VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE ? "ENABLED" : "DISABLED";
        Formatting color = VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE ? Formatting.GREEN : Formatting.RED;
        
        context.getSource().sendFeedback(() -> 
            Text.literal("Dynamic dialogue " + status).formatted(color), false);
        
        return 1;
    }
    
    private static int setProvider(CommandContext<ServerCommandSource> context) {
        String provider = StringArgumentType.getString(context, "provider").toLowerCase();
        
        VillagersRebornConfig.LLM_PROVIDER = provider;
        
        // Reinitialize the dialogue manager
        LLMDialogueManager.initialize();
        
        context.getSource().sendFeedback(() -> 
            Text.literal("LLM provider set to: " + provider).formatted(Formatting.GREEN), false);
        
        context.getSource().sendFeedback(() -> 
            Text.literal("Provider configured: " + (LLMDialogueManager.isConfigured() ? "YES" : "NO"))
                .formatted(LLMDialogueManager.isConfigured() ? Formatting.GREEN : Formatting.RED), false);
        
        return 1;
    }
    
    private static int testConnection(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> 
            Text.literal("Testing LLM connection...").formatted(Formatting.YELLOW), false);
        
        LLMDialogueManager.testConnection()
            .thenAccept(success -> {
                if (success) {
                    source.sendFeedback(() -> 
                        Text.literal("Connection test PASSED!").formatted(Formatting.GREEN), false);
                } else {
                    source.sendFeedback(() -> 
                        Text.literal("Connection test FAILED!").formatted(Formatting.RED), false);
                }
            })
            .exceptionally(throwable -> {
                source.sendFeedback(() -> 
                    Text.literal("Connection test ERROR: " + throwable.getMessage()).formatted(Formatting.RED), false);
                return null;
            });
        
        return 1;
    }
    
    private static VillagerEntity getTargetVillager(ServerCommandSource source) {
        if (source.getPlayer() == null) return null;
        
        HitResult hitResult = source.getPlayer().raycast(10.0, 1.0f, false);
        
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) hitResult).getEntity();
            if (entity instanceof VillagerEntity) {
                return (VillagerEntity) entity;
            }
        }
        
        return null;
    }
    
    private static int showSetupInstructions(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> 
            Text.literal("=== Dynamic Dialogue Setup ===").formatted(Formatting.GOLD, Formatting.BOLD), false);
        
        source.sendFeedback(() -> 
            Text.literal("\nChoose your setup method:").formatted(Formatting.AQUA), false);
        
        source.sendFeedback(() -> 
            Text.literal("\n🎯 ").formatted(Formatting.GREEN)
                .append(Text.literal("GUIDED SETUP").formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal(" (Recommended for beginners)").formatted(Formatting.GRAY)), false);
        
        source.sendFeedback(() -> 
            Text.literal("   Step-by-step wizard with help and explanations").formatted(Formatting.WHITE), false);
        
        source.sendFeedback(() -> 
            Text.literal("   → ").formatted(Formatting.WHITE)
                .append(Text.literal("/dialogue-setup").formatted(Formatting.YELLOW)
                    .styled(style -> style.withClickEvent(new ClickEvent.RunCommand("/dialogue-setup")))), false);
        
        source.sendFeedback(() -> 
            Text.literal("\n⚡ ").formatted(Formatting.BLUE)
                .append(Text.literal("QUICK SETUP").formatted(Formatting.BLUE, Formatting.BOLD))
                .append(Text.literal(" (If you already have an API key)").formatted(Formatting.GRAY)), false);
        
        source.sendFeedback(() -> 
            Text.literal("   Gemini: ").formatted(Formatting.WHITE)
                .append(Text.literal("/dialogue-setup quick gemini YOUR_API_KEY").formatted(Formatting.YELLOW)
                    .styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/dialogue-setup quick gemini ")))), false);
        
        source.sendFeedback(() -> 
            Text.literal("   OpenRouter: ").formatted(Formatting.WHITE)
                .append(Text.literal("/dialogue-setup quick openrouter YOUR_API_KEY").formatted(Formatting.YELLOW)
                    .styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand("/dialogue-setup quick openrouter ")))), false);
        
        if (source.getPlayer() != null) {
            source.sendFeedback(() -> 
                Text.literal("\n🖥️ ").formatted(Formatting.LIGHT_PURPLE)
                    .append(Text.literal("GUI SETUP").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                    .append(Text.literal(" (Client-side only)").formatted(Formatting.GRAY)), false);
            
            source.sendFeedback(() -> 
                Text.literal("   Visual interface with easy configuration").formatted(Formatting.WHITE), false);
            
            source.sendFeedback(() -> 
                Text.literal("   → ").formatted(Formatting.WHITE)
                    .append(Text.literal("/dialogue-config").formatted(Formatting.YELLOW)
                        .styled(style -> style.withClickEvent(new ClickEvent.RunCommand("/dialogue-config")))), false);
        }
        
        source.sendFeedback(() -> 
            Text.literal("\n📚 Need help? Use: ").formatted(Formatting.WHITE)
                .append(Text.literal("/dialogue-help").formatted(Formatting.AQUA)
                    .styled(style -> style.withClickEvent(new ClickEvent.RunCommand("/dialogue-help")))), false);
        
        return 1;
    }
}