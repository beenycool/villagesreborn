package com.beeny.mixin;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerEmotionalBehavior;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public class TradeOfferMixin {
    
    // NOTE: The method "createNewOffer" does not exist in VillagerEntity
    // Trade offer modification is handled in VillagerEntityMixin.onTradeComplete() instead
    // This mixin is disabled to prevent compilation errors
    // @Inject(method = "createNewOffer", at = @At("RETURN"))
    private void modifyTradeOfferBasedOnHappiness_DISABLED(CallbackInfoReturnable<TradeOffer> cir) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null || cir.getReturnValue() == null) return;
        
        TradeOffer offer = cir.getReturnValue();
        int happiness = data.getHappiness();
        
        // Modify trade offers based on happiness and emotional state
        float emotionalModifier = VillagerEmotionalBehavior.getEmotionalTradeModifier(data);
        
        ItemStack buyItem = offer.getOriginalFirstBuyItem();
        int originalCount = buyItem.getCount();
        
        // Apply happiness modifier
        if (happiness > 80) {
            // Very happy villagers give better deals
            if (buyItem.getCount() > 1) {
                buyItem.setCount(Math.max(1, buyItem.getCount() - 1));
            }
        } else if (happiness < 30) {
            // Unhappy villagers demand more
            if (buyItem.getCount() < buyItem.getMaxCount()) {
                buyItem.setCount(Math.min(buyItem.getMaxCount(), buyItem.getCount() + 1));
            }
        }
        
        // Apply emotional modifier
        int emotionalAdjustment = Math.round(originalCount * emotionalModifier);
        int newCount = Math.max(1, Math.min(buyItem.getMaxCount(), buyItem.getCount() + emotionalAdjustment));
        buyItem.setCount(newCount);
        
        // Total trades affect prices with scaling benefits
        if (data.getTotalTrades() > 25) {
            // Experienced traders (25+ trades) give small discounts
            if (buyItem.getCount() > 1 && Math.random() < 0.15) {
                buyItem.setCount(Math.max(1, buyItem.getCount() - 1));
            }
        }
        
        if (data.getTotalTrades() > 75) {
            // Veteran traders (75+ trades) give better discounts
            if (buyItem.getCount() > 1 && Math.random() < 0.25) {
                buyItem.setCount(Math.max(1, buyItem.getCount() - 1));
            }
        }
        
        if (data.getTotalTrades() > 150) {
            // Master traders (150+ trades) occasionally give significant discounts
            if (buyItem.getCount() > 2 && Math.random() < 0.1) {
                buyItem.setCount(Math.max(1, buyItem.getCount() - 2));
            }
        }
        
        // Favorite customers get scaling benefits
        if (!data.getFavoritePlayerId().isEmpty()) {
            // Base favorite customer discount
            if (buyItem.getCount() > 1 && Math.random() < 0.2) {
                buyItem.setCount(Math.max(1, buyItem.getCount() - 1));
            }
            
            // Long-term favorite customers get even better deals
            if (data.getTotalTrades() > 100) {
                if (buyItem.getCount() > 1 && Math.random() < 0.15) {
                    buyItem.setCount(Math.max(1, buyItem.getCount() - 1));
                }
            }
        }
        
        // Novice traders (first 5 trades) might make pricing mistakes
        if (data.getTotalTrades() < 5) {
            if (Math.random() < 0.1) {
                // Accidentally give discount (inexperience)
                if (buyItem.getCount() > 1) {
                    buyItem.setCount(Math.max(1, buyItem.getCount() - 1));
                } else {
                    // Or ask for slightly more (nervousness)
                    if (buyItem.getCount() < buyItem.getMaxCount()) {
                        buyItem.setCount(Math.min(buyItem.getMaxCount(), buyItem.getCount() + 1));
                    }
                }
            }
        }
    }
}