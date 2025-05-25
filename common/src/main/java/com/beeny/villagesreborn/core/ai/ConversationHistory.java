package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.NBTCompound;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal ConversationHistory implementation for TDD
 */
public class ConversationHistory {
    private final List<ConversationInteraction> interactions = new ArrayList<>();

    public void addInteraction(ConversationInteraction interaction) {
        interactions.add(interaction);
    }

    public List<ConversationInteraction> getInteractions() {
        return new ArrayList<>(interactions);
    }

    public List<ConversationInteraction> getRecent(int count) {
        int start = Math.max(0, interactions.size() - count);
        return new ArrayList<>(interactions.subList(start, interactions.size()));
    }

    public int size() {
        return interactions.size();
    }

    public void cleanup(long threshold, int maxSize) {
        // Remove old interactions
        interactions.removeIf(i -> i.getTimestamp() < threshold);
        
        // Limit size
        while (interactions.size() > maxSize) {
            interactions.remove(0);
        }
    }

    public NBTCompound toNBT() {
        NBTCompound nbt = new NBTCompound();
        nbt.putInt("size", interactions.size());
        return nbt;
    }

    public static ConversationHistory fromNBT(NBTCompound nbt) {
        return new ConversationHistory();
    }
}