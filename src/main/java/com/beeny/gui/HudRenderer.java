package com.beeny.gui;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

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
        HudRenderCallback.EVENT.register((DrawContext context, float tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            EventNotificationManager.getInstance().render(context, client, tickDelta);
        });
    }
}