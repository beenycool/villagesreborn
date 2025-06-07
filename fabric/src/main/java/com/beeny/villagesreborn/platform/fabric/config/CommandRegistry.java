package com.beeny.villagesreborn.platform.fabric.config;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers commands for Villages Reborn
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);
    
    /**
     * Registers all Villages Reborn commands
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerSettingsTestCommand(dispatcher);
        });
        
        LOGGER.info("Registered Villages Reborn commands");
    }
    
    /**
     * Registers the settings test command
     */
    private static void registerSettingsTestCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("villagesreborn")
                .then(CommandManager.literal("test-settings")
                    .requires(source -> source.hasPermissionLevel(2)) // Require OP level
                    .executes(context -> SettingsTestCommand.execute(context.getSource()))
                )
        );
    }
} 