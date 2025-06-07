package com.beeny.villagesreborn.platform.fabric.gui.steps;

import com.beeny.villagesreborn.platform.fabric.gui.WizardStep;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WelcomeStep implements WizardStep {
    private WizardStep.StepContext context;

    @Override
    public void init(WizardStep.StepContext context) {
        this.context = context;
        int centerX = context.getWidth() / 2;
        int currentY = context.getHeight() / 4;
        
        TextWidget welcomeText = new TextWidget(
            centerX - 200, currentY, 400, 60,
            Text.literal("Welcome to Villages Reborn!\n\n")
                .append(Text.literal("This wizard will configure AI-powered village mechanics").formatted(Formatting.GRAY)),
            context.getTextRenderer()
        );
        context.addDrawableChild(welcomeText);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render additional welcome screen elements
        if (this.context != null) {
            int centerX = this.context.getWidth() / 2;
            int bottomY = this.context.getHeight() - 30;
            
            // Show progress indicator
            String progressText = "Step 1 of 5";
            int textWidth = this.context.getTextRenderer().getWidth(progressText);
            context.drawText(this.context.getTextRenderer(), progressText, 
                centerX - textWidth / 2, bottomY, 0xAAAAAA, false);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }
}