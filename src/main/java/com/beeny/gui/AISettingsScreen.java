// File: src/main/java/com/beeny/gui/AISettingsScreen.java
package com.beeny.gui;

import com.beeny.config.VillagesConfig;
import com.beeny.ai.ModelType; // Assuming ModelType enum exists
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Screen for configuring AI settings in Villages Reborn mod.
 * Allows users to select AI provider, set API keys, and adjust model parameters.
 */
public class AISettingsScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final Screen parent;
    private final VillagesConfig config;
    private final VillagesConfig.LLMSettings llmSettings;

    private TextFieldWidget apiKeyField;
    private TextFieldWidget endpointField;
    private CyclingButtonWidget<String> providerButton;
    private CyclingButtonWidget<String> modelButton;
    private TextFieldWidget contextLengthField;
    private TextFieldWidget temperatureField;
    private CyclingButtonWidget<Boolean> localModelButton;
    private CyclingButtonWidget<Boolean> enableAdvancedConversationsButton;

    public AISettingsScreen(Screen parent) {
        super(Text.literal("AI Settings"));
        this.parent = parent;
        this.config = VillagesConfig.getInstance();
        this.llmSettings = config.getLLMSettings();
    }

    @Override
    protected void init() {
        int y = this.height / 4;
        int buttonWidth = 200;
        int labelWidth = 120;
        int fieldX = this.width / 2 - buttonWidth / 2 + labelWidth + 5;
        int fieldWidth = buttonWidth - labelWidth - 5;
        int spacing = 24;

        // --- AI Provider Settings ---

        // Provider Selection
        List<String> providers = Arrays.asList("openai", "anthropic", "deepseek", "gemini", "mistral", "azure", "cohere", "openrouter", "local");
        providerButton = CyclingButtonWidget.<String>builder(Text::literal)
            .values(providers)
            .initially(llmSettings.getProvider() != null ? llmSettings.getProvider().toLowerCase() : "deepseek")
            .tooltip(value -> Tooltip.of(Text.literal("Select the AI service provider.")))
            .build(fieldX - labelWidth - 5, y, buttonWidth, 20, Text.literal("AI Provider"));
        this.addDrawableChild(providerButton);
        y += spacing;

        // API Key
        apiKeyField = new TextFieldWidget(this.textRenderer, fieldX, y, fieldWidth, 20, Text.literal("API Key"));
        apiKeyField.setText(llmSettings.getApiKey() != null ? llmSettings.getApiKey() : "");
        apiKeyField.setMaxLength(128);
        this.addDrawableChild(apiKeyField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("API Key:"), button -> {})
            .dimensions(fieldX - labelWidth - 5, y, labelWidth, 20)
            .tooltip(Tooltip.of(Text.literal("The API key for your selected AI provider")))
            .build());
        y += spacing;

        // Endpoint URL
        endpointField = new TextFieldWidget(this.textRenderer, fieldX, y, fieldWidth, 20, Text.literal("API Endpoint"));
        endpointField.setText(llmSettings.getEndpoint() != null ? llmSettings.getEndpoint() : "");
        endpointField.setMaxLength(256);
        this.addDrawableChild(endpointField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Endpoint:"), button -> {})
            .dimensions(fieldX - labelWidth - 5, y, labelWidth, 20)
            .tooltip(Tooltip.of(Text.literal("The endpoint URL for the AI API (leave blank for default)")))
            .build());
        y += spacing;

        // Model Selection
        List<String> modelIds = Arrays.asList(ModelType.getAllModelIds());
        modelButton = CyclingButtonWidget.<String>builder(Text::literal)
            .values(modelIds)
            .initially(llmSettings.getModelType() != null ? llmSettings.getModelType() : ModelType.DEEPSEEK_CODER.getId())
            .tooltip(value -> Tooltip.of(Text.literal("Select the specific AI model to use")))
            .build(fieldX - labelWidth - 5, y, buttonWidth, 20, Text.literal("Model"));
        this.addDrawableChild(modelButton);
        y += spacing;

        // Context Length
        contextLengthField = new TextFieldWidget(this.textRenderer, fieldX, y, fieldWidth, 20, Text.literal("Context Length"));
        contextLengthField.setText(String.valueOf(llmSettings.getContextLength()));
        contextLengthField.setMaxLength(5);
        this.addDrawableChild(contextLengthField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Context Length:"), button -> {})
            .dimensions(fieldX - labelWidth - 5, y, labelWidth, 20)
            .tooltip(Tooltip.of(Text.literal("Maximum tokens in conversation history (2048-8192)")))
            .build());
        y += spacing;

        // Temperature
        temperatureField = new TextFieldWidget(this.textRenderer, fieldX, y, fieldWidth, 20, Text.literal("Temperature"));
        temperatureField.setText(String.format("%.2f", llmSettings.getTemperature()));
        temperatureField.setMaxLength(4);
        this.addDrawableChild(temperatureField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Temperature:"), button -> {})
            .dimensions(fieldX - labelWidth - 5, y, labelWidth, 20)
            .tooltip(Tooltip.of(Text.literal("Controls randomness in responses (0.0-1.0)")))
            .build());
        y += spacing;

        // Use Local Model Toggle
        localModelButton = CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(llmSettings.isLocalModel())
            .tooltip(value -> Tooltip.of(Text.literal("Use a locally downloaded model instead of an API")))
            .build(fieldX - labelWidth - 5, y, buttonWidth, 20, Text.literal("Use Local Model"));
        this.addDrawableChild(localModelButton);
        y += spacing;
        
        // Enable Advanced Conversations
        enableAdvancedConversationsButton = CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(llmSettings.isAdvancedConversationsEnabled())
            .tooltip(value -> Tooltip.of(Text.literal("Enable more complex conversations with villagers")))
            .build(fieldX - labelWidth - 5, y, buttonWidth, 20, Text.literal("Advanced Conversations"));
        this.addDrawableChild(enableAdvancedConversationsButton);
        y += spacing;

        // Save and Back buttons
        int bottomButtonY = this.height - 40;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
            .dimensions(this.width / 2 - 102, bottomButtonY, 100, 20)
            .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.client.setScreen(parent))
            .dimensions(this.width / 2 + 2, bottomButtonY, 100, 20)
            .build());
    }

    private void saveAndClose() {
        llmSettings.setProvider(providerButton.getValue());
        llmSettings.setApiKey(apiKeyField.getText());
        llmSettings.setEndpoint(endpointField.getText());
        llmSettings.setModelType(modelButton.getValue());
        llmSettings.setLocalModel(localModelButton.getValue());
        llmSettings.setAdvancedConversationsEnabled(enableAdvancedConversationsButton.getValue());

        try {
            llmSettings.setContextLength(Integer.parseInt(contextLengthField.getText()));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid context length value: {}", contextLengthField.getText());
            // Keep default or previous value if parsing fails
        }
        
        try {
            llmSettings.setTemperature(Float.parseFloat(temperatureField.getText()));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid temperature value: {}", temperatureField.getText());
            // Keep default or previous value if parsing fails
        }

        config.save();
        // If needed, we could re-initialize the LLM service here
        this.client.setScreen(parent);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw description
        String description = "Configure AI settings for village interactions";
        context.drawCenteredTextWithShadow(
            this.textRenderer, 
            description,
            this.width / 2, 
            35, 
            0xAAAAAA
        );
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        // Don't save automatically on ESC, use the Save button
        this.client.setScreen(parent);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}