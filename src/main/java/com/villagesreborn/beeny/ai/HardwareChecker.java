package com.villagesreborn.beeny.ai;

import java.lang.management.ManagementFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.cuda.CUDA;

public class HardwareChecker {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long RAM_THRESHOLD_LOW = 4096; // 4GB
    private static final long RAM_THRESHOLD_HIGH = 16384; // 16GB
    private static final int CPU_THRESHOLD_LOW = 4;
    private static final int CPU_THRESHOLD_HIGH = 8;
    private static final long GPU_THRESHOLD_LOW = 2048; // 2GB
    private static final long GPU_THRESHOLD_HIGH = 8192; // 8GB

    public enum PerformanceRating {
        LOW("Low", "May experience performance issues - consider upgrading hardware", 0.5f),
        MEDIUM("Medium", "Adequate for basic village operations", 0.75f),
        HIGH("High", "Optimal for advanced village features", 1.0f);

        public final String displayName;
        public final String advisory;
        public final float performanceFactor;

        PerformanceRating(String displayName, String advisory, float performanceFactor) {
            this.displayName = displayName;
            this.advisory = advisory;
            this.performanceFactor = performanceFactor;
        }
    }

    public static PerformanceRating getPerformanceRating() {
        LOGGER.debug("Performing hardware check...");
        boolean result = getAvailableRAM() < MIN_RAM ||
                        getAvailableCPUCores() < MIN_CORES ||
                        getGPUMemory() < MIN_GPU_MEM;
        LOGGER.info("Low-end system detected: {}", result);
        return result;
    }

    public static String getAccelerationMode() {
        try {
            if (CUDA.isAvailable()) {
                LOGGER.info("CUDA acceleration available");
                return "cuda";
            }
        } catch (Exception e) {
            LOGGER.error("CUDA check failed: {}", e.getMessage());
        }

        try {
            if (CLPlatform.listCLPlatforms().size() > 0) {
                LOGGER.info("OpenCL acceleration available");
                return "opencl";
            }
        } catch (Exception e) {
            LOGGER.error("OpenCL check failed: {}", e.getMessage());
        }

        LOGGER.warn("No GPU acceleration available");
        return "cpu";
    }

    private static long getAvailableRAM() {
        return ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getTotalMemorySize() / (1024 * 1024);
    }

    private static int getAvailableCPUCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    private static long getGPUMemory() {
        try {
            if (CUDA.isAvailable()) {
                CUDA.cuInit(0);
                int[] count = new int[1];
                CUDA.cuDeviceGetCount(count);
                if (count[0] > 0) {
                    long[] device = new long[1];
                    CUDA.cuDeviceGet(device, 0);
                    long[] mem = new long[1];
                    CUDA.cuDeviceTotalMem(mem, device[0]);
                    LOGGER.debug("Detected CUDA VRAM: {} MB", mem[0] / (1024 * 1024));
                    return mem[0] / (1024 * 1024);
                }
            }
        } catch (Exception e) {
            LOGGER.error("CUDA memory check failed: {}", e.getMessage());
        }

        try {
            CLPlatform[] platforms = CLPlatform.listCLPlatforms();
            for (CLPlatform platform : platforms) {
                for (CLDevice device : platform.listCLDevices()) {
                    long globalMemSize = device.getInfoLong(CL_DEVICE_GLOBAL_MEM_SIZE);
                    LOGGER.debug("Detected OpenCL VRAM: {} MB", globalMemSize / (1024 * 1024));
                    return globalMemSize / (1024 * 1024);
                }
            }
        } catch (Exception e) {
            LOGGER.error("OpenCL memory check failed: {}", e.getMessage());
        }

        LOGGER.warn("Failed to detect GPU memory, using default");
        return 2048; // Fallback value
    }
