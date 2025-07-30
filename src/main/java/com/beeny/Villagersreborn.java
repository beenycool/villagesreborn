package com.beeny;

import com.beeny.commands.VillagerCommands;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.data.VillagerData;
import com.beeny.network.VillagerTeleportPacket;
import com.beeny.network.UpdateVillagerNotesPacket;
import com.beeny.network.VillagerMarriagePacket;
import com.beeny.registry.ModItems;
import com.beeny.system.VillagerRelationshipManager;
import com.beeny.system.VillagerScheduleManager;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

public class Villagersreborn implements ModInitializer {
	public static final String MOD_ID = "villagersreborn";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static final Random RANDOM = new Random();
	private static int tickCounter = 0;

	// Attachment for villager name (legacy - kept for compatibility)
	public static final AttachmentType<String> VILLAGER_NAME = AttachmentRegistry.<String>builder()
			.persistent(Codec.STRING)
			.buildAndRegister(Identifier.of(MOD_ID, "villager_name"));
	
	// New attachment for comprehensive villager data
	public static final AttachmentType<VillagerData> VILLAGER_DATA = AttachmentRegistry.<VillagerData>builder()
			.persistent(VillagerData.CODEC)
			.initializer(VillagerData::new)
			.buildAndRegister(Identifier.of(MOD_ID, "villager_data"));

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Villages Reborn mod...");
		
		// Load configuration
		com.beeny.config.ConfigManager.loadConfig();
		
		// Register items
		ModItems.initialize();
		
		// Register commands
		VillagerCommands.register();
		
		// Register networking
		VillagerTeleportPacket.register();
		UpdateVillagerNotesPacket.register();
		VillagerMarriagePacket.register();
		
		// Register events
		registerEvents();
		
		// Register tick events
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			
			// Check for marriages every 5 seconds (100 ticks)
			if (tickCounter % 100 == 0) {
				server.getWorlds().forEach(world -> {
					if (world instanceof ServerWorld serverWorld) {
						checkForMarriages(serverWorld);
					}
				});
			}
			
			// Update villager ages every minute (1200 ticks)
			if (tickCounter % 1200 == 0) {
				server.getWorlds().forEach(world -> {
					if (world instanceof ServerWorld serverWorld) {
						updateVillagerAges(serverWorld);
					}
				});
			}
			
			// Schedule updates every 10 seconds
			if (tickCounter % 200 == 0) {
				server.getWorlds().forEach(world -> {
					if (world instanceof ServerWorld serverWorld) {
						VillagerScheduleManager.updateSchedules(serverWorld);
					}
				});
			}
			
