package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.common.NBTCompound;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class QuirkExecutionIntegrationTest {
    @Mock private VillagerEntity villager;
    @Mock private Supplier<Long> tickSupplier;
    @Mock private NBTCompound nbtCompound;
    private DefaultRitualScheduler scheduler;
    private QuirkDefinition testQuirk;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        QuirkRegistry.clearRegistry();
        
        scheduler = new DefaultRitualScheduler(tickSupplier);
        
        testQuirk = QuirkDefinition.builder("test", "Test Quirk")
            .precondition(v -> true)
            .action(v -> v.getPersistentData().putString("quirk_executed", "true"))
            .build();
        
        QuirkRegistry.register(testQuirk);
    }

    @Test
    void quirkExecution_integrationTest() {
        when(tickSupplier.get()).thenReturn(100L, 200L);
        when(villager.getPersistentData()).thenReturn(nbtCompound);
        
        // Schedule and execute quirk
        scheduler.scheduleQuirkCheck(villager);
        scheduler.performScheduledChecks();
        
        // Verify quirk was executed
        verify(nbtCompound).putString("quirk_executed", "true");
    }
}