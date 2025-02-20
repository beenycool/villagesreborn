package com.beeny;

import com.beeny.village.VillagerManager;
import com.beeny.ai.LLMService;
import com.beeny.commands.ModCommands;
import com.beeny.setup.LLMConfig;
import com.beeny.setup.SetupWizard;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.village.VillagerAI;

public class Villagesreborn implements ModInitializer {
    public static final String MOD_ID = "villagesreborn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static SetupWizard setupWizard;
    private final VillagerManager villagerManager = VillagerManager.getInstance();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Villages Reborn...");
        
        // Initialize setup wizard
        setupWizard = new SetupWizard();
        
        // Initialize LLM service with configuration
        LLMService.initialize(setupWizard.getLlmConfig());
        
        // Register commands
        ModCommands.register();
        
        // Register event handlers
        registerEvents();
        
        // Register chat listener
        registerChatListener();
        
        LOGGER.info("Villages Reborn initialization complete!");
    }

    public static SetupWizard getSetupWizard() {
        return setupWizard;
    }

    private void registerEvents() {
        // Register villager spawn events
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof VillagerEntity villager) {
                villagerManager.onVillagerSpawn(villager, world);
            }
        });

        // Register villager tick events
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof VillagerEntity villager) {
                villagerManager.removeVillager(villager.getUuid());
            }
        });
    }

    private void registerChatListener() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            handler.player.networkHandler.registerReceiver(ChatMessageC2SPacket.class, (context, packet) -> {
                handlePlayerMessage(handler.player, packet.getChatMessage());
            });
        });
    }

    private void handlePlayerMessage(ServerPlayerEntity player, String message) {
        VillagerManager villagerManager = VillagerManager.getInstance();
        String[] words = message.split(" ");
        if (words.length > 1 && words[0].equalsIgnoreCase("hello")) {
            String villagerName = words[1];
            VillagerEntity villager = villagerManager.findNearbyVillagerByName(player, villagerName);
            if (villager != null) {
                VillagerAI villagerAI = villagerManager.getVillagerAI(villager.getUuid());
                villagerAI.generateBehavior("greeting").thenAccept(response -> {
                    player.sendMessage(Text.literal(villager.getName().getString() + ": " + response), false);
                });
            }
        }
    }
}