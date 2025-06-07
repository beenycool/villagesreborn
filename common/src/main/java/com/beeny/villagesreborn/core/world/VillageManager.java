package com.beeny.villagesreborn.core.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillageManager {

    private final Map<UUID, Village> villages = new HashMap<>();

    public Village createVillage(String name) {
        UUID id = UUID.randomUUID();
        Village village = new Village(id, name);
        villages.put(id, village);
        return village;
    }

    public void deleteVillage(UUID id) {
        villages.remove(id);
    }

    public Village getVillage(UUID id) {
        return villages.get(id);
    }
} 