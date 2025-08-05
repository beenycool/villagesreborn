package com.beeny;

import com.beeny.data.VillagerData;
import com.beeny.network.*;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin delegator preserved for compatibility. Real logic split into:
 * - ModInitializer (initialization, registration, setup)
 * - EventHandler (event handling)
 * - TickHandler (server tick periodic tasks)
 */
public class Villagersreborn implements ModInitializer {
    public static final String MOD_ID = com.beeny.constants.StringConstants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final AttachmentType<String> VILLAGER_NAME = AttachmentRegistry.<String>builder()
            .persistent(Codec.STRING)
            .buildAndRegister(Identifier.of(MOD_ID, "villager_name"));

    public static final AttachmentType<VillagerData> VILLAGER_DATA = AttachmentRegistry.<VillagerData>builder()
            .persistent(VillagerData.CODEC)
            .initializer(VillagerData::new)
            .buildAndRegister(Identifier.of(MOD_ID, "villager_data"));

    @Override
    public void onInitialize() {
        // Initialize the mod directly since the separate ModInitializer was removed
        LOGGER.info("Initializing VillagersReborn mod...");
        
        // Register commands
        com.beeny.commands.VillagerCommandRegistry.register();
        
        // Register network packets
        OpenFamilyTreePacket.register();
        FamilyTreeDataPacket.register();
        UpdateVillagerNotesPacket.register();
        TestLLMConnectionPacket.register();
        VillagerTeleportPacket.register();
        TestLLMConnectionResultPacket.register();
        RequestVillagerListPacket.register();
        VillagerMarriagePacket.register();
        
        LOGGER.info("VillagersReborn mod initialized successfully!");
    }
}