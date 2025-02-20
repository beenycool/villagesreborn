package com.beeny;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import com.beeny.gui.SetupScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagesrebornClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.currentScreen instanceof SetupScreen) {
                client.setScreen(null);
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