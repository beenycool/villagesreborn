package com.beeny.villagesreborn.platform.fabric.gui.world;

import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Custom widget for Villages Reborn world creation settings
 * Integrates into Minecraft's Create World screen via mixin injection
 */
public class VillagesRebornTab extends ScrollableWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornTab.class);
    
    // UI scaling helpers
    private int getScaledSectionSpacing() {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return (int) Math.max(20, 25 / guiScale * 2);
    }
    
    private int getScaledWidgetHeight() {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return (int) Math.max(16, 20 / guiScale * 2);
    }
    
    private int getScaledWidgetWidth() {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return Math.min(200, (int) Math.max(150, 200 / guiScale * 2));
    }
    
    private int getScaledSliderWidth() {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return Math.min(180, (int) Math.max(120, 180 / guiScale * 2));
    }
    
    private int getScaledLabelHeight() {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return (int) Math.max(12, 15 / guiScale * 2);
    }
    
    // Configuration
    private VillagesRebornWorldSettings settings;
    private final List<ClickableWidget> configWidgets;
    private final List<ClickableWidget> allWidgets;
    
    // UI Sections
    private VillagerAISection aiSection;
    private VillageExpansionSection expansionSection;
    private FeatureToggleSection featureSection;
    private PerformanceSection performanceSection;
    
    public VillagesRebornTab(int width, int height) {
        // Use safe text creation for testing
        super(0, 0, width, height, safeTranslatable("villagesreborn.world_creation.tab_content"));
        
        this.configWidgets = new ArrayList<>();
        this.allWidgets = new ArrayList<>();
        
        // Initialize settings with hardware-based defaults
        try {
            var hardwareInfo = HardwareInfoManager.getInstance().getHardwareInfo();
            this.settings = VillagesRebornWorldSettings.createDefaults(hardwareInfo);
            LOGGER.info("Initialized world creation settings for hardware tier: {}", hardwareInfo.getHardwareTier());
        } catch (Exception e) {
            LOGGER.error("Failed to get hardware info, using basic defaults", e);
            this.settings = new VillagesRebornWorldSettings();
        }
        
        initializeUI();
    }
    
    /**
     * Initializes the UI layout with all configuration sections
     */
    private void initializeUI() {
        int yPos = 10;
        
        // Get text renderer safely for testing
        var client = MinecraftClient.getInstance();
        var textRenderer = client != null ? client.textRenderer : null;
        
        // Add title
        if (textRenderer != null) {
            var titleText = safeTranslatableFormatted("villagesreborn.world_creation.title", Formatting.GOLD, Formatting.BOLD);
            TextWidget titleWidget = new TextWidget(
                0, yPos, getScaledWidgetWidth(), getScaledLabelHeight(),
                titleText,
                textRenderer
            );
            allWidgets.add(titleWidget);
        }
        yPos += getScaledLabelHeight() + 10;
        
        // Initialize sections
        this.aiSection = new VillagerAISection(settings, this::onSettingChanged);
        this.expansionSection = new VillageExpansionSection(settings, this::onSettingChanged);
        this.featureSection = new FeatureToggleSection(settings, this::onSettingChanged);
        this.performanceSection = new PerformanceSection(settings, this::onSettingChanged);
        
        // Add sections to layout
        yPos = addSection(yPos, "AI Configuration", aiSection, textRenderer);
        yPos = addSection(yPos, "Village Expansion", expansionSection, textRenderer);
        yPos = addSection(yPos, "Feature Toggles", featureSection, textRenderer);
        yPos = addSection(yPos, "Performance", performanceSection, textRenderer);
        
        // Add reset button
        yPos += 15;
        var resetText = safeTranslatable("villagesreborn.world_creation.reset_defaults");
        if (resetText != null) {
            ButtonWidget resetButton = ButtonWidget.builder(
                resetText,
                button -> resetToDefaults()
            ).dimensions(0, yPos, getScaledWidgetWidth(), getScaledWidgetHeight()).build();
            allWidgets.add(resetButton);
            configWidgets.add(resetButton);
        }
    }
    
    /**
     * Safely gets translatable text, returns fallback for testing
     */
    private static Text safeTranslatable(String key) {
        try {
            // Simply use Text.translatable - let Minecraft handle missing translations
            return Text.translatable(key);
        } catch (Exception e) {
            LOGGER.warn("Failed to translate key '{}': {}", key, e.getMessage());
            return Text.literal(key.substring(key.lastIndexOf('.') + 1));
        }
    }

    /**
     * Safely gets translatable text with formatting, returns fallback for testing
     */
    private static Text safeTranslatableFormatted(String key, Formatting... formatting) {
        try {
            // Simply use Text.translatable with formatting - let Minecraft handle missing translations
            return Text.translatable(key).formatted(formatting);
        } catch (Exception e) {
            LOGGER.warn("Failed to translate key '{}': {}", key, e.getMessage());
            return Text.literal(key.substring(key.lastIndexOf('.') + 1)).formatted(formatting);
        }
    }
    
    /**
     * Adds a configuration section with header
     */
    private int addSection(int yPos, String titleKey, ConfigurationSection section, net.minecraft.client.font.TextRenderer textRenderer) {
        // Section header
        if (textRenderer != null) {
            var sectionText = safeTranslatableFormatted("villagesreborn.world_creation.section." + titleKey.toLowerCase().replace(" ", "_"), Formatting.AQUA, Formatting.UNDERLINE);
            if (sectionText != null) {
                TextWidget sectionHeader = new TextWidget(
                    0, yPos, getScaledWidgetWidth(), getScaledLabelHeight(),
                    sectionText,
                    textRenderer
                );
                allWidgets.add(sectionHeader);
            }
        }
        yPos += getScaledLabelHeight() + 5;
        
        // Section widgets
        for (ClickableWidget widget : section.getWidgets()) {
            widget.setY(yPos);
            allWidgets.add(widget);
            configWidgets.add(widget);
            yPos += getScaledWidgetHeight() + 5;
        }
        
        return yPos + getScaledSectionSpacing();
    }
    
    /**
     * Called when any setting is changed
     */
    private void onSettingChanged() {
        // Validate settings after each change
        settings.validate();
        LOGGER.debug("Settings updated: {}", settings);
    }
    
    /**
     * Resets all settings to hardware-based defaults
     */
    private void resetToDefaults() {
        try {
            var hardwareInfo = HardwareInfoManager.getInstance().getHardwareInfo();
            var defaultSettings = VillagesRebornWorldSettings.createDefaults(hardwareInfo);
            
            // Copy values from defaults
            copySettingsFrom(defaultSettings);
            
            // Update UI widgets
            aiSection.updateFromSettings();
            expansionSection.updateFromSettings();
            featureSection.updateFromSettings();
            performanceSection.updateFromSettings();
            
            LOGGER.info("Reset settings to defaults for hardware tier: {}", hardwareInfo.getHardwareTier());
            
        } catch (Exception e) {
            LOGGER.error("Failed to reset to defaults", e);
        }
    }
    
    /**
     * Copies settings from another settings object
     */
    private void copySettingsFrom(VillagesRebornWorldSettings other) {
        settings.setVillagerMemoryLimit(other.getVillagerMemoryLimit());
        settings.setAiAggressionLevel(other.getAiAggressionLevel());
        settings.setEnableAdvancedAI(other.isEnableAdvancedAI());
        settings.setAutoExpansionEnabled(other.isAutoExpansionEnabled());
        settings.setMaxVillageSize(other.getMaxVillageSize());
        settings.setExpansionRate(other.getExpansionRate());
        // Phase 2 fields
        settings.setBiomeSpecificExpansion(other.isBiomeSpecificExpansion());
        settings.setMaxCaravanDistance(other.getMaxCaravanDistance());
        settings.setInterdimensionalVillages(other.isInterdimensionalVillages());
        settings.setVillageGenerationDensity(other.getVillageGenerationDensity());
        settings.setElectionsEnabled(other.isElectionsEnabled());
        settings.setAssistantVillagersEnabled(other.isAssistantVillagersEnabled());
        settings.setDynamicTradingEnabled(other.isDynamicTradingEnabled());
        settings.setVillagerRelationships(other.isVillagerRelationships());
        settings.setAdaptivePerformance(other.isAdaptivePerformance());
        settings.setTickOptimizationLevel(other.getTickOptimizationLevel());
    }
    
    /**
     * Gets the current settings
     */
    public VillagesRebornWorldSettings getSettings() {
        return settings;
    }
    
    /**
     * Called when the tab is closed
     */
    public void onClose() {
        LOGGER.debug("Villages Reborn tab closed with settings: {}", settings);
    }
    
    // ScrollableWidget implementation for 1.21.4
    @Override
    public double getDeltaYPerScroll() {
        return 20.0; // Scroll 20 pixels per scroll step
    }
    
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render all widgets
        for (ClickableWidget widget : allWidgets) {
            if (widget.visible) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }
    
    public List<? extends Element> children() {
        return allWidgets;
    }
    
    public List<? extends Selectable> selectables() {
        return configWidgets;
    }
    
    @Override
    protected int getContentsHeightWithPadding() {
        // Calculate total height of all widgets plus spacing
        int totalHeight = 20; // Top padding
        totalHeight += allWidgets.size() * (getScaledWidgetHeight() + 5); // Widgets with spacing
        totalHeight += 4 * getScaledSectionSpacing(); // Section spacing
        totalHeight += 50; // Bottom padding
        return totalHeight;
    }
    
    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, 
                   Text.translatable("villagesreborn.world_creation.narration"));
    }

    /**
     * Abstract base class for configuration sections
     */
    public abstract static class ConfigurationSection {
        protected final VillagesRebornWorldSettings settings;
        protected final Runnable changeListener;
        protected final List<ClickableWidget> widgets = new ArrayList<>();
        
        public ConfigurationSection(VillagesRebornWorldSettings settings, Runnable changeListener) {
            this.settings = settings;
            this.changeListener = changeListener;
            createWidgets();
        }
        
        protected abstract void createWidgets();
        public abstract void updateFromSettings();
        
        public List<ClickableWidget> getWidgets() {
            return widgets;
        }
        
        protected void notifyChange() {
            if (changeListener != null) {
                changeListener.run();
            }
        }
    }
}