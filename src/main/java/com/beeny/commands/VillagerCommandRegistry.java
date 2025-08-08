package com.beeny.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import com.beeny.commands.TTSCommands;

/**
 * Registry for all villager-related commands.
 * This class coordinates the registration of all villager command modules.
 */
public class VillagerCommandRegistry {
    
    /**
     * Registers all villager commands with the server
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register(VillagerCommandRegistry::registerCommands);
    }
    
    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                       CommandRegistryAccess registryAccess,
                                       CommandManager.RegistrationEnvironment environment) {
        
        // Create the main villager command
        LiteralArgumentBuilder<ServerCommandSource> villagerCommand = CommandManager.literal("villager");
        
        // Register all command modules as sub-commands
        VillagerAICommands.register(villagerCommand);
        VillagerManagementCommands.register(villagerCommand);
        VillagerFamilyCommands.register(villagerCommand);
        VillagerDataCommands.register(villagerCommand);
        VillagerActivityCommands.register(villagerCommand, registryAccess);
        VillagerListCommands.register(dispatcher);
        TTSCommands.register(dispatcher);
        
        // Register the main command
        dispatcher.register(villagerCommand);
    }
}