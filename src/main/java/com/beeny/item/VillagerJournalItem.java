package com.beeny.item;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class VillagerJournalItem extends Item {
    private static final int SCAN_RADIUS = 50; 

    public VillagerJournalItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            
            return ActionResult.SUCCESS;
        }
        
        
        List<VillagerEntity> villagers = world.getEntitiesByClass(
            VillagerEntity.class,
            new Box(
                user.getX() - SCAN_RADIUS,
                user.getY() - SCAN_RADIUS,
                user.getZ() - SCAN_RADIUS,
                user.getX() + SCAN_RADIUS,
                user.getY() + SCAN_RADIUS,
                user.getZ() + SCAN_RADIUS
            ),
            villager -> villager.isAlive()
        );
        
        if (villagers.isEmpty()) {
            user.sendMessage(Text.literal("§eNo villagers found within " + SCAN_RADIUS + " blocks."), false);
        } else {
            user.sendMessage(Text.literal("§aFound " + villagers.size() + " villager(s) within " + SCAN_RADIUS + " blocks."), false);
            for (VillagerEntity villager : villagers) {
                String name = villager.hasCustomName() ? villager.getCustomName().getString() : com.beeny.constants.StringConstants.UNNAMED_VILLAGER;
                BlockPos pos = villager.getBlockPos();
                user.sendMessage(Text.literal("§7- " + name + " at [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"), false);
            }
        }
        
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) {
            
            return ActionResult.SUCCESS;
        }
        
        
        return ActionResult.SUCCESS;
    }
}