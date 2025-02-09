package com.villagesreborn.beeny.entities;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.EnumSet;

public class AssessItemBehavior extends Goal {
    private final Villager villager;
    private ItemEntity droppedItem;

    public AssessItemBehavior(Villager villager) {
        this.villager = villager;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (villager.isAiDisabled()) {
            return false;
        }

        World world = villager.getWorld();
        if (world == null) {
            return false;
        }

        // Check for nearby dropped items
        droppedItem = world.getEntitiesByClass(ItemEntity.class, villager.getBoundingBox().expand(2.0D), itemEntity -> true)
                .stream().findFirst().orElse(null);


        return droppedItem != null;
    }

    @Override
    public boolean shouldContinue() {
        return droppedItem != null && droppedItem.isAlive() && villager.squaredDistanceTo(droppedItem) < 4.0D;
    }

    @Override
    public void start() {
        if (droppedItem != null) {
            villager.getNavigation().startMovingTo(droppedItem, 0.6D);
        }
    }

    @Override
    public void tick() {
        if (droppedItem != null && villager.squaredDistanceTo(droppedItem) < 1.5D) {
            assessItemQuality(droppedItem.getStack());
            droppedItem.remove(ItemEntity.RemovalReason.DISCARDED); // Remove the item after assessment
            droppedItem = null; // Reset droppedItem
        }
    }

    @Override
    public void stop() {
        villager.getNavigation().stop();
        droppedItem = null;
    }

    private void assessItemQuality(ItemStack itemStack) {
        if (isGoodQualityItem(itemStack)) {
            depositGoodItem(itemStack);
        } else {
            chuckBadItem(itemStack);
        }
    }

    private boolean isGoodQualityItem(ItemStack itemStack) {
        VillagerBank bank = villager.getBank();
        int prosperity = villager.getVillage().getProsperityLevel();
        
        // Dynamic quality assessment based on multiple factors
        int qualityScore = 0;
        
        // Base item value (configurable)
        qualityScore += bank.getItemValue(itemStack.getItem()) * itemStack.getCount();
        
        // Enchantment bonus
        if (itemStack.hasEnchantments()) {
            qualityScore += itemStack.getEnchantments().size() * 50;
        }
        
        // Durability factor
        if (itemStack.isDamageable()) {
            float durabilityRatio = 1.0f - ((float)itemStack.getDamage() / itemStack.getMaxDamage());
            qualityScore += durabilityRatio * 100;
        }
        
        // Storage status factors
        int storageCapacity = bank.getTotalStorageCapacity();
        int currentStorage = bank.getCurrentStorageUsed();
        float storageRatio = (float)currentStorage / storageCapacity;
        
        // Adjust score based on storage utilization
        if (storageRatio > 0.9) {
            qualityScore *= 0.5; // Reduce score when storage is nearly full
        } else if (storageRatio < 0.5) {
            qualityScore *= 1.2; // Increase score when storage has space
        }
        
        // Prosperity modifiers
        float prosperityModifier = 1.0f + (prosperity / 1000.0f);
        qualityScore *= prosperityModifier;
        
        // Stack size bonus
        if (itemStack.getCount() > 32) {
            qualityScore *= 1.5;
        } else if (itemStack.getCount() > 16) {
            qualityScore *= 1.2;
        }
        
        // Minimum quality threshold based on prosperity
        int minThreshold = Math.max(100, prosperity / 10);
        boolean isAboveThreshold = qualityScore >= minThreshold;
        
        // Final check for storage capacity
        return isAboveThreshold && bank.hasCapacityFor(itemStack);
    }

    private void depositGoodItem(ItemStack itemStack) {
        VillagerBank bank = villager.getBank();
        if (bank.deposit(itemStack.copy())) {
            String itemName = itemStack.getItem().getName().getString();
            if (itemStack.hasEnchantments()) {
                villager.say("An enchanted " + itemName + "! This will be valuable for our village.");
            } else if (itemStack.getCount() >= 16) {
                villager.say("A large stack of " + itemName + "! Our storage is growing.");
            } else {
                villager.say("What a fine " + itemName + "! This will help our village.");
            }
            
            // Trigger prosperity increase
            villager.getVillage().increaseProsperity(10);
        } else {
            villager.say("Our bank is full! I'll have to discard this " + itemStack.getItem().getName().getString());
            chuckItem(itemStack);
        }
    }

    private void chuckBadItem(ItemStack itemStack) {
        String itemName = itemStack.getItem().getName().getString();
        String message;
        
        if (itemStack.isDamageable() && itemStack.getDamage() > 0) {
            message = "A broken " + itemName + "? We can't use this junk!";
        } else if (itemStack.getCount() == 1) {
            message = "A single " + itemName + " isn't worth keeping!";
        } else if (villager.getBank().getCurrentStorageUsed() > 0.9 * villager.getBank().getTotalStorageCapacity()) {
            message = "Even a decent " + itemName + " gets tossed when we're full!";
        } else {
            message = "Ugh, this " + itemName + " is worthless!";
        }
        
        villager.say(message);
        chuckItem(itemStack);
    }

    private void chuckItem(ItemStack itemStack) {
        World world = villager.getWorld();
        if (world != null) {
            // Throw item away with more forceful velocity
            ItemEntity itemEntity = new ItemEntity(world, 
                villager.getX(), villager.getY() + 1.5, villager.getZ(), 
                itemStack.copy());
            
            double velocityMultiplier = 0.4;
            itemEntity.setVelocity(
                villager.getRandom().nextGaussian() * velocityMultiplier,
                0.4 + villager.getRandom().nextFloat() * 0.3,
                villager.getRandom().nextGaussian() * velocityMultiplier);
                
            // Add rotation effect
            itemEntity.setYaw(villager.getRandom().nextFloat() * 360.0F);
            itemEntity.setPitch(villager.getRandom().nextFloat() * 360.0F);
            
            world.spawnEntity(itemEntity);
        }
    }
}
