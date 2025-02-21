package com.beeny;

import com.beeny.village.VillagerManager;
import com.beeny.ai.LLMService;
import com.beeny.commands.ModCommands;
import com.beeny.setup.LLMConfig;
import com.beeny.setup.SystemSpecs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.village.VillagerAI;

public class Villagesreborn implements ModInitializer {
    public static final String MOD_ID = "villagesreborn";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static LLMConfig llmConfig;
    private static SystemSpecs systemSpecs;
    private final VillagerManager villagerManager = VillagerManager.getInstance();

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Villages Reborn...");
        
        // Initialize system specs and analyze hardware capabilities
        initializeSystemSpecs();
        
        // Initialize and load LLM configuration
        initializeLLMConfig();
        
        // Initialize services
        LLMService.getInstance().initialize(llmConfig);
        ModCommands.register();
        
        // Register event handlers
        registerEvents();
        registerChatListener();
        
        LOGGER.info("Villages Reborn initialization complete!");
    }

    private void initializeSystemSpecs() {
        systemSpecs = new SystemSpecs();
        systemSpecs.analyzeSystem();

        if (!systemSpecs.hasAdequateResources()) {
            LOGGER.warn("System resources may be inadequate for optimal performance:");
            LOGGER.warn("Available RAM: {} MB", systemSpecs.getAvailableRam());
            LOGGER.warn("CPU Threads: {}", systemSpecs.getCpuThreads());
            LOGGER.warn("GPU Support: {}", systemSpecs.hasGpuSupport());
        }
    }

    private void initializeLLMConfig() {
        llmConfig = new LLMConfig();
        llmConfig.loadConfig();

        if (!llmConfig.isSetupComplete()) {
            LOGGER.info("First-time setup detected, initializing with system-specific settings");
            llmConfig.initialize(systemSpecs);
            llmConfig.setSetupComplete(true);
        }

        // Adjust settings based on current system state
        if (systemSpecs.getAvailableRam() < 4096) { // Less than 4GB
            LOGGER.warn("Low memory detected, reducing context length");
            llmConfig.setContextLength(1024);
        }

        if (!systemSpecs.hasGpuSupport()) {
            LOGGER.warn("No GPU support detected, disabling GPU acceleration");
            llmConfig.setUseGPU(false);
        }
    }

    public static LLMConfig getLLMConfig() {
        return llmConfig;
    }

    public static SystemSpecs getSystemSpecs() {
        return systemSpecs;
    }

    private void registerEvents() {
        // Villager spawn events
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world instanceof ServerWorld && entity instanceof VillagerEntity villager) {
                villagerManager.onVillagerSpawn(villager, world);
            }
        });

        // Villager unload events
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (world instanceof ServerWorld && entity instanceof VillagerEntity villager) {
                villagerManager.removeVillager(villager.getUuid());
            }
        });
    }

    private void registerChatListener() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            handlePlayerMessage(sender, message.getContent().getString());
        });
    }

    private void handlePlayerMessage(ServerPlayerEntity player, String message) {
        String[] words = message.split(" ");
        if (words.length > 1 && words[0].equalsIgnoreCase("hello")) {
            String villagerName = words[1];
            VillagerEntity villager = villagerManager.findNearbyVillagerByName(player, villagerName);
            
            if (villager != null) {
                VillagerAI villagerAI = villagerManager.getVillagerAI(villager.getUuid());
                villagerAI.generateBehavior("greeting")
                    .thenAccept(response -> {
                        player.sendMessage(Text.literal(villager.getName().getString() + ": " + response), false);
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Error generating villager response", e);
                        player.sendMessage(Text.literal("The villager seems distracted..."), false);
                        return null;
                    });
            } else {
                player.sendMessage(Text.literal("No villager named '" + villagerName + "' found nearby."), false);
            }
        }
    }

    // Shutdown hook to clean up resources
    public static void shutdown() {
        if (llmConfig != null) {
            LOGGER.info("Saving LLM configuration...");
            llmConfig.saveConfig();
        }
        LLMService.getInstance().shutdown();
    }
}