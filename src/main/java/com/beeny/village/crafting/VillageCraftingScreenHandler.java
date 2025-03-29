package com.beeny.village.crafting;

import com.beeny.village.VillageCraftingManager;
import com.beeny.network.VillageCraftingNetwork;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Handles the server-side logic for the Village Crafting Screen.
 * This class processes crafting requests and manages the interaction between
 * the player and villager for cultural crafting.
 */
public class VillageCraftingScreenHandler extends ScreenHandler {
    private final VillagerEntity villager;
    private final PlayerEntity player;
    private final String culture;
    
    /**
     * Creates a new screen handler for the village crafting interface.
     * 
     * @param syncId The synchronization ID
     * @param playerInventory The player's inventory
     * @param villager The villager involved in crafting
     * @param culture The culture of the village
     */
    public VillageCraftingScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager, String culture) {
        super(null, syncId); // We don't need a type as this is used only for server-side logic
        this.player = playerInventory.player;
        this.villager = villager;
        this.culture = culture;
    }
    
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return player.squaredDistanceTo(villager) < 64.0;
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY; // No slot transfers in this screen
    }
}