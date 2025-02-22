package com.beeny;

import net.fabricmc.api.ModInitializer;
import com.beeny.village.VillageCraftingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Villagesreborn implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Villages Reborn mod");
        
        // Initialize the crafting manager
        VillageCraftingManager.getInstance();
        
        LOGGER.info("Villages Reborn mod initialization complete");
    }
}
