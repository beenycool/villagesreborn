package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.platform.fabric.ModContent;
import com.beeny.villagesreborn.platform.fabric.block.VillageChronicleBlockClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;

/**
 * Fabric platform implementation for Villages Reborn
 * Handles Fabric-specific initialization and platform integration
 */
@Environment(EnvType.CLIENT)
public class VillagesRebornFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.getBlockState(hitResult.getBlockPos()).isOf(ModContent.VILLAGE_CHRONICLE_BLOCK)) {
                VillageChronicleBlockClient.onUseClient(world, hitResult.getBlockPos());
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}