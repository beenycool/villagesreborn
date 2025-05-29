package com.beeny.villagesreborn.platform.fabric.testing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MockitoValidationTest {
    @Mock private Runnable mockRunnable;
    @InjectMocks private TestSubject subject;

    static class TestSubject {
        private final Runnable runnable;
        TestSubject(Runnable runnable) { this.runnable = runnable; }
        void run() { runnable.run(); }
    }

    @Test
    void mockInjectionWorks() {
        subject.run();
        org.mockito.Mockito.verify(mockRunnable).run();
    }
}