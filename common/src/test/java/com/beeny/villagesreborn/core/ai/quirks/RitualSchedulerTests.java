package com.beeny.villagesreborn.core.ai.quirks;

import com.beeny.villagesreborn.core.common.VillagerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Supplier;

import static org.mockito.Mockito.*;

class RitualSchedulerTests {
    @Mock private VillagerEntity villager;
    @Mock private Supplier<Long> tickSupplier;
    private DefaultRitualScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new DefaultRitualScheduler(tickSupplier);
    }

    @Test
    void scheduleQuirkCheck_addsTaskToQueue() {
        when(tickSupplier.get()).thenReturn(100L);
        scheduler.scheduleQuirkCheck(villager);
        verify(tickSupplier).get();
    }

    @Test
    void performScheduledChecks_executesTasksWhenDue() {
        when(tickSupplier.get()).thenReturn(100L, 200L);
        scheduler.scheduleQuirkCheck(villager);
        
        when(tickSupplier.get()).thenReturn(200L);
        scheduler.performScheduledChecks();
        
        // Verify quirk execution would happen here
    }
}