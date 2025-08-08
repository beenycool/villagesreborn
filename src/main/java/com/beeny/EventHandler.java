package com.beeny;

import com.beeny.constants.VillagerConstants;
import com.beeny.data.VillagerData;
import com.beeny.network.OpenFamilyTreePacket;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

public final class EventHandler {

    private EventHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof VillagerEntity villager && !world.isClient) {
                ItemStack heldItem = player.getStackInHand(hand);
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);

                if (data == null) return ActionResult.PASS;

                if (heldItem.isEmpty() && player.isSneaking()) {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        Villagersreborn.LOGGER.info("[INFO] [EventHandler] [register] - Sending OpenFamilyTreePacket for villager ID: {}", villager.getId());
                        ServerPlayNetworking.send(serverPlayer, new OpenFamilyTreePacket(villager.getId()));
                        return ActionResult.SUCCESS;
                    }
                }

                if (!heldItem.isEmpty() && isFoodItem(heldItem)) {
                    Identifier foodId = Registries.ITEM.getId(heldItem.getItem());
                    String foodRegistryName = foodId.toString();
                    String foodDisplayName = heldItem.getName().getString();

                    if (data.getFavoriteFood().isEmpty()) {
                        data.setFavoriteFood(foodRegistryName);
                        data.adjustHappiness(VillagerConstants.Relationship.FAVORITE_FOOD_FIRST_TIME_HAPPINESS);

                        com.beeny.system.VillagerMemoryEnhancer.updatePlayerMemory(villager, player, "gift");

                        player.sendMessage(Text.literal(data.getName() + " now loves " + foodDisplayName + "!")
                            .formatted(Formatting.GREEN), false);
                    } else if (data.getFavoriteFood().equals(foodRegistryName)) {
                        data.adjustHappiness(VillagerConstants.Relationship.FAVORITE_FOOD_REPEAT_HAPPINESS);
                        if (!player.isCreative()) {
                            heldItem.decrement(VillagerConstants.Relationship.ONE_ITEM);
                        }

                        com.beeny.system.VillagerMemoryEnhancer.updatePlayerMemory(villager, player, "gift");

                        ((ServerWorld) world).spawnParticles(ParticleTypes.HEART,
                            villager.getX(),
                            villager.getY() + VillagerConstants.Relationship.HEART_OFFSET_Y,
                            villager.getZ(),
                            VillagerConstants.Relationship.HEART_COUNT,
                            VillagerConstants.Relationship.PARTICLE_SPREAD,
                            VillagerConstants.Relationship.PARTICLE_SPREAD,
                            VillagerConstants.Relationship.PARTICLE_SPREAD,
                            VillagerConstants.Relationship.PARTICLE_SPEED);

                        world.playSound(
                            null,
                            villager.getBlockPos(),
                            SoundEvents.ENTITY_VILLAGER_YES,
                            SoundCategory.NEUTRAL,
                            1.0f,
                            1.0f
                        );

                        player.sendMessage(
                            Text.literal(
                                data.getName() + " is happy! (+" +
                                VillagerConstants.Relationship.FAVORITE_FOOD_REPEAT_HAPPINESS +
                                " happiness)"
                            ).formatted(Formatting.GREEN),
                            false
                        );
                    } else {
                        data.adjustHappiness(VillagerConstants.Relationship.NON_FAVORITE_FOOD_HAPPINESS);
                        if (!player.isCreative()) {
                            heldItem.decrement(VillagerConstants.Relationship.ONE_ITEM);
                        }

                        com.beeny.system.VillagerMemoryEnhancer.updatePlayerMemory(villager, player, "gift");

                        Identifier favoriteFoodId = Identifier.tryParse(data.getFavoriteFood());
                        String favoriteFoodDisplayName = data.getFavoriteFood();
                        if (favoriteFoodId != null) {
                            net.minecraft.item.Item favoriteFoodItem = Registries.ITEM.get(favoriteFoodId);
                            if (favoriteFoodItem != null) {
                                favoriteFoodDisplayName = favoriteFoodItem.getName().getString();
                            }
                        }
                        player.sendMessage(Text.literal(data.getName() + " prefers " + favoriteFoodDisplayName)
                            .formatted(Formatting.YELLOW), false);
                    }

                    return ActionResult.SUCCESS;
                }

                if (heldItem.isOf(Items.EMERALD)) {
                    String playerUuid = player.getUuidAsString();
                    data.updatePlayerRelation(playerUuid, VillagerConstants.Relationship.EMERALD_REPUTATION_DELTA);
                    data.adjustHappiness(VillagerConstants.Relationship.EMERALD_HAPPINESS_DELTA);

                    if (!player.isCreative()) {
                        heldItem.decrement(VillagerConstants.Relationship.ONE_ITEM);
                    }

                    ((ServerWorld) world).spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        villager.getX(),
                        villager.getY() + VillagerConstants.Relationship.HAPPY_VILLAGER_OFFSET_Y,
                        villager.getZ(),
                        VillagerConstants.Relationship.HAPPY_VILLAGER_COUNT,
                        VillagerConstants.Relationship.PARTICLE_SPREAD,
                        VillagerConstants.Relationship.PARTICLE_SPREAD,
                        VillagerConstants.Relationship.PARTICLE_SPREAD,
                        VillagerConstants.Relationship.PARTICLE_SPEED
                    );

                    int reputation = data.getPlayerReputation(playerUuid);
                    player.sendMessage(Text.literal("Your reputation with " + data.getName() +
                        " increased! (" + reputation + ")")
                        .formatted(Formatting.GREEN), false);

                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isFoodItem(ItemStack stack) {
        return stack.getItem().getComponents().contains(DataComponentTypes.FOOD);
    }
}