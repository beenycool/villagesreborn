package com.beeny.villagesreborn.platform.fabric.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.font.TextRenderer;

public class StepRenderer {
    private final TextRenderer textRenderer;
    private final int screenWidth;
    private final int screenHeight;

    public StepRenderer(TextRenderer textRenderer, int screenWidth, int screenHeight) {
        this.textRenderer = textRenderer;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void renderStepIndicator(DrawContext context, int currentStep, int totalSteps) {
        int centerX = screenWidth / 2;
        int stepY = 20;
        int stepSpacing = 20;
        
        for (int i = 0; i < totalSteps; i++) {
            int x = centerX - ((totalSteps - 1) * stepSpacing / 2) + (i * stepSpacing);
            int color = i == currentStep ? 0xFFFFFF : (i < currentStep ? 0x00FF00 : 0x666666);
            context.fill(x - 5, stepY, x + 5, stepY + 2, color);
        }
    }

    public ButtonWidget createNavigationButton(String text, Runnable action, int position) {
        int buttonWidth = 80;
        int buttonHeight = 20;
        int startY = screenHeight - 30;
        int centerX = screenWidth / 2;
        int buttonSpacing = 10;
        
        int x = position == 0 ? centerX - buttonWidth - buttonSpacing : centerX + buttonSpacing;
        return ButtonWidget.builder(Text.literal(text), button -> action.run())
            .position(x, startY)
            .size(buttonWidth, buttonHeight)
            .build();
    }

    public void renderTitle(DrawContext context, String title) {
        int titleWidth = textRenderer.getWidth(title);
        int x = (screenWidth - titleWidth) / 2;
        context.drawText(textRenderer, title, x, 5, 0xFFFFFF, false);
    }
}