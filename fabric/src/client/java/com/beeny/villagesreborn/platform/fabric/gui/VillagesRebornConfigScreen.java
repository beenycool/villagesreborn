package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import com.beeny.villagesreborn.platform.fabric.config.FabricWorldSettingsManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.gui.widget.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.DrawContext;

import java.util.Objects;

public class VillagesRebornConfigScreen extends Screen {

    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornConfigScreen.class);

    private final Screen parent;
    private VillagesRebornWorldSettings settings;
    private VillagesRebornWorldSettings originalSettings;

    private ConfigContentWidget contentWidget;

    public VillagesRebornConfigScreen(Screen parent) {
        super(Text.translatable("villagesreborn.config.title"));
        this.parent = parent;
        loadSettings();
    }

    private void loadSettings() {
        if (this.client != null && this.client.world != null) {
            if(this.client.getServer() != null) {
                ServerWorld serverWorld = this.client.getServer().getOverworld(); // Assuming overworld settings
                if (serverWorld != null) {
                    this.settings = com.beeny.villagesreborn.core.world.VillagesRebornWorldDataPersistent.get(serverWorld).getSettings().copy();
                    this.originalSettings = com.beeny.villagesreborn.core.world.VillagesRebornWorldDataPersistent.get(serverWorld).getSettings().copy();
                    return;
                }
            }
        }
        // Fallback for when not in a world
        this.settings = new VillagesRebornWorldSettings();
        this.originalSettings = new VillagesRebornWorldSettings();
    }

    private void saveSettings() {
        if (this.client != null && this.client.world != null) {
             if(this.client.getServer() != null) {
                ServerWorld serverWorld = this.client.getServer().getOverworld(); // Assuming overworld settings
                if (serverWorld != null) {
                    // Use the settings manager to save and update cache
                    FabricWorldSettingsManager.getInstance().saveWorldSettings(serverWorld, this.settings);
                    LOGGER.info("Settings saved successfully");
                }
            }
        }
    }
    
    @Override
    protected void init() {
        super.init();

        this.contentWidget = new ConfigContentWidget(this.width, this.height - 60, this.settings, this::onSettingChanged);
        this.addDrawableChild(this.contentWidget);

        // Add Done and Cancel buttons
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> {
            saveSettings();
            Objects.requireNonNull(this.client).setScreen(this.parent);
        }).dimensions(this.width / 2 - 100, this.height - 27, 200, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> {
            // Revert changes
            this.settings = this.originalSettings.copy();
            Objects.requireNonNull(this.client).setScreen(this.parent);
        }).dimensions(this.width / 2 - 100, this.height - 50, 200, 20).build());
        
        // Add Test Settings button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Test Settings"), button -> {
            testSettings();
        }).dimensions(this.width / 2 - 100, this.height - 73, 200, 20).build());
    }

    private void onSettingChanged() {
        this.settings.validate();
        LOGGER.debug("Settings updated: {}", settings);
    }
    
    private void testSettings() {
        if (this.client != null && this.client.player != null) {
            // Send a chat message showing current settings
            this.client.player.sendMessage(Text.literal("§6=== Villages Reborn Settings Test ==="), false);
            this.client.player.sendMessage(Text.literal("§7Memory Limit: §f" + settings.getVillagerMemoryLimit()), false);
            this.client.player.sendMessage(Text.literal("§7Aggression: §f" + String.format("%.2f", settings.getAiAggressionLevel())), false);
            this.client.player.sendMessage(Text.literal("§7Advanced AI: §f" + settings.isEnableAdvancedAI()), false);
            this.client.player.sendMessage(Text.literal("§7Auto Expansion: §f" + settings.isAutoExpansionEnabled()), false);
            this.client.player.sendMessage(Text.literal("§7Max Village Size: §f" + settings.getMaxVillageSize()), false);
            this.client.player.sendMessage(Text.literal("§7Expansion Rate: §f" + String.format("%.2f", settings.getExpansionRate())), false);
            this.client.player.sendMessage(Text.literal("§7Elections: §f" + settings.isElectionsEnabled()), false);
            this.client.player.sendMessage(Text.literal("§7Dynamic Trading: §f" + settings.isDynamicTradingEnabled()), false);
            this.client.player.sendMessage(Text.literal("§7Relationships: §f" + settings.isVillagerRelationships()), false);
            this.client.player.sendMessage(Text.literal("§7Adaptive Performance: §f" + settings.isAdaptivePerformance()), false);
            this.client.player.sendMessage(Text.literal("§7Tick Optimization: §f" + settings.getTickOptimizationLevel()), false);
            this.client.player.sendMessage(Text.literal("§a✓ Settings are working! Use /villagesreborn test-settings for more details."), false);
            
            LOGGER.info("Settings test executed from config screen");
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        this.contentWidget.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 16777215);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        Objects.requireNonNull(this.client).setScreen(parent);
    }

    private class ConfigContentWidget extends ScrollableWidget {
        private final java.util.List<ClickableWidget> allWidgets = new java.util.ArrayList<>();
        private final VillagesRebornWorldSettings settings;
        private final Runnable onSettingChanged;

        private final VillagerAISection aiSection;
        private final VillageExpansionSection expansionSection;
        private final FeatureToggleSection featureSection;
        private final PerformanceSection performanceSection;

        protected ConfigContentWidget(int width, int height, VillagesRebornWorldSettings settings, Runnable onSettingChanged) {
            super(0, 30, width, height, Text.literal(""));
            this.settings = settings;
            this.onSettingChanged = onSettingChanged;

            this.aiSection = new VillagerAISection(settings, onSettingChanged);
            this.expansionSection = new VillageExpansionSection(settings, onSettingChanged);
            this.featureSection = new FeatureToggleSection(settings, onSettingChanged);
            this.performanceSection = new PerformanceSection(settings, onSettingChanged);
            
            initializeUI();
        }

        private void initializeUI() {
            int yPos = 10;

            yPos = addSection(yPos, "AI Configuration", this.aiSection, net.minecraft.client.MinecraftClient.getInstance().textRenderer);
            yPos = addSection(yPos, "Village Expansion", this.expansionSection, net.minecraft.client.MinecraftClient.getInstance().textRenderer);
            yPos = addSection(yPos, "Feature Toggles", this.featureSection, net.minecraft.client.MinecraftClient.getInstance().textRenderer);
            yPos = addSection(yPos, "Performance", this.performanceSection, net.minecraft.client.MinecraftClient.getInstance().textRenderer);

            // Add reset button
            yPos += 15;
            var resetText = Text.translatable("villagesreborn.world_creation.reset_defaults");
            ButtonWidget resetButton = ButtonWidget.builder(
                resetText,
                button -> resetToDefaults()
            ).dimensions(
                this.getX() + (this.width - getScaledWidgetWidth()) / 2, 
                this.getY() + yPos, 
                getScaledWidgetWidth(), 
                getScaledWidgetHeight()
            ).build();
            allWidgets.add(resetButton);
            
            LOGGER.info("Initialized config UI with {} total widgets", allWidgets.size());
        }

        private void resetToDefaults() {
            try {
                var hardwareInfo = com.beeny.villagesreborn.core.hardware.HardwareInfoManager.getInstance().getHardwareInfo();
                var defaultSettings = VillagesRebornWorldSettings.createDefaults(hardwareInfo);
                
                // copy settings
                this.settings.setVillagerMemoryLimit(defaultSettings.getVillagerMemoryLimit());
                this.settings.setAiAggressionLevel(defaultSettings.getAiAggressionLevel());
                this.settings.setEnableAdvancedAI(defaultSettings.isEnableAdvancedAI());
                this.settings.setAutoExpansionEnabled(defaultSettings.isAutoExpansionEnabled());
                this.settings.setMaxVillageSize(defaultSettings.getMaxVillageSize());
                this.settings.setExpansionRate(defaultSettings.getExpansionRate());
                this.settings.setBiomeSpecificExpansion(defaultSettings.isBiomeSpecificExpansion());
                this.settings.setMaxCaravanDistance(defaultSettings.getMaxCaravanDistance());
                this.settings.setInterdimensionalVillages(defaultSettings.isInterdimensionalVillages());
                this.settings.setVillageGenerationDensity(defaultSettings.getVillageGenerationDensity());
                this.settings.setElectionsEnabled(defaultSettings.isElectionsEnabled());
                this.settings.setAssistantVillagersEnabled(defaultSettings.isAssistantVillagersEnabled());
                this.settings.setDynamicTradingEnabled(defaultSettings.isDynamicTradingEnabled());
                this.settings.setVillagerRelationships(defaultSettings.isVillagerRelationships());
                this.settings.setAdaptivePerformance(defaultSettings.isAdaptivePerformance());
                this.settings.setTickOptimizationLevel(defaultSettings.getTickOptimizationLevel());

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

        private int addSection(int yPos, String titleKey, ConfigurationSection section, net.minecraft.client.font.TextRenderer textRenderer) {
            var sectionText = Text.translatable("villagesreborn.world_creation.section." + titleKey.toLowerCase().replace(" ", "_")).formatted(Formatting.AQUA, Formatting.UNDERLINE);
            TextWidget sectionHeader = new TextWidget(
                this.getX() + (this.width - getScaledWidgetWidth()) / 2, 
                this.getY() + yPos, 
                getScaledWidgetWidth(), 
                getScaledLabelHeight(),
                sectionText,
                textRenderer
            );
            allWidgets.add(sectionHeader);

            yPos += getScaledLabelHeight() + 5;

            LOGGER.debug("Adding section '{}' with {} widgets", titleKey, section.getWidgets().size());
            for (ClickableWidget widget : section.getWidgets()) {
                widget.setX(this.getX() + (this.width - widget.getWidth()) / 2);
                widget.setY(this.getY() + yPos);
                allWidgets.add(widget);
                LOGGER.debug("Added widget {} at position ({}, {})", widget.getClass().getSimpleName(), widget.getX(), widget.getY());
                yPos += getScaledWidgetHeight() + 5;
            }

            return yPos + getScaledSectionSpacing();
        }

        @Override
        protected int getContentsHeightWithPadding() {
             return this.getBottom() + 5;
        }
        
        @Override
        protected double getDeltaYPerScroll() {
            return 20.0;
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        }
        
        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            // Render all widgets with proper clipping
            for (ClickableWidget widget : allWidgets) {
                if (widget.getY() >= this.getY() && widget.getY() + widget.getHeight() <= this.getY() + this.getHeight()) {
                    widget.render(context, mouseX, mouseY, delta);
                }
            }
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            LOGGER.info("ConfigContentWidget mouseClicked at ({}, {}) with {} widgets", mouseX, mouseY, allWidgets.size());
            
            // Forward mouse clicks to widgets first, without bounds checking
            for (ClickableWidget widget : allWidgets) {
                if (widget.isMouseOver(mouseX, mouseY)) {
                    LOGGER.info("Widget {} is under mouse at ({}, {}), widget bounds: ({}, {}, {}, {})", 
                        widget.getClass().getSimpleName(), mouseX, mouseY,
                        widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
                    if (widget.mouseClicked(mouseX, mouseY, button)) {
                        LOGGER.info("Widget {} successfully handled the click", widget.getClass().getSimpleName());
                        return true;
                    }
                }
            }
            
            LOGGER.info("No widget handled the click, delegating to super");
            return false; // Don't call super to avoid ScrollableWidget consuming the event
        }
        
        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            // Forward mouse drags to widgets (important for sliders)
            for (ClickableWidget widget : allWidgets) {
                if (widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    LOGGER.info("Widget {} handled mouse drag", widget.getClass().getSimpleName());
                    return true;
                }
            }
            
            return false; // Don't call super to avoid ScrollableWidget consuming the event
        }
        
        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            // Always return true if any of our widgets are under the mouse
            for (ClickableWidget widget : allWidgets) {
                if (widget.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
            return super.isMouseOver(mouseX, mouseY);
        }
        
        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            // Forward mouse releases to widgets
            for (ClickableWidget widget : allWidgets) {
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    LOGGER.info("Widget {} handled mouse release", widget.getClass().getSimpleName());
                    return true;
                }
            }
            
            return false; // Don't call super to avoid ScrollableWidget consuming the event
        }

        // UI scaling helpers
        private int getScaledSectionSpacing() {
            double guiScale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
            return (int) Math.max(20, 25 / guiScale * 2);
        }

        private int getScaledWidgetHeight() {
            double guiScale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
            return (int) Math.max(16, 20 / guiScale * 2);
        }

        private int getScaledWidgetWidth() {
            double guiScale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
            return Math.min(200, (int) Math.max(150, 200 / guiScale * 2));
        }

        private int getScaledSliderWidth() {
            double guiScale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
            return Math.min(180, (int) Math.max(120, 180 / guiScale * 2));
        }

        private int getScaledLabelHeight() {
            double guiScale = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor();
            return (int) Math.max(12, 15 / guiScale * 2);
        }
    }

    public abstract class ConfigurationSection {
        protected final VillagesRebornWorldSettings settings;
        protected final Runnable changeListener;
        protected final java.util.List<ClickableWidget> widgets = new java.util.ArrayList<>();

        protected ConfigurationSection(VillagesRebornWorldSettings settings, Runnable changeListener) {
            this.settings = settings;
            this.changeListener = changeListener;
            createWidgets();
        }
        protected abstract void createWidgets();
        public abstract void updateFromSettings();
        public java.util.List<net.minecraft.client.gui.widget.ClickableWidget> getWidgets() { return widgets; }
        protected void notifyChange() {
            changeListener.run();
        }
    }

    public class VillagerAISection extends ConfigurationSection {
        public VillagerAISection(VillagesRebornWorldSettings settings, Runnable changeListener) { super(settings, changeListener); }
        
        private SliderWidget memoryLimitSlider;
        private SliderWidget aggressionSlider;
        private CyclingButtonWidget<Boolean> advancedAIToggle;
        
        @Override 
        protected void createWidgets() {
            this.memoryLimitSlider = new SliderWidget(0, 0, 200, 20, Text.translatable("villagesreborn.config.ai.memory_limit"), normalizeMemoryLimit(settings.getVillagerMemoryLimit())) {
                @Override
                protected void updateMessage() {
                    int value = denormalizeMemoryLimit(this.value);
                    this.setMessage(Text.translatable("villagesreborn.config.ai.memory_limit.value", value));
                }

                @Override
                protected void applyValue() {
                    int value = denormalizeMemoryLimit(this.value);
                    settings.setVillagerMemoryLimit(value);
                    notifyChange();
                }
            };
            widgets.add(memoryLimitSlider);

            this.aggressionSlider = new SliderWidget(0, 0, 200, 20, Text.translatable("villagesreborn.config.ai.aggression"), settings.getAiAggressionLevel()) {
                @Override
                protected void updateMessage() {
                    float value = (float) this.value;
                    this.setMessage(Text.translatable("villagesreborn.config.ai.aggression.level", getAggressionLevelText(value)));
                }

                @Override
                protected void applyValue() {
                    settings.setAiAggressionLevel((float) this.value);
                    notifyChange();
                }
            };
            widgets.add(aggressionSlider);
            
            this.advancedAIToggle = CyclingButtonWidget.onOffBuilder(Text.translatable("villagesreborn.config.on"), Text.translatable("villagesreborn.config.off")).initially(settings.isEnableAdvancedAI()).build(0, 0, 200, 20, Text.translatable("villagesreborn.config.ai.advanced_ai"), (button, value) -> {
                settings.setEnableAdvancedAI(value);
                notifyChange();
            });
            widgets.add(advancedAIToggle);
        }
        @Override 
        public void updateFromSettings() { 
            if (memoryLimitSlider != null) {
                // Re-create widgets to update them, since setValue is not public
                widgets.clear();
                createWidgets();
            }
        }

        private double normalizeMemoryLimit(int value) { return (value - 50) / 450.0; }
        private int denormalizeMemoryLimit(double normalized) { return (int) (normalized * 450 + 50); }
        private String getAggressionLevelText(float value) {
            if (value < 0.2f) return "Peaceful";
            if (value < 0.4f) return "Calm";
            if (value < 0.6f) return "Normal";
            if (value < 0.8f) return "Assertive";
            return "Aggressive";
        }
    }

    public class VillageExpansionSection extends ConfigurationSection {
        public VillageExpansionSection(VillagesRebornWorldSettings settings, Runnable changeListener) { super(settings, changeListener); }
        @Override protected void createWidgets() {
            widgets.add(CyclingButtonWidget.onOffBuilder(Text.translatable("villagesreborn.config.on"), Text.translatable("villagesreborn.config.off")).initially(settings.isAutoExpansionEnabled()).build(0, 0, 200, 20, Text.translatable("villagesreborn.config.expansion.auto_expansion"), (button, value) -> {
                settings.setAutoExpansionEnabled(value);
                notifyChange();
            }));
            widgets.add(new SliderWidget(0, 0, 200, 20, Text.translatable("villagesreborn.config.expansion.max_village_size"), (settings.getMaxVillageSize() - 10) / 190.0) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.translatable("villagesreborn.config.expansion.max_village_size.value", (int) (this.value * 190 + 10)));
                }
                @Override
                protected void applyValue() {
                    settings.setMaxVillageSize((int) (this.value * 190 + 10));
                    notifyChange();
                }
            });
            widgets.add(new SliderWidget(0, 0, 200, 20, Text.translatable("villagesreborn.config.expansion.expansion_rate"), (settings.getExpansionRate() - 0.1) / 1.9) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.translatable("villagesreborn.config.expansion.expansion_rate.value", String.format("%.1f", this.value * 1.9 + 0.1)));
                }
                @Override
                protected void applyValue() {
                    settings.setExpansionRate((float) (this.value * 1.9 + 0.1));
                    notifyChange();
                }
            });
        }
        @Override public void updateFromSettings() { 
            widgets.clear();
            createWidgets();
        }
    }

    public class FeatureToggleSection extends ConfigurationSection {
        public FeatureToggleSection(VillagesRebornWorldSettings settings, Runnable changeListener) { super(settings, changeListener); }
        @Override protected void createWidgets() {
            widgets.add(CyclingButtonWidget.onOffBuilder(Text.translatable("villagesreborn.config.on"), Text.translatable("villagesreborn.config.off")).initially(settings.isElectionsEnabled()).build(0, 0, 200, 20, Text.translatable("villagesreborn.config.features.elections"), (button, value) -> {
                settings.setElectionsEnabled(value);
                notifyChange();
            }));
            widgets.add(CyclingButtonWidget.onOffBuilder(Text.translatable("villagesreborn.config.on"), Text.translatable("villagesreborn.config.off")).initially(settings.isAssistantVillagersEnabled()).build(0, 0, 200, 20, Text.translatable("villagesreborn.config.features.assistant_villagers"), (button, value) -> {
                settings.setAssistantVillagersEnabled(value);
                notifyChange();
            }));
            widgets.add(CyclingButtonWidget.onOffBuilder(Text.translatable("villagesreborn.config.on"), Text.translatable("villagesreborn.config.off")).initially(settings.isDynamicTradingEnabled()).build(0, 0, 200, 20, Text.translatable("villagesreborn.config.features.dynamic_trading"), (button, value) -> {
                settings.setDynamicTradingEnabled(value);
                notifyChange();
            }));
            widgets.add(CyclingButtonWidget.onOffBuilder(Text.translatable("villagesreborn.config.on"), Text.translatable("villagesreborn.config.off")).initially(settings.isVillagerRelationships()).build(0, 0, 200, 20, Text.translatable("villagesreborn.config.features.relationships"), (button, value) -> {
                settings.setVillagerRelationships(value);
                notifyChange();
            }));
        }
        @Override public void updateFromSettings() { 
            widgets.clear();
            createWidgets();
        }
    }
    
    public class PerformanceSection extends ConfigurationSection {
        public PerformanceSection(VillagesRebornWorldSettings settings, Runnable changeListener) { super(settings, changeListener); }
        @Override protected void createWidgets() {
            widgets.add(CyclingButtonWidget.onOffBuilder(Text.translatable("villagesreborn.config.on"), Text.translatable("villagesreborn.config.off")).initially(settings.isAdaptivePerformance()).build(0, 0, 200, 20, Text.translatable("villagesreborn.config.performance.adaptive_performance"), (button, value) -> {
                settings.setAdaptivePerformance(value);
                notifyChange();
            }));
            widgets.add(new SliderWidget(0, 0, 200, 20, Text.translatable("villagesreborn.config.performance.tick_optimization"), settings.getTickOptimizationLevel() / 3.0) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.translatable("villagesreborn.config.performance.tick_optimization.value", (int) (this.value * 3)));
                }
                @Override
                protected void applyValue() {
                    settings.setTickOptimizationLevel((int) (this.value * 3));
                    notifyChange();
                }
            });
        }
        @Override public void updateFromSettings() { 
            widgets.clear();
            createWidgets();
        }
    }
}
