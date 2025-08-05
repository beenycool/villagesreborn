package com.beeny.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SimpleConfigScreen extends Screen {
    private final Screen parent;
    
    public SimpleConfigScreen(Screen parent) {
        super(Text.literal(com.beeny.constants.StringConstants.UI_TITLE_SETTINGS));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Title
        addDrawableChild(ButtonWidget.builder(
            Text.literal(com.beeny.constants.StringConstants.UI_TITLE_SETTINGS).formatted(Formatting.GOLD),
            button -> {}).dimensions(centerX - 100, centerY - 60, 200, 20).build()).active = false;
        
        // Test button
        addDrawableChild(ButtonWidget.builder(
            Text.literal(com.beeny.constants.StringConstants.UI_SIMPLE_TEST_SCREEN),
            button -> {}).dimensions(centerX - 100, centerY - 30, 200, 20).build()).active = false;
        
        // Open full settings
        addDrawableChild(ButtonWidget.builder(
            Text.literal(com.beeny.constants.StringConstants.UI_OPEN_FULL_SETTINGS),
            button -> {
                try {
                    this.client.setScreen(new VillagersRebornConfigScreen(this.parent));
                } catch (IllegalStateException | NullPointerException e) {
                    var player = this.client.player;
                    if (player != null) {
                        player.sendMessage(Text.literal(com.beeny.constants.StringConstants.UI_ERROR_PREFIX + e.getMessage()).formatted(Formatting.RED), false);
                    }
                    e.printStackTrace();
                }
            }).dimensions(centerX - 100, centerY, 200, 20).build());
        
        // Done button
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, 
            button -> this.client.setScreen(parent))
            .dimensions(centerX - 50, centerY + 40, 100, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Let the super class handle the background rendering
        super.render(context, mouseX, mouseY, delta);
        
        // Draw some text to confirm the screen is working
        context.drawCenteredTextWithShadow(this.textRenderer,
            "If you can see this, the screen is working!",
            this.width / 2, this.height / 2 + 70, 0xFFFFFF);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}