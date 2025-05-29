package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.config.AIConfig;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class QuirkDefinition {
    private final String id;
    private final String name;
    private final Predicate<VillagerEntity> precondition;
    private final Function<VillagerEntity, Float> weight;
    private final Consumer<VillagerEntity> action;

    private QuirkDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.precondition = builder.precondition;
        this.weight = builder.weight;
        this.action = builder.action;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean canApply(VillagerEntity villager) {
        return precondition.test(villager);
    }

    public float getWeight(VillagerEntity villager) {
        return weight.apply(villager);
    }

    public void apply(VillagerEntity villager) {
        action.accept(villager);
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static class Builder {
        private final String id;
        private final String name;
        private Predicate<VillagerEntity> precondition = v -> true;
        private Function<VillagerEntity, Float> weight = v -> AIConfig.getInstance().getDefaultQuirkWeight();
        private Consumer<VillagerEntity> action = v -> {};

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder precondition(Predicate<VillagerEntity> precondition) {
            this.precondition = precondition;
            return this;
        }

        public Builder weight(Function<VillagerEntity, Float> weight) {
            this.weight = weight;
            return this;
        }

        public Builder action(Consumer<VillagerEntity> action) {
            this.action = action;
            return this;
        }

        public QuirkDefinition build() {
            return new QuirkDefinition(this);
        }
    }
}