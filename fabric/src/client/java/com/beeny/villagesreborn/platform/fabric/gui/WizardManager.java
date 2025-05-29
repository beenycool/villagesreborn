package com.beeny.villagesreborn.platform.fabric.gui;

import java.util.ArrayList;
import java.util.List;

public class WizardManager {
    private final List<WizardStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;
    private final StepFactory stepFactory;

    public WizardManager(StepFactory stepFactory) {
        this.stepFactory = stepFactory;
        initializeSteps();
    }

    private void initializeSteps() {
        steps.add(stepFactory.createWelcomeStep());
        steps.add(stepFactory.createHardwareStep(((DefaultStepFactory)stepFactory).hardwareInfo));
        steps.add(stepFactory.createProviderStep(((DefaultStepFactory)stepFactory).llmManager));
        steps.add(stepFactory.createModelStep(((DefaultStepFactory)stepFactory).llmManager));
        steps.add(stepFactory.createSummaryStep(((DefaultStepFactory)stepFactory).setupConfig));
    }

    public WizardStep getCurrentStep() {
        return steps.get(currentStepIndex);
    }

    public List<WizardStep> getSteps() {
        return steps;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public int getTotalSteps() {
        return steps.size();
    }

    public boolean hasNext() {
        return currentStepIndex < steps.size() - 1;
    }

    public boolean hasPrevious() {
        return currentStepIndex > 0;
    }

    public boolean isLastStep() {
        return currentStepIndex == steps.size() - 1;
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
        this.currentStepIndex = Math.max(0, Math.min(steps.size() - 1, step));
    }
}