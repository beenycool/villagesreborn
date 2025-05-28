package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Welcome screen for first-time setup of Villages Reborn
 * Displays hardware detection results and allows LLM provider/model selection
 */
public class WelcomeScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeScreen.class);
    
    private final HardwareInfoManager hardwareManager;
    private final LLMProviderManager llmManager;
    private final FirstTimeSetupConfig setupConfig;
    
    private HardwareInfo detectedHardware;
    private LLMProvider selectedProvider = LLMProvider.OPENAI;
    private String selectedModel = "gpt-3.5-turbo";
    
    // UI Components
    private CyclingButtonWidget<LLMProvider> providerButton;
    private CyclingButtonWidget<String> modelButton;
    private ButtonWidget saveButton;
    private ButtonWidget skipButton;
    
    // Validation and state
    private final List<String> validationErrors = new ArrayList<>();
    private boolean isProviderValidated = false;
    
    public WelcomeScreen(HardwareInfoManager hardwareManager, LLMProviderManager llmManager, 
                        FirstTimeSetupConfig setupConfig) {
        super(Text.literal("Villages Reborn - Welcome Setup"));
        this.hardwareManager = hardwareManager;
        this.llmManager = llmManager;
        this.setupConfig = setupConfig;
        
        // Detect hardware on creation
        this.detectedHardware = hardwareManager.getHardwareInfo();
        LOGGER.info("Welcome screen initialized with hardware: {}", detectedHardware);
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 60;
        int spacing = 25;
        int currentY = startY;
        
        // Welcome title
        TextWidget titleWidget = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("Welcome to Villages Reborn!").formatted(Formatting.GOLD, Formatting.BOLD),
            this.textRenderer);
        this.addDrawableChild(titleWidget);
        currentY += spacing + 10;
        
        // Hardware detection section
        addHardwareSection(centerX, currentY);
        currentY += spacing * 4;
        
        // LLM Provider selection
        addProviderSection(centerX, currentY);
        currentY += spacing * 3;
        
        // Action buttons
        addActionButtons(centerX, currentY);
        
        // Initialize with validation
        validateConfiguration();
    }
    
    private void addHardwareSection(int centerX, int startY) {
        int currentY = startY;
        
        // Hardware detection header
        TextWidget hardwareHeader = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("Hardware Detection Results").formatted(Formatting.AQUA, Formatting.UNDERLINE),
            this.textRenderer);
        this.addDrawableChild(hardwareHeader);
        currentY += 25;
        
        // RAM info
        TextWidget ramInfo = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("RAM: " + detectedHardware.getRamGB() + " GB"),
            this.textRenderer);
        this.addDrawableChild(ramInfo);
        currentY += 20;
        
        // CPU info  
        TextWidget cpuInfo = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("CPU Cores: " + detectedHardware.getCpuCores()),
            this.textRenderer);
        this.addDrawableChild(cpuInfo);
        currentY += 20;
        
        // AVX2 support
        String avx2Status = detectedHardware.hasAvx2Support() ? "Supported" : "Not Supported";
        Formatting avx2Color = detectedHardware.hasAvx2Support() ? Formatting.GREEN : Formatting.YELLOW;
        TextWidget avx2Info = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("AVX2: " + avx2Status).formatted(avx2Color),
            this.textRenderer);
        this.addDrawableChild(avx2Info);
        currentY += 20;
        
        // Hardware tier
        HardwareTier tier = detectedHardware.getHardwareTier();
        Formatting tierColor = switch (tier) {
            case HIGH -> Formatting.GREEN;
            case MEDIUM -> Formatting.YELLOW;
            case LOW -> Formatting.RED;
            case UNKNOWN -> Formatting.GRAY;
        };
        TextWidget tierInfo = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("Performance Tier: " + tier.name()).formatted(tierColor, Formatting.BOLD),
            this.textRenderer);
        this.addDrawableChild(tierInfo);
    }
    
    private void addProviderSection(int centerX, int startY) {
        int currentY = startY;
        
        // Provider selection header
        TextWidget providerHeader = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("LLM Provider Configuration").formatted(Formatting.LIGHT_PURPLE, Formatting.UNDERLINE),
            this.textRenderer);
        this.addDrawableChild(providerHeader);
        currentY += 25;
        
        // Provider selection button
        List<LLMProvider> providers = Arrays.asList(LLMProvider.values());
        this.providerButton = CyclingButtonWidget.<LLMProvider>builder(provider -> Text.literal("Provider: " + provider.getDisplayName()))
            .values(providers)
            .initially(selectedProvider)
            .build(centerX - 100, currentY, 200, 20, Text.literal("LLM Provider"), (button, provider) -> {
                this.selectedProvider = provider;
                updateModelOptions();
                validateConfiguration();
                LOGGER.debug("Selected provider: {}", provider);
            });
        this.addDrawableChild(providerButton);
        currentY += 25;
        
        // Model selection button (will be populated based on provider)
        this.modelButton = CyclingButtonWidget.<String>builder(model -> Text.literal("Model: " + model))
            .values(getRecommendedModels())
            .initially(selectedModel)
            .build(centerX - 100, currentY, 200, 20, Text.literal("Model"), (button, model) -> {
                this.selectedModel = model;
                validateConfiguration();
                LOGGER.debug("Selected model: {}", model);
            });
        this.addDrawableChild(modelButton);
        currentY += 25;
        
        // Model recommendation note
        TextWidget recommendationNote = new TextWidget(centerX - 150, currentY, 300, 20,
            Text.literal("Models recommended for your hardware tier").formatted(Formatting.GRAY, Formatting.ITALIC),
            this.textRenderer);
        this.addDrawableChild(recommendationNote);
    }
    
    private void addActionButtons(int centerX, int startY) {
        // Save and continue button
        this.saveButton = ButtonWidget.builder(Text.literal("Save & Continue"), button -> {
            saveConfiguration();
        })
        .position(centerX - 110, startY)
        .size(100, 20)
        .build();
        this.addDrawableChild(saveButton);
        
        // Skip setup button
        this.skipButton = ButtonWidget.builder(Text.literal("Skip Setup"), button -> {
            skipSetup();
        })
        .position(centerX + 10, startY)
        .size(100, 20)
        .build();
        this.addDrawableChild(skipButton);
    }
    
    private List<String> getRecommendedModels() {
        return llmManager.getRecommendedModels(selectedProvider, detectedHardware.getHardwareTier());
    }
    
    private void updateModelOptions() {
        List<String> models = getRecommendedModels();
        if (!models.isEmpty()) {
            selectedModel = models.get(0); // Select first recommended model
        }
        
        // Remove and re-add the model button with updated options
        this.remove(modelButton);
        int centerX = this.width / 2;
        int modelButtonY = 60 + 25 * 8; // Calculate Y position
        
        this.modelButton = CyclingButtonWidget.<String>builder(model -> Text.literal("Model: " + model))
            .values(models)
            .initially(selectedModel)
            .build(centerX - 100, modelButtonY, 200, 20, Text.literal("Model"), (button, model) -> {
                this.selectedModel = model;
                validateConfiguration();
                LOGGER.debug("Selected model: {}", model);
            });
        this.addDrawableChild(modelButton);
    }
    
    private void validateConfiguration() {
        validationErrors.clear();
        
        // Validate provider selection
        if (selectedProvider == null) {
            validationErrors.add("Provider must be selected");
        }
        
        // Validate model selection
        if (selectedModel == null || selectedModel.isEmpty()) {
            validationErrors.add("Model must be selected");
        }
        
        // Hardware compatibility check
        if (selectedProvider != null && selectedModel != null && detectedHardware != null) {
            var compatibilityResult = llmManager.validateProviderCompatibility(selectedProvider, detectedHardware.getHardwareTier());
            if (compatibilityResult.getType() == LLMProviderManager.ValidationResult.Type.WARNING) {
                // Don't add as error, just log warning
                LOGGER.warn("Provider compatibility warning: {}", compatibilityResult.getMessage());
            }
        }
        
        updateUIValidationState();
    }
    
    private void updateUIValidationState() {
        boolean isValid = validationErrors.isEmpty();
        
        // Only update UI components if they've been initialized (not in tests)
        if (saveButton != null) {
            saveButton.active = isValid;
        }
        
        if (!isValid) {
            LOGGER.debug("Validation errors: {}", validationErrors);
        }
    }
    
    private void saveConfiguration() {
        if (!validationErrors.isEmpty()) {
            LOGGER.warn("Cannot save configuration with validation errors: {}", validationErrors);
            return;
        }
        
        try {
            setupConfig.completeSetup(selectedProvider, selectedModel);
            LOGGER.info("Setup completed with provider: {}, model: {}", selectedProvider, selectedModel);
            closeScreen();
        } catch (Exception e) {
            LOGGER.error("Failed to save configuration", e);
            // In a real implementation, we'd show an error dialog to the user
        }
    }
    
    private void skipSetup() {
        LOGGER.info("User skipped first-time setup");
        closeScreen();
    }
    
    private void closeScreen() {
        MinecraftClient.getInstance().setScreen(null);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render dark background
        context.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean shouldPause() {
        return false; // Don't pause the game
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Prevent accidental closure
    }
    
    // Getter methods for testing
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    public List<String> getAvailableModels() {
        return getRecommendedModels();
    }
    
    public String getSelectedModel() {
        return selectedModel;
    }
    
    public LLMProvider getSelectedProvider() {
        return selectedProvider;
    }
    
    public ButtonWidget getContinueButton() {
        return saveButton; // May be null in tests - that's expected
    }
    
    public HardwareInfo getHardwareInfo() {
        return detectedHardware;
    }
    
    // Setter methods for testing
    public void setSelectedProvider(LLMProvider provider) {
        this.selectedProvider = provider;
        validateConfiguration();
    }
    
    public void setSelectedModel(String model) {
        this.selectedModel = model;
        validateConfiguration();
    }
}