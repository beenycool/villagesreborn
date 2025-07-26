package com.beeny.registry;

import com.beeny.Villagersreborn;
import com.beeny.item.VillagerJournalItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final VillagerJournalItem VILLAGER_JOURNAL = register("villager_journal", new VillagerJournalItem(new Item.Settings()));

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(Villagersreborn.MOD_ID, name), item);
    }

    public static void initialize() {
        // This method is called to ensure the class is loaded and items are registered
    }
}