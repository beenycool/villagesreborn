package com.beeny;

import com.beeny.commands.TestTTSCommand;
import com.beeny.data.VillagerData;
import com.beeny.network.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Thin delegator preserved for compatibility. Real logic split into:
 * - ModInitializer (initialization, registration, setup)
 * - EventHandler (event handling)
 * - TickHandler (server tick periodic tasks)
 */
public class Villagersreborn implements ModInitializer {
    public static final String MOD_ID = com.beeny.constants.StringConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final AttachmentType<String> VILLAGER_NAME = AttachmentRegistry.<String>builder()
            .persistent(Codec.STRING)
            .buildAndRegister(Identifier.of(MOD_ID, "villager_name"));

    public static final AttachmentType<VillagerData> VILLAGER_DATA = AttachmentRegistry.<VillagerData>builder()
            .persistent(VillagerData.CODEC)
            .initializer(VillagerData::new)
            .buildAndRegister(Identifier.of(MOD_ID, "villager_data"));

    @Override
    public void onInitialize() {
        // Initialize the mod directly since the separate ModInitializer was removed
        LOGGER.info("Initializing VillagersReborn mod...");
        
        // Register commands
        com.beeny.commands.VillagerCommandRegistry.register();
        
        // Register TTS test command using callback
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("testtts")
                    .then(
                        argument("text", StringArgumentType.greedyString())
                            .executes(context -> new TestTTSCommand().run(context))
                    )
            );
        });
        
        // Register network packets
        OpenFamilyTreePacket.register();
        FamilyTreeDataPacket.register();
        UpdateVillagerNotesPacket.register();
        TestLLMConnectionPacket.register();
        VillagerTeleportPacket.register();
        TestLLMConnectionResultPacket.register();
        RequestVillagerListPacket.register();
        VillagerMarriagePacket.register();
        
        // Register server lifecycle events for AI system
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, initializing AI World Manager...");
            try {
                com.beeny.ai.AIWorldManagerRefactored.initialize(server);
                LOGGER.info("AI World Manager initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize AI World Manager", e);
            }
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, shutting down AI World Manager...");
            try {
                com.beeny.ai.AIWorldManagerRefactored.cleanup();
                LOGGER.info("AI World Manager shutdown complete");
            } catch (Exception e) {
                LOGGER.error("Error during AI World Manager shutdown", e);
            }
        });
        
        // Register tick handlers
        com.beeny.TickHandler.register();
        
        LOGGER.info("VillagersReborn mod initialized successfully!");
    }
}