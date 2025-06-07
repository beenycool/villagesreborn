package com.beeny.villagesreborn.core.world;

import java.util.UUID;

public class DiplomacyManager {

    private final VillageManager villageManager;

    public DiplomacyManager(VillageManager villageManager) {
        this.villageManager = villageManager;
    }

    public void declareWar(UUID village1Id, UUID village2Id) {
        Village village1 = villageManager.getVillage(village1Id);
        Village village2 = villageManager.getVillage(village2Id);
        if (village1 != null && village2 != null) {
            village1.addEnemy(village2Id);
            village2.addEnemy(village1Id);
        }
    }

    public void makePeace(UUID village1Id, UUID village2Id) {
        Village village1 = villageManager.getVillage(village1Id);
        Village village2 = villageManager.getVillage(village2Id);
        if (village1 != null && village2 != null) {
            village1.removeAlly(village2Id);
            village1.removeEnemy(village2Id);
            village2.removeAlly(village1Id);
            village2.removeEnemy(village1Id);
        }
    }

    public void formTradeAgreement(UUID village1Id, UUID village2Id) {
        Village village1 = villageManager.getVillage(village1Id);
        Village village2 = villageManager.getVillage(village2Id);
        if (village1 != null && village2 != null) {
            village1.addTradePartner(village2Id);
            village2.addTradePartner(village1Id);
        }
    }
} 