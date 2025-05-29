package com.beeny.villagesreborn.platform.fabric.gui.world;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration section for Feature Toggle settings
 */
public class FeatureToggleSection extends VillagesRebornTab.ConfigurationSection {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureToggleSection.class);
    
    private Map<String, CyclingButtonWidget<Boolean>> featureToggles;
    
    public FeatureToggleSection(VillagesRebornWorldSettings settings, Runnable changeListener) {
        super(settings, changeListener);
    }
    
    // GUI scaling helpers
    private int getScaledSliderWidth() {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return Math.min(200, (int) Math.max(120, 200 / guiScale * 2));
    }
    
    private int getScaledWidgetHeight() {
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        return (int) Math.max(16, 20 / guiScale * 2);
    }
    
    @Override
    protected void createWidgets() {
        // Initialize the map here since this method is called from parent constructor
        featureToggles = new LinkedHashMap<>();
        
        // Elections Toggle
        CyclingButtonWidget<Boolean> electionsToggle = createFeatureToggle(
            "elections",
            settings.isElectionsEnabled(),
            (value) -> settings.setElectionsEnabled(value)
        );
        featureToggles.put("elections", electionsToggle);
        widgets.add(electionsToggle);
        
        // Assistant Villagers Toggle
        CyclingButtonWidget<Boolean> assistantsToggle = createFeatureToggle(
            "assistant_villagers",
            settings.isAssistantVillagersEnabled(),
            (value) -> settings.setAssistantVillagersEnabled(value)
        );
        featureToggles.put("assistant_villagers", assistantsToggle);
        widgets.add(assistantsToggle);
        
        // Dynamic Trading Toggle
        CyclingButtonWidget<Boolean> tradingToggle = createFeatureToggle(
            "dynamic_trading",
            settings.isDynamicTradingEnabled(),
            (value) -> settings.setDynamicTradingEnabled(value)
        );
        featureToggles.put("dynamic_trading", tradingToggle);
        widgets.add(tradingToggle);
        
        // Villager Relationships Toggle
        CyclingButtonWidget<Boolean> relationshipsToggle = createFeatureToggle(
            "villager_relationships",
            settings.isVillagerRelationships(),
            (value) -> settings.setVillagerRelationships(value)
        );
        featureToggles.put("villager_relationships", relationshipsToggle);
        widgets.add(relationshipsToggle);
    }
    
    @Override
    public void updateFromSettings() {
        CyclingButtonWidget<Boolean> electionsToggle = featureToggles.get("elections");
        if (electionsToggle != null) {
            electionsToggle.setValue(settings.isElectionsEnabled());
        }
        
        CyclingButtonWidget<Boolean> assistantsToggle = featureToggles.get("assistant_villagers");
        if (assistantsToggle != null) {
            assistantsToggle.setValue(settings.isAssistantVillagersEnabled());
        }
        
        CyclingButtonWidget<Boolean> tradingToggle = featureToggles.get("dynamic_trading");
        if (tradingToggle != null) {
            tradingToggle.setValue(settings.isDynamicTradingEnabled());
        }
        
        CyclingButtonWidget<Boolean> relationshipsToggle = featureToggles.get("villager_relationships");
        if (relationshipsToggle != null) {
            relationshipsToggle.setValue(settings.isVillagerRelationships());
        }
    }
    
    /**
     * Creates a feature toggle button with consistent styling
     */
    private CyclingButtonWidget<Boolean> createFeatureToggle(String featureKey, boolean initialValue, 
                                                            java.util.function.Consumer<Boolean> valueConsumer) {
        return CyclingButtonWidget.onOffBuilder(
            safeTranslatable("villagesreborn.config.enabled"),
            safeTranslatable("villagesreborn.config.disabled")
        ).initially(initialValue)
         .build(0, 0, getScaledSliderWidth(), getScaledWidgetHeight(),
               safeTranslatable("villagesreborn.config.features." + featureKey),
               (button, value) -> {
                   valueConsumer.accept(value);
                   notifyChange();
                   LOGGER.debug("Feature '{}' set to: {}", featureKey, value);
               });
    }

    /**
     * Safely gets translatable text, returns fallback for testing
     */
    private static Text safeTranslatable(String key) {
        try {
            var text = Text.translatable(key);
            return text != null ? text : Text.literal(key.substring(key.lastIndexOf('.') + 1));
        } catch (Exception e) {
            return Text.literal(key.substring(key.lastIndexOf('.') + 1));
        }
    }
}