package com.beeny.client.gui;

import com.beeny.config.VillagersRebornConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class VillagersRebornConfigScreen extends Screen {
    private final Screen parent;
    
    // Temporary values for editing
    private boolean tempEnableDynamicDialogue;
    private String tempLlmProvider;
    private String tempLlmModel;
    private double tempLlmTemperature;
    private int tempLlmMaxTokens;
    private int tempLlmRequestTimeout;
    private boolean tempFallbackToStatic;
    private boolean tempEnableDialogueCache;
    private int tempDialogueCacheSize;
    private int tempConversationHistoryLimit;
    private int tempHappinessDecayRate;
    private int tempHappinessRecoveryRate;
    private int tempVillagerScanRadius;
    private int tempAiThreadPoolSize;
    
    // GUI Components
    private TextFieldWidget modelField;
    
    public VillagersRebornConfigScreen(Screen parent) {
        super(Text.literal(com.beeny.constants.StringConstants.UI_TITLE_SETTINGS));
        this.parent = parent;
        loadTempValues();
    }
    
    private void loadTempValues() {
        tempEnableDynamicDialogue = VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE;
        tempLlmProvider = VillagersRebornConfig.LLM_PROVIDER;
        tempLlmModel = VillagersRebornConfig.LLM_MODEL;
        tempLlmTemperature = VillagersRebornConfig.LLM_TEMPERATURE;
        tempLlmMaxTokens = VillagersRebornConfig.LLM_MAX_TOKENS;
        tempLlmRequestTimeout = VillagersRebornConfig.LLM_REQUEST_TIMEOUT;
        tempFallbackToStatic = VillagersRebornConfig.FALLBACK_TO_STATIC;
        tempEnableDialogueCache = VillagersRebornConfig.ENABLE_DIALOGUE_CACHE;
        tempDialogueCacheSize = VillagersRebornConfig.DIALOGUE_CACHE_SIZE;
        tempConversationHistoryLimit = VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT;
        tempHappinessDecayRate = VillagersRebornConfig.HAPPINESS_DECAY_RATE;
        tempHappinessRecoveryRate = VillagersRebornConfig.HAPPINESS_RECOVERY_RATE;
        tempVillagerScanRadius = VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS;
        tempAiThreadPoolSize = VillagersRebornConfig.AI_THREAD_POOL_SIZE;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = 40;
        int spacing = 25;
        int currentY = startY;
        
        // Title
        addDrawableChild(ButtonWidget.builder(Text.literal(com.beeny.constants.StringConstants.UI_TITLE_SETTINGS).formatted(Formatting.GOLD, Formatting.BOLD),
            button -> {}).dimensions(centerX - 100, 20, 200, 20).build()).active = false;
        
        // AI Settings Section
        addDrawableChild(ButtonWidget.builder(Text.literal(com.beeny.constants.StringConstants.UI_AI_SETTINGS).formatted(Formatting.AQUA, Formatting.BOLD),
            button -> {}).dimensions(centerX - 100, currentY, 200, 20).build()).active = false;
        currentY += spacing;
        
        // Enable Dynamic Dialogue
        addDrawableChild(CyclingButtonWidget.onOffBuilder(tempEnableDynamicDialogue)
            .build(centerX - 100, currentY, 200, 20, 
                Text.literal(com.beeny.constants.StringConstants.UI_DYNAMIC_DIALOGUE),
                (button, value) -> tempEnableDynamicDialogue = value));
        currentY += spacing;
        
        // LLM Provider
        addDrawableChild(CyclingButtonWidget.builder((String provider) -> Text.literal(com.beeny.constants.StringConstants.UI_PROVIDER_LABEL_FN + provider))
            .values("gemini", "openrouter", "local")
            .initially(tempLlmProvider)
            .build(centerX - 100, currentY, 200, 20, 
                Text.literal("LLM Provider"), 
                (button, value) -> tempLlmProvider = value));
        currentY += spacing;
        
        // Model Field
        modelField = new TextFieldWidget(this.textRenderer, centerX - 100, currentY, 200, 20, Text.literal(com.beeny.constants.StringConstants.UI_MODEL));
        modelField.setText(tempLlmModel);
        modelField.setChangedListener(text -> tempLlmModel = text);
        addDrawableChild(modelField);
        currentY += spacing;
        
        // Temperature Slider
        addDrawableChild(new SliderWidget(centerX - 100, currentY, 200, 20, 
            Text.literal(com.beeny.constants.StringConstants.UI_TEMPERATURE_LABEL_FN + String.format("%.2f", tempLlmTemperature)),
            tempLlmTemperature) {
            @Override
            protected void updateMessage() {
                tempLlmTemperature = this.value;
                this.setMessage(Text.literal(com.beeny.constants.StringConstants.UI_TEMPERATURE_LABEL_FN + String.format("%.2f", tempLlmTemperature)));
            }
            @Override
            protected void applyValue() {
                tempLlmTemperature = this.value;
            }
        });
        currentY += spacing;
        
        // Max Tokens Slider  
        addDrawableChild(new SliderWidget(centerX - 100, currentY, 200, 20, 
            Text.literal(com.beeny.constants.StringConstants.UI_MAX_TOKENS_LABEL_FN + tempLlmMaxTokens),
            (tempLlmMaxTokens - 50) / 450.0) {
            @Override
            protected void updateMessage() {
                tempLlmMaxTokens = (int) (50 + this.value * 450);
                this.setMessage(Text.literal(com.beeny.constants.StringConstants.UI_MAX_TOKENS_LABEL_FN + tempLlmMaxTokens));
            }
            @Override
            protected void applyValue() {
                tempLlmMaxTokens = (int) (50 + this.value * 450);
            }
        });
        currentY += spacing;
        
        // Fallback to Static
        addDrawableChild(CyclingButtonWidget.onOffBuilder(tempFallbackToStatic)
            .build(centerX - 100, currentY, 200, 20, 
                Text.literal(com.beeny.constants.StringConstants.UI_FALLBACK_TO_STATIC),
                (button, value) -> tempFallbackToStatic = value));
        currentY += spacing;
        
        // Enable Dialogue Cache
        addDrawableChild(CyclingButtonWidget.onOffBuilder(tempEnableDialogueCache)
            .build(centerX - 100, currentY, 200, 20, 
                Text.literal(com.beeny.constants.StringConstants.UI_ENABLE_DIALOGUE_CACHE),
                (button, value) -> tempEnableDialogueCache = value));
        currentY += spacing + 10;
        
        // Villager Settings Section
        addDrawableChild(ButtonWidget.builder(Text.literal(com.beeny.constants.StringConstants.UI_VILLAGER_SETTINGS).formatted(Formatting.GREEN, Formatting.BOLD),
            button -> {}).dimensions(centerX - 100, currentY, 200, 20).build()).active = false;
        currentY += spacing;
        
        // Happiness Decay Rate
        addDrawableChild(new SliderWidget(centerX - 100, currentY, 200, 20, 
            Text.literal("Happiness Decay: " + tempHappinessDecayRate), 
            (tempHappinessDecayRate - 1) / 9.0) {
            @Override
            protected void updateMessage() {
                tempHappinessDecayRate = (int) (1 + this.value * 9);
                this.setMessage(Text.literal("Happiness Decay: " + tempHappinessDecayRate));
            }
            @Override
            protected void applyValue() {
                tempHappinessDecayRate = (int) (1 + this.value * 9);
            }
        });
        currentY += spacing;
        
        // Happiness Recovery Rate
        addDrawableChild(new SliderWidget(centerX - 100, currentY, 200, 20, 
            Text.literal("Happiness Recovery: " + tempHappinessRecoveryRate), 
            (tempHappinessRecoveryRate - 1) / 9.0) {
            @Override
            protected void updateMessage() {
                tempHappinessRecoveryRate = (int) (1 + this.value * 9);
                this.setMessage(Text.literal("Happiness Recovery: " + tempHappinessRecoveryRate));
            }
            @Override
            protected void applyValue() {
                tempHappinessRecoveryRate = (int) (1 + this.value * 9);
            }
        });
        currentY += spacing;
        
        // Villager Scan Radius
        addDrawableChild(new SliderWidget(centerX - 100, currentY, 200, 20, 
            Text.literal("Scan Radius: " + tempVillagerScanRadius + " chunks"), 
            (tempVillagerScanRadius - 1) / 15.0) {
            @Override
            protected void updateMessage() {
                tempVillagerScanRadius = (int) (1 + this.value * 15);
                this.setMessage(Text.literal("Scan Radius: " + tempVillagerScanRadius + " chunks"));
            }
            @Override
            protected void applyValue() {
                tempVillagerScanRadius = (int) (1 + this.value * 15);
            }
        });
        currentY += spacing + 20;
        
        // Action Buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("AI Dialogue Setup"), 
            button -> {
                MinecraftClient.getInstance().setScreen(new DialogueConfigScreen(this));
            }).dimensions(centerX - 205, currentY, 100, 20).build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal(com.beeny.constants.StringConstants.UI_RESET_DEFAULTS),
            button -> {
                resetToDefaults();
            }).dimensions(centerX - 100, currentY, 100, 20).build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal(com.beeny.constants.StringConstants.UI_SAVE_APPLY),
            button -> {
                saveSettings();
                this.client.setScreen(parent);
            }).dimensions(centerX + 5, currentY, 100, 20).build());
        
        addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, 
            button -> {
                this.client.setScreen(parent);
            }).dimensions(centerX + 110, currentY, 95, 20).build());
    }
    
    private void resetToDefaults() {
        tempEnableDynamicDialogue = true;
        tempLlmProvider = "gemini";
        tempLlmModel = "";
        tempLlmTemperature = 0.7;
        tempLlmMaxTokens = 150;
        tempLlmRequestTimeout = 10000;
        tempFallbackToStatic = true;
        tempEnableDialogueCache = true;
        tempDialogueCacheSize = 1000;
        tempConversationHistoryLimit = 10;
        tempHappinessDecayRate = 1;
        tempHappinessRecoveryRate = 1;
        tempVillagerScanRadius = 8;
        tempAiThreadPoolSize = 4;
        
        // Refresh the screen
        this.clearAndInit();
    }
    
    private void saveSettings() {
        VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE = tempEnableDynamicDialogue;
        VillagersRebornConfig.LLM_PROVIDER = tempLlmProvider;
        VillagersRebornConfig.LLM_MODEL = tempLlmModel;
        VillagersRebornConfig.LLM_TEMPERATURE = tempLlmTemperature;
        VillagersRebornConfig.LLM_MAX_TOKENS = tempLlmMaxTokens;
        VillagersRebornConfig.LLM_REQUEST_TIMEOUT = tempLlmRequestTimeout;
        VillagersRebornConfig.FALLBACK_TO_STATIC = tempFallbackToStatic;
        VillagersRebornConfig.ENABLE_DIALOGUE_CACHE = tempEnableDialogueCache;
        VillagersRebornConfig.DIALOGUE_CACHE_SIZE = tempDialogueCacheSize;
        VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT = tempConversationHistoryLimit;
        VillagersRebornConfig.HAPPINESS_DECAY_RATE = tempHappinessDecayRate;
        VillagersRebornConfig.HAPPINESS_RECOVERY_RATE = tempHappinessRecoveryRate;
        VillagersRebornConfig.VILLAGER_SCAN_CHUNK_RADIUS = tempVillagerScanRadius;
        VillagersRebornConfig.AI_THREAD_POOL_SIZE = tempAiThreadPoolSize;
        
        // Save to file
        // Send config update request to server (packet logic should be implemented here)
        // Example: ClientPlayNetworking.send(new UpdateConfigPacket(temp values...));
        // UI can show a toast or message on success/failure.
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Prevent blur effect to avoid "Can only blur once per frame" error
        if (this.client != null && this.client.world != null) {
            // Draw a simple gradient background without blur
            context.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        } else {
            super.renderBackground(context, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render background
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // Render widgets using super.render() which handles children rendering
        super.render(context, mouseX, mouseY, delta);
        
        // Add some help text
        if (mouseX >= width / 2 - 100 && mouseX <= width / 2 + 100) {
            String helpText = getHelpText(mouseY);
            if (helpText != null) {
                context.drawTooltip(this.textRenderer, Text.literal(helpText), mouseX, mouseY);
            }
        }
    }
    
    private String getHelpText(int mouseY) {
        // Add contextual help based on mouse position
        if (mouseY >= 65 && mouseY <= 85) {
            return "Enable AI-powered dynamic dialogue for villagers";
        } else if (mouseY >= 90 && mouseY <= 110) {
            return "Choose AI provider: Gemini (free), OpenRouter (paid), or Local";
        } else if (mouseY >= 115 && mouseY <= 135) {
            return "Specific AI model name (e.g., gemini-1.5-pro, gpt-4o)";
        } else if (mouseY >= 140 && mouseY <= 160) {
            return "Controls randomness in AI responses (0.0-1.0)";
        }
        return null;
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}