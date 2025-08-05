package com.beeny.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;

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
                                       net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.RegistrationEnvironment environment) {
        
        // Register all command modules
        VillagerAICommands.register(dispatcher);
        VillagerManagementCommands.register(dispatcher);
        VillagerFamilyCommands.register(dispatcher);
        VillagerDataCommands.register(dispatcher);
    }
}