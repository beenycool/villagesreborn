package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.platform.fabric.gui.steps.*;

public class DefaultStepFactory implements StepFactory {
    public final HardwareInfo hardwareInfo;
    public final LLMProviderManager llmManager;
    public final FirstTimeSetupConfig setupConfig;

    public DefaultStepFactory(HardwareInfo hardwareInfo, LLMProviderManager llmManager,
                             FirstTimeSetupConfig setupConfig) {
        this.hardwareInfo = hardwareInfo;
        this.llmManager = llmManager;
        this.setupConfig = setupConfig;
    }

    @Override
    public WizardStep createWelcomeStep() {
        return new WelcomeStep();
    }

    @Override
    public WizardStep createHardwareStep(HardwareInfo hardwareInfo) {
        return new HardwareStep(hardwareInfo);
    }

    @Override
    public WizardStep createProviderStep(LLMProviderManager llmManager) {
        return new ProviderStep(llmManager);
    }

    @Override
    public WizardStep createModelStep(LLMProviderManager llmManager) {
        return new ModelStep(llmManager);
    }

    @Override
    public WizardStep createSummaryStep(FirstTimeSetupConfig setupConfig) {
        return new SummaryStep(setupConfig);
    }
}