package com.beeny.villagesreborn.core.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal MemoryBank implementation for TDD
 */
public class MemoryBank {
    private final List<String> significantMemories = new ArrayList<>();

    public List<String> getSignificantMemories() {
        return new ArrayList<>(significantMemories);
    }

    public void addSignificantMemory(String memory) {
        significantMemories.add(memory);
    }
}