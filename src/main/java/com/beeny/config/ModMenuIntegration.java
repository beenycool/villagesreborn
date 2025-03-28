package com.beeny.config;

import com.beeny.Villagesreborn;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * ModMenu integration for Villages Reborn
 * Provides a config screen factory for the mod menu
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            // Create and return the config screen
            return new ClothConfigProvider().createConfigScreen(parent);
        };
    }
}