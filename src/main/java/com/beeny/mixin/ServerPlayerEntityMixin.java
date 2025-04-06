package com.beeny.mixin;

import com.beeny.accessors.ServerPlayerEntityAccessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement; // Added import
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
        // Check if the compound exists using contains with the correct type
        if (nbt.contains("VillagesRebornData", NbtElement.COMPOUND_TYPE)) {
            // Retrieve the compound directly
            villagesreborn_persistentData = nbt.getCompound("VillagesRebornData");
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
