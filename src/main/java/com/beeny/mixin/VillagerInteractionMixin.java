package com.beeny.mixin;

import com.beeny.gui.TutorialScreen;
import com.beeny.gui.VillageCraftingScreen;
import com.beeny.village.SpawnRegion;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerDialogue;
import com.beeny.village.VillagerFeedbackHelper;
import com.beeny.village.VillagerManager;
import com.beeny.village.VillagerMemory;
import com.beeny.village.crafting.VillageCraftingScreenHandler;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerInteractionMixin {

  private static final Logger vr_LOGGER =
      LoggerFactory.getLogger("VillagesRebornInteractionMixin");

  @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
  private void onInteract(
      PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    VillagerEntity thisVillager = (VillagerEntity) (Object) this;
    World world = thisVillager.getWorld();
    VillagerManager vm = VillagerManager.getInstance();
    VillagerAI villagerAI = vm.getVillagerAI(thisVillager.getUuid());

    // Show tutorial when targeting villager (client-side)
    if (player.getWorld().isClient) {
      TutorialScreen.showIfRelevant(
          net.minecraft.client.MinecraftClient.getInstance(), "targeting_villager");
    }

    // Ensure AI is initialized before proceeding (important check)
    if (villagerAI == null && !world.isClient) {
      vr_LOGGER.warn(
          "VillagerAI was null during interaction for {}. Attempting initialization.",
          thisVillager.getUuid());
      // Ensure initialization runs on the server thread if called from here (though entity load
      // hook is preferred)
      MinecraftServer server = VillagerManager.getServerInstance();
      if (server != null) {
        server.execute(() -> vm.onVillagerSpawn(thisVillager, (ServerWorld) world));
        villagerAI = vm.getVillagerAI(thisVillager.getUuid()); // Re-fetch after potential init
      }
      if (villagerAI == null) {
        vr_LOGGER.error(
            "Failed to initialize VillagerAI for {} during interaction. Aborting custom interaction.",
            thisVillager.getUuid());
        // Let vanilla handle it if AI init fails completely, or provide a basic non-AI
        // interaction
        // For now, let vanilla handle:
        // return;
        // OR, provide a basic message and cancel:
        if (!world.isClient)
          player.sendMessage(Text.literal("This villager seems unresponsive."), false);
        cir.setReturnValue(ActionResult.FAIL); // Indicate failure but consume interaction
        return;
      }
    }

    // --- Sneak Interaction: Show Personality ---
    if (player.isSneaking()) {
      if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) { // Server-side logic
        VillagerDialogue dialogue = vm.getVillagerDialogue(thisVillager.getUuid());
        MinecraftServer server = VillagerManager.getServerInstance(); // Get server instance safely

        if (dialogue != null && server != null) {
          // Use a dedicated method in VillagerDialogue if available, otherwise use generic
          // interaction
          // Let's assume a generic interaction prompt for summary:
          String personalityPrompt =
              "Briefly describe your personality and core values based on your experiences.";

          // Send thinking effect immediately
          VillagerFeedbackHelper.showThinkingEffect(thisVillager);
          final VillagerAI finalVillagerAI = villagerAI; // Capture for lambda
          dialogue // Removed stray 'dialogue' from the previous line
              .generatePlayerInteraction(
                  player, personalityPrompt) // Use a specific interaction type if defined
              .orTimeout(7, TimeUnit.SECONDS) // Add timeout
              .handleAsync(
                  (summary, error) -> { // Use handleAsync for error handling on server thread
                    if (error != null) {
                      vr_LOGGER.error(
                          "Failed to generate personality summary for {}: {}",
                          thisVillager.getUuid(),
                          error.getMessage());
                      // Fallback to basic personality string
                      serverPlayer.sendMessage(
                          Text.literal(
                              "§e[" + thisVillager.getName().getString() + "'s Personality]§r\n"
                                  + (finalVillagerAI != null ? finalVillagerAI.getPersonality() : "Unknown")), // Use final variable
                          false);
                    } else {
                      serverPlayer.sendMessage(
                          Text.literal(
                              "§e[" + thisVillager.getName().getString() + "'s Personality]§r\n§f"
                                  + summary),
                          false);
                      // Add speaking effect after successful generation
                      VillagerFeedbackHelper.showSpeakingEffect(thisVillager);
                    }
                    return null; // Required return for handle
                  },
                  server::execute); // Ensure execution on the server thread
        } else {
          // Fallback if dialogue system isn't ready or AI isn't fully loaded
          String basePersonality =
              (villagerAI != null) ? villagerAI.getPersonality() : "Unknown";
          serverPlayer.sendMessage(
              Text.literal(
                  "§e[" + thisVillager.getName().getString() + "'s Personality]§r\n"
                      + basePersonality),
              false);
          if (villagerAI == null) {
            serverPlayer.sendMessage(Text.literal("§7(AI still initializing...)§r"), false);
          }
        }
      }
      cir.setReturnValue(ActionResult.SUCCESS); // Consume the interaction
      return; // Don't process further
    }

    // --- Normal Interaction --- (Rest of the method as previously defined)
    vr_LOGGER.debug(
        "Player {} interacted with Villager {}", player.getName().getString(), thisVillager.getUuid());

    boolean isClient = world.isClient;
    boolean atWorkstation = isAtWorkstation(thisVillager);
    vr_LOGGER.debug("Villager {} at workstation: {}", thisVillager.getUuid(), atWorkstation);

    if (atWorkstation) {
      // --- Workstation Interaction ---
      if (isClient) {
        SpawnRegion region = vm.getNearestSpawnRegion(thisVillager.getBlockPos());
        String culture = (region != null) ? region.getCultureAsString() : "default";
        vr_LOGGER.debug("Client opening VillageCraftingScreen for culture: {}", culture);
        // Ensure we run this on the client thread if called from networking later
        net.minecraft.client.MinecraftClient.getInstance()
            .execute(
                () ->
                    net.minecraft.client.MinecraftClient.getInstance().setScreen(
                        new VillageCraftingScreen(thisVillager, culture)));
      } else {
        // Server-side workstation interaction handling
        vr_LOGGER.debug(
            "Server handling workstation interaction for Villager {}", thisVillager.getUuid());
        
        if (villagerAI != null) {
          villagerAI.updateActivity("interacting_workstation");
          
          // If the player is a server player, create a screen handler for tracking the interaction
          if (player instanceof ServerPlayerEntity serverPlayer) {
            SpawnRegion region = vm.getNearestSpawnRegion(thisVillager.getBlockPos());
            String culture = (region != null) ? region.getCultureAsString() : "default";
            
            // No need to create an active screen handler here as we'll use the networking system
            // to handle crafting requests when they come in
            
            // The client will send a crafting request when a recipe is selected
            // which will be handled by the VillageCraftingNetwork class
            
            // Just give visual feedback that the villager is ready for crafting
            serverPlayer.sendMessage(
                Text.literal("§6" + thisVillager.getName().getString() + ": §f" + 
                              "What would you like me to craft for you?"),
                false);
            VillagerFeedbackHelper.showTalkingAnimation(thisVillager);
          }
        }
      }
      cir.setReturnValue(ActionResult.SUCCESS); // Prevent vanilla trading screen

    } else {
      // --- Non-Workstation Interaction (Dialogue) ---
      if (!isClient) {
        if (villagerAI != null && player instanceof ServerPlayerEntity serverPlayer) {
          vr_LOGGER.debug(
              "Server handling dialogue interaction for Villager {}", thisVillager.getUuid());
          MinecraftServer server = VillagerManager.getServerInstance(); // Get server instance safely
          if (server == null) {
            vr_LOGGER.error("Server instance is null during dialogue interaction!");
            cir.setReturnValue(ActionResult.FAIL);
            return;
          }

          // Animate villager
          thisVillager.getLookControl().lookAt(player, 30F, 30F);
          VillagerFeedbackHelper.showTalkingAnimation(thisVillager);

          VillagerDialogue dialogue = vm.getVillagerDialogue(thisVillager.getUuid());
          if (dialogue != null) {
            VillagerFeedbackHelper.showThinkingEffect(thisVillager);
            dialogue
                .generatePlayerInteraction(player, "greeting")
                .orTimeout(7, TimeUnit.SECONDS) // Add timeout
                .handleAsync(
                    (response, error) -> { // Use handleAsync
                      String messageToSend;
                      if (error != null) {
                        vr_LOGGER.error(
                            "Failed to generate greeting for {}: {}",
                            thisVillager.getUuid(),
                            error.getMessage());
                        messageToSend = "Hmm?"; // Fallback message
                      } else {
                        Map<String, String> parsed = vm.parseInteractionResponse(response); // Use
                        // manager's
                        // parser
                        String dialogueText = parsed.getOrDefault("DIALOGUE", "Hello there.");
                        // Maybe add actions later? String actions = parsed.getOrDefault("ACTIONS",
                        // "");
                        messageToSend = dialogueText;
                      }

                      serverPlayer.sendMessage(
                          Text.literal("§6" + thisVillager.getName().getString() + ": §f" + messageToSend),
                          false);
                      VillagerFeedbackHelper.showSpeakingEffect(thisVillager);

                      // Inform player about chat interaction method (only once per session maybe?)
                      serverPlayer.sendMessage(
                          Text.literal("§7(Talk back by typing their name in chat)§r"),
                          true // Use action bar
                          );
                      return null; // Required for handle
                    },
                    server::execute); // Ensure execution on the server thread
          } else {
            serverPlayer.sendMessage(Text.literal(thisVillager.getName().getString() + ": Hmm?"), false); // Fallback
            vr_LOGGER.warn("VillagerDialogue system not found for {}", thisVillager.getUuid());
          }
          villagerAI.updateActivity("talking_to_player");
        } else {
          vr_LOGGER.warn(
              "Dialogue interaction skipped: AI null ({}) or player not ServerPlayerEntity ({})",
              villagerAI == null,
              !(player instanceof ServerPlayerEntity));
        }
      }
      // Prevent vanilla screen always, server handles response sending.
      cir.setReturnValue(ActionResult.SUCCESS);
    }
  }

  private boolean isAtWorkstation(VillagerEntity villager) {
    VillagerProfession profession = villager.getVillagerData().getProfession();
    if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
      vr_LOGGER.debug("Villager {} has no profession or is a nitwit", villager.getUuid());
      return false;
    }

    Block expectedWorkstation = getWorkstationForProfession(profession);
    if (expectedWorkstation == null) {
      vr_LOGGER.warn("No workstation mapping found for profession: {}", profession.toString());
      return false;
    }

    // Primary check: JOB_SITE memory module - need to extract BlockPos from GlobalPos
    Optional<net.minecraft.util.math.GlobalPos> jobSiteOpt =
        villager.getBrain().getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.JOB_SITE);
    
    if (jobSiteOpt.isPresent()) {
      // Extract the BlockPos from GlobalPos and check dimension
      net.minecraft.util.math.GlobalPos globalPos = jobSiteOpt.get();
      if (!globalPos.dimension().equals(villager.getWorld().getRegistryKey())) {
        vr_LOGGER.debug("Villager {} JOB_SITE is in different dimension", villager.getUuid());
        return false;
      }
      
      BlockPos poiPos = globalPos.pos();
      double distance = villager.getBlockPos().getSquaredDistance(poiPos);
      BlockState poiState = villager.getWorld().getBlockState(poiPos);
      
      // Debug information about the POI
      vr_LOGGER.debug("Villager {} has JOB_SITE at {} (distance: {}, block: {})", 
          villager.getUuid(), poiPos, Math.sqrt(distance), poiState.getBlock().toString());
      
      // Check distance - villager needs to be close enough to the workstation
      if (distance <= 16.0) { // 4 blocks squared
        // Check if the block at the POI position matches the expected workstation block
        if (poiState.getBlock() == expectedWorkstation) {
          vr_LOGGER.debug("Villager {} confirmed at workstation POI: {}", 
              villager.getUuid(), poiPos);
          return true;
        } else {
          vr_LOGGER.debug("Villager {} JOB_SITE block mismatch: expected {}, found {}", 
              villager.getUuid(), expectedWorkstation.toString(), poiState.getBlock().toString());
        }
      } else {
        vr_LOGGER.debug("Villager {} is too far from JOB_SITE ({}), distance: {}", 
            villager.getUuid(), poiPos, Math.sqrt(distance));
      }
    } else {
      vr_LOGGER.debug("Villager {} has no JOB_SITE memory", villager.getUuid());
    }

    // Only use fallback as a last resort - keeping it minimal for special cases
    // like newly assigned workstations where the memory might not be updated yet
    BlockPos vilPos = villager.getBlockPos();
    for (int y = 0; y <= 1; y++) {  // Only check at feet and head level
      for (int x = -1; x <= 1; x++) {
        for (int z = -1; z <= 1; z++) {
          if (x == 0 && y == 0 && z == 0) continue;
          BlockPos checkPos = vilPos.add(x, y, z);
          if (villager.getWorld().getBlockState(checkPos).getBlock() == expectedWorkstation) {
            vr_LOGGER.debug("Villager {} found at workstation via fallback check at {}", 
                villager.getUuid(), checkPos);
            return true;
          }
        }
      }
    }
    
    vr_LOGGER.debug("Villager {} is not at their workstation", villager.getUuid());
    return false;
  }

  private static Block getWorkstationForProfession(VillagerProfession profession) {
    if (profession == VillagerProfession.FARMER) return Blocks.COMPOSTER;
    if (profession == VillagerProfession.LIBRARIAN) return Blocks.LECTERN;
    if (profession == VillagerProfession.ARMORER) return Blocks.BLAST_FURNACE;
    if (profession == VillagerProfession.WEAPONSMITH) return Blocks.GRINDSTONE;
    if (profession == VillagerProfession.TOOLSMITH) return Blocks.SMITHING_TABLE;
    if (profession == VillagerProfession.CLERIC) return Blocks.BREWING_STAND;
    if (profession == VillagerProfession.FISHERMAN) return Blocks.BARREL;
    if (profession == VillagerProfession.FLETCHER) return Blocks.FLETCHING_TABLE;
    if (profession == VillagerProfession.SHEPHERD) return Blocks.LOOM;
    if (profession == VillagerProfession.BUTCHER) return Blocks.SMOKER;
    if (profession == VillagerProfession.LEATHERWORKER) return Blocks.CAULDRON;
    if (profession == VillagerProfession.MASON) return Blocks.STONECUTTER;
    if (profession == VillagerProfession.CARTOGRAPHER) return Blocks.CARTOGRAPHY_TABLE;
    // In case Minecraft adds new professions in the future
    if (profession == VillagerProfession.NITWIT) return null;
    if (profession == VillagerProfession.NONE) return null;
    
    vr_LOGGER.warn("Unknown profession encountered: {}", profession.toString());
    return null;
  }
}