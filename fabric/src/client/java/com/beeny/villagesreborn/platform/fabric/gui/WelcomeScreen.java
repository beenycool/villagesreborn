package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Refactored setup wizard using WizardManager and StepRenderer
 */
public class WelcomeScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeScreen.class);
    
    private final WizardManager wizardManager;
    private StepRenderer stepRenderer;  // Remove final to allow late initialization
    private final ConfigManager configManager;
    private final HardwareInfo detectedHardware;
    
    // Navigation UI
    private ButtonWidget nextButton;
    private ButtonWidget backButton;
    
    // Render state caching
    private int lastRenderedStep = -1;
    private boolean needsRedraw = true;
    
    // Performance monitoring
    private final PerformanceMonitor performanceMonitor = new PerformanceMonitor("WelcomeScreen");
    
    public WelcomeScreen(HardwareInfoManager hardwareManager, LLMProviderManager llmManager,
                        FirstTimeSetupConfig setupConfig) {
        super(Text.literal("Villages Reborn - Setup Wizard"));
        this.detectedHardware = hardwareManager.getHardwareInfo();
        
        // Create components with dependency injection
        StepFactory stepFactory = new DefaultStepFactory(detectedHardware, llmManager, setupConfig);
        this.wizardManager = new WizardManager(stepFactory);
        this.configManager = new ConfigManager(setupConfig);
        
        LOGGER.info("Setup wizard initialized with hardware: {}", detectedHardware);
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();
        // Initialize StepRenderer here after Minecraft has set up textRenderer and dimensions
        this.stepRenderer = new StepRenderer(textRenderer, width, height);
        getCurrentStep().init(stepContext);
        addNavigationButtons();
    }

    private final WizardStep.StepContext stepContext = new WizardStep.StepContext() {
        @Override
        public TextRenderer getTextRenderer() {
            return textRenderer;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void addDrawableChild(ClickableWidget widget) {
            WelcomeScreen.this.addDrawableChild(widget);
        }
    };

    private WizardStep getCurrentStep() {
        return wizardManager.getCurrentStep();
    }

    private void addNavigationButtons() {
        int centerX = this.width / 2;
        int startY = this.height - 30;
        
        // Back button
        this.backButton = stepRenderer.createNavigationButton("Back", 
            this::previousStep, 0);
        this.backButton.active = wizardManager.hasPrevious();
        this.addDrawableChild(backButton);

        // Next/Finish button
        String buttonText = wizardManager.isLastStep() ? "Finish" : "Next";
        this.nextButton = stepRenderer.createNavigationButton(buttonText, 
            this::handleNextAction, 1);
        this.nextButton.active = getCurrentStep().isValid();
        this.addDrawableChild(nextButton);
    }

    private void handleNextAction() {
        if (wizardManager.isLastStep()) {
            saveConfiguration();
        } else {
            wizardManager.nextStep();
            needsRedraw = true; // Mark for redraw
            init();
        }
    }

    private void previousStep() {
        if (wizardManager.hasPrevious()) {
            wizardManager.previousStep();
            needsRedraw = true; // Mark for redraw
            init();
        }
    }

    private void saveConfiguration() {
        try {
            configManager.saveAllSteps(wizardManager.getSteps());
            LOGGER.info("Setup completed successfully");
            closeScreen();
        } catch (Exception e) {
            LOGGER.error("Failed to save configuration", e);
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("Configuration save failed!"), false);
            }
        }
    }
    
    private void closeScreen() {
        MinecraftClient.getInstance().setScreen(null);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        performanceMonitor.startFrame();
        
        // Only redraw background if necessary
        if (needsRedraw) {
            context.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);
            needsRedraw = false;
        }
        
        super.render(context, mouseX, mouseY, delta);
        
        // Cache step indicator and title rendering
        int currentStep = wizardManager.getCurrentStepIndex();
        if (lastRenderedStep != currentStep) {
            stepRenderer.renderStepIndicator(context, currentStep, wizardManager.getTotalSteps());
            stepRenderer.renderTitle(context, "Villages Reborn - Setup Wizard");
            lastRenderedStep = currentStep;
        }
        
        getCurrentStep().render(context, mouseX, mouseY, delta);
        
        performanceMonitor.endFrame();
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
    
    // Test methods
    public int getCurrentStepIndex() {
        return wizardManager.getCurrentStepIndex();
    }
    
    public void setCurrentStepIndex(int step) {
        wizardManager.setCurrentStepIndex(step);
        init();
    }
    
    // Methods expected by WelcomeScreenSimpleTest
    public void setSelectedProvider(String provider) {
        // Delegate to appropriate step
        for (WizardStep step : wizardManager.getSteps()) {
            if (step instanceof com.beeny.villagesreborn.platform.fabric.gui.steps.ProviderStep) {
                ((com.beeny.villagesreborn.platform.fabric.gui.steps.ProviderStep) step).setSelectedProvider(provider);
                break;
            }
        }
    }
    
    public void setSelectedModel(String model) {
        // Delegate to appropriate step
        for (WizardStep step : wizardManager.getSteps()) {
            if (step instanceof com.beeny.villagesreborn.platform.fabric.gui.steps.ModelStep) {
                ((com.beeny.villagesreborn.platform.fabric.gui.steps.ModelStep) step).setSelectedModel(model);
                break;
            }
        }
    }
    
    public String getSelectedProvider() {
        // Delegate to appropriate step
        for (WizardStep step : wizardManager.getSteps()) {
            if (step instanceof com.beeny.villagesreborn.platform.fabric.gui.steps.ProviderStep) {
                return ((com.beeny.villagesreborn.platform.fabric.gui.steps.ProviderStep) step).getSelectedProvider().toString();
            }
        }
        return null;
    }
    
    public String getSelectedModel() {
        // Delegate to appropriate step
        for (WizardStep step : wizardManager.getSteps()) {
            if (step instanceof com.beeny.villagesreborn.platform.fabric.gui.steps.ModelStep) {
                return ((com.beeny.villagesreborn.platform.fabric.gui.steps.ModelStep) step).getSelectedModel();
            }
        }
        return null;
    }
    
    public java.util.List<String> getValidationErrors() {
        java.util.List<String> errors = new java.util.ArrayList<>();
        for (WizardStep step : wizardManager.getSteps()) {
            if (!step.isValid()) {
                errors.add("Step " + step.getClass().getSimpleName() + " is invalid");
            }
        }
        return errors;
    }
}