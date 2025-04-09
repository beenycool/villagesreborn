package com.beeny.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT) // ModMenu is client-side
public class ModMenuIntegration implements ModMenuApi {

    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesReborn/ModMenu");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Return a factory that creates the config screen using AutoConfig
        return parent -> {
            var configHolder = ModConfigManager.getConfigHolder();
            if (configHolder != null) {
                return AutoConfig.getConfigScreen(ModConfig.class, parent).get();
            } else {
                // Log an error and potentially return a fallback screen or null
                LOGGER.error("ModConfigManager holder is null! Cannot create config screen.");
                // Returning null might cause ModMenu to show an error or disable the button.
                // Consider creating a simple screen explaining the error.
                return null;
            }
        };
    }
}