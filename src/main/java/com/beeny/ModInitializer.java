package com.beeny;

import com.beeny.commands.VillagerCommands;
import com.beeny.dialogue.LLMDialogueManager;
import com.beeny.network.FamilyTreeDataPacket;
import com.beeny.network.OpenFamilyTreePacket;
import com.beeny.network.RequestVillagerListPacket;
import com.beeny.network.TestLLMConnectionPacket;
import com.beeny.network.TestLLMConnectionResultPacket;
import com.beeny.network.UpdateVillagerNotesPacket;
import com.beeny.network.VillagerMarriagePacket;
import com.beeny.network.VillagerTeleportPacket;
import com.beeny.registry.ModItems;
import com.beeny.system.ServerVillagerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModInitializer implements ModInitializer {

    public static final String MOD_ID = com.beeny.constants.StringConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[INFO] [ModInitializer] [onInitialize] - Initializing Villages Reborn mod...");

        com.beeny.config.ConfigManager.loadConfig();

        String apiKey = System.getenv(com.beeny.constants.StringConstants.ENV_API_KEY);
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("[WARN] [ModInitializer] [onInitialize] - Villagers Reborn API key is not set! Please set the environment variable VILLAGERS_REBORN_API_KEY before starting the server.");
            LOGGER.warn("[WARN] [ModInitializer] [onInitialize] - For user convenience, a placeholder 'llmApiKey' is present in the config file. Do NOT put your API key there; use the environment variable for security.");
        }

        ModItems.initialize();
        VillagerCommands.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerVillagerManager.getInstance().initialize(server);
            try {
                LLMDialogueManager.initialize();
            } catch (Exception e) {
                LOGGER.error("[ERROR] [ModInitializer] [onInitialize] - Failed to initialize LLMDialogueManager: {}", e.getMessage(), e);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                LLMDialogueManager.shutdown();
            } catch (Exception e) {
                LOGGER.error("[ERROR] [ModInitializer] [onInitialize] - Failed to shutdown LLMDialogueManager: {}", e.getMessage(), e);
            }
        });

        VillagerTeleportPacket.register();
        UpdateVillagerNotesPacket.register();
        VillagerMarriagePacket.register();
        OpenFamilyTreePacket.register();
        FamilyTreeDataPacket.register();
        RequestVillagerListPacket.register();
        TestLLMConnectionPacket.register();
        TestLLMConnectionResultPacket.register();

        ServerPlayNetworking.registerGlobalReceiver(TestLLMConnectionPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                LLMDialogueManager.testConnectionSecure().thenAccept(success -> {
                    String message = success ? com.beeny.constants.StringConstants.MSG_CONNECTION_SUCCESS : com.beeny.constants.StringConstants.MSG_CONNECTION_FAILED;
                    TestLLMConnectionResultPacket result = new TestLLMConnectionResultPacket(success, message);
                    ServerPlayNetworking.send(context.player(), result);
                });
            });
        });

        EventHandler.register();
        TickHandler.register();

        LOGGER.info("[INFO] [ModInitializer] [onInitialize] - Villages Reborn mod initialized successfully!");
    }
}