package com.beeny;

import net.fabricmc.api.ModInitializer;
import com.beeny.village.VillageCraftingManager;
import com.beeny.setup.SystemSpecs;
import com.beeny.setup.LLMConfig;
import net.minecraft.entity.player.PlayerEntity;
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

    /** Singleton instance */
    private static Villagesreborn INSTANCE;

    /**
     * Get the singleton instance of the mod
     * @return Mod instance
     */
    public static Villagesreborn getInstance() {
        return INSTANCE;
    }
    
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
        
        // Set singleton instance
        INSTANCE = this;
        
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

    /**
     * Request village information for the player's current location
     * @param player The player to get village information for
     */
    public void pingVillageInfo(PlayerEntity player) {
        // This would be implemented to send village info to the client
        // For now it's a stub to fix compilation issues
        LOGGER.info("Village info requested for player: " + player.getName().getString());
    }
}
