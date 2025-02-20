package com.beeny.village;

import java.util.UUID;

public class Relationship {
    private final UUID villager1;
    private final UUID villager2;
    private String type;

    public Relationship(UUID villager1, UUID villager2, String type) {
        this.villager1 = villager1;
        this.villager2 = villager2;
        this.type = type;
    }

    public UUID getVillager1() {
        return villager1;
    }

    public UUID getVillager2() {
        return villager2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}