package com.villagesreborn.beeny.systems;

import java.util.ArrayDeque;
import java.util.Deque;

public class MemorySystem {
    private final Deque<String> memoryLog = new ArrayDeque<>(10);
    private final int maxMemories = 50;
    
    public void logEvent(String event) {
        memoryLog.addLast(formatEvent(event));
        // Memory log maintenance (lines 86-88)
        if (memoryLog.size() > maxMemories) {
            int overflow = memoryLog.size() - maxMemories;
            memoryLog.subList(0, overflow).clear();
        }
    }
    
    private String formatEvent(String rawEvent) {
        return String.format("[%tF %<tT] %s", System.currentTimeMillis(), rawEvent);
    }
    
    public String getRecentMemories() {
        return String.join("\n", memoryLog);
    }
    
    public void clearMemories() {
        memoryLog.clear();
    }

    // Debugging feature to detect potential memory leaks
    public void debugMemoryLogSize() {
        if (memoryLog.size() >= maxMemories) {
            Logger.warn("Memory log size is at maximum capacity or beyond. Potential memory leak.");
            // Optionally, log more details or trigger a memory dump for deeper analysis.
        } else {
            Logger.debug("Memory log size is nominal: " + memoryLog.size() + "/" + maxMemories);
        }
    }
}
