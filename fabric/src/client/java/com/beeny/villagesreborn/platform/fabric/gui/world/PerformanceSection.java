package com.beeny.villagesreborn.platform.fabric.gui.world;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration section for Performance settings
 */
public class PerformanceSection extends VillagesRebornTab.ConfigurationSection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceSection.class);
    
    private CyclingButtonWidget<Boolean> adaptivePerformanceToggle;
    private SliderWidget tickOptimizationSlider;
    
    public PerformanceSection(VillagesRebornWorldSettings settings, Runnable changeListener) {
        super(settings, changeListener);
    }
    
    @Override
    protected void createWidgets() {
        // Adaptive Performance Toggle
        this.adaptivePerformanceToggle = CyclingButtonWidget.onOffBuilder(
            safeTranslatable("villagesreborn.config.on"),
            safeTranslatable("villagesreborn.config.off")
        ).initially(settings.isAdaptivePerformance())
         .build(0, 0, 200, 20,
               safeTranslatable("villagesreborn.config.performance.adaptive_performance"),
               (button, value) -> {
                   settings.setAdaptivePerformance(value);
                   updateOptimizationControlEnabled(!value);
                   notifyChange();
               });
        widgets.add(adaptivePerformanceToggle);
        
        // Tick Optimization Level Slider (0-3)
        this.tickOptimizationSlider = new SliderWidget(
            0, 0, 200, 20,
            safeTranslatable("villagesreborn.config.performance.tick_optimization"),
            normalizeOptimizationLevel(settings.getTickOptimizationLevel())
        ) {
            @Override
            protected void updateMessage() {
                int value = denormalizeOptimizationLevel(this.value);
                String levelText = getOptimizationLevelText(value);
                this.setMessage(safeTranslatable("villagesreborn.config.performance.tick_optimization.level", levelText));
            }
            
            @Override
            protected void applyValue() {
                int value = denormalizeOptimizationLevel(this.value);
                settings.setTickOptimizationLevel(value);
                notifyChange();
            }
        };
        widgets.add(tickOptimizationSlider);
        
        // Set initial enabled state - if adaptive performance is on, manual optimization is disabled
        updateOptimizationControlEnabled(!settings.isAdaptivePerformance());
    }
    
    @Override
    public void updateFromSettings() {
        if (adaptivePerformanceToggle != null) {
            adaptivePerformanceToggle.setValue(settings.isAdaptivePerformance());
            updateOptimizationControlEnabled(!settings.isAdaptivePerformance());
        }
        
        // Recreate slider since setValue is not accessible in 1.21.4
        if (tickOptimizationSlider != null) {
            createWidgets();
        }
    }
    
    /**
     * Updates enabled state of manual optimization controls
     * When adaptive performance is enabled, manual controls are disabled
     */
    private void updateOptimizationControlEnabled(boolean enabled) {
        if (tickOptimizationSlider != null) {
            tickOptimizationSlider.active = enabled;
        }
    }
    
    /**
     * Normalizes optimization level value to 0-1 range for slider
     */
    private double normalizeOptimizationLevel(int value) {
        return value / 3.0; // Range: 0-3 -> 0-1
    }
    
    /**
     * Denormalizes slider value back to optimization level range
     */
    private int denormalizeOptimizationLevel(double normalized) {
        return (int) Math.round(normalized * 3);
    }
    
    /**
     * Gets human-readable optimization level text
     */
    private String getOptimizationLevelText(int level) {
        return switch (level) {
            case 0 -> "None (Max Quality)";
            case 1 -> "Low (High Quality)";
            case 2 -> "Medium (Balanced)";
            case 3 -> "High (Max Performance)";
            default -> "Unknown";
        };
    }

    /**
     * Safely gets translatable text, returns fallback for testing
     */
    private static Text safeTranslatable(String key, Object... args) {
        try {
            var text = args.length > 0 ? Text.translatable(key, args) : Text.translatable(key);
            return text != null ? text : Text.literal(key.substring(key.lastIndexOf('.') + 1));
        } catch (Exception e) {
            return Text.literal(key.substring(key.lastIndexOf('.') + 1));
        }
    }
}