package com.beeny.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    private boolean hasSpawnedBefore = false;

    @Inject(method = "onSpawn", at = @At("HEAD"))
    private void onPlayerSpawn(CallbackInfo ci) {
        this.hasSpawnedBefore = true;
    }

    public boolean hasSpawnedBefore() {
        return this.hasSpawnedBefore;
    }
}
