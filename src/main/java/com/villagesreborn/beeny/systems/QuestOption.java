package com.villagesreborn.beeny.systems;

import java.util.function.Predicate;

public class QuestOption {
    private final String optionDescription;
    private final int nextQuestId; // or null if it concludes the quest
    private final Predicate<VillageState> condition; // Optional condition for branching
    // Additional fields for consequences

    public QuestOption(String optionDescription, int nextQuestId, Predicate<VillageState> condition) {
        this.optionDescription = optionDescription;
        this.nextQuestId = nextQuestId;
        this.condition = condition;
    }

    public String getOptionDescription() {
        return optionDescription;
    }

    public int getNextQuestId() {
        return nextQuestId;
    }

    public Predicate<VillageState> getCondition() {
        return condition;
    }
}
