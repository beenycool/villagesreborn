package com.beeny;

import com.beeny.gui.SetupScreen;
import com.beeny.setup.LLMConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagesrebornClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private boolean setupScreenShown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!setupScreenShown && client.world != null) {
                setupScreenShown = true;
                LLMConfig llmConfig = Villagesreborn.getLLMConfig();
                client.setScreen(new SetupScreen(llmConfig));
            }
        });

        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                // Future: Render village-related HUD elements here
            }
        });

        LOGGER.info("Villages Reborn client initialized successfully");
    }
}