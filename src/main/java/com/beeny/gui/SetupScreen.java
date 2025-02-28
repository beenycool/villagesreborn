package com.beeny.gui;

import com.beeny.setup.ModelDownloader;
import com.beeny.setup.SystemSpecs;
import com.beeny.setup.SystemSpecs.SystemTier;
import com.beeny.setup.LLMConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.RenderLayer;
import java.util.List;
import java.util.function.Function;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class SetupScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final LLMConfig llmConfig;
    private final AnimationRenderer animationRenderer = new AnimationRenderer();
    private boolean uiInitialized = false;

    // UI positioning
    private int leftX, fieldWidth, fieldHeight, padding, currentY;

    private TextFieldWidget apiKeyField;
    private TextFieldWidget endpointField;
    private CyclingButtonWidget<String> modelTypeButton;
    private CyclingButtonWidget<String> providerButton;
    private SliderWidget temperatureSlider;
    private SliderWidget contextLengthSlider;

    // Update model types to include models from the new providers
    private static final List<String> MODEL_TYPES = List.of(
        // OpenAI models
        "gpt-3.5-turbo", 
        "gpt-4", 
        "gpt-4-turbo",
        // Anthropic models
        "claude-2.1",
        "claude-3-opus",
        "claude-3-sonnet", 
        // Mistral models
        "mistral-small",
        "mistral-medium",
        "mistral-large-latest",
        // Gemini models
        "gemini-1.0-pro",
        "gemini-1.5-pro",
        // Cohere models
        "command",
        "command-light",
        "command-nightly",
        // Local models
        "llama2"
    );
    
    // Update providers list to include the new options
    private static final List<String> PROVIDERS = List.of(
        "azure", 
        "openai", 
        "anthropic", 
        "gemini", 
        "cohere", 
        "mistral", 
        "local"
    );

    private static final int ANIM_FRAME_TIME = 20;
    private static final Function<Identifier, RenderLayer> TEXTURE_LAYER = id -> RenderLayer.getGui();
    private static final Identifier[] FRAMES = new Identifier[] {
        Identifier.of("villagesreborn", "textures/gui/anim_frame1.png"),
        Identifier.of("villagesreborn", "textures/gui/anim_frame2.png"),
        Identifier.of("villagesreborn", "textures/gui/anim_frame3.png")
    };
    
    private float progress = 0;
    private int currentFrame = 0;
    private int frameTimer = 0;
    private final Text statusText;

    public SetupScreen(LLMConfig llmConfig) {
        super(Text.literal("Villages Reborn Setup"));
        this.llmConfig = llmConfig;
        this.statusText = Text.literal("Setting up...");
    }

    @Override
    public void tick() {
        if (!animationRenderer.isComplete()) {
            animationRenderer.tick();
        } else {
            frameTimer++;
            if (frameTimer >= ANIM_FRAME_TIME) {
                frameTimer = 0;
                currentFrame = (currentFrame + 1) % FRAMES.length;
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (!animationRenderer.isComplete()) {
            animationRenderer.render(context, width, height, delta);
        } else {
            // Draw title
            context.drawTextWithShadow(textRenderer, this.title,
                (width - textRenderer.getWidth(this.title)) / 2, 20, 0xFFFFFF);
            
            // Draw current animation frame
            int frameSize = 128;
            int x = (this.width - frameSize) / 2;
            int y = (this.height - frameSize) / 2 - 30;
            
            // Draw texture using the layer-based method with GUI layer
            context.drawGuiTexture(TEXTURE_LAYER, FRAMES[currentFrame], x, y, frameSize, frameSize);
            
            // Draw progress bar
            int barWidth = 200;
            int barHeight = 10;
            int barX = (this.width - barWidth) / 2;
            int barY = this.height / 2 + 50;
            
            // Background
            context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333);
            // Progress
            context.fill(
                barX, barY,
                barX + (int)(barWidth * progress),
                barY + barHeight,
                0xFF00FF00
            );
            
            // Draw status text
            context.drawTextWithShadow(
                this.textRenderer,
                this.statusText,
                (width - textRenderer.getWidth(this.statusText)) / 2,
                barY + barHeight + 10,
                0xFFFFFF
            );
        }
    }

    @Override
    protected void init() {
        if (!animationRenderer.isComplete()) {
            // Wait for animation to complete
            return;
        }
        if (uiInitialized) return;
        uiInitialized = true;

        leftX = width / 2 - 100;
        fieldWidth = 200;
        fieldHeight = 20;
        padding = 24;
        currentY = 50;

        // Display hardware info
        SystemSpecs specs = com.beeny.Villagesreborn.getSystemSpecs();
        Map<String, String> gpu = specs.getGpuInfo();
        String hardwareText = String.format("CPU: %d cores, RAM: %d MB, GPU: %s",
                specs.getCpuThreads(), specs.getAvailableRam(), gpu.getOrDefault("renderer", "Unknown"));

        // Display hardware info as button
        addDrawableChild(ButtonWidget.builder(Text.literal(hardwareText), button -> {})
            .dimensions(leftX, currentY, fieldWidth, fieldHeight)
            .build());
        currentY += padding;

        // Determine recommended model based on system tier
        String recommendedModel;
        int modelSize;
        SystemTier tier = specs.getSystemTier();
        switch (tier) {
            case HIGH:
                recommendedModel = "advanced_model.bin";
                modelSize = 500;
                break;
            case MEDIUM:
                recommendedModel = "standard_model.bin";
                modelSize = 200;
                break;
            default:
                recommendedModel = "basic_model.bin";
                modelSize = 50;
                break;
        }
        
        // Display recommended model info
        Text recommendationText = Text.literal("Recommended AI Model: " + recommendedModel + " (" + modelSize + " MB)");
        addDrawableChild(ButtonWidget.builder(recommendationText, button -> {})
            .dimensions(leftX, currentY, fieldWidth, fieldHeight)
            .build());
        currentY += padding;

        // Download prompt
        addDrawableChild(ButtonWidget.builder(Text.literal("Download this model to enhance villager AI?"), button -> {})
            .dimensions(leftX, currentY, fieldWidth, fieldHeight)
            .build());
        currentY += padding;

        // Yes, Download button
        ButtonWidget downloadButton = ButtonWidget.builder(Text.literal("Yes, Download"), button -> {
            String expectedHash = "dummyhash";
            ModelDownloader.downloadModel(recommendedModel, expectedHash, progress -> {
                LOGGER.info("Download progress: {}%", progress * 100);
                setProgress(progress);
            }).thenAccept(modelPath -> {
                llmConfig.setModelType(recommendedModel);
                llmConfig.setSetupComplete(true);
                llmConfig.saveConfig();
                close();
            }).exceptionally(e -> {
                LOGGER.error("Download failed", e);
                useDefaultModel();
                return null;
            });
        })
        .dimensions(leftX, currentY, fieldWidth / 2 - 2, fieldHeight)
        .build();
        addDrawableChild(downloadButton);

        // No, Use Default button
        ButtonWidget declineButton = ButtonWidget.builder(Text.literal("No, Use Default"), button -> {
            useDefaultModel();
            close();
        })
        .dimensions(leftX + fieldWidth / 2 + 2, currentY, fieldWidth / 2 - 2, fieldHeight)
        .build();
        addDrawableChild(declineButton);

        currentY += padding;

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

        // Provider selection - updating the provider first, then model list based on provider
        providerButton = CyclingButtonWidget.<String>builder(value -> Text.literal("Provider: " + value))
            .values(PROVIDERS)
            .initially(PROVIDERS.contains(llmConfig.getProvider()) ? llmConfig.getProvider() : PROVIDERS.get(0))
            .build(leftX, currentY, fieldWidth, fieldHeight, Text.literal("Provider"));

        // After provider selection, update the model types accordingly
        providerButton.onValueChange(provider -> updateModelListForProvider(provider));
        
        // Add a button press listener
        providerButton.setMessage(Text.literal("Provider: " + providerButton.getValue()));
        providerButton.setValueChangeListener(provider -> updateModelListForProvider(provider));
        
        addDrawableChild(providerButton);
        currentY += padding;
        
        // Model Type - default models are loaded, will be updated when provider changes
        modelTypeButton = CyclingButtonWidget.<String>builder(value -> Text.literal("Model: " + value))
            .values(getModelsForProvider(providerButton.getValue()))
            .initially(MODEL_TYPES.contains(llmConfig.getModelType()) ? llmConfig.getModelType() : getDefaultModelForProvider(providerButton.getValue()))
            .build(leftX, currentY, fieldWidth, fieldHeight, Text.literal("Model Type"));
        addDrawableChild(modelTypeButton);
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
        })
        .dimensions(leftX, this.height - 28, fieldWidth, fieldHeight)
        .build();
        addDrawableChild(doneButton);
    }
    
    // Helper method to update model list when provider changes
    private void updateModelListForProvider(String provider) {
        // Re-create model button with filtered models
        List<String> providerModels = getModelsForProvider(provider);
        String defaultModel = getDefaultModelForProvider(provider);
        
        // Replace the existing model button with new filtered models
        this.remove(modelTypeButton);
        modelTypeButton = CyclingButtonWidget.<String>builder(value -> Text.literal("Model: " + value))
            .values(providerModels)
            .initially(defaultModel)
            .build(leftX, currentY - padding, fieldWidth, fieldHeight, Text.literal("Model Type"));
        addDrawableChild(modelTypeButton);
        
        // Update endpoint for specific providers
        updateEndpointForProvider(provider);
    }
    
    // Helper method to get appropriate models for each provider
    private List<String> getModelsForProvider(String provider) {
        return switch(provider) {
            case "openai" -> List.of("gpt-3.5-turbo", "gpt-4", "gpt-4-turbo");
            case "anthropic" -> List.of("claude-2.1", "claude-3-opus", "claude-3-sonnet");
            case "azure" -> List.of("gpt-3.5-turbo", "gpt-4");
            case "gemini" -> List.of("gemini-1.0-pro", "gemini-1.5-pro");
            case "cohere" -> List.of("command", "command-light", "command-nightly");
            case "mistral" -> List.of("mistral-small", "mistral-medium", "mistral-large-latest");
            case "local" -> List.of("llama2");
            default -> List.of("gpt-3.5-turbo");
        };
    }
    
    // Helper method to set default model for each provider
    private String getDefaultModelForProvider(String provider) {
        return switch(provider) {
            case "openai" -> "gpt-3.5-turbo";
            case "anthropic" -> "claude-2.1";
            case "azure" -> "gpt-3.5-turbo";
            case "gemini" -> "gemini-1.5-pro";
            case "cohere" -> "command";
            case "mistral" -> "mistral-large-latest";
            case "local" -> "llama2";
            case "deepseek" -> "deepseek-1.0";
            default -> "gpt-3.5-turbo";
        };
    }
    
    // Helper method to update endpoint based on provider
    private void updateEndpointForProvider(String provider) {
        String endpoint = switch(provider) {
            case "openai" -> "https://api.openai.com/v1";
            case "anthropic" -> "https://api.anthropic.com/v1";
            case "azure" -> endpointField.getText(); // Keep existing Azure endpoint
            case "gemini" -> "https://generativelanguage.googleapis.com/v1beta";
            case "cohere" -> "https://api.cohere.ai/v1";
            case "mistral" -> "https://api.mistral.ai/v1";
            case "local" -> "http://localhost:8080";
            default -> "https://api.openai.com/v1";
        };
        
        // Only update if the endpoint is empty or if switching providers
        if (endpointField.getText().isEmpty() || !provider.equals(llmConfig.getProvider())) {
            endpointField.setText(endpoint);
        }
    }

    private void saveSettings() {
        llmConfig.setApiKey(apiKeyField.getText());
        llmConfig.setEndpoint(endpointField.getText());
        llmConfig.setModelType(modelTypeButton.getValue());
        llmConfig.setProvider(providerButton.getValue());
        llmConfig.setSetupComplete(true);
    }

    private void useDefaultModel() {
        Path defaultPath = FabricLoader.getInstance().getConfigDir()
                .resolve("villagesreborn/models/basic_model.bin");
        try {
            Files.createDirectories(defaultPath.getParent());
            try (InputStream is = getClass().getResourceAsStream("/models/basic_model.bin");
                 OutputStream os = Files.newOutputStream(defaultPath)) {
                if (is != null) {
                    is.transferTo(os);
                } else {
                    LOGGER.error("Default model resource not found");
                }
            }
            llmConfig.setModelType("basic_model.bin");
            llmConfig.setSetupComplete(true);
            llmConfig.saveConfig();
        } catch (IOException e) {
            LOGGER.error("Failed to copy default model", e);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public void setProgress(float progress) {
        this.progress = MathHelper.clamp(progress, 0.0f, 1.0f);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
