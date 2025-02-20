package com.beeny.setup;

import com.mojang.blaze3d.platform.GlStateManager;
import com.sun.management.OperatingSystemMXBean;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public class SystemSpecs {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final long MIN_RECOMMENDED_RAM = 4096; // 4GB in MB
    private static final int MIN_RECOMMENDED_THREADS = 4;

    private long totalRam;
    private long availableRam;
    private int cpuThreads;
    private boolean gpuSupport;
    private Map<String, String> gpuInfo;

    public void analyzeSystem() {
        LOGGER.info("Analyzing system specifications...");
        
        // Get RAM information
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        totalRam = osBean.getTotalMemorySize() / (1024 * 1024); // Convert to MB
        availableRam = osBean.getFreeMemorySize() / (1024 * 1024);
        
        LOGGER.info("Memory - Total: {}MB, Available: {}MB", totalRam, availableRam);
        if (availableRam < MIN_RECOMMENDED_RAM) {
            LOGGER.warn("Available RAM ({} MB) is below recommended minimum ({} MB)", 
                availableRam, MIN_RECOMMENDED_RAM);
        }

        // Get CPU information
        cpuThreads = Runtime.getRuntime().availableProcessors();
        LOGGER.info("CPU Threads: {}", cpuThreads);
        if (cpuThreads < MIN_RECOMMENDED_THREADS) {
            LOGGER.warn("CPU thread count ({}) is below recommended minimum ({})", 
                cpuThreads, MIN_RECOMMENDED_THREADS);
        }

        // Check GPU capabilities
        analyzeGPUCapabilities();
    }

    private void analyzeGPUCapabilities() {
        gpuInfo = new HashMap<>();
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getWindow() == null) {
                LOGGER.warn("Cannot analyze GPU capabilities - Minecraft client not initialized");
                gpuSupport = false;
                return;
            }

            // Make sure we're on the render thread
            if (!client.isOnThread()) {
                client.execute(this::getGPUInfo);
            } else {
                getGPUInfo();
            }

            // Determine GPU support based on collected information
            determineGPUSupport();

        } catch (Exception e) {
            LOGGER.error("Error analyzing GPU capabilities", e);
            gpuSupport = false;
        }
    }

    private void getGPUInfo() {
        gpuInfo.put("vendor", GL11.glGetString(GL11.GL_VENDOR));
        gpuInfo.put("renderer", GL11.glGetString(GL11.GL_RENDERER));
        gpuInfo.put("version", GL11.glGetString(GL11.GL_VERSION));
        
        LOGGER.info("GPU Information:");
        LOGGER.info("  Vendor: {}", gpuInfo.get("vendor"));
        LOGGER.info("  Renderer: {}", gpuInfo.get("renderer"));
        LOGGER.info("  OpenGL Version: {}", gpuInfo.get("version"));
    }

    private void determineGPUSupport() {
        String vendor = gpuInfo.get("vendor");
        String renderer = gpuInfo.get("renderer");
        
        // Check for common GPU vendors
        gpuSupport = vendor != null && renderer != null && (
            vendor.toLowerCase().contains("nvidia") ||
            vendor.toLowerCase().contains("amd") ||
            vendor.toLowerCase().contains("intel") ||
            renderer.toLowerCase().contains("geforce") ||
            renderer.toLowerCase().contains("radeon") ||
            renderer.toLowerCase().contains("iris")
        );

        if (gpuSupport) {
            LOGGER.info("GPU support detected: {}", renderer);
        } else {
            LOGGER.warn("No dedicated GPU detected or using basic graphics");
        }
    }

    public long getTotalRam() {
        return totalRam;
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

    public Map<String, String> getGpuInfo() {
        return new HashMap<>(gpuInfo);
    }

    public boolean hasAdequateResources() {
        return availableRam >= MIN_RECOMMENDED_RAM && 
               cpuThreads >= MIN_RECOMMENDED_THREADS && 
               gpuSupport;
    }
}