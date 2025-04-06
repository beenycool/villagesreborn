package com.beeny.mixin;

import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerFeedbackHelper;
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
        
        if (!message.startsWith("/")) {
            VillagerManager vm = VillagerManager.getInstance();
            
            for (VillagerAI villagerAI : vm.getActiveVillagers()) {
                VillagerEntity villager = villagerAI.getVillager();
                if (villager == null || !villager.isAlive()) continue;
                
                String villagerName = villager.getName().getString();
                String messageLower = message.toLowerCase();
                String villagerNameLower = villagerName.toLowerCase();
                
                boolean isAddressed = false;
                String playerText = "";
                
                // Check for various formats: "Name message", "Hello Name!", etc.
                if (messageLower.startsWith(villagerNameLower + " ") || messageLower.equals(villagerNameLower)) {
                    isAddressed = true;
                    playerText = message.length() > villagerName.length() ? 
                        message.substring(villagerName.length() + 1).trim() : "";
                } else {
                    // Match greeting patterns like "hello John!" or "hi John, how are you?"
                    String greetingPattern = "^(hello|hi|hey|good morning|good afternoon|good evening)\\s+" + 
                        villagerNameLower.replace(".", "\\.") + "[!.]*\\s*(.*)";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(greetingPattern);
                    java.util.regex.Matcher matcher = pattern.matcher(messageLower);
                    
                    if (matcher.matches()) {
                        isAddressed = true;
                        String remainingText = matcher.group(2);
                        playerText = remainingText.isEmpty() ? "hello" : remainingText;
                    }
                }
                
                if (isAddressed) {
                    if (villager.squaredDistanceTo(player) <= 256) {
                        handleVillagerInteraction(villager, villagerAI, villagerName, playerText);
                        ci.cancel();
                    } else {
                        player.sendMessage(
                            Text.of("§7" + villagerName + " is too far away to hear you.§r"),
                            false
                        );
                        ci.cancel();
                    }
                    break;
                }
            }
        }
    }

    private void handleVillagerInteraction(VillagerEntity villager, VillagerAI villagerAI, String villagerName, String playerText) {
        if (!playerText.isEmpty()) {
            player.sendMessage(Text.of("§7You: §f" + playerText + "§r"), false);
            
            villager.getLookControl().lookAt(player, 30F, 30F);
            VillagerFeedbackHelper.showTalkingAnimation(villager);
            VillagerFeedbackHelper.showThinkingEffect(villager);
            
            villagerAI.generateDialogue(playerText, null)
                .thenAccept(response -> {
                    if (player.isAlive() && villager.isAlive()) {
                        String profession = villager.getVillagerData().getProfession().toString(); // Re-applying based on user info
                        String formattedResponse = VillagerFeedbackHelper.formatSpeech(profession, response);
                        
                        player.sendMessage(
                            Text.of("§6" + villagerName + "§r: " + formattedResponse),
                            false
                        );
                        
                        VillagerFeedbackHelper.showSpeakingEffect(villager);
                        VillagerFeedbackHelper.showTalkingAnimation(villager);
                    }
                });
        } else {
            VillagerFeedbackHelper.showThinkingEffect(villager);
            
            villagerAI.generateDialogue("Player mentioned my name", null)
                .thenAccept(greeting -> {
                    if (player.isAlive() && villager.isAlive()) {
                        String profession = villager.getVillagerData().getProfession().toString(); // Re-applying based on user info
                        String formattedGreeting = VillagerFeedbackHelper.formatSpeech(profession, greeting);
                        
                        player.sendMessage(
                            Text.of("§6" + villagerName + "§r: " + formattedGreeting),
                            false
                        );
                        
                        VillagerFeedbackHelper.showSpeakingEffect(villager);
                        VillagerFeedbackHelper.showTalkingAnimation(villager);
                    }
                });
        }
    }
}