package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;

public interface StepFactory {
    WizardStep createWelcomeStep();
    WizardStep createHardwareStep(HardwareInfo hardwareInfo);
    WizardStep createProviderStep(LLMProviderManager llmManager);
    WizardStep createModelStep(LLMProviderManager llmManager);
    WizardStep createSummaryStep(FirstTimeSetupConfig setupConfig);
}