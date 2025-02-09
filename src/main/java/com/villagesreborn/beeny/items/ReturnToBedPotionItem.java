package com.villagesreborn.beeny.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import java.util.Optional;

public class ReturnToBedPotionItem extends Item {
    public ReturnToBedPotionItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) user;
            Optional<GlobalPos> lastBedPos = player.getSpawnPointPosition();
            if (lastBedPos.isPresent()) {
                GlobalPos bedPos = lastBedPos.get();
                player.teleport(
                    (ServerWorld) world,
                    bedPos.getPos().getX() + 0.5, 
                    bedPos.getPos().getY(), 
                    bedPos.getPos().getZ() + 0.5, 
                    player.getYaw(), 
                    player.getPitch()
                );
                player.sendMessage(new LiteralText("You have been returned to your bed."), false);
            } else {
                player.sendMessage(new LiteralText("No valid bed spawn found."), false);
            }
        }
        return super.finishUsing(stack, world, user);
    }
}
