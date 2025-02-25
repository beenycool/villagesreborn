package com.beeny.mixin;

import com.beeny.gui.VillageCraftingScreen;
import com.beeny.gui.VillagerDialogueScreen;
import com.beeny.village.SpawnRegion;
import com.beeny.village.VillageCraftingManager;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public class VillagerInteractionMixin {
    
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        // Initialize the VillagerAI if it doesn't exist yet
        VillagerManager vm = VillagerManager.getInstance();
        VillagerAI villagerAI = vm.getVillagerAI(villager.getUuid());
        if (villagerAI == null && !player.getWorld().isClient) {
            // Register this villager with our system if not already tracked
            vm.registerVillager(villager, (ServerPlayerEntity) player);
            villagerAI = vm.getVillagerAI(villager.getUuid());
        }
        
        // Only proceed with custom interaction if we have an AI for this villager
        if (villagerAI != null || player.getWorld().isClient) {
            if (!player.getWorld().isClient) {
                // Server-side handling
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
                    String culture = region != null ? region.getCulture() : "default";
                    
                    // Check if the villager is at their workstation
                    if (isAtWorkstation(villager)) {
                        // Update villager activity
                        if (villagerAI != null) {
                            villagerAI.updateActivity("trading");
                        }
                        
                        // Let the client handle the screen opening
                        cir.setReturnValue(ActionResult.SUCCESS);
                    } else {
                        // Update villager activity for conversation
                        if (villagerAI != null) {
                            villagerAI.updateActivity("conversing");
                            
                            // Generate contextual greeting if first time
                            if (!player.getScoreboardTags().contains("met_" + villager.getUuid())) {
                                player.addScoreboardTag("met_" + villager.getUuid());
                                villagerAI.generateDialogue("First meeting", null)
                                    .thenAccept(greeting -> {
                                        serverPlayer.sendMessage(
                                            Text.of(villager.getName().getString() + ": " + greeting),
                                            false
                                        );
                                    });
                            }
                        }
                        
                        cir.setReturnValue(ActionResult.SUCCESS);
                    }
                }
            } else {
                // Client-side handling - open appropriate screen
                SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
                String culture = region != null ? region.getCulture() : "default";
                
                if (isAtWorkstation(villager)) {
                    // Open crafting screen when at workstation
                    net.minecraft.client.MinecraftClient.getInstance().setScreen(
                        new VillageCraftingScreen(villager, culture)
                    );
                } else {
                    // Open dialogue screen when not at workstation
                    net.minecraft.client.MinecraftClient.getInstance().setScreen(
                        new VillagerDialogueScreen(villager)
                    );
                }
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }

    private boolean isAtWorkstation(VillagerEntity villager) {
        // Define workstation blocks for each profession
        VillagerProfession profession = villager.getVillagerData().getProfession();
        Block workstation = null;
        
        // Match profession to workstation block
        if (profession == VillagerProfession.FARMER) {
            workstation = Blocks.COMPOSTER;
        } else if (profession == VillagerProfession.LIBRARIAN) {
            workstation = Blocks.LECTERN;
        } else if (profession == VillagerProfession.ARMORER) {
            workstation = Blocks.BLAST_FURNACE;
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            workstation = Blocks.GRINDSTONE;
        } else if (profession == VillagerProfession.TOOLSMITH) {
            workstation = Blocks.SMITHING_TABLE;
        } else if (profession == VillagerProfession.CLERIC) {
            workstation = Blocks.BREWING_STAND;
        } else if (profession == VillagerProfession.FISHERMAN) {
            workstation = Blocks.BARREL;
        } else if (profession == VillagerProfession.FLETCHER) {
            workstation = Blocks.FLETCHING_TABLE;
        } else if (profession == VillagerProfession.SHEPHERD) {
            workstation = Blocks.LOOM;
        } else if (profession == VillagerProfession.BUTCHER) {
            workstation = Blocks.SMOKER;
        } else if (profession == VillagerProfession.LEATHERWORKER) {
            workstation = Blocks.CAULDRON;
        } else if (profession == VillagerProfession.MASON) {
            workstation = Blocks.STONECUTTER;
        } else if (profession == VillagerProfession.CARTOGRAPHER) {
            workstation = Blocks.CARTOGRAPHY_TABLE;
        }
        
        if (workstation == null) return false;
        
        // Check blocks nearby, not just directly below (more flexible detection)
        BlockPos vilPos = villager.getBlockPos();
        for (int y = -1; y <= 0; y++) { // Check below and same level
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = vilPos.add(x, y, z);
                    if (villager.getWorld().getBlockState(checkPos).getBlock() == workstation) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
}