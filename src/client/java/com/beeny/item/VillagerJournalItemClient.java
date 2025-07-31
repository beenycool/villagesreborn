package com.beeny.item;

import com.beeny.client.gui.VillagerJournalScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

@Environment(EnvType.CLIENT)
public class VillagerJournalItemClient {
    private static final int SCAN_RADIUS = 50; 

    public static void openVillagerJournal(World world, PlayerEntity player) {
        if (player == null) return;

        BlockPos playerPos = player.getBlockPos();
        Box searchBox = new Box(
            playerPos.getX() - SCAN_RADIUS,
            playerPos.getY() - SCAN_RADIUS,
            playerPos.getZ() - SCAN_RADIUS,
            playerPos.getX() + SCAN_RADIUS,
            playerPos.getY() + SCAN_RADIUS,
            playerPos.getZ() + SCAN_RADIUS
        );

        List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, searchBox, villager -> true);
        
        if (villagers.isEmpty()) {
            player.sendMessage(Text.literal("§eNo villagers found within " + SCAN_RADIUS + " blocks."), false);
            return;
        }

        
        MinecraftClient.getInstance().setScreen(new VillagerJournalScreen(villagers));
    }
}