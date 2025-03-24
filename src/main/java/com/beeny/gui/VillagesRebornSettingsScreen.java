package com.beeny.gui;

import com.beeny.setup.LLMConfig;
import com.beeny.setup.ModelDownloader;
import com.beeny.setup.SystemSpecs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagesRebornSettingsScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final Screen parent;
    private final GameOptions gameOptions;
    private final LLMConfig llmConfig;
    private final SystemSpecs systemSpecs;
    private TextFieldWidget apiKeyField;
    private TextFieldWidget endpointField;
    private ButtonWidget adaptiveScalingButton;
    private ButtonWidget aiComplexityButton;

    public VillagesRebornSettingsScreen(Screen parent, GameOptions gameOptions, LLMConfig llmConfig) {
        super(Text.literal("Villages Reborn Settings"));
        this.parent = parent;
        this.gameOptions = gameOptions;
        this.llmConfig = llmConfig;
        this.systemSpecs = new SystemSpecs();
        this.systemSpecs.analyzeSystem();
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = 40;

        this.apiKeyField = new TextFieldWidget(this.textRenderer, centerX, startY, buttonWidth, buttonHeight, Text.literal("API Key"));
        this.apiKeyField.setText(llmConfig.getApiKey());
        this.apiKeyField.setMaxLength(100);
        this.addDrawableChild(this.apiKeyField);

        startY += 30;
        this.endpointField = new TextFieldWidget(this.textRenderer, centerX, startY, buttonWidth, buttonHeight, Text.literal("Endpoint"));
        this.endpointField.setText(llmConfig.getEndpoint());
        this.endpointField.setMaxLength(100);
        this.addDrawableChild(this.endpointField);

        startY += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Download Local AI Model"), button -> downloadLocalAIModel())
            .dimensions(centerX, startY, buttonWidth, buttonHeight)
            .build());

        startY += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("UI Settings"), button -> openUISettings())
            .dimensions(centerX, startY, buttonWidth, buttonHeight)
            .build());

        startY += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveSettings())
            .dimensions(centerX, startY, buttonWidth, buttonHeight)
            .build());

        startY += 30;
        this.adaptiveScalingButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Adaptive Scaling: " + (systemSpecs.isAdaptiveScalingEnabled() ? "ON" : "OFF")),
            button -> toggleAdaptiveScaling())
            .dimensions(centerX, startY, buttonWidth, buttonHeight)
            .build());

        startY += 30;
        this.aiComplexityButton = this.addDrawableChild(ButtonWidget.builder(
            Text.literal("AI Complexity: " + getComplexityLabel(systemSpecs.getAIComplexity())),
            button -> cycleAIComplexity())
            .dimensions(centerX, startY, buttonWidth, buttonHeight)
            .build());

        startY += 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.client.setScreen(parent))
            .dimensions(centerX, startY, buttonWidth, buttonHeight)
            .build());
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Update and display performance metrics
        systemSpecs.updatePerformanceMetrics();
        int centerX = this.width / 2 - 100; // Match button width positioning
        int metricsY = 250; // Position below other elements
        
        context.drawTextWithShadow(this.textRenderer, 
            String.format("CPU Usage: %.1f%%", systemSpecs.getCpuUsage() * 100), 
            centerX, metricsY, 0xFFFFFF);
        
        context.drawTextWithShadow(this.textRenderer,
            String.format("Memory Usage: %.1f%%", systemSpecs.getMemoryUsage() * 100),
            centerX, metricsY + 15, 0xFFFFFF);
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void saveSettings() {
        llmConfig.setApiKey(apiKeyField.getText());
        llmConfig.setEndpoint(endpointField.getText());
        llmConfig.saveConfig();
        LOGGER.info("Settings saved: API Key={}, Endpoint={}", apiKeyField.getText(), endpointField.getText());
        
        // Update system specs when saving
        systemSpecs.updatePerformanceMetrics();
    }

    private void toggleAdaptiveScaling() {
        boolean newState = !systemSpecs.isAdaptiveScalingEnabled();
        SystemSpecs.setAdaptiveScaling(newState);
        adaptiveScalingButton.setMessage(Text.literal("Adaptive Scaling: " + (newState ? "ON" : "OFF")));
    }

    private void cycleAIComplexity() {
        int currentLevel = systemSpecs.getAIComplexity();
        int newLevel = (currentLevel + 1) % 3;
        SystemSpecs.setAIComplexity(newLevel);
        aiComplexityButton.setMessage(Text.literal("AI Complexity: " + getComplexityLabel(newLevel)));
    }

    private String getComplexityLabel(int level) {
        return switch(level) {
            case 0 -> "Basic";
            case 1 -> "Moderate";
            case 2 -> "Full";
            default -> "Unknown";
        };
    }
    
    private void openUISettings() {
        this.client.setScreen(new VillageUISettingsScreen(this));
    }

    private void downloadLocalAIModel() {
        SystemSpecs specs = new SystemSpecs();
        specs.analyzeSystem();
        String modelName;
        switch (specs.getSystemTier()) {
            case HIGH -> modelName = "advanced_model.bin";
            case MEDIUM -> modelName = "standard_model.bin";
            default -> modelName = "basic_model.bin";
        }
        String expectedHash = "placeholder_hash";

        ModelDownloader.downloadModel(modelName, expectedHash, progress -> {
            LOGGER.info("Download progress: {}%", progress * 100);
        }).thenAccept(modelPath -> {
            llmConfig.setModelType(modelName);
            llmConfig.setProvider("local");
            llmConfig.saveConfig();
            this.client.setScreen(new OptionsScreen(this.parent, this.gameOptions));
            LOGGER.info("Local AI model downloaded and configured: {}", modelPath);
        }).exceptionally(throwable -> {
            LOGGER.error("Failed to download local AI model", throwable);
            return null;
        });
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
