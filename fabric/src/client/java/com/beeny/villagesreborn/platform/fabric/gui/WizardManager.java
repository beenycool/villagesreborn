package com.beeny.villagesreborn.platform.fabric.gui;

import java.util.ArrayList;
import java.util.List;

public class WizardManager {
    private final List<WizardStep> steps = new ArrayList<>(5); // Pre-size for known step count
    private int currentStepIndex = 0;
    private final StepFactory stepFactory;
    private boolean stepsInitialized = false;

    public WizardManager(StepFactory stepFactory) {
        this.stepFactory = stepFactory;
        // Lazy initialization - don't create steps until needed
    }

    private void ensureStepsInitialized() {
        if (!stepsInitialized) {
            initializeSteps();
            stepsInitialized = true;
        }
    }

    private void initializeSteps() {
        steps.add(stepFactory.createWelcomeStep());
        steps.add(stepFactory.createHardwareStep(((DefaultStepFactory)stepFactory).hardwareInfo));
        steps.add(stepFactory.createProviderStep(((DefaultStepFactory)stepFactory).llmManager));
        steps.add(stepFactory.createModelStep(((DefaultStepFactory)stepFactory).llmManager));
        steps.add(stepFactory.createSummaryStep(((DefaultStepFactory)stepFactory).setupConfig));
    }

    public WizardStep getCurrentStep() {
        ensureStepsInitialized();
        return steps.get(currentStepIndex);
    }

    public List<WizardStep> getSteps() {
        ensureStepsInitialized();
        return steps;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public int getTotalSteps() {
        return 5; // Return constant instead of computing steps.size()
    }

    public boolean hasNext() {
        return currentStepIndex < 4; // Use constant instead of steps.size() - 1
    }

    public boolean hasPrevious() {
        return currentStepIndex > 0;
    }

    public boolean isLastStep() {
        return currentStepIndex == 4; // Use constant instead of steps.size() - 1
    }

    public void nextStep() {
        if (hasNext()) {
            currentStepIndex++;
        }
    }

    public void previousStep() {
        if (hasPrevious()) {
            currentStepIndex--;
        }
    }

    public void setCurrentStepIndex(int step) {
        this.currentStepIndex = Math.max(0, Math.min(4, step)); // Use constant
    }
}