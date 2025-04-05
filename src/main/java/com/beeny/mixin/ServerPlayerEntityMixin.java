package com.beeny.mixin;

import com.beeny.accessors.ServerPlayerEntityAccessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to implement persistent NBT data storage for ServerPlayerEntity.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements ServerPlayerEntityAccessor {

    @Unique
    private NbtCompound villagesreborn_persistentData = new NbtCompound();

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void villagesreborn_writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!villagesreborn_persistentData.isEmpty()) {
            nbt.put("VillagesRebornData", villagesreborn_persistentData.copy());
        }
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void villagesreborn_readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("VillagesRebornData", NbtCompound.COMPOUND_TYPE)) {
            villagesreborn_persistentData = nbt.getCompound("VillagesRebornData");
        } else {
            villagesreborn_persistentData = new NbtCompound();
        }
    }

    @Override
    public NbtCompound getPersistentData() {
        if (villagesreborn_persistentData == null) {
            villagesreborn_persistentData = new NbtCompound();
        }
        return villagesreborn_persistentData;
    }

    @Override
    public void setPersistentData(NbtCompound persistentData) {
        this.villagesreborn_persistentData = persistentData;
    }
}
