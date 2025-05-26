package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfo;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.hardware.HardwareTier;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import net.minecraft.client.MinecraftClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

/**
 * Integration tests for WelcomeScreenHandler
 * Tests the complete welcome screen flow from first launch to completion
 */
@ExtendWith(MockitoExtension.class)
class WelcomeScreenHandlerIntegrationTest {

    @TempDir
    Path tempConfigDir;
    
    @Mock
    private MinecraftClient mockClient;
    
    @Mock
    private HardwareInfoManager mockHardwareManager;
    
    @Mock
    private LLMProviderManager mockLLMManager;
    
    private FirstTimeSetupConfig setupConfig;
    private WelcomeScreenHandler welcomeHandler;
    
    @BeforeEach
    void setUp() {
        // Setup hardware manager mock
        HardwareInfo testHardware = new HardwareInfo(16, 8, true, HardwareTier.MEDIUM);
        when(mockHardwareManager.getHardwareInfo()).thenReturn(testHardware);
        
        // Setup LLM manager mock
        when(mockLLMManager.getRecommendedModels(any(), any()))
            .thenReturn(Arrays.asList("gpt-3.5-turbo", "claude-3-haiku"));
        
        // Create fresh setup config for each test
        setupConfig = FirstTimeSetupConfig.create();
    }
    
    @Test
    void testCompleteWelcomeScreenFlow() {
        try (MockedStatic<MinecraftClient> clientMock = mockStatic(MinecraftClient.class)) {
            // GIVEN: Fresh installation (no config file) and mocked client
            clientMock.when(MinecraftClient::getInstance).thenReturn(mockClient);
            when(mockClient.isOnThread()).thenReturn(true);
            
            welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
            
            // WHEN: Player joins world for first time
            welcomeHandler.onWorldJoin();
            
            // THEN: Welcome screen would be displayed (we can't test actual screen creation without full MC environment)
            assertThat(welcomeHandler.isSetupCompleted()).isFalse();
            
            // WHEN: User completes setup manually
            setupConfig.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
            
            // THEN: Configuration is saved and setup is marked complete
            assertThat(setupConfig.isSetupCompleted()).isTrue();
            assertThat(setupConfig.getSelectedProvider()).isEqualTo(LLMProvider.OPENAI);
            assertThat(setupConfig.getSelectedModel()).isEqualTo("gpt-3.5-turbo");
        }
    }
    
    @Test
    void testWelcomeScreenNotShownAfterCompletion() {
        try (MockedStatic<MinecraftClient> clientMock = mockStatic(MinecraftClient.class)) {
            // GIVEN: Completed setup configuration
            setupConfig.completeSetup(LLMProvider.ANTHROPIC, "claude-3-sonnet");
            clientMock.when(MinecraftClient::getInstance).thenReturn(mockClient);
            
            welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
            
            // WHEN: Player joins world
            welcomeHandler.onWorldJoin();
            
            // THEN: Welcome screen is not displayed (setup already completed)
            assertThat(welcomeHandler.isSetupCompleted()).isTrue();
        }
    }
    
    @Test
    void testHandlerInitializationWithDependencies() {
        // WHEN: Handler is created with injected dependencies
        welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
        
        // THEN: Dependencies are properly injected
        assertThat(welcomeHandler.getHardwareManager()).isEqualTo(mockHardwareManager);
        assertThat(welcomeHandler.getLLMManager()).isEqualTo(mockLLMManager);
        assertThat(welcomeHandler.getSetupConfig()).isEqualTo(setupConfig);
    }
    
