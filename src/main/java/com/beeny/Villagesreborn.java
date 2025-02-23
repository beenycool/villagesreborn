package com.beeny;

import net.fabricmc.api.ModInitializer;
import com.beeny.village.VillageCraftingManager;
import com.beeny.setup.SystemSpecs;
import com.beeny.setup.LLMConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Villagesreborn implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static SystemSpecs systemSpecs;
    private static LLMConfig llmConfig;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Villages Reborn mod");
        
        // Initialize system specifications
        systemSpecs = new SystemSpecs();
        systemSpecs.analyzeSystem();
        
        // Initialize LLM configuration
        llmConfig = new LLMConfig();
        
        // Initialize the crafting manager
        VillageCraftingManager.getInstance();
        
        LOGGER.info("Villages Reborn mod initialization complete");
    }

    public static SystemSpecs getSystemSpecs() {
        if (systemSpecs == null) {
            systemSpecs = new SystemSpecs();
            systemSpecs.analyzeSystem();
        }
        return systemSpecs;
    }

    public static LLMConfig getLLMConfig() {
        if (llmConfig == null) {
            llmConfig = new LLMConfig();
        }
        return llmConfig;
    }
}
