package com.beeny.villagesreborn.platform.fabric.block;

import com.beeny.villagesreborn.core.world.VillageChronicleManager;
import com.beeny.villagesreborn.platform.fabric.gui.world.VillageChronicleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class VillageChronicleBlockClient {
    private static final VillageChronicleManager chronicleManager = new VillageChronicleManager();

    public static void onUseClient(World world, BlockPos pos) {
        if (world.isClient) {
            UUID villageId = UUID.randomUUID();
            chronicleManager.addEvent(villageId, "A new day has dawned.");
            MinecraftClient.getInstance().setScreen(new VillageChronicleScreen(chronicleManager.getChronicle(villageId)));
        }
    }
} 