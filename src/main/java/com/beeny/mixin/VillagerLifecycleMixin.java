package com.beeny.mixin;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.ServerVillagerManager;
import com.beeny.system.VillagerPersonalityBehavior;
import com.beeny.system.VillagerRelationshipManager;
import com.beeny.util.VillagerNames;
import com.beeny.util.VillagerUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Focused mixin for villager lifecycle management.
 * Handles initialization, name generation, death handling, and profession changes.
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerLifecycleMixin extends LivingEntity {
    
    @Shadow public abstract void setVillagerData(net.minecraft.village.VillagerData villagerData);
    
    @Unique
    private int lifecycleUpdateCounter = 0;
    
    protected VillagerLifecycleMixin() {
        super(null, null);
    }
    
    @Inject(method = "<init>*", at = @At("TAIL"))
    private void initializeVillagerData(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        // Initialize villager data if not present
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
    private void updateLifecycle(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null || villager.getWorld().isClient) return;
        
        lifecycleUpdateCounter++;
        
        // Ensure name and data are properly set
        if (data.getName().isEmpty() || lifecycleUpdateCounter % 20 == 0) {
            ensureNameAndData(villager, data);
        }
        
        // Update happiness based on conditions
        if (lifecycleUpdateCounter % 100 == 0) {
            updateHappinessBasedOnConditions(villager, data);
        }
        
        // Check spouse proximity and family interactions
        if (lifecycleUpdateCounter % 400 == 0) {
            checkSpouseProximity(villager, data);
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
        
        // Cleanup names and relationships
        VillagerNames.cleanupVillager(villager.getUuidAsString());
        VillagerRelationshipManager.removeProposalTime(villager.getUuidAsString());
    }
    
    @Inject(method = "setVillagerData", at = @At("TAIL"))
    private void onProfessionChange(net.minecraft.village.VillagerData villagerData, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null) return;
        
        // Track profession change
        String professionKey = villagerData.profession().getKey()
            .map(key -> key.getValue().toString()).orElse("minecraft:none");
        data.addProfession(professionKey);
        
        // Update name to reflect new profession
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
        
        // Find the player who made the trade
        PlayerEntity trader = villager.getWorld().getClosestPlayer(villager, 5.0);
        if (trader != null) {
            String playerUuid = trader.getUuidAsString();
            int firstCount = offer.getOriginalFirstBuyItem().getCount();
            int secondCount = offer.getSecondBuyItem()
                .map(tradedItem -> {
                    // Convert TradedItem to ItemStack if possible, otherwise assume 0
                    try {
                        // Common accessor patterns across mappings:
                        // tradedItem.getItemStack() or tradedItem.asItemStack() or tradedItem.item()
                        java.lang.reflect.Method m;
                        try {
                            m = tradedItem.getClass().getMethod("getItemStack");
                        } catch (NoSuchMethodException e1) {
                            try {
                                m = tradedItem.getClass().getMethod("asItemStack");
                            } catch (NoSuchMethodException e2) {
                                try {
                                    m = tradedItem.getClass().getMethod("item");
                                } catch (NoSuchMethodException e3) {
                                    return 0;
                                }
                            }
                        }
                        Object stackObj = m.invoke(tradedItem);
                        if (stackObj instanceof net.minecraft.item.ItemStack stack) {
                            return stack.getCount();
                        }
                        return 0;
                    } catch (Exception ignore) {
                        return 0;
                    }
                })
                .orElse(0);
            int tradeValue = firstCount + secondCount;
            
            // Adjust reputation based on trade and personality
            int personalityBonus = VillagerPersonalityBehavior.getPersonalityTradeBonus(
                data.getPersonality(), data.getPlayerReputation(playerUuid));
            int reputationGain = Math.max(1, tradeValue / 10) + personalityBonus;
            
            data.adjustPlayerRelation(playerUuid, reputationGain);
            data.adjustHappiness(2);
            data.addRecentEvent("Traded with " + trader.getName().getString());
        }
    }
    
    @Unique
    private void ensureNameAndData(VillagerEntity villager, VillagerData data) {
        // Generate name if empty
        if (data.getName().isEmpty()) {
            String name = VillagerNames.generateName(villager);
            data.setName(name);
            villager.setCustomName(Text.literal(name));
        }
        
        // Initialize other data if needed
        if (data.getBirthTime() == 0) {
            data.setBirthTime(villager.getWorld().getTime());
        }
    }
    
    @Unique
    private void updateHappinessBasedOnConditions(VillagerEntity villager, VillagerData data) {
        // Night time happiness modifier
        if (villager.getWorld().getTimeOfDay() % 24000 > 13000) { 
            if (data.getPersonality().name().equals("MELANCHOLY")) {
                data.adjustHappiness(1);
            } else if (data.getPersonality().name().equals("ANXIOUS")) {
                data.adjustHappiness(-1);
            }
        }
        
        // Weather-based happiness
        if (villager.getWorld().isRaining()) {
            if (data.getPersonality().name().equals("MELANCHOLY")) {
                data.adjustHappiness(1);
            } else {
                data.adjustHappiness(-1);
            }
        }
        
        // Apply personality happiness modifier
        float personalityModifier = VillagerPersonalityBehavior.getPersonalityHappinessModifier(data.getPersonality());
        if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
            int adjustment = (int) ((personalityModifier - 1.0f) * 10);
            if (adjustment != 0) {
                data.adjustHappiness(adjustment);
            }
        }
    }
    
    @Unique
    private void checkSpouseProximity(VillagerEntity villager, VillagerData data) {
        if (data.getSpouseId().isEmpty()) return;
        
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 15);
        boolean spouseNearby = nearbyVillagers.stream()
            .anyMatch(v -> v.getUuidAsString().equals(data.getSpouseId()));
        
        if (spouseNearby) {
            data.adjustHappiness(2);
            if (ThreadLocalRandom.current().nextFloat() < 0.3f) {
                data.addRecentEvent("Spent time with " + data.getSpouseName());
            }
        } else if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
            data.adjustHappiness(-1);
            data.addRecentEvent("Misses " + data.getSpouseName());
        }
    }
    
    @Unique
    private void notifyFamilyOfDeath(VillagerEntity villager, VillagerData data) {
        if (!(villager.getWorld() instanceof ServerWorld serverWorld)) return;
        
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 50);
        
        for (VillagerEntity nearby : nearbyVillagers) {
            VillagerData nearbyData = nearby.getAttached(Villagersreborn.VILLAGER_DATA);
            if (nearbyData == null) continue;
            
            boolean isFamily = nearbyData.getSpouseId().equals(villager.getUuidAsString()) ||
                             nearbyData.getChildrenNames().contains(data.getName()) ||
                             data.getChildrenNames().contains(nearbyData.getName());
            
            if (isFamily) {
                nearbyData.adjustHappiness(-20);
                nearbyData.addRecentEvent("Mourning " + data.getName());
                
                // Remove from family relationships
                if (nearbyData.getSpouseId().equals(villager.getUuidAsString())) {
                    nearbyData.setSpouseId("");
                    nearbyData.setSpouseName("");
                }
                nearbyData.getChildrenNames().remove(data.getName());
            }
        }
    }
}