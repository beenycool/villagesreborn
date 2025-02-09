package com.villagesreborn.beeny.entities;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.UUID;

public class VillagerBank {
    private final UUID villagerId;
    private final SimpleInventory bankInventory;

    public VillagerBank(UUID villagerId) {
        this.villagerId = villagerId;
        this.bankInventory = new SimpleInventory(27); // Example capacity, can be adjusted
    }

    // Existing deposit method
    public void deposit(ItemStack itemStack) {
        for (int i = 0; i < bankInventory.size(); i++) {
            if (bankInventory.getStack(i).isEmpty()) {
                bankInventory.setStack(i, itemStack.copy());
                return;
            }
        }
    }

    // Existing withdraw method
    public ItemStack withdraw(ItemStack itemStack) {
        for (int i = 0; i < bankInventory.size(); i++) {
            ItemStack stackInSlot = bankInventory.getStack(i);
            if (ItemStack.canCombine(itemStack, stackInSlot) && stackInSlot.getCount() >= itemStack.getCount()) {
                ItemStack withdrawnStack = itemStack.copy();
                stackInSlot.decrement(itemStack.getCount());
                return withdrawnStack;
            }
        }
        return ItemStack.EMPTY;
    }

    // New storage capacity methods
    public int getItemValue(Item item) {
        // Base values for different item types
        switch (item.getTranslationKey()) {
            case "item.minecraft.diamond": return 100;
            case "item.minecraft.gold_ingot": return 50;
            case "item.minecraft.iron_ingot": return 30;
            case "item.minecraft.emerald": return 80;
            case "item.minecraft.netherite_ingot": return 150;
            default: return 10;
        }
    }

    public int getTotalStorageCapacity() {
        return bankInventory.size() * 64; // 64 items per slot
    }

    public int getCurrentStorageUsed() {
        int total = 0;
        for (int i = 0; i < bankInventory.size(); i++) {
            total += bankInventory.getStack(i).getCount();
        }
        return total;
    }

    public boolean hasCapacityFor(ItemStack stack) {
        return getCurrentStorageUsed() + stack.getCount() <= getTotalStorageCapacity();
    }

    public int getStockLevel(Item item) {
        int count = 0;
        for (int i = 0; i < bankInventory.size(); i++) {
            ItemStack stack = bankInventory.getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // Existing inventory and NBT methods
    public SimpleInventory getInventory() {
        return bankInventory;
    }

    private static final Logger LOGGER = LogManager.getLogger();

    public void writeToNBT(Compound
