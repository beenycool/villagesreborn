package com.beeny;

import com.beeny.gui.SetupScreen;
import com.beeny.setup.LLMConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagesrebornClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private boolean setupScreenShown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Villages Reborn Client initialized");

        // Register tick event to show setup screen after game loads
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!setupScreenShown && client.world != null) {
                setupScreenShown = true;
                VillagesConfig.LLMSettings llmConfig = VillagesConfig.getInstance().getLLMSettings();
                client.setScreen(new SetupScreen(llmConfig));
            }
        });
    }
}