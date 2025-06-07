package com.beeny.villagesreborn.core.world;

import java.util.ArrayList;
import java.util.List;

public class VillageChronicle {

    private final List<String> events = new ArrayList<>();

    public void addEvent(String event) {
        events.add(event);
    }

    public List<String> getEvents() {
        return new ArrayList<>(events);
    }

    @Override
    public String toString() {
        return String.join("\n", events);
    }
} 