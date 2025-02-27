package com.beeny;

import net.fabricmc.api.ModInitializer;
import com.beeny.village.VillageCraftingManager;
import com.beeny.setup.SystemSpecs;
import com.beeny.setup.LLMConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Villages Reborn mod.
 * <p>
 * Villages Reborn is a comprehensive overhaul mod that transforms Minecraft villages into 
 * culturally diverse, intelligent communities with enhanced villager AI and themed structures.
 * </p>
 * 
 * @author Beeny
 * @version 0.1.0-alpha
 */
public class Villagesreborn implements ModInitializer {
    /** Logger instance for the mod */
    public static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    
    /** System specifications for determining AI capabilities */
    private static SystemSpecs systemSpecs;
    
    /** Configuration for LLM (Large Language Model) integration */
    private static LLMConfig llmConfig;

    /**
     * Initializes the mod components when Minecraft loads.
     * <p>
     * This method is called by the Fabric mod loader during game initialization.
     * It sets up system specifications analysis, LLM configuration, and initializes
     * the villager crafting system.
     * </p>
     */
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

    /**
     * Gets the system specifications instance.
     * <p>
     * Creates and initializes a new instance if one doesn't exist.
     * </p>
     *
     * @return The SystemSpecs instance containing hardware capability information
     */
    public static SystemSpecs getSystemSpecs() {
        if (systemSpecs == null) {
            systemSpecs = new SystemSpecs();
            systemSpecs.analyzeSystem();
        }
        return systemSpecs;
    }

    /**
     * Gets the LLM configuration instance.
     * <p>
     * Creates a new instance if one doesn't exist.
     * </p>
     *
     * @return The LLMConfig instance containing AI model settings
     */
    public static LLMConfig getLLMConfig() {
        if (llmConfig == null) {
            llmConfig = new LLMConfig();
        }
        return llmConfig;
    }
}
