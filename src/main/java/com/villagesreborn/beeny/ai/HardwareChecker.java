package com.villagesreborn.beeny.ai;

import java.lang.management.ManagementFactory;

public class HardwareChecker {
    private static final long MIN_RAM = 2048; // 2GB minimum
    private static final int MIN_CORES = 2;
    private static final long MIN_GPU_MEM = 1024; // 1GB VRAM

    public static boolean isLowEndSystem() {
        return getAvailableRAM() < MIN_RAM || 
               getAvailableCPUCores() < MIN_CORES ||
               getGPUMemory() < MIN_GPU_MEM;
    }

    private static long getAvailableRAM() {
        return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getTotalMemorySize() / (1024 * 1024);
    }

    private static int getAvailableCPUCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    private static long getGPUMemory() {
        // Placeholder for GPU detection - actual implementation may require platform-specific code
        return 2048; // Default to assuming sufficient VRAM
    }
