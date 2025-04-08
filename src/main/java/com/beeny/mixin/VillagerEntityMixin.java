package com.beeny.mixin;

import com.beeny.config.VillagesConfig;
import com.beeny.village.VillagerManager; // Placeholder import
import com.beeny.village.VillageEconomyManager; // Placeholder import
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable; // Added import
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Shadow private long lastRestockTime;
    @Shadow private TradeOfferList offers;
    @Shadow public abstract World getWorld();

    /**
     * Modifies the constant value representing the cooldown between villager restocks.
     * Reads the tradingFrequencyMultiplier from the config and adjusts the cooldown accordingly.
     * A higher multiplier leads to a shorter cooldown (more frequent restocking).
     *
     * @param originalCooldown The original cooldown value (2400L).
     * @return The modified cooldown value.
     */
    @ModifyConstant(method = "restock", constant = @Constant(longValue = 2400L))
    private long modifyRestockCooldown(long originalCooldown) {
        // Cast self to access instance methods like getWorld()
        VillagerEntity self = (VillagerEntity)(Object)this;
        World world = getWorld(); // Use the shadowed abstract method

        // Ensure modification only happens on the server-side and world is available
        if (world == null || world.isClient) {
            return originalCooldown;
        }

        float multiplier = VillagesConfig.getInstance().getGameplaySettings().getTradingFrequencyMultiplier();

        // Safety check for invalid multiplier values
        if (multiplier <= 0f) {
            multiplier = 1.0f;
        }

        // Calculate the modified cooldown: Higher multiplier = shorter cooldown
        long modifiedCooldown = (long)(originalCooldown / multiplier);

        // Ensure the cooldown doesn't become excessively short (e.g., minimum 100 ticks = 5 seconds)
        return Math.max(100L, modifiedCooldown);
    }

    /**
     * Injects after vanilla trade offers are generated to apply dynamic economy adjustments.
     * @param info Callback info
     */
    @Inject(method = "fillRecipes", at = @At("RETURN"))
    private void applyDynamicEconomy(CallbackInfo info) {
        VillagerEntity self = (VillagerEntity)(Object)this;
        World world = getWorld();

        // Only apply on server-side and if the setting is enabled
        if (world == null || world.isClient || !VillagesConfig.getInstance().getGameplaySettings().isDynamicEconomyEnabled()) {
            return;
        }

        // Placeholder: Get the economic state for this village
        // This needs a proper implementation, e.g., VillageEconomyManager.getVillageEconomyState(self.getBlockPos());
        VillageEconomyManager.EconomyState economyState = VillageEconomyManager.getInstance().getEconomyState(self.getBlockPos());
        if (economyState == null) {
             //System.out.println("No economy state found for village near " + self.getBlockPos());
             return; // No economy data for this area
        }

        //System.out.println("Applying dynamic economy to offers for villager at " + self.getBlockPos());

        for (TradeOffer offer : this.offers) {
            // Adjust offer based on economyState (demand/supply)
            modifyTradeOffer(offer, economyState);
        }
    }

    /**
     * Placeholder method to modify a trade offer based on economic state.
     * Needs actual implementation based on how supply/demand is tracked.
     * @param offer The trade offer to modify.
     * @param economyState The current economic state of the village.
     */
    @Unique // Mark as unique to this mixin
    private void modifyTradeOffer(TradeOffer offer, VillageEconomyManager.EconomyState economyState) {
        ItemStack sellItem = offer.getSellItem();
        float demandFactor = economyState.getDemandFactor(sellItem.getItem()); // Example method
        float supplyFactor = economyState.getSupplyFactor(sellItem.getItem()); // Example method

        // Example adjustment: Increase price (first buy item count) if demand is high or supply is low
        // Decrease price if demand is low or supply is high. Adjust max uses similarly.
        // This requires careful balancing and access to modify TradeOffer internals (potentially via accessors/other mixins).
        // System.out.println("  - Modifying offer for " + sellItem.getItem().toString() + " Demand: " + demandFactor + " Supply: " + supplyFactor);
    }

    /**
     * Injects at the head of canBreed to potentially disable breeding based on config.
     * @param cir Callback info returnable
     */
    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void checkDynamicPopulationGrowth(CallbackInfoReturnable<Boolean> cir) {
        VillagerEntity self = (VillagerEntity)(Object)this;
        World world = getWorld();

        // Disable breeding entirely if dynamic population growth is off
        if (world != null && !world.isClient && !VillagesConfig.getInstance().getGameplaySettings().isDynamicPopulationGrowth()) {
            cir.setReturnValue(false); // Prevent breeding
        }
    }
}