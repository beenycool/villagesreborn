package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.util.InputValidator;
import com.beeny.villagesreborn.platform.fabric.gui.WizardStep;
import com.beeny.villagesreborn.platform.fabric.gui.steps.ModelStep;
import com.beeny.villagesreborn.platform.fabric.gui.steps.ProviderStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    private final FirstTimeSetupConfig setupConfig;

    public ConfigManager(FirstTimeSetupConfig setupConfig) {
        this.setupConfig = setupConfig;
    }

    public void saveAllSteps(List<WizardStep> steps) {
        try {
            // Validate inputs before saving
            validateStepInputs(steps);
            
            for (WizardStep step : steps) {
                step.saveConfiguration();
            }
            setupConfig.save();
            LOGGER.info("Configuration saved successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to save configuration", e);
            throw new RuntimeException("Configuration save failed", e);
        }
    }
    
    private void validateStepInputs(List<WizardStep> steps) {
        for (WizardStep step : steps) {
            if (step instanceof ModelStep) {
                ModelStep modelStep = (ModelStep) step;
                String selectedModel = modelStep.getSelectedModel();
                InputValidator.ValidationResult result = InputValidator.validateModelName(selectedModel);
                if (!result.isValid()) {
                    throw new IllegalArgumentException("Invalid model name: " + result.getErrorMessage());
                }
            }
            // Add validation for other step types as needed
        }
        LOGGER.debug("All step inputs validated successfully");
    }
}