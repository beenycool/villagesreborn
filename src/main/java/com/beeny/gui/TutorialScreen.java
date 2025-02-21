package com.beeny.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;

public class TutorialScreen extends Screen {
    private int currentStep = 0;
    private final String[] steps = {
        "Step 1: Talk to a villager.",
        "Step 2: Trigger an event.",
        "Step 3: Complete a task.",
        "Step 4: Explore the village.",
        "Step 5: Enjoy the game!"
    };

    protected TutorialScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height - 40, 200, 20, Text.of("Next"), button -> nextStep()));
    }

    private void nextStep() {
        if (currentStep < steps.length - 1) {
            currentStep++;
        } else {
            this.client.setScreen(null);
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        
        // Draw title
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw current step
        drawCenteredText(matrices, this.textRenderer, Text.literal(steps[currentStep]), 
            this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        
        // Draw progress dots
        int dotSpacing = 20;
        int totalWidth = (steps.length - 1) * dotSpacing;
        int startX = this.width / 2 - totalWidth / 2;
        
        for (int i = 0; i < steps.length; i++) {
            int color = i <= currentStep ? 0x00FF00 : 0x808080;
            fill(matrices, startX + i * dotSpacing - 2, this.height / 2 + 20, 
                startX + i * dotSpacing + 2, this.height / 2 + 24, color);
        }
        
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}