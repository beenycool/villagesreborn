package com.beeny.mixin;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remaining mixin for edge cases and special events.
 * Contains functionality that doesn't fit cleanly into other focused mixins.
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixinLegacy extends LivingEntity {
    
    protected VillagerEntityMixinLegacy() {
        super(null, null);
    }
    
    @Inject(method = "onStruckByLightning", at = @At("HEAD"))
    private void onLightningStrike(LightningEntity lightning, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null || villager.getWorld().isClient) return;
        
        // Lightning strike is a traumatic event
        data.adjustHappiness(-15);
        data.addRecentEvent("Struck by lightning!");
        
        // Anxious villagers are particularly affected
        if (data.getPersonality().name().equals("ANXIOUS") || 
            data.getPersonality().name().equals("NERVOUS")) {
            data.adjustHappiness(-10);
        }
    }
    
    @Inject(method = "wakeUp", at = @At("TAIL"))
    private void onWakeUp(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null || villager.getWorld().isClient) return;
        
        // Morning happiness boost for early risers
        if (data.getPersonality().name().equals("ENERGETIC") || 
            data.getPersonality().name().equals("CHEERFUL")) {
            data.adjustHappiness(1);
        }
        
        // Lazy villagers don't like waking up
        if (data.getPersonality().name().equals("LAZY")) {
            data.adjustHappiness(-1);
        }
    }
}