
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
    
    public static Item VILLAGER_JOURNAL;

    
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Villagersreborn.MOD_ID, name), item);
    }

    
    public static void initialize() {
        Villagersreborn.LOGGER.info("Registering Mod Items for " + Villagersreborn.MOD_ID);

        
        
        Identifier journalId = Identifier.of(Villagersreborn.MOD_ID, "villager_journal");
        RegistryKey<Item> journalKey = RegistryKey.of(RegistryKeys.ITEM, journalId);
        VILLAGER_JOURNAL = registerItem("villager_journal", new VillagerJournalItem(new Item.Settings().maxCount(1).registryKey(journalKey)));
    }
}