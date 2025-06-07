package com.beeny.villagesreborn.core.expansion.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaravanEntity {

    private final UUID id;
    private final UUID originVillageId;
    private final UUID destinationVillageId;
    private final List<String> cargo = new ArrayList<>();

    public CaravanEntity(UUID originVillageId, UUID destinationVillageId) {
        this.id = UUID.randomUUID();
        this.originVillageId = originVillageId;
        this.destinationVillageId = destinationVillageId;
    }

    public void addCargo(String item) {
        cargo.add(item);
    }

    public List<String> getCargo() {
        return new ArrayList<>(cargo);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOriginVillageId() {
        return originVillageId;
    }

    public UUID getDestinationVillageId() {
        return destinationVillageId;
    }
} 