package com.beeny;

import com.beeny.village.VillagerManager;
import com.beeny.ai.LLMService;
import com.beeny.config.VillagesConfig;
import com.beeny.commands.ModCommands;
import com.beeny.setup.LLMConfig;
import com.beeny.setup.SystemSpecs;
import com.beeny.village.VillagerAI;
import com.beeny.village.SpawnRegion;
import com.beeny.gui.SetupScreen;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Villagesreborn implements ModInitializer {
    public static final String MOD_ID = "villagesreborn";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final List<String> CONFLICTING_MODS = Arrays.asList(
        "mca", "villagerfix", "bettervillagers", "villagelife"
    );
    
    private final VillagerManager villagerManager = VillagerManager.getInstance();
    private static LLMConfig llmConfig;
    private static SystemSpecs systemSpecs;

    @Override
    public void onInitialize() {
        checkModConflicts();
        initializeSystemSpecs();
        initializeLLMConfig();
        registerEvents();
    }

    private void checkModConflicts() {
        List<String> activeConflicts = CONFLICTING_MODS.stream()
            .filter(mod -> FabricLoader.getInstance().isModLoaded(mod))
            .collect(Collectors.toList());

        if (!activeConflicts.isEmpty()) {
            LOGGER.warn("Detected potentially conflicting mods: {}", activeConflicts);
            LOGGER.warn("Some features may not work as expected");
            
            // Configure compatibility mode based on conflicts
            for (String conflictMod : activeConflicts) {
                switch (conflictMod) {
                    case "mca" -> disableVillagerAIOverrides();
                    case "bettervillagers" -> disableBehaviorGeneration();
                    case "villagelife" -> disableCustomEvents();
                }
            }
        }
    }

    private void disableVillagerAIOverrides() {
        VillagesConfig config = VillagesConfig.getInstance();
        config.getGeneralSettings().features.put("custom_ai", false);
        config.save();
        LOGGER.info("Disabled villager AI overrides due to mod conflicts");
    }

    private void disableBehaviorGeneration() {
        VillagesConfig config = VillagesConfig.getInstance();
        config.getGeneralSettings().features.put("llm_behaviors", false);
        config.save();
        LOGGER.info("Disabled LLM behavior generation due to mod conflicts");
    }

    private void disableCustomEvents() {
        VillagesConfig config = VillagesConfig.getInstance();
        config.getGeneralSettings().features.put("cultural_events", false);
        config.save();
        LOGGER.info("Disabled custom cultural events due to mod conflicts");
    }

    private void initializeSystemSpecs() {
        systemSpecs = new SystemSpecs();
        LOGGER.info("System performance tier: {}", systemSpecs.getPerformanceTier());
        LOGGER.info(systemSpecs.getPerformanceReport());
    }

    private void initializeLLMConfig() {
        llmConfig = new LLMConfig();
        llmConfig.load();
        
        if (!llmConfig.isConfigured()) {
            LOGGER.info("First-time setup required");
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().setScreen(new SetupScreen(llmConfig));
            });
        }
    }

    private void registerClientEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                handleClientTick(client);
            }
        });
    }

    private void handleClientTick(MinecraftClient client) {
        // Handle any client-side features
        if (client.world.getTime() % 20 == 0) { // Once per second
            checkPerformanceAndAdjust(client);
        }
    }

    private void checkPerformanceAndAdjust(MinecraftClient client) {
        if (client.getCurrentFps() < 30) {
            LOGGER.warn("Low FPS detected, reducing feature complexity");
            systemSpecs = new SystemSpecs(); // Recalculate performance tier
            
            if (systemSpecs.getPerformanceTier() > 0) {
                VillagesConfig config = VillagesConfig.getInstance();
                config.getGeneralSettings().features.put("detailed_animations", false);
                config.save();
            }
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

        // Add player join event handler
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ServerPlayerEntity player && !player.hasSpawnedBefore()) {
                handleNewPlayerSpawn(player, (ServerWorld)world);
            }
        });
    }

    private void handleNewPlayerSpawn(ServerPlayerEntity player, ServerWorld world) {
        BlockPos pos = player.getBlockPos();
        VillagerManager vm = VillagerManager.getInstance();
        VillagesConfig config = VillagesConfig.getInstance();
        
        // Check if welcome sequences are enabled
        if (!config.getGeneralSettings().showWelcomeSequence) {
            return;
        }
        
        // Check if player has seen welcome sequence before
        boolean hasSeenWelcome = llmConfig.hasSeenWelcomeSequence(player.getUuid());
        
        // Only proceed if player hasn't seen sequence or if returning player welcomes are enabled
        if (!hasSeenWelcome || config.getGeneralSettings().showWelcomeForReturningPlayers) {
            // Check if there's no nearby village
            if (vm.getNearestSpawnRegion(pos) == null) {
                String defaultCulture = llmConfig.getProvider().equals("local") ? "roman" : "victorian";
                vm.registerSpawnRegion(pos, 64, defaultCulture);
                triggerWelcomeSequence(player, world, defaultCulture);
            } else {
                // If there is a village nearby, still show welcome but don't create new one
                SpawnRegion nearestRegion = vm.getNearestSpawnRegion(pos);
                triggerWelcomeSequence(player, world, nearestRegion.getCulture());
            }
            llmConfig.setWelcomeSequenceShown(player.getUuid());
        }
    }

    private void triggerWelcomeSequence(ServerPlayerEntity player, ServerWorld world, String culture) {
        String prompt = String.format(
            "Design a welcome ceremony for a %s village in Minecraft.\n" +
            "Include:\n" +
            "1. Villager actions (e.g., wave, offer items)\n" +
            "2. Dialogue (short greeting)\n" +
            "3. Visual effects (e.g., particles)\n" +
            "Format:\n" +
            "ACTIONS: (brief action description)\n" +
            "DIALOGUE: (single line of welcome text)\n" +
            "EFFECTS: (particle effect name)",
            culture
        );

        LLMService.getInstance().generateResponse(prompt)
            .thenAccept(response -> {
                Map<String, String> ceremony = parseResponse(response);
                player.sendMessage(Text.literal("§6" + ceremony.get("DIALOGUE")), false);
                spawnWelcomingVillagers(world, player.getBlockPos(), ceremony.get("ACTIONS"), ceremony.get("EFFECTS"));
            });
    }

    private void spawnWelcomingVillagers(ServerWorld world, BlockPos pos, String actions, String effects) {
        VillagerManager vm = VillagerManager.getInstance();
        
        // Generate a culturally appropriate gift
        String culture = vm.getNearestSpawnRegion(pos).getCulture();
        String giftPrompt = String.format(
            "What would be a simple welcome gift in a %s village? Choose from:\n" +
            "1. bread (basic food)\n" +
            "2. emerald (valuable)\n" +
            "3. map (navigation)\n" +
            "4. compass (direction)\n" +
            "Respond with just the item name.",
            culture
        );

        LLMService.getInstance().generateResponse(giftPrompt)
            .thenAccept(giftResponse -> {
                ItemStack gift = switch(giftResponse.trim().toLowerCase()) {
                    case "bread" -> new ItemStack(Items.BREAD, 3);
                    case "emerald" -> new ItemStack(Items.EMERALD, 1);
                    case "map" -> new ItemStack(Items.MAP, 1);
                    case "compass" -> new ItemStack(Items.COMPASS, 1);
                    default -> new ItemStack(Items.BREAD, 1);
                };

                // Spawn villagers with gift
                for (int i = 0; i < 3; i++) {
                    double angle = Math.PI * (0.25 + 0.25 * i);
                    double x = pos.getX() + Math.cos(angle) * 3;
                    double z = pos.getZ() + Math.sin(angle) * 3;
                    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int)x, (int)z);
                    
                    VillagerEntity villager = new VillagerEntity(EntityType.VILLAGER, world);
                    villager.setPosition(x, y, z);
                    villager.setYaw((float)(angle * 180.0 / Math.PI) + 180.0F);
                    
                    // Give the middle villager the gift to offer
                    if (i == 1) {
                        villager.setStackInHand(Hand.MAIN_HAND, gift.copy());
                    }
                    
                    world.spawnEntity(villager);
                    
                    // Initialize villager with welcoming behavior
                    vm.onVillagerSpawn(villager, world);
                    VillagerAI ai = vm.getVillagerAI(villager.getUuid());
                    if (ai != null) {
                        ai.updateActivity("welcoming");
                    }

                    // Add particle effects
                    world.spawnParticles(
                        getParticleType(effects),
                        x, y + 1, z,
                        5, 0.2, 0.2, 0.2, 0.02
                    );
                }

                // Drop the gift near the player after a short delay
                world.getServer().execute(() -> {
                    world.spawnEntity(new ItemEntity(world, 
                        pos.getX(), pos.getY(), pos.getZ(),
                        gift.copy()
                    ));
                });
            });
    }

    private Map<String, String> parseResponse(String response) {
        return Arrays.stream(response.split("\n"))
            .map(line -> line.split(": ", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0],
                parts -> parts[1].trim()
            ));
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

    private net.minecraft.particle.ParticleEffect getParticleType(String effect) {
        return switch (effect.toLowerCase()) {
            case "heart" -> ParticleTypes.HEART;
            case "happy" -> ParticleTypes.HAPPY_VILLAGER;
            case "angry" -> ParticleTypes.ANGRY_VILLAGER;
            case "magic" -> ParticleTypes.ENCHANT;
            case "sparkle" -> ParticleTypes.END_ROD;
            default -> ParticleTypes.HAPPY_VILLAGER;
        };
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
