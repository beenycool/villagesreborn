package com.villagesreborn.beeny.entities;

import com.villagesreborn.beeny.systems.TaskSystem;
import com.villagesreborn.beeny.systems.GovernanceSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import net.minecraft.entity.ai.goal.PrioritizedGoals;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.world.World;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.attributes.DefaultAttributeContainer;
import net.minecraft.entity.ai.attributes.EntityAttributes;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class Villager extends net.minecraft.entity.passive.VillagerEntity {
    private VillagerBank bank;
    private String name;
    private VillagerRole role;
    private String customSkinId; // Placeholder for future skin implementation
    private static final String[] VILLAGER_FIRST_NAMES = {"Elara", "Finn", "Hazel", "Jasper", "Luna", "Milo", "Olive", "Silas", "Willow", "Arthur", "Liam", "Noah", "Olivia", "Emma", "Ava", "Sophia", "Jackson", "Aiden", "Isabella", "Mia"};    private static final String[] VILLAGER_LAST_NAMES = {"Stone", "Brook", "Meadow", "Creek", "Hill", "Valley", "Ford", "Lake", "River", "Forest", "Glen", "Field", "Dale", "Wood", "Grove", "Banks", "Shores", "Ridge", "Cliff", "Cove"};
    private static final Random RANDOM = new Random();

    private PrioritizedGoals goalSelector;
    private PrioritizedGoals targetSelector;

    // Core systems integration
    private final TaskSystem taskSystem;
    private final GovernanceSystem governanceSystem;
    private final EmotionManager emotionManager;
    private final MemorySystem memorySystem;

    public static DefaultAttributeContainer.Builder createVillagerAttributes() {
        return net.minecraft.entity.passive.VillagerEntity.createVillagerAttributes()
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.7D)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D);
    }


    public Villager(TaskSystem taskSystem, GovernanceSystem governanceSystem,
                    EmotionManager emotionManager, MemorySystem memorySystem, World world) {
        super(EntityType.VILLAGER, world); // Correct super constructor with world
        this.taskSystem = taskSystem;
        this.governanceSystem = governanceSystem;
        this.emotionManager = emotionManager;
        this.memorySystem = memorySystem;
        this.bank = new VillagerBank(this.getUuid()); // Initialize VillagerBank
        this.name = generateVillagerName(); // Method to generate villager name
        this.role = assignVillagerRole(); // Method to assign villager role
        initGoals(); // Call to initialize goals
    }

    @Override
    protected void initGoals() {
        this.goalSelector = getGoalSelector();
        this.targetSelector = getTargetSelector();
        super.initGoals();
        this.goalSelector.add(2, new PatrolBehavior(this, 0.6D)); // Example speed, higher priority
        this.goalSelector.add(1, new AssessItemBehavior(this)); // Priority 1 for item assessment
    }

    private PrioritizedGoals getGoalSelector() {
        if (this.goalSelector == null) {
            this.goalSelector = new PrioritizedGoals(this.getWorld() != null && this.getWorld().getProfiler() != null ? this.getWorld().getProfiler() : null);
        }
        return this.goalSelector;
    }

    private PrioritizedGoals getTargetSelector() {
        if (this.targetSelector == null) {
            this.targetSelector = new PrioritizedGoals(this.getWorld() != null && this.getWorld().getProfiler() != null ? this.getWorld().getProfiler() : null);
        }
        return this.targetSelector;
    }

    private String generateVillagerName() {
        String firstName = VILLAGER_FIRST_NAMES[RANDOM.nextInt(VILLAGER_FIRST_NAMES.length)];
        String lastName = VILLAGER_LAST_NAMES[RANDOM.nextInt(VILLAGER_LAST_NAMES.length)];
        String name = firstName + " " + lastName;
        //name += " [debug]"; // Optionally add debug marker to names
        return name;
    }

    private VillagerRole assignVillagerRole() {
        double traderWeight = 0.4;
        double builderWeight = 0.3;
        double defenderWeight = 0.2;
        double patrollerWeight = 0.1;

        double randomValue = RANDOM.nextDouble();
        if (randomValue < traderWeight) {
            return VillagerRole.TRADER;
        } else if (randomValue < traderWeight + builderWeight) {
            return VillagerRole.BUILDER;
        } else if (randomValue < traderWeight + builderWeight + defenderWeight) {
            return VillagerRole.DEFENDER;
        } else {
            return VillagerRole.PATROLLER;
        }
    }

    public void adjustMood(int delta, String eventType, String description) {
        emotionManager.adjustMood(delta, eventType, description, () -> {
            governanceSystem.handleMoodChange(this, emotionManager.getCurrentState());
        });
    }

    public EmotionManager.EmotionalState getCurrentEmotion() {
        return emotionManager.getCurrentState();
    }

    public void logMemory(String event) {
        memorySystem.logEvent(event);
    }

    public String getRecentMemories() {
        return memorySystem.getRecentMemories();
    }

    public VillagerBank getBank() {
        return bank;
    }

    public String getVillagerName() {
        return name;
    }

    public VillagerRole getVillagerRole() {
        return role;
    }

    public String getCustomSkinId() {
        return customSkinId;
    }

    public void setCustomSkinId(String customSkinId) {
        this.customSkinId = customSkinId;
    }

    @Override
    public void writeCustomDataToNbt(CompoundTag nbt) {
        super.writeCustomDataToNbt(nbt);
        bank.writeToNBT(nbt);
        nbt.putString("villagerName", this.name);
        nbt.putString("villagerRole", this.role.name());
        if (this.customSkinId != null) {
            nbt.putString("customSkinId", this.customSkinId);
        }
    }

    @Override
    public void readCustomDataFromNbt(CompoundTag nbt) {
        super.readCustomDataFromNbt(nbt);
        bank.readFromNBT(nbt);
        if (nbt.contains("villagerName")) {
            this.name = nbt.getString("villagerName");
        }
        if (nbt.contains("villagerRole")) {
            this.role = VillagerRole.valueOf(nbt.getString("villagerRole"));
        }
        if (nbt.contains("customSkinId")) {
            this.customSkinId = nbt.getString("customSkinId");
        }
    }
}
