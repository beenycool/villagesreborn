package com.beeny;

import com.beeny.item.VillagerJournalItemClient;
import com.beeny.network.VillagerTeleportPacketClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class VillagersRebornClient implements ClientModInitializer {
    private static KeyBinding openJournalKey;

    @Override
    public void onInitializeClient() {
        // Register client-side networking
        VillagerTeleportPacketClient.register();
        
        // Register key binding for opening journal
        openJournalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagersreborn.open_journal",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "category.villagersreborn.general"
        ));
        
        // Register tick event for key handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openJournalKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    VillagerJournalItemClient.openVillagerJournal(client.world, client.player);
                }
            }
        });
    }
}