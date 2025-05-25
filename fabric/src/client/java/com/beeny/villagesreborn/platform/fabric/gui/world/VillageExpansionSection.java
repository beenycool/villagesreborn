package com.beeny.villagesreborn.platform.fabric.gui.world;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration section for Village Expansion settings
 */
public class VillageExpansionSection extends VillagesRebornTab.ConfigurationSection {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillageExpansionSection.class);
    
    private CyclingButtonWidget<Boolean> autoExpansionToggle;
    private SliderWidget maxVillageSizeSlider;
    private SliderWidget expansionRateSlider;
    
    public VillageExpansionSection(VillagesRebornWorldSettings settings, Runnable changeListener) {
        super(settings, changeListener);
    }
    
    @Override
    protected void createWidgets() {
        // Auto Expansion Toggle
        this.autoExpansionToggle = CyclingButtonWidget.onOffBuilder(
            safeTranslatable("villagesreborn.config.on"),
            safeTranslatable("villagesreborn.config.off")
        ).initially(settings.isAutoExpansionEnabled())
         .build(0, 0, 200, 20,
               safeTranslatable("villagesreborn.config.expansion.auto_expansion"),
               (button, value) -> {
                   settings.setAutoExpansionEnabled(value);
                   notifyChange();
               });
        widgets.add(autoExpansionToggle);
        
        // Max Village Size Slider (10-100)
        this.maxVillageSizeSlider = new SliderWidget(
            0, 0, 200, 20,
            safeTranslatable("villagesreborn.config.expansion.max_village_size"),
            normalizeVillageSize(settings.getMaxVillageSize())
        ) {
            @Override
            protected void updateMessage() {
                int value = denormalizeVillageSize(this.value);
                this.setMessage(safeTranslatable("villagesreborn.config.expansion.max_village_size.value", value));
            }
            
            @Override
            protected void applyValue() {
                int value = denormalizeVillageSize(this.value);
                settings.setMaxVillageSize(value);
                notifyChange();
            }
        };
        widgets.add(maxVillageSizeSlider);
        
        // Expansion Rate Slider (0.1-2.0)
        this.expansionRateSlider = new SliderWidget(
            0, 0, 200, 20,
            safeTranslatable("villagesreborn.config.expansion.expansion_rate"),
            normalizeExpansionRate(settings.getExpansionRate())
        ) {
            @Override
            protected void updateMessage() {
                float value = denormalizeExpansionRate(this.value);
                String rateText = getExpansionRateText(value);
                this.setMessage(safeTranslatable("villagesreborn.config.expansion.expansion_rate.level", rateText));
            }
            
            @Override
            protected void applyValue() {
                float value = denormalizeExpansionRate(this.value);
                settings.setExpansionRate(value);
                notifyChange();
            }
        };
        widgets.add(expansionRateSlider);
    }
    
    @Override
    public void updateFromSettings() {
        if (autoExpansionToggle != null) {
            autoExpansionToggle.setValue(settings.isAutoExpansionEnabled());
            updateExpansionControlsEnabled(settings.isAutoExpansionEnabled());
        }
        
        // Recreate sliders since setValue is not accessible in 1.21.4
        if (maxVillageSizeSlider != null || expansionRateSlider != null) {
            createWidgets();
        }
    }
    
    /**
     * Updates enabled state of expansion controls based on auto expansion setting
     */
    private void updateExpansionControlsEnabled(boolean autoExpansionEnabled) {
        if (maxVillageSizeSlider != null) {
            maxVillageSizeSlider.active = autoExpansionEnabled;
        }
        if (expansionRateSlider != null) {
            expansionRateSlider.active = autoExpansionEnabled;
        }
    }
    
    /**
     * Normalizes village size value to 0-1 range for slider
     */
    private double normalizeVillageSize(int value) {
        return (value - 10) / 190.0; // Range: 10-200 -> 0-1
    }
    
    /**
     * Denormalizes slider value back to village size range
     */
    private int denormalizeVillageSize(double normalized) {
        return (int) (normalized * 190 + 10);
    }
    
    /**
     * Normalizes expansion rate value to 0-1 range for slider
     */
    private double normalizeExpansionRate(float value) {
        return (value - 0.1f) / 1.9f; // Range: 0.1-2.0 -> 0-1
    }
    
    /**
     * Denormalizes slider value back to expansion rate range
     */
    private float denormalizeExpansionRate(double normalized) {
        return (float) (normalized * 1.9f + 0.1f);
    }
    
    /**
     * Gets human-readable expansion rate text
     */
    private String getExpansionRateText(float value) {
        if (value < 0.5f) return "Very Slow";
        if (value < 0.8f) return "Slow";
        if (value < 1.2f) return "Normal";
        if (value < 1.5f) return "Fast";
        return "Very Fast";
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