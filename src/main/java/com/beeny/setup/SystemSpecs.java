package com.beeny.setup;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class SystemSpecs {
    private long availableRam;
    private int cpuThreads;
    private boolean gpuSupport;

    public void analyzeSystem() {
        // Get available RAM
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        availableRam = osBean.getFreeMemorySize() / (1024 * 1024); // Convert to MB

        // Get CPU threads
        cpuThreads = Runtime.getRuntime().availableProcessors();

        // Check GPU support (basic check)
        try {
            String vendor = System.getProperty("sun.java2d.opengl");
            gpuSupport = vendor != null && !vendor.isEmpty();
        } catch (Exception e) {
            gpuSupport = false;
        }
    }

    public long getAvailableRam() {
        return availableRam;
    }

    public int getCpuThreads() {
        return cpuThreads;
    }

    public boolean hasGpuSupport() {
        return gpuSupport;
    }
}