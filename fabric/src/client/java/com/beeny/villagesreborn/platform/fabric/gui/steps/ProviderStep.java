package com.beeny.villagesreborn.platform.fabric.gui.steps;

import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.core.util.InputValidator;
import com.beeny.villagesreborn.platform.fabric.gui.WizardStep;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

public class ProviderStep implements WizardStep {
    private final LLMProviderManager llmManager;
    private WizardStep.StepContext context;
    private CyclingButtonWidget<LLMProvider> providerButton;
    private LLMProvider selectedProvider = LLMProvider.OPENAI;

    public ProviderStep(LLMProviderManager llmManager) {
        this.llmManager = llmManager;
    }

    @Override
    public void init(WizardStep.StepContext context) {
        this.context = context;
        int centerX = context.getWidth() / 2;
        int currentY = context.getHeight() / 4;
        
        TextWidget titleText = new TextWidget(
            centerX - 100, currentY, 200, 20,
            Text.literal("Choose LLM Provider").formatted(Formatting.YELLOW, Formatting.BOLD),
            context.getTextRenderer()
        );
        context.addDrawableChild(titleText);
        
        currentY += 40;
        
        providerButton = CyclingButtonWidget.<LLMProvider>builder(provider -> Text.literal(provider.getDisplayName()))
            .values(Arrays.asList(LLMProvider.values()))
            .initially(selectedProvider)
            .build(centerX - 100, currentY, 200, 20, Text.literal("Provider"), (button, provider) -> {
                selectedProvider = provider;
            });
        context.addDrawableChild(providerButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render provider selection status
        if (this.context != null) {
            int centerX = this.context.getWidth() / 2;
            int statusY = this.context.getHeight() / 2 + 60;
            
            // Show selected provider info
            if (selectedProvider != null) {
                String statusText = "Selected: " + selectedProvider.getDisplayName();
                int textWidth = this.context.getTextRenderer().getWidth(statusText);
                context.drawText(this.context.getTextRenderer(), statusText, 
                    centerX - textWidth / 2, statusY, 0x00AA00, false);
            }
            
            // Show step progress
            int bottomY = this.context.getHeight() - 30;
            String progressText = "Step 2 of 5";
            int textWidth = this.context.getTextRenderer().getWidth(progressText);
            context.drawText(this.context.getTextRenderer(), progressText, 
                centerX - textWidth / 2, bottomY, 0xAAAAAA, false);
        }
    }

    @Override
    public boolean isValid() {
        if (selectedProvider == null) {
            return false;
        }
        
        // Validate provider name
        InputValidator.ValidationResult result = InputValidator.validateProviderName(selectedProvider.getDisplayName());
        return result.isValid();
    }

    public LLMProvider getSelectedProvider() {
        return selectedProvider;
    }
    
    public void setSelectedProvider(String providerName) {
        for (LLMProvider provider : LLMProvider.values()) {
            if (provider.getDisplayName().equals(providerName) || provider.name().equals(providerName)) {
                this.selectedProvider = provider;
                if (providerButton != null) {
                    providerButton.setValue(provider);
                }
                break;
            }
        }
    }
    
    @Override
    public void saveConfiguration() {
        // Save the selected provider
        if (selectedProvider != null) {
            // This would typically save to a configuration file or shared wizard state
            // For now, we just validate that the provider is properly selected
            // The actual saving would be handled by the wizard coordinator
        }
    }
}