package com.beeny.villagesreborn.core.expansion.entities;

import com.beeny.villagesreborn.core.world.DiplomacyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaravanManager {

    private final DiplomacyManager diplomacyManager;
    private final List<CaravanEntity> caravans = new ArrayList<>();

    public CaravanManager(DiplomacyManager diplomacyManager) {
        this.diplomacyManager = diplomacyManager;
    }

    public void sendCaravan(UUID originVillageId, UUID destinationVillageId) {
        // In a real implementation, we would check the diplomatic status between the villages
        // For now, we'll just create the caravan
        CaravanEntity caravan = new CaravanEntity(originVillageId, destinationVillageId);
        caravans.add(caravan);
    }

    public List<CaravanEntity> getCaravans() {
        return new ArrayList<>(caravans);
    }
} 