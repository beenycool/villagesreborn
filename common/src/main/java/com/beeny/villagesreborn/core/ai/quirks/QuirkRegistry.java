package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuirkRegistry {
    private static final Map<String, QuirkDefinition> registry = new HashMap<>();
    
    public static void register(QuirkDefinition quirk) {
        if (registry.containsKey(quirk.getId())) {
            throw new IllegalArgumentException("Quirk with ID " + quirk.getId() + " already registered");
        }
        registry.put(quirk.getId(), quirk);
    }
    
    public static List<QuirkDefinition> getApplicableQuirks(VillagerEntity villager) {
        if (villager == null) return Collections.emptyList();
        
        return registry.values().stream()
            .filter(quirk -> quirk.canApply(villager))
            .collect(Collectors.toList());
    }
    
    public static void clearRegistry() {
        registry.clear();
    }
}