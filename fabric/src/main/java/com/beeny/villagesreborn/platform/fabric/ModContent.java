package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.platform.fabric.block.VillageChronicleBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import java.util.function.Function;

public class ModContent {

    public static Block VILLAGE_CHRONICLE_BLOCK;

    public static void register() {
        VILLAGE_CHRONICLE_BLOCK = registerBlock("village_chronicle", 
            VillageChronicleBlock::new,
            AbstractBlock.Settings.create().sounds(BlockSoundGroup.WOOD).strength(2.5F),
            true);
    }
    
    private static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> blockFactory, AbstractBlock.Settings settings, boolean shouldRegisterItem) {
        // Create a registry key for the block
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("villagesreborn", name));
        
        // Add the registry key to the block settings
        AbstractBlock.Settings finalSettings = settings.registryKey(blockKey);
        
        // Create the block instance
        Block block = blockFactory.apply(finalSettings);
        
        // Register the block
        Registry.register(Registries.BLOCK, blockKey, block);
        
        // Register the item if requested
        if (shouldRegisterItem) {
            RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("villagesreborn", name));
            BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
            Registry.register(Registries.ITEM, itemKey, blockItem);
        }
        
        return block;
    }
} 