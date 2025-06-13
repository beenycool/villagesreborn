package com.beeny.villagesreborn.platform.fabric.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight performance monitoring for GUI rendering
 */
public class PerformanceMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitor.class);
    private static final boolean ENABLED = Boolean.getBoolean("villagesreborn.debug.performance");
    
    private long lastFrameTime = 0;
    private long frameCount = 0;
    private long totalRenderTime = 0;
    private final String componentName;
    
    public PerformanceMonitor(String componentName) {
        this.componentName = componentName;
    }
    
    public void startFrame() {
        if (ENABLED) {
            lastFrameTime = System.nanoTime();
        }
    }
    
    public void endFrame() {
        if (ENABLED) {
            long frameTime = System.nanoTime() - lastFrameTime;
            frameCount++;
            totalRenderTime += frameTime;
            
            // Log every 60 frames (roughly 1 second at 60fps)
            if (frameCount % 60 == 0) {
                double avgFrameTimeMs = (totalRenderTime / frameCount) / 1_000_000.0;
                LOGGER.debug("{} - Avg render time: {:.2f}ms over {} frames", 
                    componentName, avgFrameTimeMs, frameCount);
                
                // Reset for next measurement period
                if (frameCount >= 300) { // Reset every 5 seconds
                    frameCount = 0;
                    totalRenderTime = 0;
                }
            }
        }
    }
    
    public static boolean isEnabled() {
        return ENABLED;
    }
} 