			// Clean up stale proposal times every 30 seconds (600 ticks)
			if (tickCounter % 600 == 0) {
				VillagerRelationshipManager.cleanupStaleProposalTimes();
			}
		});
		
		LOGGER.info("Villages Reborn mod initialized successfully!");
	}
	
	private void registerEvents() {
		// Right-click villager with specific items
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof VillagerEntity villager && !world.isClient) {
				ItemStack heldItem = player.getStackInHand(hand);
				VillagerData data = villager.getAttached(VILLAGER_DATA);
				
				if (data == null) return ActionResult.PASS;
				
				// Feed villager their favorite food
				if (!heldItem.isEmpty() && isFoodItem(heldItem)) {
					Identifier foodId = Registries.ITEM.getId(heldItem.getItem());
					String foodRegistryName = foodId.toString();
					String foodDisplayName = heldItem.getName().getString();
					
					if (data.getFavoriteFood().isEmpty()) {
						// Set favorite food
						data.setFavoriteFood(foodRegistryName);
						data.adjustHappiness(10);
						player.sendMessage(Text.literal(data.getName() + " now loves " + foodDisplayName + "!")
							.formatted(Formatting.GREEN), false);
					} else if (data.getFavoriteFood().equals(foodRegistryName)) {
						// Feeding favorite food
						data.adjustHappiness(5);
						if (!player.isCreative()) {
							heldItem.decrement(1);
						}
						
						// Heart particles
						((ServerWorld) world).spawnParticles(ParticleTypes.HEART,
							villager.getX(), villager.getY() + 2, villager.getZ(),
							5, 0.5, 0.5, 0.5, 0.1);
						
						// Happy sound
						world.playSound(null, villager.getBlockPos(),
							SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 1.0f, 1.0f);
						
						player.sendMessage(Text.literal(data.getName() + " is happy! (+" + 5 + " happiness)")
							.formatted(Formatting.GREEN), false);
					} else {
						// Not favorite food
						data.adjustHappiness(1);
						if (!player.isCreative()) {
							heldItem.decrement(1);
						}
						
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
				
				// Give gift (emerald) to improve relationship
				if (heldItem.isOf(Items.EMERALD)) {
					String playerUuid = player.getUuidAsString();
					data.updatePlayerRelation(playerUuid, 5);
					data.adjustHappiness(3);
					
					if (!player.isCreative()) {
						heldItem.decrement(1);
					}
					
					// Sparkle particles
					((ServerWorld) world).spawnParticles(ParticleTypes.HAPPY_VILLAGER,
						villager.getX(), villager.getY() + 1, villager.getZ(),
						10, 0.5, 0.5, 0.5, 0.1);
					
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
	
	private boolean isFoodItem(ItemStack stack) {
		return stack.getItem().getComponents().contains(DataComponentTypes.FOOD) || 
			   stack.isOf(Items.BREAD) ||
			   stack.isOf(Items.CAKE) ||
			   stack.isOf(Items.COOKIE) ||
			   stack.isOf(Items.APPLE) ||
			   stack.isOf(Items.GOLDEN_APPLE) ||
			   stack.isOf(Items.CARROT) ||
			   stack.isOf(Items.POTATO) ||
			   stack.isOf(Items.BEETROOT) ||
			   stack.isOf(Items.MELON_SLICE) ||
			   stack.isOf(Items.PUMPKIN_PIE);
	}
	
	private void checkForMarriages(ServerWorld world) {
		// Get players in the world to limit search area
		List<ServerPlayerEntity> players = world.getPlayers();
		if (players.isEmpty()) return;
		
		// Create a smaller bounding box around all players (reduced from config value)
		int searchRadius = Math.min(32, VillagersRebornConfig.getBoundingBoxSize() / 4); // Max 32 blocks, or 1/4 of config
		Box searchBox = createBoundingBoxAroundPlayers(players, searchRadius);
		
		List<VillagerEntity> villagers = world.getEntitiesByClass(
			VillagerEntity.class,
			searchBox,
			v -> true
		);
		
		// Check each pair of villagers for potential marriage
		for (int i = 0; i < villagers.size(); i++) {
			VillagerEntity villager1 = villagers.get(i);
			
			for (int j = i + 1; j < villagers.size(); j++) {
				VillagerEntity villager2 = villagers.get(j);
				
				// Check if villagers are near each other before attempting marriage
				double distance = villager1.getPos().distanceTo(villager2.getPos());
				if (distance <= 10.0) { // Only consider villagers within 10 blocks of each other
					// Small chance to attempt marriage if conditions are met
					if (RANDOM.nextFloat() < 0.01f) {
						VillagerRelationshipManager.attemptMarriage(villager1, villager2);
					}
				}
			}
		}
	}
	
	private Box createBoundingBoxAroundPlayers(List<ServerPlayerEntity> players, int radius) {
			if (players.isEmpty()) {
				return new Box(0, 0, 0, 0, 0, 0);
			}
			
			// Start with the first player's position
			BlockPos pos = players.get(0).getBlockPos();
			int minX = pos.getX() - radius;
			int minY = Math.max(pos.getY() - radius, -64); // Minimum Y level
			int minZ = pos.getZ() - radius;
			int maxX = pos.getX() + radius;
			int maxY = Math.min(pos.getY() + radius, 320); // Maximum Y level
			int maxZ = pos.getZ() + radius;
			
			// Expand the box to include all other players
			for (int i = 1; i < players.size(); i++) {
				pos = players.get(i).getBlockPos();
				minX = Math.min(minX, pos.getX() - radius);
				minY = Math.min(minY, Math.max(pos.getY() - radius, -64));
				minZ = Math.min(minZ, pos.getZ() - radius);
				maxX = Math.max(maxX, pos.getX() + radius);
				maxY = Math.max(maxY, Math.min(pos.getY() + radius, 320));
				maxZ = Math.max(maxZ, pos.getZ() + radius);
			}
			
			return new Box(minX, minY, minZ, maxX, maxY, maxZ);
		}
	
	private void updateVillagerAges(ServerWorld world) {
		// Get players in the world to limit search area
		List<ServerPlayerEntity> players = world.getPlayers();
		if (players.isEmpty()) return;
		
		// Create a bounding box around all players
		int searchRadius = VillagersRebornConfig.getBoundingBoxSize();
		Box searchBox = createBoundingBoxAroundPlayers(players, searchRadius);
		
		List<VillagerEntity> villagers = world.getEntitiesByClass(
			VillagerEntity.class,
			searchBox,
			v -> true
		);
		
		for (VillagerEntity villager : villagers) {
			VillagerData data = villager.getAttached(VILLAGER_DATA);
			if (data != null) {
				data.incrementAge();
				
				// Natural happiness decay/recovery
				if (data.getHappiness() > VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD) {
					data.adjustHappiness(-VillagersRebornConfig.HAPPINESS_DECAY_RATE); // Slowly decay to neutral
				} else if (data.getHappiness() < VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD) {
					data.adjustHappiness(VillagersRebornConfig.HAPPINESS_RECOVERY_RATE); // Slowly recover to neutral
				}
			}
		}
	}
}