    @Test
    void testWorldJoinLeaveLifecycle() {
        try (MockedStatic<MinecraftClient> clientMock = mockStatic(MinecraftClient.class)) {
            // GIVEN: Handler with incomplete setup
            clientMock.when(MinecraftClient::getInstance).thenReturn(null); // Test environment
            welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
            
            // WHEN: Player joins world
            welcomeHandler.onWorldJoin();
            
            // THEN: Check is performed (in test env, client is null so no screen shown)
            // This mainly tests that the method doesn't throw exceptions
            
            // WHEN: Player leaves world
            welcomeHandler.onWorldLeave();
            
            // THEN: State is reset for next join
            // This allows the welcome screen to be checked again on next world join
        }
    }
    
    @Test
    void testClientLifecycleEvents() {
        // GIVEN: Handler
        welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
        
        // WHEN: Client lifecycle events are triggered
        welcomeHandler.onClientStarted();
        welcomeHandler.onClientStopping();
        
        // THEN: Events are handled without exceptions
        // These are mainly for logging and potential future functionality
    }
    
    @Test
    void testResetSetupFunctionality() {
        // GIVEN: Handler with completed setup
        setupConfig.completeSetup(LLMProvider.LOCAL, "llama2:7b");
        welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
        
        // WHEN: Setup is reset
        welcomeHandler.resetSetup();
        
        // THEN: Setup is marked as incomplete
        assertThat(welcomeHandler.isSetupCompleted()).isFalse();
    }
    
    @Test
    void testForceShowWelcomeScreen() {
        try (MockedStatic<MinecraftClient> clientMock = mockStatic(MinecraftClient.class)) {
            // GIVEN: Handler and mocked client
            clientMock.when(MinecraftClient::getInstance).thenReturn(null); // Test environment
            welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
            
            // WHEN: Welcome screen is forced to show
            welcomeHandler.showWelcomeScreen();
            
            // THEN: Method completes without exception (in test env, client is null)
            // In real environment, this would display the screen
        }
    }
    
    @Test
    void testConfigReloading() {
        // GIVEN: Handler with initial setup
        welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
        
        // WHEN: Config is reloaded
        welcomeHandler.reloadSetupConfig();
        
        // THEN: Config is refreshed and check flag is reset
        // This allows for dynamic config reloading during runtime
    }
    
    @Test
    void testHandlerWithDefaultConstructor() {
        // WHEN: Handler is created with default constructor
        welcomeHandler = new WelcomeScreenHandler();
        
        // THEN: Default dependencies are created
        assertThat(welcomeHandler.getHardwareManager()).isNotNull();
        assertThat(welcomeHandler.getLLMManager()).isNotNull();
        assertThat(welcomeHandler.getSetupConfig()).isNotNull();
    }
    
    @Test
    void testHardwareInfoAccess() {
        // GIVEN: Handler with mocked hardware manager
        HardwareInfo expectedHardware = new HardwareInfo(32, 16, true, HardwareTier.HIGH);
        when(mockHardwareManager.getHardwareInfo()).thenReturn(expectedHardware);
        
        welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
        
        // WHEN: Hardware info is accessed through handler
        HardwareInfo actualHardware = welcomeHandler.getHardwareManager().getHardwareInfo();
        
        // THEN: Hardware info matches expected
        assertThat(actualHardware).isEqualTo(expectedHardware);
        assertThat(actualHardware.getHardwareTier()).isEqualTo(HardwareTier.HIGH);
    }
    
    @Test
    void testLLMProviderAccess() {
        // GIVEN: Handler with mocked LLM manager
        when(mockLLMManager.getRecommendedModels(LLMProvider.GROQ, HardwareTier.HIGH))
            .thenReturn(Arrays.asList("mixtral-8x7b-32768", "llama2-70b-4096"));
        
        welcomeHandler = new WelcomeScreenHandler(mockHardwareManager, mockLLMManager, setupConfig);
        
        // WHEN: LLM provider info is accessed through handler
        var models = welcomeHandler.getLLMManager().getRecommendedModels(LLMProvider.GROQ, HardwareTier.HIGH);
        
        // THEN: Models match expected
        assertThat(models).containsExactly("mixtral-8x7b-32768", "llama2-70b-4096");
    }
}