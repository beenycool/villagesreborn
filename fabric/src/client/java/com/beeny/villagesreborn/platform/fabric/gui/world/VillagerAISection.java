package com.beeny.villagesreborn.platform.fabric.gui.world;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration section for Villager AI settings
 */
public class VillagerAISection extends VillagesRebornTab.ConfigurationSection {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerAISection.class);
    
    private SliderWidget memoryLimitSlider;
    private SliderWidget aggressionSlider;
    private CyclingButtonWidget<Boolean> advancedAIToggle;
    
    public VillagerAISection(VillagesRebornWorldSettings settings, Runnable changeListener) {
        super(settings, changeListener);
    }
    
    @Override
    protected void createWidgets() {
        // Memory Limit Slider (50-500)
        this.memoryLimitSlider = new SliderWidget(
            0, 0, 200, 20,
            safeTranslatable("villagesreborn.config.ai.memory_limit"),
            normalizeMemoryLimit(settings.getVillagerMemoryLimit())
        ) {
            @Override
            protected void updateMessage() {
                int value = denormalizeMemoryLimit(this.value);
                this.setMessage(safeTranslatable("villagesreborn.config.ai.memory_limit.value", value));
            }
            
            @Override
            protected void applyValue() {
                int value = denormalizeMemoryLimit(this.value);
                settings.setVillagerMemoryLimit(value);
                notifyChange();
            }
        };
        widgets.add(memoryLimitSlider);
        
        // AI Aggression Slider (0.0-1.0)
        this.aggressionSlider = new SliderWidget(
            0, 0, 200, 20,
            safeTranslatable("villagesreborn.config.ai.aggression"),
            settings.getAiAggressionLevel()
        ) {
            @Override
            protected void updateMessage() {
                float value = (float) this.value;
                String levelText = getAggressionLevelText(value);
                this.setMessage(safeTranslatable("villagesreborn.config.ai.aggression.level", levelText));
            }
            
            @Override
            protected void applyValue() {
                float value = (float) this.value;
                settings.setAiAggressionLevel(value);
                notifyChange();
            }
        };
        widgets.add(aggressionSlider);
        
        // Advanced AI Toggle
        this.advancedAIToggle = CyclingButtonWidget.onOffBuilder(
            safeTranslatable("villagesreborn.config.on"),
            safeTranslatable("villagesreborn.config.off")
        ).initially(settings.isEnableAdvancedAI())
         .build(0, 0, 200, 20,
               safeTranslatable("villagesreborn.config.ai.advanced_ai"),
               (button, value) -> {
                   settings.setEnableAdvancedAI(value);
                   notifyChange();
               });
        widgets.add(advancedAIToggle);
    }
    
    @Override
    public void updateFromSettings() {
        if (memoryLimitSlider != null) {
            // Create new sliders with updated values since setValue and updateMessage are not accessible
            createWidgets();
        }
    }
    
    /**
     * Normalizes memory limit value to 0-1 range for slider
     */
    private double normalizeMemoryLimit(int value) {
        return (value - 50) / 450.0; // Range: 50-500 -> 0-1
    }
    
    /**
     * Denormalizes slider value back to memory limit range
     */
    private int denormalizeMemoryLimit(double normalized) {
        // Clamp normalized value to 0-1 range first
        normalized = Math.max(0.0, Math.min(1.0, normalized));
        int result = (int) (normalized * 450 + 50);
        // Clamp result to valid range as additional safety
        return Math.max(50, Math.min(500, result));
    }
    
    /**
     * Gets human-readable aggression level text
     */
    private String getAggressionLevelText(float value) {
        if (value < 0.2f) return "Peaceful";
        if (value < 0.4f) return "Calm";
        if (value < 0.6f) return "Normal";
        if (value < 0.8f) return "Assertive";
        return "Aggressive";
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