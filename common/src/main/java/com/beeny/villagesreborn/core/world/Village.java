package com.beeny.villagesreborn.core.world;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Village {

    private final UUID id;
    private String name;
    private final Set<UUID> allies = new HashSet<>();
    private final Set<UUID> enemies = new HashSet<>();
    private final Set<UUID> tradePartners = new HashSet<>();

    public Village(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<UUID> getAllies() {
        return new HashSet<>(allies);
    }

    public void addAlly(UUID villageId) {
        allies.add(villageId);
        enemies.remove(villageId);
    }

    public Set<UUID> getEnemies() {
        return new HashSet<>(enemies);
    }

    public void addEnemy(UUID villageId) {
        enemies.add(villageId);
        allies.remove(villageId);
    }

    public Set<UUID> getTradePartners() {
        return new HashSet<>(tradePartners);
    }

    public void addTradePartner(UUID villageId) {
        tradePartners.add(villageId);
    }

    public void removeAlly(UUID villageId) {
        allies.remove(villageId);
    }

    public void removeEnemy(UUID villageId) {
        enemies.remove(villageId);
    }

    public void removeTradePartner(UUID villageId) {
        tradePartners.remove(villageId);
    }
} 