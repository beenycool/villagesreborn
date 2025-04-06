package com.beeny.mixin;

import com.beeny.accessors.ServerPlayerEntityAccessor;
import net.minecraft.nbt.NbtCompound;
// Remove NbtElement import if not needed, ensure NbtCompound is imported
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

    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void villagesreborn_writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!villagesreborn_persistentData.isEmpty()) {
            nbt.put("VillagesRebornData", villagesreborn_persistentData.copy());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void villagesreborn_readNbt(NbtCompound nbt, CallbackInfo ci) {
        // Check if the compound exists by trying to get it and checking if the Optional is present
        if (nbt.getCompound("VillagesRebornData").isPresent()) {
            // getCompound returns Optional, but we know it exists due to the contains check
            villagesreborn_persistentData = nbt.getCompound("VillagesRebornData").get();
        } else {
            villagesreborn_persistentData = new NbtCompound();
        }
    }

    @Override
    public NbtCompound villagesreborn_getPersistentData() {
        if (villagesreborn_persistentData == null) {
            villagesreborn_persistentData = new NbtCompound();
        }
        return villagesreborn_persistentData;
    }
}
