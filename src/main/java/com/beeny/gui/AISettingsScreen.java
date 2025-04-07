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
import java.util.ArrayList;
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
    private CyclingButtonWidget<Boolean> developerModeButton;
    private boolean devMode;
    private int statusTextY;
    // Fields to track previous button states for tick-based updates
    private Boolean previousDevModeState = null; // Keep only one declaration
    private String previousProviderState = null; // Keep only one declaration

    public AISettingsScreen(Screen parent) {
        super(Text.literal("AI Settings"));
        this.parent = parent;
        this.config = VillagesConfig.getInstance();
        this.llmSettings = config.getLLMSettings();
    }

    @Override
    protected void init() {
        // --- Developer Mode Toggle ---
        developerModeButton = CyclingButtonWidget.<Boolean>builder(value -> Text.literal("Dev Mode: " + (value ? "ON" : "OFF")))
            .values(true, false)
            .initially(llmSettings.isDeveloperModeEnabled())
            .tooltip(value -> Tooltip.of(Text.literal(value ? "Show all technical settings" : "Show simplified settings")))
            // Removed callback from builder chain
            .build(5, 5, 100, 20, Text.literal("Dev Mode")); // Position top-left
        this.addDrawableChild(developerModeButton);

        // --- Layout Variables ---
        this.devMode = llmSettings.isDeveloperModeEnabled(); // Initialize class field
        int y = this.height / 4; // Initial Y position
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
            // Add the callback here to update models when provider changes
            // Removed callback from builder chain
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

        // Endpoint URL (Only in Dev Mode)
        if (devMode) {
            endpointField = new TextFieldWidget(this.textRenderer, fieldX, y, fieldWidth, 20, Text.literal("API Endpoint"));
            endpointField.setText(llmSettings.getEndpoint() != null ? llmSettings.getEndpoint() : "");
            endpointField.setMaxLength(256);
            this.addDrawableChild(endpointField);
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Endpoint:"), button -> {})
                .dimensions(fieldX - labelWidth - 5, y, labelWidth, 20)
                .tooltip(Tooltip.of(Text.literal("The endpoint URL for the AI API (leave blank for default)")))
                .build());
            y += spacing;
        } else {
            endpointField = null; // Ensure it's null if hidden
        }

        // Model Selection (Only in Dev Mode - simplified view might just use provider default)
        if (devMode) {
            modelButton = CyclingButtonWidget.<String>builder(Text::literal)
                .values(List.of("(Select Provider)")) // Provide a placeholder value initially
                .initially("(Select Provider)") // Set initial value to the placeholder
                .tooltip(value -> Tooltip.of(Text.literal("Select the specific AI model to use (updates with provider)")))
                .build(fieldX - labelWidth - 5, y, buttonWidth, 20, Text.literal("Model"));
            this.addDrawableChild(modelButton);
            // Update the model list based on the initial provider selection
            updateModelList(providerButton.getValue());
            y += spacing;
        } else {
            modelButton = null; // Ensure it's null if hidden
        }

        // Context Length (Only in Dev Mode)
        if (devMode) {
            contextLengthField = new TextFieldWidget(this.textRenderer, fieldX, y, fieldWidth, 20, Text.literal("Context Length"));
            contextLengthField.setText(String.valueOf(llmSettings.getContextLength()));
            contextLengthField.setMaxLength(5);
            this.addDrawableChild(contextLengthField);
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Context Length:"), button -> {})
                .dimensions(fieldX - labelWidth - 5, y, labelWidth, 20)
                .tooltip(Tooltip.of(Text.literal("Maximum tokens in conversation history (e.g., 4096)")))
                .build());
            y += spacing;
        } else {
            contextLengthField = null;
        }

        // Temperature (Only in Dev Mode)
        if (devMode) {
            temperatureField = new TextFieldWidget(this.textRenderer, fieldX, y, fieldWidth, 20, Text.literal("Temperature"));
            temperatureField.setText(String.format("%.2f", llmSettings.getTemperature()));
            temperatureField.setMaxLength(4);
            this.addDrawableChild(temperatureField);
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Temperature:"), button -> {})
                .dimensions(fieldX - labelWidth - 5, y, labelWidth, 20)
                .tooltip(Tooltip.of(Text.literal("Controls randomness in responses (0.0-1.0)")))
                .build());
            y += spacing;
        } else {
            temperatureField = null;
        }

        // Use Local Model Toggle
        localModelButton = CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
            .values(true, false)
            .initially(llmSettings.isLocalModel())
            .tooltip(value -> Tooltip.of(Text.literal("Use a locally downloaded model instead of an API")))
            .build(fieldX - labelWidth - 5, y, buttonWidth, 20, Text.literal("Use Local Model"));
        this.addDrawableChild(localModelButton);
        y += spacing;
        
        // Enable Advanced Conversations (Only in Dev Mode)
        if (devMode) {
            enableAdvancedConversationsButton = CyclingButtonWidget.<Boolean>builder(value -> Text.literal(value ? "ON" : "OFF"))
                .values(true, false)
                .initially(llmSettings.isAdvancedConversationsEnabled())
                .tooltip(value -> Tooltip.of(Text.literal("Enable more complex conversations with villagers")))
                .build(fieldX - labelWidth - 5, y, buttonWidth, 20, Text.literal("Advanced Conversations"));
            this.addDrawableChild(enableAdvancedConversationsButton);
            y += spacing;
        } else {
            enableAdvancedConversationsButton = null;
        }

        // Test Connection Button
        testConnectionButton = ButtonWidget.builder(Text.literal("Test Connection"), button -> testConnection())
            .dimensions(this.width / 2 - 100, y, 200, 20)
            .tooltip(Tooltip.of(Text.literal("Test your current API settings")))
            .build();
        this.addDrawableChild(testConnectionButton);
        // Adjust Y position for connection status text based on dev mode
        // Use the final calculated 'y' before adding bottom buttons
        this.statusTextY = y + 5; // Initialize the field
        y += spacing + 5;

        // Save and Back buttons
        int bottomButtonY = this.height - 40;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
            .dimensions(this.width / 2 - 102, bottomButtonY, 100, 20)
            .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> this.client.setScreen(parent))
            .dimensions(this.width / 2 + 2, bottomButtonY, 100, 20)
            .build());

        // Initialize previous states after buttons are built (Keep only one block)
        if (developerModeButton != null) {
            this.previousDevModeState = developerModeButton.getValue();
        }
        if (providerButton != null) {
            this.previousProviderState = providerButton.getValue();
        }
    }

    private void testConnection() {
        if (isTestingConnection) {
            return; // Prevent multiple simultaneous tests
        }

        // Save current settings to temp
        String provider = providerButton.getValue();
        String apiKey = apiKeyField.getText();
        // Only get endpoint/model if in dev mode and fields exist
        // Use the class field 'this.devMode'
        String endpoint = (this.devMode && endpointField != null) ? endpointField.getText() : "";
        String model = (this.devMode && modelButton != null) ? modelButton.getValue() : ""; // Use provider default if not in dev mode? Needs logic in LLMService
        
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
        // Only set endpoint/model if in dev mode
        // Use the class field 'this.devMode'
        if (this.devMode) {
            llmSettings.setEndpoint(endpoint);
            llmSettings.setModelType(model);
        } else {
            // Optionally clear or set defaults when switching out of dev mode
            llmSettings.setEndpoint("");
            // Decide how to handle model - maybe set to a provider default?
            // For now, let's leave it as is, LLMService might handle defaults.
        }
        
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
        // Save settings based on whether they were visible (dev mode)
        // Use the class field 'this.devMode'
        if (this.devMode) {
            if (endpointField != null) llmSettings.setEndpoint(endpointField.getText());
            if (modelButton != null) llmSettings.setModelType(modelButton.getValue());
            if (enableAdvancedConversationsButton != null) llmSettings.setAdvancedConversationsEnabled(enableAdvancedConversationsButton.getValue());

            try {
                if (contextLengthField != null) llmSettings.setContextLength(Integer.parseInt(contextLengthField.getText()));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid context length value: {}", contextLengthField != null ? contextLengthField.getText() : "null");
            }
            try {
                if (temperatureField != null) llmSettings.setTemperature(Float.parseFloat(temperatureField.getText()));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid temperature value: {}", temperatureField != null ? temperatureField.getText() : "null");
            }
        } else {
            // Reset non-dev settings to defaults or clear them if desired when saving in simple mode
            // llmSettings.setEndpoint("");
            llmSettings.setModelType(null); // Set to null so provider uses default when not in dev mode
            // llmSettings.setContextLength(default);
            // llmSettings.setTemperature(default);
            // llmSettings.setAdvancedConversationsEnabled(false);
        }
        llmSettings.setLocalModel(localModelButton.getValue()); // Always save this
        llmSettings.setDeveloperModeEnabled(developerModeButton.getValue()); // Save the toggle state

        // Removed redundant parsing, moved into the devMode check above

        config.save();
        
        // Reinitialize LLM service with new settings
        LLMService.getInstance().setProvider(llmSettings.getProvider());
        
        this.client.setScreen(parent);
   }


   /**
    * Updates the model selection button based on the chosen provider.
    * @param selectedProvider The provider name (e.g., "gemini", "openai").
    */

   private void updateModelList(String selectedProvider) {
       if (modelButton == null) return; // Ensure button is initialized

       String[] availableModelIds = ModelType.getModelIdsForProvider(selectedProvider);
       List<String> modelIdList = Arrays.asList(availableModelIds);

       // Get current selection to try and preserve it
       String currentModelSelection = modelButton.getValue();
       String configModel = llmSettings.getModelType(); // Model saved in config

       // Determine the initial model for the new list
       String initialModel = "";
       if (!modelIdList.isEmpty()) {
           // If the model from config is valid for this provider, use it
           if (configModel != null && modelIdList.contains(configModel)) {
               initialModel = configModel;
           }
           // Else if the *currently selected* model in the UI is valid, keep it
           else if (currentModelSelection != null && modelIdList.contains(currentModelSelection)) {
                initialModel = currentModelSelection;
           }
           // Otherwise, default to the first model in the list
           else {
               initialModel = modelIdList.get(0);
           }
       }

       // Update the button's values and initial selection
       // --- Rebuild the modelButton ---
       // Store original position and dimensions (assuming they are accessible or known)
       // We know the position from the initial build (lines 99-100 in the previous read)
       int originalY = this.height / 4 + 3 * 24; // Calculate Y based on initial layout
       int buttonWidth = 200;
       int labelWidth = 120;
       int fieldX = this.width / 2 - buttonWidth / 2 + labelWidth + 5;

       // Remove the old button
       this.remove(modelButton);

       // Create the new button with the filtered list and correct initial value
       modelButton = CyclingButtonWidget.<String>builder(Text::literal)
            .values(modelIdList.isEmpty() ? List.of("(No models available)") : modelIdList) // Use placeholder if empty
            .initially(initialModel.isEmpty() ? "(No models available)" : initialModel)
            .tooltip(value -> Tooltip.of(Text.literal("Select the specific AI model to use (updates with provider)")))
            .build(fieldX - labelWidth - 5, originalY, buttonWidth, 20, Text.literal("Model"));

       // Disable if no real models are available
       modelButton.active = !modelIdList.isEmpty();

       // Add the new button
       this.addDrawableChild(modelButton);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawText(this.textRenderer, this.title, this.width / 2 - this.textRenderer.getWidth(this.title) / 2, 20, 0xFFFFFF, true);
        
        // Draw description
        String description = "Configure AI settings for village interactions";
        context.drawText(
            this.textRenderer, 
            description,
            this.width / 2 - this.textRenderer.getWidth(description) / 2, 
            35, 
            0xAAAAAA,
            true
        );
        
        // Draw connection status if not empty - fixed using drawText instead of drawCenteredText
        if (!connectionStatusText.getString().isEmpty()) {
            context.drawText(
                this.textRenderer,
                connectionStatusText,
                this.width / 2 - this.textRenderer.getWidth(connectionStatusText) / 2, // X position
                statusTextY, // Use calculated Y position
                0xFFFFFF,
                true
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
    @Override
    public void tick() {
        super.tick(); // Call superclass tick if needed

        // Check Developer Mode Button state change
        // Ensure previous state is not null before comparing
        if (developerModeButton != null && this.previousDevModeState != null && !developerModeButton.getValue().equals(this.previousDevModeState)) {
            this.previousDevModeState = developerModeButton.getValue(); // Update previous state
            // Original callback logic:
            llmSettings.setDeveloperModeEnabled(this.previousDevModeState);
            // Re-create the screen entirely to reflect the mode change
            this.client.setScreen(new AISettingsScreen(this.parent));
            return; // Exit tick early as the screen is being replaced
        }

        // Check Provider Button state change (only if clearAndInit wasn't called)
        // Ensure previous state is not null before comparing
        if (providerButton != null && this.previousProviderState != null && !providerButton.getValue().equals(this.previousProviderState)) {
            this.previousProviderState = providerButton.getValue(); // Update previous state
            // Original callback logic:
            updateModelList(this.previousProviderState); // Call update method on change
        }
    }
} // Moved tick() method inside the class and removed incorrect comment