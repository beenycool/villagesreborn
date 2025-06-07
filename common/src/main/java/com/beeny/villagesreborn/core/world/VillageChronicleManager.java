package com.beeny.villagesreborn.core.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillageChronicleManager {

    private final Map<UUID, VillageChronicle> chronicles = new HashMap<>();

    public VillageChronicle getChronicle(UUID villageId) {
        return chronicles.computeIfAbsent(villageId, id -> new VillageChronicle());
    }

    public void addEvent(UUID villageId, String event) {
        getChronicle(villageId).addEvent(event);
    }
} 