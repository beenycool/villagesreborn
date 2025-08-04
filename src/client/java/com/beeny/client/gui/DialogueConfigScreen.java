package com.beeny.client.gui;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.dialogue.LLMDialogueManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

public class DialogueConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 300;
    
    private final Screen parent;
    private TextFieldWidget apiKeyField;
    private TextFieldWidget endpointField;
    private TextFieldWidget modelField;
    private CyclingButtonWidget<String> providerButton;
    private CyclingButtonWidget<Boolean> enabledButton;
    private ButtonWidget testButton;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private ButtonWidget presetButton;
    
    private String tempProvider;
    private boolean tempEnabled;
    private String tempApiKey;
    private String tempEndpoint;
    private String tempModel;
    
    private Text statusMessage;
    private Formatting statusColor = Formatting.WHITE;
    
    public DialogueConfigScreen(Screen parent) {
        super(Text.literal("Dynamic Dialogue Configuration"));
        this.parent = parent;
        
        // Load current settings
        this.tempProvider = VillagersRebornConfig.LLM_PROVIDER;
        this.tempEnabled = VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE;
        this.tempApiKey = VillagersRebornConfig.LLM_API_KEY;
        this.tempEndpoint = VillagersRebornConfig.LLM_API_ENDPOINT;
        this.tempModel = VillagersRebornConfig.LLM_MODEL;
        
        this.statusMessage = Text.literal("Configure your AI dialogue settings below");
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = centerY - PANEL_HEIGHT / 2;
        
        // Title area starts at panelTop + 20
        int currentY = panelTop + 40;
        
        // Enable/Disable toggle
        this.enabledButton = CyclingButtonWidget.<Boolean>builder(enabled -> enabled ? Text.literal("Dynamic Dialogue: Enabled") : Text.literal("Dynamic Dialogue: Disabled"))
            .values(true, false)
            .initially(tempEnabled)
            .build(panelLeft + 20, currentY, 240, 20, Text.literal("Dynamic Dialogue"), 
                   (button, enabled) -> {
                       this.tempEnabled = enabled;
                       updateFieldStates();
                   });
        this.addDrawableChild(enabledButton);
        
        // Quick preset button
        this.presetButton = ButtonWidget.builder(Text.literal("Quick Setup"), button -> openPresetMenu())
            .dimensions(panelLeft + 160, currentY, 100, 20)
            .build();
        this.addDrawableChild(presetButton);
        
        currentY += 35;
        
        // Provider selection
        // Add "local" provider for local LLM integration
        List<String> providers = Arrays.asList("gemini", "openrouter", "local");
        this.providerButton = CyclingButtonWidget.<String>builder(value -> Text.literal("Provider: " + value.toUpperCase()))
            .values(providers)
            .initially(tempProvider)
            .build(panelLeft + 20, currentY, 200, 20, Text.literal("LLM Provider"), 
                   (button, provider) -> {
                       this.tempProvider = provider;
                       updateModelField();
                   });
        this.addDrawableChild(providerButton);
        
        currentY += 30;
        
        // API Key field
        this.addDrawableChild(createLabel(panelLeft + 20, currentY, "API Key:"));
        currentY += 15;
        this.apiKeyField = new TextFieldWidget(this.textRenderer, panelLeft + 20, currentY, 300, 20, Text.literal("API Key"));
        this.apiKeyField.setText(tempApiKey);
        this.apiKeyField.setMaxLength(200);
        this.addDrawableChild(apiKeyField);
        
        currentY += 30;
        
        // Model field
        this.addDrawableChild(createLabel(panelLeft + 20, currentY, "Model:"));
        currentY += 15;
        this.modelField = new TextFieldWidget(this.textRenderer, panelLeft + 20, currentY, 200, 20, Text.literal("Model"));
        this.modelField.setText(tempModel);
        this.addDrawableChild(modelField);
        
        currentY += 30;
        
        // Advanced: Endpoint field (optional)
        this.addDrawableChild(createLabel(panelLeft + 20, currentY, "Custom Endpoint (Optional):"));
        currentY += 15;
        this.endpointField = new TextFieldWidget(this.textRenderer, panelLeft + 20, currentY, 300, 20, Text.literal("Endpoint"));
        this.endpointField.setText(tempEndpoint);
        this.addDrawableChild(endpointField);
        
        currentY += 40;
        
        // Test connection button
        this.testButton = ButtonWidget.builder(Text.literal("Test Connection"), button -> testConnection())
            .dimensions(panelLeft + 20, currentY, 120, 20)
            .build();
        this.addDrawableChild(testButton);
        
        // Save and Cancel buttons
        this.saveButton = ButtonWidget.builder(Text.literal("Save"), button -> saveAndClose())
            .dimensions(panelLeft + 200, currentY, 60, 20)
            .build();
        this.addDrawableChild(saveButton);
        
        this.cancelButton = ButtonWidget.builder(Text.literal("Cancel"), button -> close())
            .dimensions(panelLeft + 270, currentY, 60, 20)
            .build();
        this.addDrawableChild(cancelButton);
        
        updateFieldStates();
        updateModelField();
    }
    
    private ButtonWidget createLabel(int x, int y, String text) {
        return ButtonWidget.builder(Text.literal(text), button -> {})
            .dimensions(x, y, this.textRenderer.getWidth(text), 12)
            .build();
    }
    
    private void updateFieldStates() {
        boolean enabled = tempEnabled;
        if (apiKeyField != null) apiKeyField.active = enabled;
        if (endpointField != null) endpointField.active = enabled;
        if (modelField != null) modelField.active = enabled;
        if (providerButton != null) providerButton.active = enabled;
        if (testButton != null) testButton.active = enabled;
        if (presetButton != null) presetButton.active = enabled;
    }
    
    private void updateModelField() {
        if (modelField != null) {
            String defaultModel = switch (tempProvider) {
                case "gemini" -> "gemini-1.5-flash";
                case "openrouter" -> "openai/gpt-3.5-turbo";
                default -> "";
            };
            
            if (modelField.getText().isEmpty() || 
                modelField.getText().equals("gemini-1.5-flash") || 
                modelField.getText().equals("openai/gpt-3.5-turbo")) {
                modelField.setText(defaultModel);
                tempModel = defaultModel;
            }
        }
    }
    
    private void openPresetMenu() {
        // Create preset selection popup
        PresetSelectionScreen presetScreen = new PresetSelectionScreen(this, this::applyPreset);
        if (this.client != null) {
            this.client.setScreen(presetScreen);
        }
    }
    
    private void applyPreset(PresetConfig preset) {
        this.tempProvider = preset.provider;
        this.tempModel = preset.model;
        this.tempEndpoint = preset.endpoint;
        this.tempEnabled = true;
        
        // Update UI
        if (providerButton != null) {
            providerButton.setValue(preset.provider);
        }
        if (modelField != null) {
            modelField.setText(preset.model);
        }
        if (endpointField != null) {
            endpointField.setText(preset.endpoint);
        }
        if (enabledButton != null) {
            enabledButton.setValue(true);
        }
        
        updateFieldStates();
        
        setStatusMessage(Text.literal("Applied " + preset.name + " preset. Don't forget to add your API key!"), Formatting.GREEN);
    }
    
    private void testConnection() {
        if (apiKeyField.getText().trim().isEmpty()) {
            setStatusMessage(Text.literal("Please enter an API key first"), Formatting.RED);
            return;
        }

        setStatusMessage(Text.literal("Testing connection..."), Formatting.YELLOW);

        // Prepare temporary settings for testing
        String provider = tempProvider;
        String apiKey = apiKeyField.getText().trim();
        String endpoint = endpointField.getText().trim();
        String model = modelField.getText().trim();

        // Pass temporary settings directly to the test method
        LLMDialogueManager.testConnection(provider, apiKey, endpoint, model).thenAccept(success -> {
            if (success) {
                setStatusMessage(Text.literal("✓ Connection successful!"), Formatting.GREEN);
            } else {
                setStatusMessage(Text.literal("✗ Connection failed. Check your settings."), Formatting.RED);
            }
        }).exceptionally(throwable -> {
            setStatusMessage(Text.literal("✗ Connection error: " + throwable.getMessage()), Formatting.RED);
            return null;
        });
    }
    
    private void saveAndClose() {
        // Validate settings
        if (tempEnabled && !tempProvider.equals("local") && apiKeyField.getText().trim().isEmpty()) {
            setStatusMessage(Text.literal("API key is required for this provider when dynamic dialogue is enabled"), Formatting.RED);
            return;
        }
        
        // Apply settings
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = tempEnabled;
        VillagersRebornConfig.LLM_PROVIDER = tempProvider;
        VillagersRebornConfig.LLM_API_KEY = apiKeyField.getText().trim();
        VillagersRebornConfig.LLM_API_ENDPOINT = endpointField.getText().trim();
        VillagersRebornConfig.LLM_MODEL = modelField.getText().trim();
        if ("local".equalsIgnoreCase(tempProvider)) {
            VillagersRebornConfig.LLM_LOCAL_URL = endpointField.getText().trim().isEmpty()
                ? VillagersRebornConfig.LLM_LOCAL_URL
                : endpointField.getText().trim();
        }
        
        // Reinitialize dialogue manager
        LLMDialogueManager.initialize();
        
        // Save to config file
        saveConfigToFile();
        
        close();
    }
    
    private void saveConfigToFile() {
        // Persist settings to villagersreborn.json
        try {
            // Ensure in-memory config mirrors UI state before saving
            com.beeny.config.VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = tempEnabled;
            com.beeny.config.VillagersRebornConfig.LLM_PROVIDER = tempProvider;
            com.beeny.config.VillagersRebornConfig.LLM_API_KEY = apiKeyField.getText().trim();
            com.beeny.config.VillagersRebornConfig.LLM_API_ENDPOINT = endpointField.getText().trim();
            com.beeny.config.VillagersRebornConfig.LLM_MODEL = modelField.getText().trim();
            if ("local".equalsIgnoreCase(tempProvider)) {
                com.beeny.config.VillagersRebornConfig.LLM_LOCAL_URL = endpointField.getText().trim();
            }

            // Persist to disk
            com.beeny.config.ConfigManager.saveConfig();

            // Optionally reload to verify persistence
            com.beeny.config.ConfigManager.loadConfig();

            setStatusMessage(Text.literal("✓ Settings saved!"), Formatting.GREEN);
        } catch (Exception e) {
            setStatusMessage(Text.literal("✗ Failed to save settings: " + e.getMessage()), Formatting.RED);
        }
    }
    
    private void setStatusMessage(Text message, Formatting color) {
        this.statusMessage = message;
        this.statusColor = color;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw background
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = centerY - PANEL_HEIGHT / 2;
        
        // Draw panel background
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + PANEL_HEIGHT, 0x88000000);
        context.drawBorder(panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFFFFF);
        
        // Draw title
        Text title = Text.literal("Dynamic Dialogue Setup").formatted(Formatting.BOLD);
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, centerX - titleWidth / 2, panelTop + 10, 0xFFFFFF, false);
        
        // Draw status message
        if (statusMessage != null) {
            int statusY = panelTop + PANEL_HEIGHT - 25;
            context.drawText(this.textRenderer, statusMessage.copy().formatted(statusColor), 
                           panelLeft + 20, statusY, statusColor.getColorValue(), false);
        }
        
        // Draw provider-specific help text
        String helpText = getProviderHelpText();
        if (!helpText.isEmpty()) {
            int helpY = panelTop + PANEL_HEIGHT - 45;
            context.drawText(this.textRenderer, Text.literal(helpText).formatted(Formatting.GRAY), 
                           panelLeft + 20, helpY, 0x888888, false);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private String getProviderHelpText() {
        return switch (tempProvider) {
            case "gemini" -> "Get your free API key from ai.google.dev";
            case "openrouter" -> "Get your API key from openrouter.ai - supports many models";
            default -> "";
        };
    }
    
    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
    
    // Preset configuration class
    public static class PresetConfig {
        public final String name;
        public final String provider;
        public final String model;
        public final String endpoint;
        public final String description;
        
        public PresetConfig(String name, String provider, String model, String endpoint, String description) {
            this.name = name;
            this.provider = provider;
            this.model = model;
            this.endpoint = endpoint;
            this.description = description;
        }
    }
}