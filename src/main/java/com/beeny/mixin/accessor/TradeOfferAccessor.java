package com.beeny.mixin.accessor;

import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TradeOffer.class)
public interface TradeOfferAccessor {
    @Accessor("uses")
    int getUses();

    @Accessor("maxUses")
    int getMaxUses();

    @Accessor("priceMultiplier")
    float getPriceMultiplier();
}