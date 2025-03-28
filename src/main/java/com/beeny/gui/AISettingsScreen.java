// File: src/main/java/com/beeny/gui/AISettingsScreen.java
package com.beeny.gui;

import com.beeny.config.VillagesConfig;
import com.beeny.ai.ModelType; // Assuming ModelType enum exists
import com.beeny.ai.LLMService;
import com.beeny.ai.LLMErrorHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private ButtonWidget testConnectionButton;
    private Text connectionStatusText = Text.empty();
    private boolean isTestingConnection = false;

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

        // Test Connection Button
        testConnectionButton = ButtonWidget.builder(Text.literal("Test Connection"), button -> testConnection())
            .dimensions(this.width / 2 - 100, y, 200, 20)
            .tooltip(Tooltip.of(Text.literal("Test your current API settings")))
            .build();
        this.addDrawableChild(testConnectionButton);
        y += spacing + 5; // Add extra spacing after test button

        // Save and Back buttons
        int bottomButtonY = this.height - 40;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
            .dimensions(this.width / 2 - 102, bottomButtonY, 100, 20)
            .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.client.setScreen(parent))
            .dimensions(this.width / 2 + 2, bottomButtonY, 100, 20)
            .build());
    }

    private void testConnection() {
        if (isTestingConnection) {
            return; // Prevent multiple simultaneous tests
        }

        // Save current settings to temp
        String provider = providerButton.getValue();
        String apiKey = apiKeyField.getText();
        String endpoint = endpointField.getText();
        String model = modelButton.getValue();
        
        // Validate input
        if (apiKey.isEmpty() && !provider.equals("local")) {
            connectionStatusText = Text.literal("API Key is required").formatted(Formatting.RED);
            return;
        }

        // Update UI to show testing state
        connectionStatusText = Text.literal("Testing connection...").formatted(Formatting.YELLOW);
        isTestingConnection = true;
        testConnectionButton.active = false;

        // Apply temporary settings for test
        llmSettings.setProvider(provider);
        llmSettings.setApiKey(apiKey);
        llmSettings.setEndpoint(endpoint);
        llmSettings.setModelType(model);
        
        // Need to save config to apply these settings
        config.save();
        
        // Initialize LLM with new settings
        LLMService llmService = LLMService.getInstance();
        
        // Test connection
        CompletableFuture<Boolean> testFuture = llmService.testConnection();
        
        testFuture.thenAccept(success -> {
            if (success) {
                connectionStatusText = Text.literal("Connection successful!").formatted(Formatting.GREEN);
            } else {
                connectionStatusText = Text.literal("Connection failed. Check settings and try again.").formatted(Formatting.RED);
            }
            isTestingConnection = false;
            testConnectionButton.active = true;
        }).exceptionally(e -> {
            connectionStatusText = Text.literal("Error: " + e.getMessage()).formatted(Formatting.RED);
            isTestingConnection = false;
            testConnectionButton.active = true;
            return null;
        });
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
        
        // Reinitialize LLM service with new settings
        LLMService.getInstance().setProvider(llmSettings.getProvider());
        
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
        
        // Draw connection status if not empty
        if (!connectionStatusText.getString().isEmpty()) {
            context.drawCenteredText(
                this.textRenderer,
                connectionStatusText,
                this.width / 2,
                this.height / 4 + 24 * 8 + 5, // Position below test button
                0xFFFFFF
            );
        }
        
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