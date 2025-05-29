package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.llm.LLMApiClientImpl;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.platform.fabric.error.PermanentError;
import com.beeny.villagesreborn.platform.fabric.error.TransientError;
import com.beeny.villagesreborn.platform.fabric.gui.WelcomeScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fabric client-side initialization for Villages Reborn
 * Handles client-specific features and UI components
 */
@Environment(EnvType.CLIENT)
public class VillagesRebornFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn-fabric-client");
    
    private static WelcomeScreenHandler welcomeScreenHandler;
    private static final AtomicInteger retryCount = new AtomicInteger(0);
    private static boolean degradedMode = false;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client for Fabric");
        
        try {
            initializeLogging();
            validateClientEnvironment();
            setupClientFeatures();
            registerEventHandlers();
            
            LOGGER.info("Villages Reborn Fabric client initialization completed successfully");
            
        } catch (Exception e) {
            handleInitializationFailure(e);
            throw new RuntimeException("Fabric client module initialization failed", e);
        }
    }

    private void initializeLogging() {
        LOGGER.debug("Logging initialized for Villages Reborn Fabric client");
    }

    private void validateClientEnvironment() {
        // Basic environment validation
        if (MinecraftClient.getInstance() == null) {
            throw new RuntimeException("Minecraft client instance not available");
        }
        LOGGER.debug("Client environment validation passed");
    }

    private void handleInitializationFailure(Throwable throwable) {
        try {
            Exception categorized = categorizeError(throwable);
            
            if (categorized instanceof TransientError) {
                handleTransientError((TransientError) categorized);
            } else if (categorized instanceof PermanentError) {
                handlePermanentError((PermanentError) categorized);
            } else {
                // Fallback to original behavior
                LOGGER.error("Villages Reborn Fabric client initialization failed", throwable);
            }
        } catch (Exception e) {
            LOGGER.error("Error while handling initialization failure", e);
            LOGGER.error("Original failure", throwable);
        }
    }
    
    private Exception categorizeError(Throwable throwable) {
        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        
        // Categorize as transient errors
        if (throwable instanceof SocketTimeoutException ||
            throwable instanceof ConnectException ||
            message.contains("timeout") ||
            message.contains("connection refused") ||
            message.contains("temporary")) {
            
            int currentRetries = retryCount.get();
            long delay = calculateBackoffDelay(currentRetries);
            return new TransientError("Network or temporary failure: " + throwable.getMessage(),
                                    throwable, currentRetries, delay);
        }
        
        // Categorize as permanent errors
        if (message.contains("api key") ||
            message.contains("authentication") ||
            message.contains("configuration") ||
            message.contains("missing dependency") ||
            throwable instanceof ClassNotFoundException ||
            throwable instanceof NoClassDefFoundError) {
            
            PermanentError.ErrorSeverity severity = determineSeverity(throwable);
            boolean allowDegraded = severity != PermanentError.ErrorSeverity.HIGH;
            
            return new PermanentError("Configuration or dependency issue: " + throwable.getMessage(),
                                    throwable, severity, allowDegraded);
        }
        
        // Default to permanent error for unknown types
        return new PermanentError("Unknown initialization failure: " + throwable.getMessage(),
                                throwable, PermanentError.ErrorSeverity.MEDIUM, true);
    }
    
    private void handleTransientError(TransientError error) {
        if (error.shouldRetry()) {
            int currentRetries = retryCount.incrementAndGet();
            long delay = error.getNextRetryDelay();
            
            LOGGER.warn("Transient error occurred (retry {}/3). Retrying in {} ms: {}",
                       currentRetries, delay, error.getMessage());
            
            // Schedule retry
            Timer retryTimer = new Timer("InitializationRetry");
            retryTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        LOGGER.info("Retrying initialization (attempt {})", currentRetries + 1);
                        // Retry the failed initialization steps
                        retryInitialization();
                        retryTimer.cancel();
                    } catch (Exception e) {
                        handleInitializationFailure(e);
                        retryTimer.cancel();
                    }
                }
            }, delay);
        } else {
            LOGGER.error("Max retries exceeded for transient error, switching to degraded mode: {}",
                        error.getMessage());
            enableDegradedMode("Max retries exceeded");
        }
    }
    
    private void handlePermanentError(PermanentError error) {
        LOGGER.error("Permanent error detected (severity: {}): {}",
                    error.getSeverity(), error.getMessage());
        
        if (error.isAllowDegradedMode()) {
            enableDegradedMode(error.getMessage());
        } else {
            LOGGER.error("Critical permanent error, cannot continue: {}", error.getMessage());
            throw new RuntimeException("Critical initialization failure", error);
        }
    }
    
    private void retryInitialization() {
        // Retry only the components that might have failed
        validateClientEnvironment();
        setupClientFeatures();
        registerEventHandlers();
        LOGGER.info("Retry initialization completed successfully");
    }
    
    private void enableDegradedMode(String reason) {
        degradedMode = true;
        LOGGER.warn("Entering degraded mode due to: {}", reason);
        
        // Initialize minimal functionality
        try {
            // Set up basic welcome screen without advanced features
            welcomeScreenHandler = new WelcomeScreenHandler(
                null, // No hardware manager
                null, // No LLM provider
                FirstTimeSetupConfig.create() // Default config
            );
            
            // Register only essential event handlers
            registerBasicEventHandlers();
            
            LOGGER.info("Degraded mode initialization completed");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize degraded mode", e);
        }
    }
    
    private void registerBasicEventHandlers() {
        // Register only critical event handlers for degraded mode
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.debug("Client joined world (degraded mode)");
        });
        
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            LOGGER.debug("Minecraft client stopping (degraded mode)");
        });
    }
    
    private long calculateBackoffDelay(int retryCount) {
        // Exponential backoff: 1s, 2s, 4s
        return Math.min(1000L * (1L << retryCount), 8000L);
    }
    
    private PermanentError.ErrorSeverity determineSeverity(Throwable throwable) {
        String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
        
        if (throwable instanceof ClassNotFoundException ||
            throwable instanceof NoClassDefFoundError ||
            message.contains("critical") ||
            message.contains("minecraft client")) {
            return PermanentError.ErrorSeverity.HIGH;
        }
        
        if (message.contains("api key") ||
            message.contains("llm") ||
            message.contains("hardware")) {
            return PermanentError.ErrorSeverity.MEDIUM;
        }
        
        return PermanentError.ErrorSeverity.LOW;
    }
    
    /**
     * Check if the client is running in degraded mode
     */
    public static boolean isDegradedMode() {
        return degradedMode;
    }
    
    private void setupClientFeatures() {
        LOGGER.debug("Setting up client-specific features with enhanced dependencies");
        
        // Initialize components with dependency injection
        HardwareInfoManager hardwareManager = HardwareInfoManager.getInstance();
        LLMProviderManager llmManager = createLLMProviderManager();
        FirstTimeSetupConfig setupConfig = FirstTimeSetupConfig.loadWithMigration();
        
        // Initialize welcome screen handler with enhanced features
        welcomeScreenHandler = new WelcomeScreenHandler(
            hardwareManager,
            llmManager,
            setupConfig
        );
        
        // Pre-warm hardware detection in background thread
        CompletableFuture.runAsync(() -> {
            try {
                hardwareManager.getHardwareInfoWithFallback();
                LOGGER.debug("Hardware detection pre-warmed successfully");
            } catch (Exception e) {
                LOGGER.warn("Failed to pre-warm hardware detection", e);
            }
        });
        
        LOGGER.debug("Enhanced welcome screen handler initialized");
    }

    private LLMProviderManager createLLMProviderManager() {
        // Create LLM provider manager with API client
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
        LLMApiClientImpl apiClient = new LLMApiClientImpl(httpClient);
        return new LLMProviderManager(apiClient);
    }
    
    private void registerEventHandlers() {
        LOGGER.debug("Registering enhanced client-side event handlers");
        
        // World join event with improved timing
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.debug("Client joined world, scheduling welcome screen check");
            scheduleWelcomeScreenCheck(client);
        });
        
        // World disconnect event
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Client disconnected from world");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onWorldLeave();
            }
        });
        
        // Client lifecycle events
        ClientLifecycleEvents.CLIENT_STARTED.register((client) -> {
            LOGGER.debug("Minecraft client started");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onClientStarted();
            }
        });
        
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            LOGGER.debug("Minecraft client stopping");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onClientStopping();
            }
        });
        
        // Register client tick event for any per-tick processing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Can be used for periodic checks or updates if needed
        });
        
        LOGGER.debug("Enhanced client event handlers registered successfully");
    }

    private void scheduleWelcomeScreenCheck(MinecraftClient client) {
        // Use proper scheduling to avoid timing issues
        client.execute(() -> {
            Timer timer = new Timer("WelcomeScreenCheck");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (client.world != null && client.player != null && welcomeScreenHandler != null) {
                        client.execute(() -> welcomeScreenHandler.checkAndShowWelcomeScreen());
                    }
                    timer.cancel();
                }
            }, 2000); // 2 second delay to ensure world is fully loaded
        });
    }
    
    /**
     * Get the welcome screen handler instance
     */
    public static WelcomeScreenHandler getWelcomeScreenHandler() {
        return welcomeScreenHandler;
    }
}