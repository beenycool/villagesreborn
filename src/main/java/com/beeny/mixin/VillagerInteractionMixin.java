package com.beeny.mixin;

import com.beeny.Villagersreborn;
import com.beeny.config.AISystemConfig;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerPersonalityBehavior;
import com.beeny.system.VillagerScheduleManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Focused mixin for player-villager interactions.
 * Handles greetings, detailed info display, and interaction logic.
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerInteractionMixin extends LivingEntity {
    
    @Unique
    private int greetingCooldown = 0;
    
    protected VillagerInteractionMixin() {
        super(null, null);
    }
    
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void updateGreetingCooldown(net.minecraft.entity.ai.goal.GoalSelector goalSelector) {
        if (greetingCooldown > 0) {
            greetingCooldown--;
        }
    }
    
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onPlayerInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null || villager.getWorld().isClient) return;
        
        ItemStack heldItem = player.getStackInHand(hand);
        
        // Handle detailed info with clock
        if (heldItem.getItem() == Items.CLOCK) {
            showDetailedInfo(player, villager, data);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        
        // Handle schedule info with compass
        if (heldItem.getItem() == Items.COMPASS) {
            showScheduleInfo(player, villager);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        
        // Handle greetings
        if (player instanceof ServerPlayerEntity && greetingCooldown <= 0) {
            greetVillager(player, villager, data);
            greetingCooldown = AISystemConfig.GREETING_COOLDOWN;
        }
    }
    
    @Unique
    private void greetVillager(PlayerEntity player, VillagerEntity villager, VillagerData data) {
        String playerUuid = player.getUuidAsString();
        int reputation = data.getPlayerReputation(playerUuid);
        String personalityName = data.getPersonality().name();
        
        String greeting;
        if (reputation > 20) {
            greeting = getPositiveGreeting(data.getName(), personalityName);
        } else if (reputation > -10) {
            greeting = getNeutralGreeting(data.getName(), personalityName);
        } else {
            greeting = getNegativeGreeting(data.getName(), personalityName);
        }
        
        // Use personality-specific greeting if available
        String personalityGreeting = VillagerPersonalityBehavior.getPersonalitySpecificGreeting(
            data.getPersonality(), data.getName(), reputation);
        if (!personalityGreeting.isEmpty()) {
            greeting = personalityGreeting;
        }
        
        player.sendMessage(Text.literal(greeting).formatted(Formatting.YELLOW), true);
        
        // Adjust reputation slightly for interaction
        data.adjustPlayerRelation(playerUuid, reputation < 0 ? 2 : 1);
    }
    
    @Unique
    private String getPositiveGreeting(String name, String personality) {
        String[] greetings = switch (personality) {
            case "Friendly" -> new String[]{
                "Hello there, friend! Wonderful day, isn't it?",
                "Great to see you again! How are you doing?",
                "Welcome back! I always enjoy our chats!"
            };
            case "Cheerful" -> new String[]{
                "What a fantastic day! Hello!",
                "Oh wonderful, it's you! How delightful!",
                "Such a pleasure to see you again!"
            };
            case "Confident" -> new String[]{
                "Ah, my favorite customer returns!",
                "Excellent timing! I have just what you need!",
                "Good to see someone with fine taste!"
            };
            default -> new String[]{
                "Hello there! Good to see you again!",
                "Welcome! How can I help you today?",
                "Greetings, friend!"
            };
        };
        return greetings[ThreadLocalRandom.current().nextInt(greetings.length)];
    }
    
    @Unique
    private String getNeutralGreeting(String name, String personality) {
        String[] greetings = switch (personality) {
            case "Friendly" -> new String[]{"Hello there!", "Good day to you!", "Welcome!"};
            case "Shy" -> new String[]{"Oh... h-hello...", "Um, hi there...", "Good day..."};
            case "Serious" -> new String[]{"Greetings.", "Good day.", "How may I assist you?"};
            default -> new String[]{"Hello.", "Good day.", "Greetings."};
        };
        return greetings[ThreadLocalRandom.current().nextInt(greetings.length)];
    }
    
    @Unique
    private String getNegativeGreeting(String name, String personality) {
        String[] greetings = switch (personality) {
            case "Friendly" -> new String[]{
                "Oh... it's you again...",
                "I suppose you want something...",
                "What brings you here this time?"
            };
            case "Grumpy" -> new String[]{
                "What do you want now?",
                "Can't you see I'm busy?",
                "Make it quick."
            };
            case "Anxious" -> new String[]{
                "Oh no... not now...",
                "Please don't cause any trouble...",
                "I... I suppose you need something..."
            };
            default -> new String[]{
                "What do you want?",
                "I'm quite busy...",
                "Yes?"
            };
        };
        return greetings[ThreadLocalRandom.current().nextInt(greetings.length)];
    }
    
    @Unique
    private void showDetailedInfo(PlayerEntity player, VillagerEntity villager, VillagerData data) {
        player.sendMessage(Text.literal("=== " + data.getName() + " ===").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("Gender: " + data.getGender() + " | Age: " + data.getAge()), false);
        player.sendMessage(Text.literal("Personality: " + data.getPersonality().name()).formatted(Formatting.LIGHT_PURPLE), false);
        player.sendMessage(Text.literal("Happiness: " + data.getHappiness() + "/100").formatted(Formatting.GREEN), false);
        player.sendMessage(Text.literal("Hobby: " + data.getHobby().name()).formatted(Formatting.AQUA), false);
        
        if (!data.getSpouseName().isEmpty()) {
            player.sendMessage(Text.literal("Married to: " + data.getSpouseName()).formatted(Formatting.LIGHT_PURPLE), false);
        }
        
        if (!data.getChildrenNames().isEmpty()) {
            player.sendMessage(Text.literal("Children: " + String.join(", ", data.getChildrenNames())).formatted(Formatting.YELLOW), false);
        }
        
        String playerUuid = player.getUuidAsString();
        int reputation = data.getPlayerReputation(playerUuid);
        Formatting reputationColor = reputation > 20 ? Formatting.GREEN : 
                                   reputation > 0 ? Formatting.YELLOW : Formatting.RED;
        player.sendMessage(Text.literal("Your reputation: " + reputation).formatted(reputationColor), false);
        
        if (!data.getRecentEvents().isEmpty()) {
            player.sendMessage(Text.literal("Recent: " + String.join(", ", data.getRecentEvents())).formatted(Formatting.GRAY), false);
        }
    }
    
    @Unique
    private void showScheduleInfo(PlayerEntity player, VillagerEntity villager) {
        try {
            com.beeny.ai.AIWorldManagerRefactored aiManager = com.beeny.ai.AIWorldManagerRefactored.getInstance();
            if (aiManager != null) {
                VillagerScheduleManager scheduleManager = aiManager.getScheduleManager();
                for (Text line : scheduleManager.getScheduleInfo(villager)) {
                    player.sendMessage(line, false);
                }
            }
        } catch (Exception e) {
            // Fallback to direct call
            VillagerScheduleManager tempScheduleManager = new VillagerScheduleManager();
            for (Text line : tempScheduleManager.getScheduleInfo(villager)) {
                player.sendMessage(line, false);
            }
        }
    }
}