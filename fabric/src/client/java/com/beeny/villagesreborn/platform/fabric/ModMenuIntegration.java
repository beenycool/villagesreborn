package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.platform.fabric.gui.VillagesRebornConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new VillagesRebornConfigScreen(parent);
    }
} 