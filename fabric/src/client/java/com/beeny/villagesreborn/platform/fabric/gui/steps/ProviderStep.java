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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {}

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
}