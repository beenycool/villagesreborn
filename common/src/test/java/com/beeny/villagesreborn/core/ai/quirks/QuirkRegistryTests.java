package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class QuirkRegistryTests {
    @Mock private VillagerEntity villager1;
    @Mock private VillagerEntity villager2;
    private QuirkDefinition quirk1;
    private QuirkDefinition quirk2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        QuirkRegistry.clearRegistry();
        
        quirk1 = QuirkDefinition.builder("test1", "Test Quirk 1")
            .precondition(v -> v == villager1)
            .build();
        
        quirk2 = QuirkDefinition.builder("test2", "Test Quirk 2")
            .precondition(v -> v == villager2)
            .build();
        
        QuirkRegistry.register(quirk1);
        QuirkRegistry.register(quirk2);
    }

    @Test
    void getApplicableQuirks_returnsCorrectQuirks() {
        List<QuirkDefinition> quirks1 = QuirkRegistry.getApplicableQuirks(villager1);
        assertEquals(1, quirks1.size());
        assertEquals(quirk1, quirks1.get(0));
        
        List<QuirkDefinition> quirks2 = QuirkRegistry.getApplicableQuirks(villager2);
        assertEquals(1, quirks2.size());
        assertEquals(quirk2, quirks2.get(0));
    }

    @Test
    void register_preventsDuplicateIds() {
        QuirkDefinition duplicate = QuirkDefinition.builder("test1", "Duplicate Quirk").build();
        assertThrows(IllegalArgumentException.class, () -> QuirkRegistry.register(duplicate));
    }
}