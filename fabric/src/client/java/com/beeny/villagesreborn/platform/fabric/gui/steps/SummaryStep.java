package com.beeny.villagesreborn.platform.fabric.gui.steps;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.platform.fabric.gui.WizardStep;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SummaryStep implements WizardStep {
    private final FirstTimeSetupConfig setupConfig;
    private WizardStep.StepContext context;

    public SummaryStep(FirstTimeSetupConfig setupConfig) {
        this.setupConfig = setupConfig;
    }

    @Override
    public void init(WizardStep.StepContext context) {
        this.context = context;
        int centerX = context.getWidth() / 2;
        int currentY = context.getHeight() / 4;
        int spacing = 20;
        
        // Title
        TextWidget titleText = new TextWidget(
            centerX - 150, currentY, 300, 20,
            Text.literal("Configuration Summary").formatted(Formatting.GOLD, Formatting.BOLD),
            context.getTextRenderer()
        );
        context.addDrawableChild(titleText);
        currentY += spacing + 10;
        
        // Configuration details
        TextWidget providerText = new TextWidget(
            centerX - 150, currentY, 300, 15,
            Text.literal("Provider: ").formatted(Formatting.WHITE)
                .append(Text.literal("OpenAI").formatted(Formatting.GREEN)),
            context.getTextRenderer()
        );
        context.addDrawableChild(providerText);
        currentY += spacing;
        
        TextWidget modelText = new TextWidget(
            centerX - 150, currentY, 300, 15,
            Text.literal("Model: ").formatted(Formatting.WHITE)
                .append(Text.literal("gpt-3.5-turbo").formatted(Formatting.BLUE)),
            context.getTextRenderer()
        );
        context.addDrawableChild(modelText);
        currentY += spacing;
        
        TextWidget statusText = new TextWidget(
            centerX - 150, currentY, 300, 15,
            Text.literal("Status: ").formatted(Formatting.WHITE)
                .append(Text.literal("Ready to configure").formatted(Formatting.YELLOW)),
            context.getTextRenderer()
        );
        context.addDrawableChild(statusText);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render completion status
        if (this.context != null) {
            int centerX = this.context.getWidth() / 2;
            int bottomY = this.context.getHeight() - 50;
            
            // Show completion message
            String completionText = "Click 'Finish' to complete setup";
            int textWidth = this.context.getTextRenderer().getWidth(completionText);
            context.drawText(this.context.getTextRenderer(), completionText, 
                centerX - textWidth / 2, bottomY, 0x00FF00, false);
                
            // Show step progress
            bottomY += 20;
            String progressText = "Step 5 of 5";
            textWidth = this.context.getTextRenderer().getWidth(progressText);
            context.drawText(this.context.getTextRenderer(), progressText, 
                centerX - textWidth / 2, bottomY, 0xAAAAAA, false);
        }
    }
    
    @Override
    public boolean isValid() {
        return true;
    }
    
    @Override
    public void saveConfiguration() {
        // These would come from shared wizard state in a real implementation
        setupConfig.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
    }
}