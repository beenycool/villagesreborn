package com.beeny.commands;

/**
 * All dialogue command logic has been consolidated into VillagerCommands.java.
 * This file is now deprecated and will be removed in a future version.
 */
public class DialogueCommands {
    // Deprecated: All logic moved to VillagerCommands.java
}
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
    
        // Persist configuration to disk
        try {
            com.beeny.config.ConfigManager.saveConfig();
            context.getSource().sendFeedback(() ->
                Text.literal("Configuration saved").formatted(Formatting.GRAY), false);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to save configuration: " + e.getMessage()));
        }
        
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
    
        // Persist configuration to disk
        try {
            com.beeny.config.ConfigManager.saveConfig();
            context.getSource().sendFeedback(() ->
                Text.literal("Configuration saved").formatted(Formatting.GRAY), false);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to save configuration: " + e.getMessage()));
        }
        
        return 1;
    }
    
    private static int testConnection(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> 
            Text.literal("Testing LLM connection...").formatted(Formatting.YELLOW), false);
        
        LLMDialogueManager.testConnection(
            com.beeny.config.VillagersRebornConfig.LLM_PROVIDER,
            System.getenv("VILLAGERS_REBORN_API_KEY"),
            com.beeny.config.VillagersRebornConfig.LLM_API_ENDPOINT,
            com.beeny.config.VillagersRebornConfig.LLM_MODEL
        ).thenAccept(success -> {
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