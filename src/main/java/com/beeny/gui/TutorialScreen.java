package com.beeny.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TutorialScreen extends Screen {
    private int currentStep = 0;
    private final String[] steps = {
        "Step 1: Talk to a villager.",
        "Step 2: Trigger an event.",
        "Step 3: Complete a task.",
        "Step 4: Explore the village.",
        "Step 5: Enjoy the game!"
    };

    public TutorialScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Next"), button -> nextStep())
            .dimensions(this.width / 2 - 100, this.height - 40, 200, 20)
            .build());
    }

    private void nextStep() {
        if (currentStep < steps.length - 1) {
            currentStep++;
        } else {
            this.client.setScreen(null);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        
        // Draw title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, 
            this.width / 2, 20, 0xFFFFFF);
        
        // Draw current step
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(steps[currentStep]), 
            this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        
        // Draw progress dots
        int dotSpacing = 20;
        int totalWidth = (steps.length - 1) * dotSpacing;
        int startX = this.width / 2 - totalWidth / 2;
        
        for (int i = 0; i < steps.length; i++) {
            int color = i <= currentStep ? 0x00FF00 : 0x808080;
            context.fill(startX + i * dotSpacing - 2, this.height / 2 + 20, 
                startX + i * dotSpacing + 2, this.height / 2 + 24, color);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
