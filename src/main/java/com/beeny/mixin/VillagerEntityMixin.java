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
        // Don't assign name immediately during construction
        // Name will be assigned in mobTick when villager has proper position
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void ensureNameIsVisible(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        // Check if villager already has a custom name that wasn't set by our mod
        // This happens when a player uses a name tag on the villager
        if (villager.getCustomName() != null && !villager.hasAttached(VILLAGER_NAME)) {
            // Preserve the name set by the player and store it in our attachment
            String playerName = villager.getCustomName().getString();
            villager.setAttached(VILLAGER_NAME, playerName);
            villager.setCustomNameVisible(true);
            return;
        }
        
        // Assign name if not already assigned and villager has proper position
        if (!villager.hasAttached(VILLAGER_NAME)) {
            var pos = villager.getBlockPos();
            var world = villager.getWorld();
            
            // Only assign name if villager has a real position (not origin)
            if (world != null && !pos.equals(new net.minecraft.util.math.BlockPos(0, 0, 0))) {
                String professionName;
                
                // Check if this is a baby villager that might have been created from breeding
                if (villager.getBreedingAge() < 0) {
                    // This is a baby villager, try to inherit surname from nearby adult villagers
                    String inheritedSurname = findInheritedSurname(villager, world, pos);
                    professionName = VillagerNames.generateNameForProfession(
                        villager.getVillagerData().profession().getKey().orElse(VillagerProfession.NITWIT),
                        world,
                        pos,
                        inheritedSurname
                    );
                } else {
                    // This is an adult villager, generate name normally
                    professionName = VillagerNames.generateNameForProfession(
                        villager.getVillagerData().profession().getKey().orElse(VillagerProfession.NITWIT),
                        world,
                        pos
                    );
                }
                
                villager.setAttached(VILLAGER_NAME, professionName);
                villager.setCustomName(Text.literal(professionName));
                villager.setCustomNameVisible(true);
            }
        } else {
            // Ensure existing name is visible
            String name = villager.getAttached(VILLAGER_NAME);
            if (name != null && villager.getCustomName() == null) {
                villager.setCustomName(Text.literal(name));
                villager.setCustomNameVisible(true);
            }
        }
    }
    
    /**
     * Attempts to find an inherited surname from nearby adult villagers.
     *
     * @param babyVillager The baby villager that needs a surname
     * @param world The world the villager is in
     * @param pos The position of the baby villager
     * @return An inherited surname, or null if none found
     */
    private static String findInheritedSurname(VillagerEntity babyVillager, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) {
        // Look for nearby adult villagers (within a certain radius)
        java.util.List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            new net.minecraft.util.math.Box(pos).expand(5.0), // 5 block radius
            entity -> entity != babyVillager && entity.getBreedingAge() >= 0 // Only adult villagers
        );
        
        // If we found nearby adult villagers, try to inherit a surname from one of them
        if (!nearbyVillagers.isEmpty()) {
            // Pick a random nearby villager to inherit from
            VillagerEntity parentVillager = nearbyVillagers.get(world.random.nextInt(nearbyVillagers.size()));
            
            // Get the parent's name
            String parentName = parentVillager.getAttached(VILLAGER_NAME);
            if (parentName != null) {
                // Extract the surname from the parent's name
                return VillagerNames.extractSurname(parentName);
            }
        }
        
        // No surname found to inherit
        return null;
    }

    @Inject(method = "setVillagerData", at = @At("TAIL"))
    private void updateNameOnProfessionChange(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        // Only update if villager already has a name (preserve first name)
        if (villager.hasAttached(VILLAGER_NAME)) {
            String currentName = villager.getAttached(VILLAGER_NAME);
            String newName = VillagerNames.updateProfessionInName(
                currentName,
                villager.getVillagerData().profession().getKey().orElse(VillagerProfession.NITWIT),
                villager.getWorld(),
                villager.getBlockPos()
            );
            villager.setAttached(VILLAGER_NAME, newName);
            villager.setCustomName(Text.literal(newName));
            villager.setCustomNameVisible(true);
        }
    }
}