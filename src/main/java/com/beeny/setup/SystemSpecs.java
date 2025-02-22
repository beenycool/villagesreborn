package com.beeny.setup;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
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
    private SystemTier systemTier;

    private final int availableProcessors;
    private final long maxMemory;
    private final int performanceTier;
    private final Map<String, Integer> aiCallQuotas = new HashMap<>();
    private final Map<String, Long> lastAiCalls = new HashMap<>();

    public SystemSpecs() {
        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        this.maxMemory = Runtime.getRuntime().maxMemory();
        this.performanceTier = calculatePerformanceTier();
        initializeQuotas();
    }

    public enum SystemTier {
        HIGH, MEDIUM, LOW
    }

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
        determineSystemTier();
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

    private void determineSystemTier() {
        if (cpuThreads >= 8 && availableRam >= 16384 && isHighEndGpu()) {
            systemTier = SystemTier.HIGH;
        } else if (cpuThreads >= 4 && availableRam >= 8192 && isMidRangeGpu()) {
            systemTier = SystemTier.MEDIUM;
        } else {
            systemTier = SystemTier.LOW;
        }
        LOGGER.info("System determined to be {} tier", systemTier);
    }

    private boolean isHighEndGpu() {
        String renderer = gpuInfo.get("renderer");
        if (renderer == null) return false;
        return renderer.contains("RTX") || 
               renderer.contains("RX 6") || 
               renderer.contains("RX 7") ||
               renderer.contains("Arc");
    }

    private boolean isMidRangeGpu() {
        String renderer = gpuInfo.get("renderer");
        if (renderer == null) return false;
        return renderer.contains("GTX") || 
               renderer.contains("RX 5") ||
               renderer.contains("Iris") ||
               renderer.contains("Vega");
    }

    private int calculatePerformanceTier() {
        // Tier 0: Low-end (2GB RAM, 2 cores)
        // Tier 1: Mid-range (4GB RAM, 4 cores)
        // Tier 2: High-end (8GB+ RAM, 6+ cores)
        int memoryScore = (int)(maxMemory / (1024L * 1024L * 1024L)) / 2; // GB of RAM / 2
        int cpuScore = availableProcessors / 2;
        return Math.min(2, Math.min(memoryScore, cpuScore));
    }

    private void initializeQuotas() {
        // Base quotas per minute, scaled by performance tier
        int baseQuota = switch(performanceTier) {
            case 0 -> 10;  // Low-end: 10 calls per minute
            case 1 -> 30;  // Mid-range: 30 calls per minute
            case 2 -> 60;  // High-end: 60 calls per minute
            default -> 20; // Fallback: 20 calls per minute
        };

        aiCallQuotas.put("villager_behavior", baseQuota);
        aiCallQuotas.put("cultural_event", baseQuota / 2);
        aiCallQuotas.put("building_generation", baseQuota / 3);
    }

    public boolean canMakeAiCall(String type) {
        long now = System.currentTimeMillis();
        Long lastCall = lastAiCalls.get(type);
        
        if (lastCall == null) {
            lastAiCalls.put(type, now);
            return true;
        }

        int quota = aiCallQuotas.getOrDefault(type, 10);
        long minimumInterval = 60000 / quota; // Convert quota per minute to interval in ms
        
        if (now - lastCall >= minimumInterval) {
            lastAiCalls.put(type, now);
            return true;
        }
        
        return false;
    }

    public int getMaxConcurrentGeneration() {
        return Math.max(1, availableProcessors / 2);
    }

    public int getVillagerUpdateInterval() {
        return switch(performanceTier) {
            case 0 -> 40;  // Update every 2 seconds
            case 1 -> 20;  // Update every 1 second
            case 2 -> 10;  // Update every 0.5 seconds
            default -> 30; // Fallback: 1.5 seconds
        };
    }

    public int getMaxActiveVillagers() {
        return switch(performanceTier) {
            case 0 -> 20;   // Low-end: 20 active villagers
            case 1 -> 50;   // Mid-range: 50 active villagers
            case 2 -> 100;  // High-end: 100 active villagers
            default -> 30;  // Fallback: 30 active villagers
        };
    }

    public boolean shouldUseComplexPathfinding() {
        return performanceTier > 0;
    }

    public boolean shouldUseDetailedAnimations() {
        return performanceTier > 1;
    }

    public int getStructureGenerationRadius() {
        return switch(performanceTier) {
            case 0 -> 32;   // Small villages
            case 1 -> 64;   // Medium villages
            case 2 -> 96;   // Large villages
            default -> 48;  // Fallback size
        };
    }

    public int getPerformanceTier() {
        return performanceTier;
    }

    public String getPerformanceReport() {
        return String.format(
            "System Performance Report:\n" +
            "CPU Cores: %d\n" +
            "Max Memory: %d GB\n" +
            "Performance Tier: %d\n" +
            "Max Active Villagers: %d\n" +
            "Village Size: %d blocks\n" +
            "Using Complex Pathfinding: %b\n" +
            "Using Detailed Animations: %b",
            availableProcessors,
            maxMemory / (1024 * 1024 * 1024),
            performanceTier,
            getMaxActiveVillagers(),
            getStructureGenerationRadius(),
            shouldUseComplexPathfinding(),
            shouldUseDetailedAnimations()
        );
    }

    public SystemTier getSystemTier() {
        return systemTier;
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
        return gpuInfo != null ? new HashMap<>(gpuInfo) : new HashMap<>();
    }

    public boolean hasAdequateResources() {
        return availableRam >= MIN_RECOMMENDED_RAM && 
               cpuThreads >= MIN_RECOMMENDED_THREADS && 
               gpuSupport;
    }
}