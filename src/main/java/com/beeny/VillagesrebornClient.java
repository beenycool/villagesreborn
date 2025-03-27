package com.beeny;

import com.beeny.gui.VillageInfoHud;
import com.beeny.network.VillageCraftingClientNetwork;
import com.beeny.network.VillageCraftingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagesrebornClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn-client");
    private static VillageInfoHud villageInfoHud;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client");
        
        // Register village info HUD
        villageInfoHud = new VillageInfoHud();
        HudRenderCallback.EVENT.register((context, tickDelta) -> 
            villageInfoHud.render(context, context.getClient()));
            
        // Register key bindings
        KeyBinding infoKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.info",
            GLFW.GLFW_KEY_B,
            "category.villagesreborn.keys"
        ));
        
        // Register client-side network handlers
        registerNetworking();
        
        LOGGER.info("Villages Reborn client initialization complete");
    }
    
    private void registerNetworking() {
        LOGGER.info("Registering client-side networking handlers");
        
        // Register client-side packet handlers if needed
        // For now, we're only sending packets to the server, not receiving any
        
        // Just logging to confirm that the client side registration is complete
        LOGGER.info("Client-side network handlers registered successfully");
    }
    
    public static VillageInfoHud getVillageInfoHud() {
        return villageInfoHud;
    }
}
