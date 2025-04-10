package com.beeny.village;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.border.WorldBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.util.math.Box;

import java.util.List;

public class VillagerWorldTickHandler implements ServerTickEvents.EndWorldTick {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final VillagerWorldTickHandler INSTANCE = new VillagerWorldTickHandler();
    private int tickCounter = 0;
    private static final int VILLAGERS_PER_TICK = 5;

    private VillagerWorldTickHandler() {
    }

    public static VillagerWorldTickHandler getInstance() {
        return INSTANCE;
    }

    public static void init() {
        LOGGER.info("Initializing VillagerWorldTickHandler");
        ServerTickEvents.END_WORLD_TICK.register(INSTANCE);
    }

    @Override
    public void onEndTick(ServerWorld world) {
        tickCounter++;
        if (tickCounter % 2 != 0) {
            return;
        }

        VillagerManager vm = VillagerManager.getInstance();

        // Calculate interval for villager activity updates based on config
        float activityMultiplier = VillagesConfig.getInstance().getGameplaySettings().getVillagerActivityFrequencyMultiplier();
        if (activityMultiplier <= 0) activityMultiplier = 1.0f; // Prevent division by zero or negative intervals
        int baseActivityInterval = 100; // Default: 100 ticks (5 seconds)
        // Multiplier > 1 means less frequent (longer interval), Multiplier < 1 means more frequent (shorter interval)
        int adjustedActivityInterval = (int) (baseActivityInterval / activityMultiplier);
        // Clamp interval to reasonable bounds (e.g., 1 second to 1 minute)
        adjustedActivityInterval = Math.max(20, Math.min(1200, adjustedActivityInterval));

        if (tickCounter % adjustedActivityInterval == 0) {
            vm.updateVillagerActivities(world);
        }

        WorldBorder border = world.getWorldBorder();
        Box searchBox = new Box(border.getBoundWest(), world.getBottomY(), border.getBoundSouth(), border.getBoundEast(), world.getHeight(), border.getBoundNorth());
        List<VillagerEntity> loadedVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            villager -> true
        );

        int startIdx = (tickCounter / 2) % Math.max(1, loadedVillagers.size());
        int endIdx = Math.min(startIdx + VILLAGERS_PER_TICK, loadedVillagers.size());

        for (int i = startIdx; i < endIdx; i++) {
            VillagerEntity villager = loadedVillagers.get(i);
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());

            if (ai != null) {
                ai.tickBehavior(world);
            }
        }
    }

    public void processVillageEvents(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            VillagerManager vm = VillagerManager.getInstance();

            if (tickCounter % 1200 == 0) {
                vm.updateCulturalEvents(world);
            }

            if (tickCounter % 6000 == 0) {
                vm.updateVillageStats(world);
            }
        }
    }
}