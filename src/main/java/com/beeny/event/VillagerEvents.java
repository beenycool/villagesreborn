package com.beeny.event;

import com.beeny.config.VillagesConfig;
import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final Map<BlockPos, UUID> chestOwnership = new HashMap<>();
    private static final Map<BlockPos, List<ItemStack>> previousInventoryContents = new HashMap<>();

    public static void registerTheftDetection() {
        UseBlockCallback.EVENT.register(VillagerEvents::onPlayerUseBlock);
        PlayerBlockBreakEvents.AFTER.register(VillagerEvents::onPlayerBreakBlock);
        LOGGER.info("Registered theft detection events");
    }

    private static ActionResult onPlayerUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (world.isClient() || !VillagesConfig.getInstance().getGameplaySettings().isTheftDetectionEnabled()) return ActionResult.PASS;
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory inventory) {
                storeInventoryContents(pos, inventory);
                if (!chestOwnership.containsKey(pos)) checkAndAssignChestOwnership(pos, (ServerWorld)world);
                if (chestOwnership.containsKey(pos)) {
                    UUID ownerId = chestOwnership.get(pos);
                    VillagerEntity owner = findVillagerById(ownerId, (ServerWorld)world);
                    if (owner != null && player.isSneaking()) player.sendMessage(Text.of("§6This chest belongs to " + owner.getName().getString() + "..."), true);
                }
            }
        }
        return ActionResult.PASS;
    }

    private static void onPlayerBreakBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClient() || !VillagesConfig.getInstance().getGameplaySettings().isTheftDetectionEnabled()) return;
        if (chestOwnership.containsKey(pos)) {
            UUID ownerId = chestOwnership.get(pos);
            VillagerEntity owner = findVillagerById(ownerId, (ServerWorld)world);
            if (owner != null) {
                VillagerAI villagerAI = VillagerManager.getInstance().getVillagerAI(ownerId);
                if (villagerAI != null) {
                    villagerAI.recordTheft(player.getUuid(), "storage chest", pos);
                    player.sendMessage(Text.of("§c" + owner.getName().getString() + " notices you breaking their chest!"), false);
                }
            }
            chestOwnership.remove(pos);
        }
    }

    public static void checkForStolenItems(BlockPos pos, World world) {
        if (!previousInventoryContents.containsKey(pos) || !chestOwnership.containsKey(pos)) return;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof Inventory currentInventory)) return;
        List<ItemStack> previousItems = previousInventoryContents.get(pos);
        List<ItemStack> stolenItems = new ArrayList<>();
        for (int i = 0; i < previousItems.size(); i++) {
            ItemStack previousStack = previousItems.get(i);
            ItemStack currentStack = i < currentInventory.size() ? currentInventory.getStack(i) : ItemStack.EMPTY;
            if (!previousStack.isEmpty() && (currentStack.isEmpty() || currentStack.getCount() < previousStack.getCount())) stolenItems.add(previousStack.copy());
        }
        if (!stolenItems.isEmpty() && world instanceof ServerWorld serverWorld) {
            UUID ownerId = chestOwnership.get(pos);
            notifyOwnerAboutTheft(ownerId, stolenItems, pos, serverWorld);
        }
        previousInventoryContents.remove(pos);
    }

    private static void storeInventoryContents(BlockPos pos, Inventory inventory) {
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) contents.add(inventory.getStack(i).copy());
        previousInventoryContents.put(pos, contents);
    }

    private static void checkAndAssignChestOwnership(BlockPos pos, ServerWorld world) {
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(VillagerEntity.class, new Box(pos).expand(16), villager -> true);
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity closest = nearbyVillagers.get(0);
            double closestDistance = closest.getBlockPos().getSquaredDistance(pos);
            for (VillagerEntity villager : nearbyVillagers) {
                double distance = villager.getBlockPos().getSquaredDistance(pos);
                if (distance < closestDistance) {
                    closest = villager;
                    closestDistance = distance;
                }
            }
            if (closestDistance <= 64) {
                chestOwnership.put(pos, closest.getUuid());
                LOGGER.debug("Assigned chest at {} to villager {}", pos, closest.getName().getString());
            }
        }
    }

    private static VillagerEntity findVillagerById(UUID id, ServerWorld world) {
        for (VillagerEntity villager : world.getEntitiesByClass(VillagerEntity.class, new Box(world.getSpawnPos()).expand(256), v -> true)) {
            if (villager.getUuid().equals(id)) return villager;
        }
        return null;
    }

    private static void notifyOwnerAboutTheft(UUID ownerId, List<ItemStack> stolenItems, BlockPos pos, ServerWorld world) {
        VillagerEntity owner = findVillagerById(ownerId, world);
        if (owner == null) return;
        VillagerAI villagerAI = VillagerManager.getInstance().getVillagerAI(ownerId);
        if (villagerAI == null) return;
        PlayerEntity thief = findNearestPlayer(world, pos, 8);
        if (thief == null) return;
        StringBuilder itemDesc = new StringBuilder();
        for (int i = 0; i < Math.min(3, stolenItems.size()); i++) {
            if (i > 0) itemDesc.append(i == stolenItems.size() - 1 ? " and " : ", ");
            itemDesc.append(stolenItems.get(i).getCount()).append("x ").append(stolenItems.get(i).getName().getString());
        }
        if (stolenItems.size() > 3) itemDesc.append(" and other items");
        villagerAI.recordTheft(thief.getUuid(), itemDesc.toString(), pos);
        thief.sendMessage(Text.of("§c" + owner.getName().getString() + " noticed you taking " + itemDesc + " from their chest!"), false);
        VillageInfluenceManager influenceManager = VillageInfluenceManager.getInstance();
        if (influenceManager != null) influenceManager.recordTheft(thief.getUuid(), owner.getUuid(), stolenItems.size());
    }

    private static PlayerEntity findNearestPlayer(ServerWorld world, BlockPos pos, int maxDistance) {
        PlayerEntity closest = null;
        double closestDistance = maxDistance * maxDistance;
        for (PlayerEntity player : world.getPlayers()) {
            double distance = player.getBlockPos().getSquaredDistance(pos);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }
        return closest;
    }

    public static void registerChestOwnership(BlockPos chestPos, UUID villagerUuid) {
        chestOwnership.put(chestPos, villagerUuid);
    }

    public static boolean isChestOwned(BlockPos pos) {
        return chestOwnership.containsKey(pos);
    }

    public static UUID getChestOwner(BlockPos pos) {
        return chestOwnership.get(pos);
    }
}
