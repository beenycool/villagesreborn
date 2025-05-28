package com.beeny.villagesreborn.core.memory;

import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.common.NBTCompound;
import com.beeny.villagesreborn.core.common.VillagerEntity;

/**
 * Handles NBT serialization/deserialization for villager entity data
 */
public class VillagerEntityData {
    public static final String VILLAGERS_REBORN_KEY = "villagesreborn";
    
    /**
     * Store villager brain data to NBT
     */
    public void storeVillagerBrain(VillagerEntity villager, VillagerBrain brain) {
        NBTCompound persistentData = villager.getPersistentData();
        NBTCompound villagerData = persistentData.getCompound(VILLAGERS_REBORN_KEY);
        villagerData.put("brain", brain.toNBT());
        persistentData.put(VILLAGERS_REBORN_KEY, villagerData);
    }
    
    /**
     * Load villager brain data from NBT
     */
    public VillagerBrain loadVillagerBrain(VillagerEntity villager) {
        NBTCompound persistentData = villager.getPersistentData();
        
        if (!persistentData.contains(VILLAGERS_REBORN_KEY)) {
            return createDefaultBrain(villager);
        }
        
        NBTCompound villagerData = persistentData.getCompound(VILLAGERS_REBORN_KEY);
        
        if (!villagerData.contains("brain")) {
            return createDefaultBrain(villager);
        }
        
        try {
            NBTCompound brainNBT = villagerData.getCompound("brain");
            return VillagerBrain.fromNBT(brainNBT);
        } catch (Exception e) {
            // Handle corrupted data gracefully
            return createDefaultBrain(villager);
        }
    }
    
    private VillagerBrain createDefaultBrain(VillagerEntity villager) {
        return new VillagerBrain(villager.getUUID());
    }
}