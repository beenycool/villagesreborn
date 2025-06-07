package com.beeny.villagesreborn.platform.fabric.block;

import com.beeny.villagesreborn.core.world.VillageChronicleManager;
import com.beeny.villagesreborn.platform.fabric.gui.world.VillageChronicleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class VillageChronicleBlockClient {
    private static final VillageChronicleManager chronicleManager = new VillageChronicleManager();

    public static void onUseClient(World world, BlockPos pos) {
        if (world.isClient) {
            // Generate a deterministic village ID based on the block position
            // This ensures the same village chronicle is accessed for the same location
            UUID villageId = generateVillageIdFromPosition(pos);
            chronicleManager.addEvent(villageId, "A new day has dawned.");
            MinecraftClient.getInstance().setScreen(new VillageChronicleScreen(chronicleManager.getChronicle(villageId)));
        }
    }
    
    /**
     * Generates a deterministic UUID based on block position
     * This ensures that the same location always produces the same village ID
     */
    private static UUID generateVillageIdFromPosition(BlockPos pos) {
        // Create a unique string representation of the position
        String positionString = String.format("village_%d_%d_%d", pos.getX(), pos.getY(), pos.getZ());
        
        // Generate a deterministic UUID using nameUUIDFromBytes
        return UUID.nameUUIDFromBytes(positionString.getBytes(StandardCharsets.UTF_8));
    }
} 