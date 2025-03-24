package com.beeny;

import com.beeny.ai.ModelChecker;
import com.beeny.gui.ModelDownloadScreen;
import com.beeny.gui.SetupScreen;
import com.beeny.gui.VillageInfoHud;
import com.beeny.gui.ConversationHud;
import com.beeny.gui.EventNotificationManager;
import com.beeny.gui.VillageDebugScreen;
import com.beeny.setup.LLMConfig;
import com.beeny.api.VillageHudAPI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagesrebornClient implements ClientModInitializer, VillageHudAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private boolean setupScreenShown = false;
    private boolean modelCheckDone = false;
    private int tickCounter = 0;
    private static final int SETUP_DELAY_TICKS = 40;
    
    private static VillageHudAPI API;
    
    private final VillageInfoHud villageInfoHud;
    private final EventNotificationManager eventManager;
    private final ConversationHud conversationHud;
    
    private KeyBinding toggleHudKeyBinding;
    private KeyBinding villagePingKeyBinding;
    private KeyBinding villageDebugKeyBinding;
    private KeyBinding cycleConversationPositionKeyBinding;
    
    private boolean hudEnabled = true;
    
    public VillagesrebornClient() {
        API = this;
        villageInfoHud = new VillageInfoHud();
        eventManager = new EventNotificationManager();
        conversationHud = new ConversationHud();
    }
    
    public static VillageHudAPI getInstance() {
        return API != null ? API : VillageHudAPI.getInstance();
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client");
        registerKeybindings();
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!setupScreenShown && client.world != null) {
                if (tickCounter >= SETUP_DELAY_TICKS) {
                    setupScreenShown = true;
                    LLMConfig llmConfig = Villagesreborn.getLLMConfig();
                    
                    if (!modelCheckDone) {
                        modelCheckDone = true;
                        String provider = llmConfig.getProvider();
                        boolean needsLocalModel = !ModelChecker.hasLocalModel() && 
                                                 (provider.equals("local") || !llmConfig.isConfigured());
                        
                        if (needsLocalModel) {
                            client.setScreen(new ModelDownloadScreen(null, llmConfig));
                            return;
                        }
                    }
                    
                    if (!llmConfig.isConfigured()) {
                        client.setScreen(new SetupScreen(llmConfig));
                    }
                }
                tickCounter++;
            }
            
            handleKeybindings(client);
        });
        
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null && !client.options.hudHidden && hudEnabled) {
                villageInfoHud.render(context, client);
                eventManager.render(context, client, tickDelta);
                conversationHud.render(context, client);
            }
        });

        registerEventHandlers();
        LOGGER.info("Villages Reborn client initialized successfully");
    }
    
    private void registerKeybindings() {
        toggleHudKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.togglehud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.villagesreborn.general"
        ));
        
        villagePingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.villageping",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.villagesreborn.general"
        ));
        
        villageDebugKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.villagedebug",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.villagesreborn.debug"
        ));
        
        cycleConversationPositionKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.cycleconversationposition",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.villagesreborn.general"
        ));
    }
    
    private void handleKeybindings(MinecraftClient client) {
        if (toggleHudKeyBinding.wasPressed()) {
            hudEnabled = !hudEnabled;
            if (client.player != null) {
                client.player.sendMessage(
                    Text.of("Village HUD: " + (hudEnabled ? "§aEnabled" : "§cDisabled")),
                    true
                );
            }
        }
        
        if (villagePingKeyBinding.wasPressed() && client.player != null) {
            Villagesreborn.getInstance().pingVillageInfo(client.player);
        }
        
        if (villageDebugKeyBinding.wasPressed() && 
                InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 
                GLFW.GLFW_KEY_LEFT_SHIFT)) {
            client.setScreen(new VillageDebugScreen());
        }
        
        if (cycleConversationPositionKeyBinding.wasPressed()) {
            cycleConversationPosition();
        }
    }
    
    private void cycleConversationPosition() {
        ConversationHud.HudPosition currentPosition = conversationHud.getPosition();
        ConversationHud.HudPosition newPosition;
        
        switch (currentPosition) {
            case TOP_LEFT -> newPosition = ConversationHud.HudPosition.TOP_RIGHT;
            case TOP_RIGHT -> newPosition = ConversationHud.HudPosition.BOTTOM_RIGHT;
            case BOTTOM_RIGHT -> newPosition = ConversationHud.HudPosition.BOTTOM_LEFT;
            default -> newPosition = ConversationHud.HudPosition.TOP_LEFT;
        }
        
        conversationHud.setPosition(newPosition);
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                Text.of("Conversation indicator position: " + newPosition.name()),
                true
            );
        }
    }
    
    private void registerEventHandlers() {
    }
    
    @Override
    public void showEventNotification(String title, String description, int durationTicks) {
        if (hudEnabled) {
            eventManager.addNotification(title, description, durationTicks);
        }
    }
    
    @Override
    public void updateVillageInfo(String cultureName, int prosperity, int safety, int population) {
        villageInfoHud.update(cultureName, prosperity, safety, population);
    }
    
    public void showHudTemporarily(long durationMillis) {
        villageInfoHud.forceDisplay(durationMillis);
    }
    
    public boolean isHudEnabled() {
        return hudEnabled;
    }
    
    public void setHudEnabled(boolean enabled) {
        this.hudEnabled = enabled;
    }
    
    public void startConversation(VillagerEntity villager) {
        conversationHud.startConversation(villager);
    }
    
    public void endConversation() {
        conversationHud.endConversation();
    }
    
    public void updateConversation() {
        conversationHud.updateInteraction();
    }
    
    public boolean isInConversation() {
        return conversationHud.isConversationActive();
    }
    
    public VillagerEntity getCurrentConversationVillager() {
        return conversationHud.getCurrentVillager();
    }
}
