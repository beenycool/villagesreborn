package com.beeny;

import com.beeny.item.VillagerJournalItemClient;
import com.beeny.network.VillagerTeleportPacketClient;
import com.beeny.network.OpenFamilyTreePacketClient;
import com.beeny.network.FamilyTreeDataPacketClient;
import com.beeny.network.RequestVillagerListPacketClient;
import com.beeny.commands.ClientDialogueCommands;
import com.beeny.client.gui.SimpleConfigScreen;
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
    private static KeyBinding openSettingsKey;

    @Override
    public void onInitializeClient() {
        
        VillagerTeleportPacketClient.register();
        OpenFamilyTreePacketClient.register();
        FamilyTreeDataPacketClient.register();
        RequestVillagerListPacketClient.register();
        
        // Register client commands
        ClientDialogueCommands.register();
        
        
        openJournalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            com.beeny.constants.StringConstants.KB_OPEN_JOURNAL,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            com.beeny.constants.StringConstants.KB_CATEGORY_GENERAL
        ));
        
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            com.beeny.constants.StringConstants.KB_OPEN_SETTINGS,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            com.beeny.constants.StringConstants.KB_CATEGORY_GENERAL
        ));
        
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openJournalKey.wasPressed()) {
                if (client.player != null && client.world != null) {
                    VillagerJournalItemClient.openVillagerJournal(client.world, client.player);
                }
            }
            
            while (openSettingsKey.wasPressed()) {
                if (client.player != null) {
                    try {
                        client.execute(() -> {
                            client.setScreen(new SimpleConfigScreen(client.currentScreen));
                        });
                    } catch (Exception e) {
                        client.player.sendMessage(Text.literal(com.beeny.constants.StringConstants.UI_ERROR_PREFIX + e.getMessage()), false);
                        com.beeny.Villagersreborn.LOGGER.error("Error opening settings screen", e);
                    }
                }
            }
        });
    }
}