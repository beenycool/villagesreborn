package com.beeny.villagesreborn.platform.fabric.gui.steps;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.platform.fabric.gui.WizardStep;
import net.minecraft.client.gui.DrawContext;

public class SummaryStep implements WizardStep {
    private final FirstTimeSetupConfig setupConfig;

    public SummaryStep(FirstTimeSetupConfig setupConfig) {
        this.setupConfig = setupConfig;
    }

    @Override
    public void init(WizardStep.StepContext context) {}
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {}
    
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