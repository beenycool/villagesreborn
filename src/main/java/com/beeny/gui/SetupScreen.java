package com.beeny.gui;

import com.beeny.config.VillagesConfig; // Use main config
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import java.util.function.Function;

public class SetupScreen extends Screen {
    private final VillagesConfig config; // Use main config instance
    private final VillagesConfig.LLMSettings llmSettings; // Get nested settings
    private final AnimationRenderer animationRenderer = new AnimationRenderer();
    private static final Identifier BACKGROUND = Identifier.of("villagesreborn", "textures/gui/anim_frame1.png");
    private static final Function<Identifier, RenderLayer> TEXTURE_LAYER = id -> RenderLayer.getGui();

    public SetupScreen(VillagesConfig config) { // Accept main config
        super(Text.literal("Villages Reborn"));
        this.config = config;
        this.llmSettings = config.getLLMSettings(); // Get nested settings
    }

    @Override
    protected void init() {
        if (!animationRenderer.isComplete()) {
            return;
        }

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = width / 2 - buttonWidth / 2;
        int centerY = height / 2;
        int spacing = 24;

        // Quick Start button
        addDrawableChild(ButtonWidget.builder(Text.literal("Quick Start (Recommended)"), button -> {
            // QuickStartMode and SetupComplete are part of the old LLMConfig, not needed here.
            // Set provider and model directly in the main config's LLMSettings.
            llmSettings.setProvider("deepseek");
            llmSettings.setModel("deepseek-coder"); // Use setModel
            config.save(); // Save the main config
            close();
        })
        .dimensions(centerX, centerY - spacing, buttonWidth, buttonHeight)
        .build());

        // Advanced Setup button
        addDrawableChild(ButtonWidget.builder(Text.literal("Advanced Setup"), button -> {
            // QuickStartMode is part of the old LLMConfig.
            // Pass the main config to ModelDownloadScreen
            client.setScreen(new ModelDownloadScreen(this, config));
        })
        .dimensions(centerX, centerY, buttonWidth, buttonHeight)
        .build());

        // Skip button
        addDrawableChild(ButtonWidget.builder(Text.literal("Skip AI Features"), button -> {
            // SetupComplete is part of the old LLMConfig.
            // Just save the main config to persist any defaults if needed.
            config.save();
            close();
        })
        .dimensions(centerX, centerY + spacing, buttonWidth, buttonHeight)
        .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;

        if (!animationRenderer.isComplete()) {
            animationRenderer.render(context, width, height, delta);
            return;
        }

        // Draw background
        context.fill(0, 0, width, height, 0xFF000000);

        // Draw title
        String title = "Greetings, traveler!";
        int titleX = centerX - textRenderer.getWidth(title) / 2;
        context.drawText(textRenderer, title, titleX, height / 4, 0xFFFFFF, true);

        // Draw subtitle
        String subtitle = "Choose Your Setup Mode";
        int subtitleX = centerX - textRenderer.getWidth(subtitle) / 2;
        context.drawText(textRenderer, subtitle, subtitleX, height / 4 + 20, 0xFFFFFF, true);

        // Draw option descriptions
        int descY = height / 2 - 48;
        int descColor = 0xAAAAAA;
        
        String quickStartDesc = "Start playing immediately with basic AI features";
        context.drawText(textRenderer, quickStartDesc,
            centerX - textRenderer.getWidth(quickStartDesc) / 2, descY, descColor, true);
            
        String advancedDesc = "Configure advanced AI settings and models";
        context.drawText(textRenderer, advancedDesc,
            centerX - textRenderer.getWidth(advancedDesc) / 2, descY + 24, descColor, true);
            
        String skipDesc = "Play without AI-enhanced features";
        context.drawText(textRenderer, skipDesc,
            centerX - textRenderer.getWidth(skipDesc) / 2, descY + 48, descColor, true);

        // Draw background image
        int imageSize = 128;
        // Updated to use drawTexture for 1.21.4
        context.drawTexture(TEXTURE_LAYER, BACKGROUND, centerX - imageSize / 2, height / 3, 0.0F, 0.0F, imageSize, imageSize, imageSize, imageSize);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        if (!animationRenderer.isComplete()) {
            animationRenderer.tick();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
