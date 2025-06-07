package com.beeny.villagesreborn.platform.fabric.gui.steps;

import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.core.util.InputValidator;
import com.beeny.villagesreborn.platform.fabric.gui.WizardStep;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

public class ModelStep implements WizardStep {
    private final LLMProviderManager llmManager;
    private WizardStep.StepContext context;
    private CyclingButtonWidget<String> modelButton;
    private String selectedModel = "gpt-3.5-turbo";

    public ModelStep(LLMProviderManager llmManager) {
        this.llmManager = llmManager;
    }

    @Override
    public void init(WizardStep.StepContext context) {
        this.context = context;
        int centerX = context.getWidth() / 2;
        int currentY = context.getHeight() / 4;
        
        TextWidget titleText = new TextWidget(
            centerX - 100, currentY, 200, 20,
            Text.literal("Choose AI Model").formatted(Formatting.BLUE, Formatting.BOLD),
            context.getTextRenderer()
        );
        context.addDrawableChild(titleText);
        
        currentY += 40;
        
        // Simplified model list for now
        var models = Arrays.asList("gpt-3.5-turbo", "gpt-4", "claude-3-sonnet");
        
        modelButton = CyclingButtonWidget.<String>builder(Text::literal)
            .values(models)
            .initially(selectedModel)
            .build(centerX - 100, currentY, 200, 20, Text.literal("Model"), (button, model) -> {
                selectedModel = InputValidator.sanitizeInput(model);
            });
        context.addDrawableChild(modelButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render model selection status
        if (this.context != null) {
            int centerX = this.context.getWidth() / 2;
            int statusY = this.context.getHeight() / 2 + 60;
            
            // Show selected model info
            if (selectedModel != null && !selectedModel.isEmpty()) {
                String statusText = "Selected: " + selectedModel;
                int textWidth = this.context.getTextRenderer().getWidth(statusText);
                context.drawText(this.context.getTextRenderer(), statusText, 
                    centerX - textWidth / 2, statusY, 0x0088FF, false);
            }
            
            // Show step progress
            int bottomY = this.context.getHeight() - 30;
            String progressText = "Step 3 of 5";
            int textWidth = this.context.getTextRenderer().getWidth(progressText);
            context.drawText(this.context.getTextRenderer(), progressText, 
                centerX - textWidth / 2, bottomY, 0xAAAAAA, false);
        }
    }

    @Override
    public boolean isValid() {
        if (selectedModel == null || selectedModel.isEmpty()) {
            return false;
        }
        
        InputValidator.ValidationResult result = InputValidator.validateModelName(selectedModel);
        return result.isValid();
    }

    public String getSelectedModel() {
        return selectedModel;
    }
    
    public void setSelectedModel(String model) {
        if (model != null) {
            this.selectedModel = InputValidator.sanitizeInput(model);
            if (modelButton != null) {
                modelButton.setValue(model);
            }
        }
    }
    
    @Override
    public void saveConfiguration() {
        // Save the selected model
        if (selectedModel != null && !selectedModel.isEmpty()) {
            // This would typically save to a configuration file or shared wizard state
            // For now, we just validate that the model is properly selected
            // The actual saving would be handled by the wizard coordinator
        }
    }
}