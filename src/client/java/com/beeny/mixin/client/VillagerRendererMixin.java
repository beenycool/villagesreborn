package com.beeny.mixin.client;

import com.beeny.village.CulturalTextureManager;
import com.beeny.village.VillagerManager;
import com.beeny.village.SpawnRegion;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntityRenderer.class)
public class VillagerRendererMixin {
    
    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void getCustomTexture(VillagerEntity villager, CallbackInfoReturnable<Identifier> cir) {
        VillagerManager manager = VillagerManager.getInstance();
        SpawnRegion region = manager.getNearestSpawnRegion(villager.getBlockPos());
        
        if (region != null) {
            String culture = region.getCulture();
            Identifier customTexture = CulturalTextureManager.getTextureForCulture(culture);
            cir.setReturnValue(customTexture);
        }
    }
}