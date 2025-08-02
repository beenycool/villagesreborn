package com.beeny;

import com.beeny.commands.VillagerCommands;
import com.beeny.config.VillagersRebornConfig;
import com.beeny.data.VillagerData;
import com.beeny.dialogue.LLMDialogueManager;
import com.beeny.network.VillagerTeleportPacket;
import com.beeny.network.UpdateVillagerNotesPacket;
import com.beeny.network.VillagerMarriagePacket;
import com.beeny.network.OpenFamilyTreePacket;
import com.beeny.network.FamilyTreeDataPacket;
import com.beeny.network.RequestVillagerListPacket;
import com.beeny.network.TestLLMConnectionPacket;
import com.beeny.network.TestLLMConnectionResultPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.beeny.registry.ModItems;
import com.beeny.system.VillagerRelationshipManager;
import com.beeny.system.VillagerScheduleManager;
import com.beeny.system.ServerVillagerManager;
import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

	
	public static final AttachmentType<String> VILLAGER_NAME = AttachmentRegistry.<String>builder()
			.persistent(Codec.STRING)
			.buildAndRegister(Identifier.of(MOD_ID, "villager_name"));
	
	
	public static final AttachmentType<VillagerData> VILLAGER_DATA = AttachmentRegistry.<VillagerData>builder()
			.persistent(VillagerData.CODEC)
			.initializer(VillagerData::new)
			.buildAndRegister(Identifier.of(MOD_ID, "villager_data"));

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Villages Reborn mod...");
		
		
		com.beeny.config.ConfigManager.loadConfig();
		
		
		ModItems.initialize();
		
		
		VillagerCommands.register();
		
		// Initialize ServerVillagerManager on server start
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerVillagerManager.getInstance().initialize(server);
			// Initialize LLM dialogue system
			try {
				LLMDialogueManager.initialize();
			} catch (Exception e) {
				System.err.println("Failed to initialize LLMDialogueManager: " + e.getMessage());
				e.printStackTrace();
				// Optionally, add further handling here if needed
			}
		});
		
		// Shutdown LLM on server stop
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			try {
				LLMDialogueManager.shutdown();
			} catch (Exception e) {
				System.err.println("Failed to shutdown LLMDialogueManager: " + e.getMessage());
				e.printStackTrace();
				// Optionally, add further handling here if needed
			}
		});
		
		
		VillagerTeleportPacket.register();
		UpdateVillagerNotesPacket.register();
		VillagerMarriagePacket.register();
		OpenFamilyTreePacket.register();
		FamilyTreeDataPacket.register();
		RequestVillagerListPacket.register();
		TestLLMConnectionPacket.register();
		TestLLMConnectionResultPacket.register();
		
		
		registerEvents();
		
		
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter++;
			
			
			if (tickCounter % 100 == 0) {
				server.getWorlds().forEach(world -> {
					if (world instanceof ServerWorld serverWorld) {
						checkForMarriages(serverWorld);
					}
				});
			}
			
			
			if (tickCounter % 1200 == 0) {
				server.getWorlds().forEach(world -> {
					if (world instanceof ServerWorld serverWorld) {
						updateVillagerAges(serverWorld);
					}
				});
			}
			
			
			if (tickCounter % 200 == 0) {
				server.getWorlds().forEach(world -> {
					if (world instanceof ServerWorld serverWorld) {
						VillagerScheduleManager.updateSchedules(serverWorld);
					}
				});
			}
			
			
			if (tickCounter % 600 == 0) {
				VillagerRelationshipManager.cleanupStaleProposalTimes();
			}
		});
		
		LOGGER.info("Villages Reborn mod initialized successfully!");
	}
	
	private void registerEvents() {
		
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (entity instanceof VillagerEntity villager && !world.isClient) {
				ItemStack heldItem = player.getStackInHand(hand);
				VillagerData data = villager.getAttached(VILLAGER_DATA);
				
				if (data == null) return ActionResult.PASS;
				
				// Check for empty hand (right-click to open family tree)
				if (heldItem.isEmpty() && player.isSneaking()) {
					if (player instanceof ServerPlayerEntity serverPlayer) {
						LOGGER.info("[Villagersreborn] Sending OpenFamilyTreePacket for villager ID: " + villager.getId());
						// Send packet to open family tree GUI
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
						data.adjustHappiness(10);
						player.sendMessage(Text.literal(data.getName() + " now loves " + foodDisplayName + "!")
							.formatted(Formatting.GREEN), false);
					} else if (data.getFavoriteFood().equals(foodRegistryName)) {
						
						data.adjustHappiness(5);
						if (!player.isCreative()) {
							heldItem.decrement(1);
						}
						
						
						((ServerWorld) world).spawnParticles(ParticleTypes.HEART,
							villager.getX(), villager.getY() + 2, villager.getZ(),
							5, 0.5, 0.5, 0.5, 0.1);
						
						
						world.playSound(null, villager.getBlockPos(),
							SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 1.0f, 1.0f);
						
						player.sendMessage(Text.literal(data.getName() + " is happy! (+" + 5 + " happiness)")
							.formatted(Formatting.GREEN), false);
					} else {
						
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
				
				
				if (heldItem.isOf(Items.EMERALD)) {
					String playerUuid = player.getUuidAsString();
					data.updatePlayerRelation(playerUuid, 5);
					data.adjustHappiness(3);
					
					if (!player.isCreative()) {
						heldItem.decrement(1);
					}
					
					
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
		return stack.getItem().getComponents().contains(DataComponentTypes.FOOD);
	}
	
	private void checkForMarriages(ServerWorld world) {
		
		List<ServerPlayerEntity> players = world.getPlayers();
		if (players.isEmpty()) return;
		
		
		int searchRadius = Math.min(32, VillagersRebornConfig.getBoundingBoxSize() / 4); 
		Box searchBox = createBoundingBoxAroundPlayers(players, searchRadius);
		
		List<VillagerEntity> villagers = world.getEntitiesByClass(
			VillagerEntity.class,
			searchBox,
			v -> true
		);
		
		
		for (int i = 0; i < villagers.size(); i++) {
			VillagerEntity villager1 = villagers.get(i);
			
			for (int j = i + 1; j < villagers.size(); j++) {
				VillagerEntity villager2 = villagers.get(j);
				
				
				double distance = villager1.getPos().distanceTo(villager2.getPos());
				if (distance <= 10.0) { 
					
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
			
			
			BlockPos pos = players.get(0).getBlockPos();
			int minX = pos.getX() - radius;
			int minY = Math.max(pos.getY() - radius, -64); 
			int minZ = pos.getZ() - radius;
			int maxX = pos.getX() + radius;
			int maxY = Math.min(pos.getY() + radius, 320); 
			int maxZ = pos.getZ() + radius;
			
			
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
		
		List<ServerPlayerEntity> players = world.getPlayers();
		if (players.isEmpty()) return;
		
		
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
				
				
				if (data.getHappiness() > VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD) {
					data.adjustHappiness(-VillagersRebornConfig.HAPPINESS_DECAY_RATE); 
				} else if (data.getHappiness() < VillagersRebornConfig.HAPPINESS_NEUTRAL_THRESHOLD) {
					data.adjustHappiness(VillagersRebornConfig.HAPPINESS_RECOVERY_RATE); 
				}
			}
		}
	}
}