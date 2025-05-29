package com.beeny.villagesreborn.platform.fabric.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.font.TextRenderer;

public interface WizardStep {
    interface StepContext {
        TextRenderer getTextRenderer();
        int getWidth();
        int getHeight();
        void addDrawableChild(ClickableWidget widget);
    }
    
    void init(StepContext context);
    void render(DrawContext context, int mouseX, int mouseY, float delta);
    boolean isValid();
    default void saveConfiguration() {}
}