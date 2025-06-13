package com.beeny.villagesreborn.platform.fabric.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.font.TextRenderer;

public class StepRenderer {
    private final TextRenderer textRenderer;
    private final int screenWidth;
    private final int screenHeight;
    
    // Cached calculations
    private final int centerX;
    private final int stepY = 20;
    private final int stepSpacing = 20;
    private final int startY;
    private final int buttonWidth = 80;
    private final int buttonHeight = 20;
    private final int buttonSpacing = 10;
    
    // Cache for step indicator positions
    private int[] stepPositions;
    private int lastTotalSteps = -1;

    public StepRenderer(TextRenderer textRenderer, int screenWidth, int screenHeight) {
        this.textRenderer = textRenderer;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // Pre-calculate commonly used values
        this.centerX = screenWidth / 2;
        this.startY = screenHeight - 30;
    }

    public void renderStepIndicator(DrawContext context, int currentStep, int totalSteps) {
        // Cache step positions if total steps changed
        if (lastTotalSteps != totalSteps) {
            stepPositions = new int[totalSteps];
            for (int i = 0; i < totalSteps; i++) {
                stepPositions[i] = centerX - ((totalSteps - 1) * stepSpacing / 2) + (i * stepSpacing);
            }
            lastTotalSteps = totalSteps;
        }
        
        // Render using cached positions
        for (int i = 0; i < totalSteps; i++) {
            int color = i == currentStep ? 0xFFFFFF : (i < currentStep ? 0x00FF00 : 0x666666);
            context.fill(stepPositions[i] - 5, stepY, stepPositions[i] + 5, stepY + 2, color);
        }
    }

    public ButtonWidget createNavigationButton(String text, Runnable action, int position) {
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