package com.beeny.mixin;

import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ChatMessageMixin {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        String message = packet.chatMessage();
        
        // Check if this is talking to a villager (format: "VillagerName message")
        if (!message.startsWith("/")) {
            VillagerManager vm = VillagerManager.getInstance();
            VillagerEntity targetVillager = null;
            
            // Try to find the villager being addressed
            for (VillagerAI villagerAI : vm.getActiveVillagers()) {
                VillagerEntity villager = villagerAI.getVillager();
                if (villager == null || !villager.isAlive()) continue;
                
                String villagerName = villager.getName().getString();
                if (message.startsWith(villagerName + " ") || message.equals(villagerName)) {
                    // Found the villager being addressed
                    targetVillager = villager;
                    
                    // Extract just the message part (everything after the name and space)
                    String playerText = message.length() > villagerName.length() ? 
                        message.substring(villagerName.length() + 1).trim() : "";
                    
                    // Only proceed if the villager is close enough (16 blocks)
                    if (villager.squaredDistanceTo(player) <= 256) {
                        // Format the player's message
                        if (!playerText.isEmpty()) {
                            player.sendMessage(Text.of("§7You: §f" + playerText + "§r"), false);
                            
                            // Generate AI response
                            villagerAI.generateDialogue(playerText, null)
                                .thenAccept(response -> {
                                    // Only show the response if the player and villager are still valid
                                    if (player.isAlive() && villager.isAlive()) {
                                        player.sendMessage(
                                            Text.of("§6" + villagerName + "§r: " + response),
                                            false
                                        );
                                    }
                                });
                        } else {
                            // Just the villager's name was mentioned, generate a greeting
                            villagerAI.generateDialogue("Player mentioned my name", null)
                                .thenAccept(greeting -> {
                                    if (player.isAlive() && villager.isAlive()) {
                                        player.sendMessage(
                                            Text.of("§6" + villagerName + "§r: " + greeting),
                                            false
                                        );
                                    }
                                });
                        }
                        
                        // Make the villager look at the player
                        villager.getLookControl().lookAt(player, 30F, 30F);
                        
                        // Cancel the normal chat message
                        ci.cancel();
                        break;
                    } else {
                        // Villager is too far away
                        player.sendMessage(
                            Text.of("§7" + villagerName + " is too far away to hear you.§r"),
                            false
                        );
                        ci.cancel();
                        break;
                    }
                }
            }
        }
    }
}