package com.beeny.gui;

import com.beeny.setup.LLMConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.RenderLayer;
import java.util.function.Function;

public class SetupScreen extends Screen {
    private final LLMConfig llmConfig;
    private final AnimationRenderer animationRenderer = new AnimationRenderer();
    private static final Identifier BACKGROUND = Identifier.of("villagesreborn", "textures/gui/anim_frame1.png");
    private static final Function<Identifier, RenderLayer> TEXTURE_LAYER = id -> RenderLayer.getGui();

    public SetupScreen(LLMConfig llmConfig) {
        super(Text.literal("Villages Reborn"));
        this.llmConfig = llmConfig;
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

        // "Yes, let's go!" button
        addDrawableChild(ButtonWidget.builder(Text.literal("Yes, let's go!"), button -> {
            client.setScreen(new ModelDownloadScreen(this, llmConfig));
        })
        .dimensions(centerX, centerY - 12, buttonWidth, buttonHeight)
        .build());

        // "Skip for now" button
        addDrawableChild(ButtonWidget.builder(Text.literal("Skip for now"), button -> {
            llmConfig.setSetupComplete(true);
            llmConfig.saveConfig();
            close();
        })
        .dimensions(centerX, centerY + 12, buttonWidth, buttonHeight)
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
        String subtitle = "Want smarter villagers?";
        int subtitleX = centerX - textRenderer.getWidth(subtitle) / 2;
        context.drawText(textRenderer, subtitle, subtitleX, height / 4 + 20, 0xFFFFFF, true);

        // Draw background image
        int imageSize = 128;
        context.drawGuiTexture(
            TEXTURE_LAYER,
            BACKGROUND,
            centerX - imageSize / 2,
            height / 3,
            imageSize,
            imageSize
        );

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
