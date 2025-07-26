package com.beeny.mixin;

import com.beeny.util.VillagerNames;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import com.mojang.serialization.Codec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin {
    private static final AttachmentType<String> VILLAGER_NAME = AttachmentRegistry.<String>builder()
            .persistent(Codec.STRING)
            .buildAndRegister(Identifier.of("villagersreborn", "villager_name"));

    @Inject(method = "<init>*", at = @At("TAIL"))
    private void assignRandomName(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        if (!villager.hasAttached(VILLAGER_NAME)) {
            String professionName = VillagerNames.generateNameForProfession(villager.getVillagerData().profession().getKey().orElse(VillagerProfession.NITWIT));
            villager.setAttached(VILLAGER_NAME, professionName);
            villager.setCustomName(Text.literal(professionName));
            villager.setCustomNameVisible(true);
        }
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void ensureNameIsVisible(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        if (villager.hasAttached(VILLAGER_NAME)) {
            String name = villager.getAttached(VILLAGER_NAME);
            if (name != null && villager.getCustomName() == null) {
                villager.setCustomName(Text.literal(name));
                villager.setCustomNameVisible(true);
            }
        }
    }

    @Inject(method = "setVillagerData", at = @At("TAIL"))
    private void updateNameOnProfessionChange(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        // Only update if villager already has a name (preserve first name)
        if (villager.hasAttached(VILLAGER_NAME)) {
            String currentName = villager.getAttached(VILLAGER_NAME);
            String newName = VillagerNames.updateProfessionInName(currentName, villager.getVillagerData().profession().getKey().orElse(VillagerProfession.NITWIT));
            villager.setAttached(VILLAGER_NAME, newName);
            villager.setCustomName(Text.literal(newName));
            villager.setCustomNameVisible(true);
        }
    }
}