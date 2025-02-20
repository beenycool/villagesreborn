package com.beeny.gui;

import com.beeny.setup.LLMConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class SetupScreen extends Screen {
    private final LLMConfig llmConfig;

    public SetupScreen(LLMConfig llmConfig) {
        super(Text.literal("Villages Reborn Setup"));
        this.llmConfig = llmConfig;
    }

    @Override
    protected void init() {
        super.init();
        // Add setup screen elements here
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Done"),
            button -> this.close()
        ).dimensions(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
