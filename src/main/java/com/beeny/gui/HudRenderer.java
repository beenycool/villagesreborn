package com.beeny.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
// Fix: Add semicolon to the end of the next line
import net.minecraft.client.render.RenderTickCounter;

/**
 * Registers and manages all HUD elements for the Villages Reborn mod.
 */
public class HudRenderer {
    private static boolean initialized = false;

    /**
     * Initializes and registers all HUD renderers.
     * Should be called during client initialization.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register the EventNotificationManager renderer
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            EventNotificationManager.getInstance().render(context, client, tickCounter.tickDelta); // Final attempt at field access based on docs
        });
    }
}