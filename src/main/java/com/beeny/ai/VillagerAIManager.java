package com.beeny.ai;

import com.beeny.ai.core.AISubsystem;
import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Refactored AI state manager that implements AISubsystem interface.
 * Manages general AI state and coordination between other subsystems.
 */
public class VillagerAIManager implements AISubsystem {

    public static class VillagerAIState {
        private final Map<String, Object> context = new ConcurrentHashMap<>();
        public volatile boolean isAIActive = true;

        public void setContext(@NotNull String key, @NotNull Object value) {
            context.put(key, value);
        }

        public Object getContext(@NotNull String key) {
            return context.get(key);
        }

        public Map<String, Object> getAllContext() {
            return new HashMap<>(context);
        }
    }

    // Instance state
    private final Map<String, VillagerAIState> aiStates = new ConcurrentHashMap<>();

    public void initializeVillagerAI(@NotNull VillagerEntity villager) {
        String uuid = villager.getUuidAsString();
        aiStates.computeIfAbsent(uuid, k -> new VillagerAIState());
    }

    public VillagerAIState getVillagerAIState(@NotNull VillagerEntity villager) {
        String uuid = villager.getUuidAsString();
        return aiStates.computeIfAbsent(uuid, k -> {
            initializeVillagerAI(villager);
            return aiStates.get(uuid);
        });
    }

    public void clearVillagerAI(@NotNull String villagerUuid) {
        aiStates.remove(villagerUuid);
    }


    public void onVillagerTrade(@NotNull VillagerEntity villager, @NotNull net.minecraft.entity.player.PlayerEntity player, boolean successful, int value) {
        VillagerAIState state = getVillagerAIState(villager);
        state.setContext("last_trade_successful", successful);
        state.setContext("last_trade_value", value);
        state.setContext("last_trade_player", player.getUuidAsString());
    }

    // Tool interaction methods
    public void useHoeTool(VillagerEntity villager) {
        int radius = com.beeny.config.AIConfig.HOE_RADIUS;
        float growthBoost = com.beeny.config.AIConfig.GROWTH_BOOST;
        
        BlockPos villagerPos = villager.getBlockPos();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = villagerPos.add(x, 0, z);
                net.minecraft.block.BlockState state = villager.getWorld().getBlockState(checkPos);
                if (state.getBlock() instanceof net.minecraft.block.CropBlock) {
                    int age = state.get(net.minecraft.block.CropBlock.AGE);
                    if (age < net.minecraft.block.CropBlock.MAX_AGE) {
                        villager.getWorld().setBlockState(checkPos, state.with(net.minecraft.block.CropBlock.AGE, age + 1));
                    }
                }
            }
        }
    }

    public void useFishingRod(VillagerEntity villager) {
        int radius = com.beeny.config.AIConfig.FISHING_RADIUS;
        double successChance = com.beeny.config.AIConfig.FISHING_SUCCESS_CHANCE;
        
        if (villager.getWorld().getRandom().nextDouble() < successChance) {
            net.minecraft.entity.ItemEntity fishEntity = new net.minecraft.entity.ItemEntity(
                villager.getWorld(),
                villager.getX(),
                villager.getY(),
                villager.getZ(),
                new net.minecraft.item.ItemStack(net.minecraft.item.Items.COD)
            );
            villager.getWorld().spawnEntity(fishEntity);
        }
    }

    private List<net.minecraft.block.BlockState> findNearbyCrops(VillagerEntity villager, int radius) {
        BlockPos pos = villager.getBlockPos();
        List<net.minecraft.block.BlockState> crops = new ArrayList<>();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                net.minecraft.block.BlockState state = villager.getWorld().getBlockState(checkPos);
                if (state.getBlock() instanceof net.minecraft.block.CropBlock) {
                    crops.add(state);
                }
            }
        }
        return crops;
    }

    public java.util.List<net.minecraft.text.Text> getVillagerAIStatus(@NotNull VillagerEntity villager) {
        java.util.List<net.minecraft.text.Text> status = new java.util.ArrayList<>();
        VillagerAIState state = getVillagerAIState(villager);

        status.add(net.minecraft.text.Text.literal("AI Status: " + (state.isAIActive ? "Active" : "Inactive")));
        status.add(net.minecraft.text.Text.literal("Context size: " + state.context.size()));

        return status;
    }


    // Methods that were previously static - now migrated to be called through AIWorldManager
    public Map<String, Object> getGlobalAIAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("total_managed_ais", aiStates.size());
        analytics.put("active_ais", (int) aiStates.values().stream().filter(state -> state.isAIActive).count());
        return analytics;
    }

    // AISubsystem implementation
    @Override
    public void initializeVillager(@NotNull VillagerEntity villager, @NotNull VillagerData data) {
        initializeVillagerAI(villager);
    }

    @Override
    public void updateVillager(@NotNull VillagerEntity villager) {
        // General AI state updates can be done here
        // For now, just ensure the state exists
        getVillagerAIState(villager);
    }

    @Override
    public void cleanupVillager(@NotNull String villagerUuid) {
        clearVillagerAI(villagerUuid);
    }

    @Override
    public boolean needsUpdate(@NotNull VillagerEntity villager) {
        VillagerAIState state = aiStates.get(villager.getUuidAsString());
        return state != null && state.isAIActive;
    }

    @Override
    public long getUpdateInterval() {
        return com.beeny.config.AIConfig.AI_MANAGER_UPDATE_INTERVAL;
    }

    @Override
    @NotNull
    public String getSubsystemName() {
        return "AIManager";
    }

    @Override
    public int getPriority() {
        return com.beeny.config.AIConfig.AI_MANAGER_PRIORITY;
    }

    @Override
    public void performMaintenance() {
        // Clean up inactive states periodically
        aiStates.entrySet().removeIf(entry -> !entry.getValue().isAIActive);
    }

    @Override
    public void shutdown() {
        aiStates.clear();
    }

    @Override
    @NotNull
    public Map<String, Object> getAnalytics() {
        return getGlobalAIAnalytics();
    }
}