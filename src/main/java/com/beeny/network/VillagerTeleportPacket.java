package com.beeny.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.MagmaBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import java.util.Set;
import net.minecraft.network.packet.s2c.play.PositionFlag;

public record VillagerTeleportPacket(int villagerId) implements CustomPayload {
    public static final Id<VillagerTeleportPacket> ID = new Id<>(Identifier.of("villagersreborn", "villager_teleport"));
    
    public static final PacketCodec<RegistryByteBuf, VillagerTeleportPacket> CODEC = PacketCodec.of(
        (value, buf) -> buf.writeInt(value.villagerId),
        buf -> new VillagerTeleportPacket(buf.readInt())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int villagerId = payload.villagerId();
            
            context.server().execute(() -> {
                Entity entity = player.getWorld().getEntityById(villagerId);
                
                if (!(entity instanceof VillagerEntity)) {
                    player.sendMessage(Text.literal("§cEntity is not a villager!"), false);
                    return;
                }
                
                VillagerEntity villager = (VillagerEntity) entity;
                
                if (!villager.isRemoved()) {
                    Vec3d villagerPos = villager.getPos();
                    
                    // Teleport player to villager with safe positioning
                    Vec3d safePos = findSafePosition(villagerPos, player);
                    player.teleport((ServerWorld)player.getWorld(), safePos.x, safePos.y, safePos.z, Set.<PositionFlag>of(), 0.0f, 0.0f, false);
                    
                    // Play teleport sound and send confirmation message
                    player.getWorld().playSound(null, safePos.x, safePos.y, safePos.z, 
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    
                    String villagerName = villager.hasCustomName() ? 
                        villager.getCustomName().getString() : "Unnamed Villager";
                    player.sendMessage(Text.literal("§aTeleported to " + villagerName + "!"), false);
                } else {
                    player.sendMessage(Text.literal("§cVillager not found or has moved away!"), false);
                }
            });
        });
    }
    
    private static Vec3d findSafePosition(Vec3d villagerPos, ServerPlayerEntity player) {
        // Try to find a safe position near the villager
        Vec3d[] offsets = {
            new Vec3d(2, 0, 0),   // East
            new Vec3d(-2, 0, 0),  // West
            new Vec3d(0, 0, 2),   // South
            new Vec3d(0, 0, -2),  // North
            new Vec3d(1, 0, 1),   // Southeast
            new Vec3d(-1, 0, 1),  // Southwest
            new Vec3d(1, 0, -1),  // Northeast
            new Vec3d(-1, 0, -1), // Northwest
            new Vec3d(3, 0, 0),   // Extended East
            new Vec3d(-3, 0, 0),  // Extended West
            new Vec3d(0, 0, 3),   // Extended South
            new Vec3d(0, 0, -3),  // Extended North
            new Vec3d(2, 1, 0),   // East +1 up
            new Vec3d(-2, 1, 0),  // West +1 up
            new Vec3d(0, 1, 2),   // South +1 up
            new Vec3d(0, 1, -2),  // North +1 up
        };
        
        for (Vec3d offset : offsets) {
            Vec3d testPos = villagerPos.add(offset);
            if (isSafePosition(testPos, player)) {
                return testPos;
            }
        }
        
        // Enhanced fallback: search for safe positions in a wider area
        Vec3d safeFallback = findSafeFallbackPosition(villagerPos, player);
        if (safeFallback != null) {
            return safeFallback;
        }
        
        // Last resort: check if villager position itself is safe
        if (isSafePosition(villagerPos, player)) {
            return villagerPos;
        }
        
        // Emergency fallback: find any safe position nearby
        return findEmergencySafePosition(villagerPos, player);
    }
    
    private static Vec3d findSafeFallbackPosition(Vec3d centerPos, ServerPlayerEntity player) {
        // Search in a 5x5 area around the villager
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 2; y++) {
                    Vec3d testPos = centerPos.add(x, y, z);
                    if (isSafePosition(testPos, player)) {
                        return testPos;
                    }
                }
            }
        }
        return null;
    }
    
    private static Vec3d findEmergencySafePosition(Vec3d centerPos, ServerPlayerEntity player) {
        // Emergency search: find the closest safe position even if far away
        double bestDistance = Double.MAX_VALUE;
        Vec3d bestPos = null;
        
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -2; y <= 3; y++) {
                    Vec3d testPos = centerPos.add(x, y, z);
                    if (isSafePosition(testPos, player)) {
                        double distance = testPos.distanceTo(centerPos);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestPos = testPos;
                        }
                    }
                }
            }
        }
        
        // If still no safe position found, return player's current position as last resort
        return bestPos != null ? bestPos : player.getPos();
    }
    
    private static boolean isSafePosition(Vec3d pos, ServerPlayerEntity player) {
        // Use BlockPos.ofFloored() to correctly convert Vec3d to BlockPos without truncating decimals
        BlockPos blockPos = BlockPos.ofFloored(pos);
        
        // Check for solid ground below the position
        BlockPos groundPos = blockPos.down();
        BlockState groundState = player.getWorld().getBlockState(groundPos);
        boolean hasSolidGround = !groundState.isAir() && groundState.getCollisionShape(player.getWorld(), groundPos).getBoundingBox().getLengthY() >= 0.0625;
        
        // Check if the full 2-block height is clear for the player
        BlockState feetState = player.getWorld().getBlockState(blockPos);
        BlockState headState = player.getWorld().getBlockState(blockPos.up());
        
        boolean feetClear = feetState.isAir() || !feetState.shouldSuffocate(player.getWorld(), blockPos);
        boolean headClear = headState.isAir() || !headState.shouldSuffocate(player.getWorld(), blockPos.up());
        
        // Additional safety checks for hazardous materials
        boolean isLava = player.getWorld().getBlockState(blockPos).getFluidState().isIn(FluidTags.LAVA) ||
                        player.getWorld().getBlockState(blockPos.up()).getFluidState().isIn(FluidTags.LAVA) ||
                        player.getWorld().getBlockState(groundPos).getFluidState().isIn(FluidTags.LAVA);
        
        boolean isWater = player.getWorld().getBlockState(blockPos).getFluidState().isIn(FluidTags.WATER) ||
                         player.getWorld().getBlockState(blockPos.up()).getFluidState().isIn(FluidTags.WATER);
        
        // Check for dangerous blocks like fire, magma, cactus, etc.
        boolean isDangerous = player.getWorld().getBlockState(blockPos).isIn(BlockTags.FIRE) ||
                             player.getWorld().getBlockState(blockPos.up()).isIn(BlockTags.FIRE) ||
                             player.getWorld().getBlockState(groundPos).isIn(BlockTags.FIRE) ||
                             player.getWorld().getBlockState(blockPos).getBlock() instanceof MagmaBlock ||
                             player.getWorld().getBlockState(groundPos).getBlock() instanceof MagmaBlock ||
                             player.getWorld().getBlockState(blockPos).getBlock() instanceof CactusBlock ||
                             player.getWorld().getBlockState(blockPos.up()).getBlock() instanceof CactusBlock;
        
        return hasSolidGround && feetClear && headClear && !isLava && !isDangerous;
    }
}