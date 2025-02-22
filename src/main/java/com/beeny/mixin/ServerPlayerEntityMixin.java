package com.beeny.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.beeny.util.SpawnTracker;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements SpawnTracker {
    private boolean hasSpawnedBefore = false;

    @Inject(method = "onSpawn", at = @At("HEAD"))
    private void onFirstSpawn(CallbackInfo ci) {
        if (!this.hasSpawnedBefore) {
            this.hasSpawnedBefore = true;
        }
    }

    @Override
    public boolean hasSpawnedBefore() {
        return this.hasSpawnedBefore;
    }
}
