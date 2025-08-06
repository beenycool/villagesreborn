package com.beeny.util;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class VillagerDataUtils {
    
    /**
     * Safely gets VillagerData from a villager entity
     */
    public static Optional<VillagerData> getVillagerData(VillagerEntity villager) {
        if (villager == null) return Optional.empty();
        return Optional.ofNullable(villager.getAttached(Villagersreborn.VILLAGER_DATA));
    }
    
    /**
     * Gets VillagerData or returns a new default instance
     */
    public static VillagerData getVillagerDataOrDefault(VillagerEntity villager) {
        return getVillagerData(villager).orElse(new VillagerData());
    }
    
    /**
     * Gets VillagerData or returns null if not present
     */
    public static VillagerData getVillagerDataOrNull(VillagerEntity villager) {
        return getVillagerData(villager).orElse(null);
    }
    
    /**
     * Executes an action if VillagerData is present
     */
    public static void ifVillagerDataPresent(VillagerEntity villager, Consumer<VillagerData> action) {
        getVillagerData(villager).ifPresent(action);
    }
    
    /**
     * Executes an action and returns a result if VillagerData is present, otherwise returns default
     */
    public static <T> T mapVillagerData(VillagerEntity villager, Function<VillagerData, T> mapper, T defaultValue) {
        return getVillagerData(villager).map(mapper).orElse(defaultValue);
    }
    
    /**
     * Executes an action and returns a result if VillagerData is present
     */
    public static <T> Optional<T> mapVillagerData(VillagerEntity villager, Function<VillagerData, T> mapper) {
        return getVillagerData(villager).map(mapper);
    }
    
    /**
     * Checks if villager has valid data attached
     */
    public static boolean hasVillagerData(VillagerEntity villager) {
        return getVillagerData(villager).isPresent();
    }
    
    /**
     * Gets villager name safely, returns default if no data or name
     */
    public static String getVillagerName(VillagerEntity villager, String defaultName) {
        return mapVillagerData(villager, VillagerData::getName, defaultName);
    }
    
    /**
     * Gets villager name or "Unnamed Villager" if not available
     */
    public static String getVillagerName(VillagerEntity villager) {
        return getVillagerName(villager, com.beeny.constants.StringConstants.UNNAMED_VILLAGER);
    }
    
    /**
     * Gets villager happiness safely, returns 0 if no data
     */
    public static int getVillagerHappiness(VillagerEntity villager) {
        return mapVillagerData(villager, VillagerData::getHappiness, 0);
    }
    
    /**
     * Gets villager age safely, returns 0 if no data
     */
    public static int getVillagerAge(VillagerEntity villager) {
        return mapVillagerData(villager, VillagerData::getAge, 0);
    }
    
    /**
     * Safely updates villager data if present
     */
    public static boolean updateVillagerData(VillagerEntity villager, Consumer<VillagerData> updater) {
        Optional<VillagerData> dataOpt = getVillagerData(villager);
        if (dataOpt.isPresent()) {
            updater.accept(dataOpt.get());
            return true;
        }
        return false;
    }
    
    /**
     * Ensures villager has data, creating it if necessary
     */
    public static VillagerData ensureVillagerData(VillagerEntity villager) {
        VillagerData data = getVillagerDataOrNull(villager);
        if (data == null) {
            data = new VillagerData();
            villager.setAttached(Villagersreborn.VILLAGER_DATA, data);
        }
        return data;
    }
}