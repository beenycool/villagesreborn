package com.beeny.gui;

import com.beeny.setup.LLMConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import java.util.List;

public class SetupScreen extends Screen {
    private final LLMConfig llmConfig;
    private TextFieldWidget apiKeyField;
    private TextFieldWidget endpointField;
    private CyclingButtonWidget<String> modelTypeButton;
    private CyclingButtonWidget<String> providerButton;
    private SliderWidget temperatureSlider;
    private SliderWidget contextLengthSlider;

    private static final List<String> MODEL_TYPES = List.of("gpt-3.5-turbo", "gpt-4", "llama2", "claude-v2");
    private static final List<String> PROVIDERS = List.of("openai", "anthropic", "local");

    public SetupScreen(LLMConfig llmConfig) {
        super(Text.literal("Villages Reborn Setup"));
        this.llmConfig = llmConfig;
    }

    @Override
    protected void init() {
        super.init();
        int leftX = width / 2 - 100;
        int fieldWidth = 200;
        int fieldHeight = 20;
        int padding = 24;
        int currentY = 50;

        // API Key
        apiKeyField = new TextFieldWidget(textRenderer, leftX, currentY, fieldWidth, fieldHeight, Text.empty());
        apiKeyField.setText(llmConfig.getApiKey());
        apiKeyField.setMaxLength(100);
        addDrawableChild(apiKeyField);
        currentY += padding;

        // Endpoint
        endpointField = new TextFieldWidget(textRenderer, leftX, currentY, fieldWidth, fieldHeight, Text.empty());
        endpointField.setText(llmConfig.getEndpoint());
        endpointField.setMaxLength(100);
        addDrawableChild(endpointField);
        currentY += padding;

        // Model Type
        modelTypeButton = CyclingButtonWidget.<String>builder(value -> Text.literal("Model: " + value))
            .values(MODEL_TYPES)
            .initially(MODEL_TYPES.contains(llmConfig.getModelType()) ? llmConfig.getModelType() : MODEL_TYPES.get(0))
            .build(leftX, currentY, fieldWidth, fieldHeight, Text.literal("Model Type"));
        addDrawableChild(modelTypeButton);
        currentY += padding;

        // Provider
        providerButton = CyclingButtonWidget.<String>builder(value -> Text.literal("Provider: " + value))
            .values(PROVIDERS)
            .initially(PROVIDERS.contains(llmConfig.getProvider()) ? llmConfig.getProvider() : PROVIDERS.get(0))
            .build(leftX, currentY, fieldWidth, fieldHeight, Text.literal("Provider"));
        addDrawableChild(providerButton);
        currentY += padding;

        // Temperature Slider
        temperatureSlider = new SliderWidget(leftX, currentY, fieldWidth, fieldHeight, 
            Text.literal("Temperature: " + String.format("%.1f", llmConfig.getTemperature())), 
            llmConfig.getTemperature()) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal("Temperature: " + String.format("%.1f", value)));
            }

            @Override
            protected void applyValue() {
                llmConfig.setTemperature(value);
            }
        };
        addDrawableChild(temperatureSlider);
        currentY += padding;

        // Context Length Slider
        int minContext = 1024;
        int maxContext = 8192;
        double normalizedContextLength = (double)(llmConfig.getContextLength() - minContext) / (maxContext - minContext);
        contextLengthSlider = new SliderWidget(leftX, currentY, fieldWidth, fieldHeight,
            Text.literal("Context Length: " + llmConfig.getContextLength()),
            normalizedContextLength) {
            @Override
            protected void updateMessage() {
                int length = minContext + (int)(value * (maxContext - minContext));
                setMessage(Text.literal("Context Length: " + length));
            }

            @Override
            protected void applyValue() {
                int length = minContext + (int)(value * (maxContext - minContext));
                llmConfig.setContextLength(length);
            }
        };
        addDrawableChild(contextLengthSlider);
        currentY += padding;

        // Save & Done Button
        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            saveSettings();
            this.close();
        }).dimensions(leftX, this.height - 28, fieldWidth, fieldHeight).build();
        addDrawableChild(doneButton);
    }

    private void saveSettings() {
        llmConfig.setApiKey(apiKeyField.getText());
        llmConfig.setEndpoint(endpointField.getText());
        llmConfig.setModelType(modelTypeButton.getValue());
        llmConfig.setProvider(providerButton.getValue());
        llmConfig.setSetupComplete(true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw labels
        int labelX = width / 2 - 100;
        context.drawTextWithShadow(textRenderer, Text.literal("API Key:"), labelX, 40, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Endpoint:"), labelX, 84, 0xFFFFFF);
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
