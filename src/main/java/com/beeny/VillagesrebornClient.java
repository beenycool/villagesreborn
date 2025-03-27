package com.beeny;

import com.beeny.gui.VillageInfoHud;
import com.beeny.network.VillageCraftingClientNetwork;
import com.beeny.network.VillageCraftingNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

public class VillagesrebornClient implements ClientModInitializer {
    private static VillageInfoHud villageInfoHud;
    
    @Override
    public void onInitializeClient() {
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
    }
    
    private void registerNetworking() {
        // Register our networking components on the client side
        // The actual implementation of VillageCraftingClientNetwork is already created
    }
    
    public static VillageInfoHud getVillageInfoHud() {
        return villageInfoHud;
    }
}
