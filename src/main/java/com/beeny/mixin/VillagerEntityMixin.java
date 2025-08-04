package com.beeny.mixin;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerRelationshipManager;
import com.beeny.system.VillagerScheduleManager;
import com.beeny.system.ServerVillagerManager;
import com.beeny.util.VillagerNames;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.entity.LightningEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends LivingEntity {
    @Shadow public abstract void setVillagerData(net.minecraft.village.VillagerData villagerData);
    
    
    @Unique
    private int greetingCooldown = 0;
    
    @Unique
    private int updateCounter = 0;
    
    protected VillagerEntityMixin() {
        super(null, null);
    }
    
    @Inject(method = "<init>*", at = @At("TAIL"))
    private void initializeVillagerData(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        
        if (!villager.hasAttached(Villagersreborn.VILLAGER_DATA)) {
            VillagerData data = new VillagerData();
            villager.setAttached(Villagersreborn.VILLAGER_DATA, data);
            
            // Track this villager with the ServerVillagerManager
            if (!villager.getWorld().isClient && villager.getWorld() instanceof ServerWorld) {
                ServerVillagerManager.getInstance().trackVillager(villager);
            }
        }
    }
    
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void onMobTick(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null) return;
        
        updateCounter++;
        
        
        if (data.getName().isEmpty() || updateCounter % 20 == 0) {
            ensureNameAndData(villager, data);
        }
        
        
        if (greetingCooldown > 0) {
            greetingCooldown--;
        }
        
        
        if (updateCounter % 100 == 0) {
            updateHappinessBasedOnConditions(villager, data);
        }
        
        
        if (updateCounter % 200 == 0 && !data.getSpouseId().isEmpty()) {
            checkSpouseProximity(villager, data);
        }
    }
    
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteractMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (player.getWorld().isClient) return;
        
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null) return;
        
        
        if (player.isSneaking() && hand == Hand.MAIN_HAND) {
            showDetailedInfo(player, villager, data);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }
        
        
        if (greetingCooldown == 0 && hand == Hand.MAIN_HAND && player.getStackInHand(hand).isEmpty()) {
            greetVillager(player, villager, data);
            greetingCooldown = 100; 
        }
    }
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onVillagerDeath(DamageSource source, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null || villager.getWorld().isClient) return;
        
        // Untrack this villager
        ServerVillagerManager.getInstance().untrackVillager(villager.getUuid());
        
        if (!data.getSpouseName().isEmpty() || !data.getChildrenNames().isEmpty()) {
            notifyFamilyOfDeath(villager, data);
        }
        
        
        VillagerNames.cleanupVillager(villager.getUuidAsString());
        VillagerRelationshipManager.removeProposalTime(villager.getUuidAsString());
    }
    
    @Inject(method = "setVillagerData", at = @At("TAIL"))
    private void onProfessionChange(net.minecraft.village.VillagerData villagerData, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null) return;
        
        
        String professionKey = villagerData.profession().getKey()
            .map(key -> key.getValue().toString()).orElse("minecraft:none");
        data.addProfession(professionKey);
        
        
        if (!data.getName().isEmpty()) {
            String newName = VillagerNames.updateProfessionInName(
                data.getName(),
                villagerData.profession().getKey().orElse(VillagerProfession.NITWIT),
                villager.getWorld(),
                villager.getBlockPos()
            );
            data.setName(newName);
            villager.setCustomName(Text.literal(newName));
        }
    }
    
    @Inject(method = "afterUsing", at = @At("TAIL"))
    private void onTradeComplete(TradeOffer offer, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null) return;
        
        
        data.incrementTrades();
        
        
        data.adjustHappiness(2);
        
        
        Entity customer = villager.getCustomer();
        if (customer instanceof PlayerEntity player) {
            data.updatePlayerRelation(player.getUuidAsString(), 1);
            
            
            if (data.getTotalTrades() > 10 && data.getFavoritePlayerId().isEmpty()) {
                int reputation = data.getPlayerReputation(player.getUuidAsString());
                if (reputation > 20) {
                    data.setFavoritePlayerId(player.getUuidAsString());
                    player.sendMessage(Text.literal(data.getName() + " now considers you their favorite customer!")
                        .formatted(Formatting.GOLD), false);
                }
            }
        }
    }
    
    @Inject(method = "onStruckByLightning", at = @At("HEAD"))
    private void onStruckByLightning(ServerWorld world, LightningEntity lightning, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data != null) {
            
            String[] dramaticPersonalities = {"Energetic", "Confident", "Cheerful"};
            data.setPersonality(dramaticPersonalities[ThreadLocalRandom.current().nextInt(dramaticPersonalities.length)]);
            data.adjustHappiness(-20); 
        }
    }
    
    @Inject(method = "wakeUp", at = @At("TAIL"))
    private void onWakeUp(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data != null) {
            
            if (data.getHappiness() > 70) {
                data.adjustHappiness(2); 
            }
        }
    }
    
    @Unique
    private void ensureNameAndData(VillagerEntity villager, VillagerData data) {
        
        if (data.getName().isEmpty()) {
            var pos = villager.getBlockPos();
            var world = villager.getWorld();
            
            
            if (world != null && !pos.equals(new net.minecraft.util.math.BlockPos(0, 0, 0))) {
                String generatedName = VillagerNames.generateNameForProfession(
                    world,
                    pos
                );
                
                data.setName(generatedName);
                
                
                boolean isMale = (pos.getX() + pos.getZ()) % 2 == 0;
                data.setGender(isMale ? "Male" : "Female");
                
                
                data.setBirthPlace(String.format("X:%d Y:%d Z:%d", pos.getX(), pos.getY(), pos.getZ()));
                
                
                villager.setAttached(Villagersreborn.VILLAGER_NAME, generatedName);
            }
        }
        
        
        Text activitySuffix = Text.empty();
        if (villager.getWorld() instanceof ServerWorld) {
            VillagerScheduleManager.Activity activity = VillagerScheduleManager.getCurrentActivity(villager);
            activitySuffix = Text.literal(" [" + activity.description + "]")
                .formatted(Formatting.GRAY);
        }
        
        villager.setCustomName(Text.literal(data.getName()).append(activitySuffix));
        villager.setCustomNameVisible(true);
    }
    
    @Unique
    private void updateHappinessBasedOnConditions(VillagerEntity villager, VillagerData data) {
        
        if (villager.getWorld().getTimeOfDay() % 24000 > 13000) { 
            BlockPos bedPos = villager.getSleepingPosition().orElse(null);
            if (bedPos == null) {
                data.adjustHappiness(-1); 
            }
        }
        
        
        if (villager.getVillagerData().profession() != VillagerProfession.NITWIT && 
            villager.getVillagerData().profession() != VillagerProfession.NONE) {
            
            if (villager.getVillagerData().level() > 0) {
                data.adjustHappiness(1); 
            }
        }
        
        
        // Use ServerVillagerManager for efficient nearby villager lookup
        List<VillagerEntity> nearbyVillagers = new java.util.ArrayList<>();
        double range = 10.0;
        Box searchBox = new Box(
            villager.getX() - range, villager.getY() - range, villager.getZ() - range,
            villager.getX() + range, villager.getY() + range, villager.getZ() + range
        );
        List<VillagerEntity> found = villager.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            v -> v != villager && v.getWorld() == villager.getWorld()
        );
        nearbyVillagers.addAll(found);
        
        if (nearbyVillagers.size() > 2) {
            data.adjustHappiness(1);
        } else if (nearbyVillagers.isEmpty()) {
            data.adjustHappiness(-1);
        }
    }
    
    @Unique
    private void checkSpouseProximity(VillagerEntity villager, VillagerData data) {
        if (data.getSpouseId().isEmpty()) return;
        
        // Use ServerVillagerManager instead of scanning the world
        VillagerEntity spouse = ServerVillagerManager.getInstance().getVillager(UUID.fromString(data.getSpouseId()));
        
        if (spouse != null && spouse.isAlive()) {
            double distance = villager.getPos().distanceTo(spouse.getPos());
            if (distance < 20) {
                data.adjustHappiness(1);
            } else if (distance > 100) {
                data.adjustHappiness(-1);
            }
        }
    }
    
    @Unique
    private void greetVillager(PlayerEntity player, VillagerEntity villager, VillagerData data) {
        String playerUuid = player.getUuidAsString();
        int reputation = data.getPlayerReputation(playerUuid);
        
        String greeting;
        Formatting color;
        
        if (reputation > 50) {
            greeting = getPositiveGreeting(data.getName(), data.getPersonality());
            color = Formatting.GREEN;
            data.adjustHappiness(1);
        } else if (reputation < -20) {
            greeting = getNegativeGreeting(data.getName(), data.getPersonality());
            color = Formatting.RED;
        } else {
            greeting = getNeutralGreeting(data.getName(), data.getPersonality());
            color = Formatting.WHITE;
        }
        
        player.sendMessage(Text.literal(greeting).formatted(color), true);
        
        
        data.updatePlayerRelation(playerUuid, 1);
    }
    
    @Unique
    private String getPositiveGreeting(String name, String personality) {
        String[] greetings = switch (personality) {
            case "Friendly" -> new String[]{
                "Hello dear friend! How wonderful to see you!",
                "My friend! Welcome back!",
                "Always a pleasure to see you!"
            };
            case "Shy" -> new String[]{
                "Oh, h-hello... nice to see you again...",
                "You're back... that's nice...",
                "Hi... I was hoping you'd visit..."
            };
            case "Grumpy" -> new String[]{
                "Oh, it's you. I suppose you're alright.",
                "Hmph. At least you're better than most.",
                "You again? Well, could be worse."
            };
            default -> new String[]{
                "Good to see you again!",
                "Welcome back, friend!",
                "Hello there!"
            };
        };
        return greetings[ThreadLocalRandom.current().nextInt(greetings.length)];
    }
    
    @Unique
    private String getNeutralGreeting(String name, String personality) {
        String[] greetings = switch (personality) {
            case "Friendly" -> new String[]{"Hello there!", "Good day to you!", "Welcome!"};
            case "Shy" -> new String[]{"H-hello...", "Oh, um, hi...", "..."};
            case "Grumpy" -> new String[]{"What now?", "Yes?", "Hmph."};
            default -> new String[]{"Hello.", "Greetings.", "Good day."};
        };
        return greetings[ThreadLocalRandom.current().nextInt(greetings.length)];
    }
    
    @Unique
    private String getNegativeGreeting(String name, String personality) {
        String[] greetings = switch (personality) {
            case "Friendly" -> new String[]{
                "Oh... it's you...",
                "I'd rather not talk right now...",
                "Please leave me be..."
            };
            case "Grumpy" -> new String[]{
                "You! Get away from me!",
                "Haven't you done enough?",
                "Bah! Leave me alone!"
            };
            default -> new String[]{
                "Go away.",
                "I don't want to talk to you.",
                "Leave me alone."
            };
        };
        return greetings[ThreadLocalRandom.current().nextInt(greetings.length)];
    }
    
    @Unique
    private void showDetailedInfo(PlayerEntity player, VillagerEntity villager, VillagerData data) {
        player.sendMessage(Text.literal("=== " + data.getName() + " ===").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("Gender: " + data.getGender() + " | Age: " + data.getAgeInDays()), false);
        player.sendMessage(Text.literal("Personality: " + data.getPersonality() + " | Mood: " + data.getHappinessDescription()), false);
        player.sendMessage(Text.literal("Hobby: " + data.getHobby()), false);
        
        if (!data.getFavoriteFood().isEmpty()) {
            player.sendMessage(Text.literal("Favorite Food: " + data.getFavoriteFood()).formatted(Formatting.GREEN), false);
        }
        
        if (!data.getSpouseName().isEmpty()) {
            player.sendMessage(Text.literal("Married to: " + data.getSpouseName()).formatted(Formatting.RED), false);
        }
        
        if (!data.getChildrenNames().isEmpty()) {
            player.sendMessage(Text.literal("Children: " + String.join(", ", data.getChildrenNames())).formatted(Formatting.LIGHT_PURPLE), false);
        }
        
        player.sendMessage(Text.literal("Total Trades: " + data.getTotalTrades()).formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("Your Reputation: " + data.getPlayerReputation(player.getUuidAsString())).formatted(Formatting.AQUA), false);
    }
    
    @Unique
    private void notifyFamilyOfDeath(VillagerEntity villager, VillagerData data) {
        if (!(villager.getWorld() instanceof ServerWorld serverWorld)) return;
        
        
        if (!data.getSpouseId().isEmpty()) {
            // Use ServerVillagerManager instead of scanning the world
            VillagerEntity spouse = ServerVillagerManager.getInstance().getVillager(UUID.fromString(data.getSpouseId()));
            
            if (spouse != null) {
                VillagerData spouseData = spouse.getAttached(Villagersreborn.VILLAGER_DATA);
                if (spouseData != null) {
                    spouseData.setWidowed();
                    spouseData.adjustHappiness(-50);
                    spouseData.setNotes("Lost spouse " + data.getName() + " - Forever in mourning");
                }
            }
        }
        
        
        serverWorld.getPlayers().forEach(player -> {
            if (player.getPos().distanceTo(villager.getPos()) < 100) {
                player.sendMessage(Text.literal("💔 " + data.getName() + " has passed away...")
                    .formatted(Formatting.DARK_RED), false);
            }
        });
    }
}