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
    private TextFieldWidget apiKeyField;
    private TextFieldWidget endpointField;

    public VillagesRebornSettingsScreen(Screen parent, GameOptions gameOptions, LLMConfig llmConfig) {
        super(Text.literal("Villages Reborn Settings"));
        this.parent = parent;
        this.gameOptions = gameOptions;
        this.llmConfig = llmConfig;
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
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveSettings())
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
        super.render(context, mouseX, mouseY, delta);
    }

    private void saveSettings() {
        llmConfig.setApiKey(apiKeyField.getText());
        llmConfig.setEndpoint(endpointField.getText());
        llmConfig.saveConfig();
        LOGGER.info("Settings saved: API Key={}, Endpoint={}", apiKeyField.getText(), endpointField.getText());
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