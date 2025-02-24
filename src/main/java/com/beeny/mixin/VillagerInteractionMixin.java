package com.beeny.mixin;

import com.beeny.gui.VillageCraftingScreen;
import com.beeny.village.VillageCraftingManager;
import com.beeny.village.VillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public class VillagerInteractionMixin {
    
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        if (!player.getWorld().isClient) {
            // Server-side handling
            if (player instanceof ServerPlayerEntity serverPlayer) {
                VillagerManager vm = VillagerManager.getInstance();
                String culture = vm.getNearestSpawnRegion(villager.getBlockPos()).getCulture();
                
                // Check if the villager is at their workstation
                if (isAtWorkstation(villager)) {
                    // Handle crafting logic here
                    // The client will open the screen, and when they click craft,
                    // it will trigger another interaction that we'll handle here
                    cir.setReturnValue(ActionResult.SUCCESS);
                }
            }
        } else {
            // Client-side handling
            VillagerManager vm = VillagerManager.getInstance();
            String culture = vm.getNearestSpawnRegion(villager.getBlockPos()).getCulture();
            if (isAtWorkstation(villager)) {
                // Open crafting screen when at workstation
                net.minecraft.client.MinecraftClient.getInstance().setScreen(
                    new VillageCraftingScreen(villager, culture)
                );
            } else {
                // Open dialogue screen when not at workstation
                net.minecraft.client.MinecraftClient.getInstance().setScreen(
                    new com.beeny.gui.VillagerDialogueScreen(villager)
                );
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    private boolean isAtWorkstation(VillagerEntity villager) {
        // Check if the villager is near their workstation based on profession
        net.minecraft.block.Block workstation = switch(villager.getVillagerData().getProfession().toString()) {
            case "FARMER" -> net.minecraft.block.Blocks.COMPOSTER;
            case "LIBRARIAN" -> net.minecraft.block.Blocks.LECTERN;
            case "BLACKSMITH", "ARMORER" -> net.minecraft.block.Blocks.SMITHING_TABLE;
            case "CLERIC" -> net.minecraft.block.Blocks.BREWING_STAND;
            default -> null;
        };
        
        if (workstation == null) return false;
        
        return villager.getWorld().getBlockState(villager.getBlockPos().down()).getBlock() == workstation;
    }
}