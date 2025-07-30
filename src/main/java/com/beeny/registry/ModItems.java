// src/main/java/com/beeny/registry/ModItems.java
package com.beeny.registry;

import com.beeny.Villagersreborn;
import com.beeny.item.VillagerJournalItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {
    // Declare the item, but DO NOT initialize it here.
    public static Item VILLAGER_JOURNAL;

    // A simple helper method for registration.
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Villagersreborn.MOD_ID, name), item);
    }

    // All registration logic now happens here.
    public static void initialize() {
        Villagersreborn.LOGGER.info("Registering Mod Items for " + Villagersreborn.MOD_ID);

        // This is the correct time to create and register the item.
        // It happens when onInitialize is called, not during static class loading.
        Identifier journalId = Identifier.of(Villagersreborn.MOD_ID, "villager_journal");
        RegistryKey<Item> journalKey = RegistryKey.of(RegistryKeys.ITEM, journalId);
        VILLAGER_JOURNAL = registerItem("villager_journal", new VillagerJournalItem(new Item.Settings().maxCount(1).registryKey(journalKey)));
    }
}