package com.beeny.villagesreborn.core.conversation;

import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.ai.VillagerBrainManager;
import com.beeny.villagesreborn.core.ai.VillagerProximityDetector;
import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.Player;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ConversationRouterImpl
 */
@DisplayName("Conversation Router Tests")
class ConversationRouterTest {

    @Mock
    private VillagerProximityDetector proximityDetector;
    
    @Mock
    private VillagerBrainManager brainManager;
    
    @Mock
    private ResponseDeliveryManager responseManager;
    
    @Mock
    private LLMApiClient llmApiClient;
    
    @Mock
    private Player player;
    
    @Mock
    private VillagerEntity villager;
    
    @Mock
    private VillagerBrain villagerBrain;

    private ConversationRouterImpl conversationRouter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        conversationRouter = new ConversationRouterImpl(
            proximityDetector, brainManager, responseManager, llmApiClient
        );
        
        // Setup default mocks
        UUID playerUUID = UUID.randomUUID();
        UUID villagerUUID = UUID.randomUUID();
        
        when(player.getUUID()).thenReturn(playerUUID);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getBlockPos()).thenReturn(new BlockPos(0, 64, 0));
        
        when(villager.getUUID()).thenReturn(villagerUUID);
        when(villager.getName()).thenReturn("TestVillager");
        when(villager.getProfession()).thenReturn("farmer");
        when(villager.getBlockPos()).thenReturn(new BlockPos(5, 64, 5));
        
        when(brainManager.getBrain(villager)).thenReturn(villagerBrain);
        when(villagerBrain.getPersonalityTraits()).thenReturn(mock(com.beeny.villagesreborn.core.ai.PersonalityProfile.class));
        when(villagerBrain.getCurrentMood()).thenReturn(mock(com.beeny.villagesreborn.core.ai.MoodState.class));
        when(villagerBrain.getCurrentMood().getOverallMood()).thenReturn(com.beeny.villagesreborn.core.ai.MoodCategory.CONTENT);
        when(villagerBrain.getPersonalityTraits().generateDescription()).thenReturn("Friendly and helpful");
    }

    @Test
    @DisplayName("Should route message to single villager")
    void shouldRouteMessageToSingleVillager() throws Exception {
        String testMessage = "Hello villager!";
        ConversationResponse mockResponse = ConversationResponse.success("Hello there!", 10, 100);
        
        when(llmApiClient.generateConversationResponse(any(ConversationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResponse));
        
        conversationRouter.routeMessage(player, villager, testMessage);
        
        // Give async operation time to complete
        Thread.sleep(100);
        
        verify(brainManager).getBrain(villager);
        verify(llmApiClient).generateConversationResponse(any(ConversationRequest.class));
    }

    @Test
    @DisplayName("Should route conversation to nearby villagers")
    void shouldRouteConversationToNearbyVillagers() throws Exception {
        ConversationContext context = new ConversationContext(
            player, "Hello everyone!", System.currentTimeMillis(), "overworld"
        );
        
        List<VillagerEntity> nearbyVillagers = Arrays.asList(villager);
        when(proximityDetector.findNearbyVillagers(any(BlockPos.class), eq(8)))
            .thenReturn(nearbyVillagers);
        
        ConversationResponse mockResponse = ConversationResponse.success("Hello there!", 10, 100);
        when(llmApiClient.generateConversationResponse(any(ConversationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResponse));
        
        conversationRouter.routeConversation(context);
        
        // Give async operation time to complete
        Thread.sleep(100);
        
        verify(proximityDetector).findNearbyVillagers(player.getBlockPos(), 8);
        verify(brainManager).getBrain(villager);
    }

    @Test
    @DisplayName("Should handle no nearby villagers")
    void shouldHandleNoNearbyVillagers() {
        ConversationContext context = new ConversationContext(
            player, "Hello everyone!", System.currentTimeMillis(), "overworld"
        );
        
        when(proximityDetector.findNearbyVillagers(any(BlockPos.class), anyInt()))
            .thenReturn(Collections.emptyList());
        
        conversationRouter.routeConversation(context);
        
        verify(proximityDetector).findNearbyVillagers(player.getBlockPos(), 8);
        verifyNoMoreInteractions(brainManager, llmApiClient, responseManager);
    }

    @Test
    @DisplayName("Should handle LLM failure gracefully")
    void shouldHandleLLMFailureGracefully() throws Exception {
        String testMessage = "Hello villager!";
        ConversationResponse failureResponse = ConversationResponse.failure("API Error");
        
        when(llmApiClient.generateConversationResponse(any(ConversationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(failureResponse));
        
        conversationRouter.routeMessage(player, villager, testMessage);
        
        // Give async operation time to complete
        Thread.sleep(100);
        
        verify(llmApiClient).generateConversationResponse(any(ConversationRequest.class));
        verifyNoInteractions(responseManager);
    }

    @Test
    @DisplayName("Should deliver successful responses")
    void shouldDeliverSuccessfulResponses() throws Exception {
        String testMessage = "Hello villager!";
        String responseText = "Hello there, friend!";
        ConversationResponse mockResponse = ConversationResponse.success(responseText, 10, 100);
        
        when(llmApiClient.generateConversationResponse(any(ConversationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResponse));
        
        conversationRouter.routeMessage(player, villager, testMessage);
        
        // Give async operation time to complete with retry logic
        int retries = 0;
        while (retries < 10) {
            try {
                verify(responseManager).deliverResponse(villager, player, responseText);
                verify(villagerBrain).addInteraction(player, testMessage, responseText);
                verify(brainManager).saveBrain(villager, villagerBrain);
                break;
            } catch (org.mockito.exceptions.verification.WantedButNotInvoked e) {
                if (retries == 9) throw e;
                Thread.sleep(50);
                retries++;
            }
        }
    }

    @Test
    @DisplayName("Should handle concurrent conversations")
    void shouldHandleConcurrentConversations() throws Exception {
        VillagerEntity villager2 = mock(VillagerEntity.class);
        VillagerBrain brain2 = mock(VillagerBrain.class);
        
        when(villager2.getUUID()).thenReturn(UUID.randomUUID());
        when(brainManager.getBrain(villager2)).thenReturn(brain2);
        when(brain2.getPersonalityTraits()).thenReturn(mock(com.beeny.villagesreborn.core.ai.PersonalityProfile.class));
        when(brain2.getCurrentMood()).thenReturn(mock(com.beeny.villagesreborn.core.ai.MoodState.class));
        when(brain2.getCurrentMood().getOverallMood()).thenReturn(com.beeny.villagesreborn.core.ai.MoodCategory.HAPPY);
        when(brain2.getPersonalityTraits().generateDescription()).thenReturn("Cheerful and talkative");
        
        ConversationContext context = new ConversationContext(
            player, "Hello everyone!", System.currentTimeMillis(), "overworld"
        );
        
        List<VillagerEntity> nearbyVillagers = Arrays.asList(villager, villager2);
        when(proximityDetector.findNearbyVillagers(any(BlockPos.class), eq(8)))
            .thenReturn(nearbyVillagers);
        
        ConversationResponse mockResponse = ConversationResponse.success("Hello!", 5, 50);
        when(llmApiClient.generateConversationResponse(any(ConversationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResponse));
        
        conversationRouter.routeConversation(context);
        
        // Give async operations time to complete
        Thread.sleep(200);
        
        verify(brainManager).getBrain(villager);
        verify(brainManager).getBrain(villager2);
        verify(llmApiClient, times(2)).generateConversationResponse(any(ConversationRequest.class));
    }

    @Test
    @DisplayName("Should handle empty or null responses")
    void shouldHandleEmptyOrNullResponses() throws Exception {
        String testMessage = "Hello villager!";
        ConversationResponse emptyResponse = ConversationResponse.success("", 0, 50);
        
        when(llmApiClient.generateConversationResponse(any(ConversationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(emptyResponse));
        
        conversationRouter.routeMessage(player, villager, testMessage);
        
        // Give async operation time to complete
        Thread.sleep(100);
        
        verifyNoInteractions(responseManager);
    }

    @Test
    @DisplayName("Should shutdown gracefully")
    void shouldShutdownGracefully() {
        assertDoesNotThrow(() -> conversationRouter.shutdown());
    }

    @Test
    @DisplayName("Should build proper conversation request")
    void shouldBuildProperConversationRequest() throws Exception {
        String testMessage = "Hello villager!";
        ConversationResponse mockResponse = ConversationResponse.success("Hello!", 10, 100);
        
        when(llmApiClient.generateConversationResponse(any(ConversationRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(mockResponse));
        
        conversationRouter.routeMessage(player, villager, testMessage);
        
        // Give async operation time to complete
        Thread.sleep(100);
        
        verify(llmApiClient).generateConversationResponse(argThat(request -> {
            assertNotNull(request.getPrompt());
            assertTrue(request.getPrompt().contains("TestPlayer"));
            assertTrue(request.getPrompt().contains(testMessage));
            assertEquals(150, request.getMaxTokens());
            assertEquals(0.8f, request.getTemperature(), 0.001f);
            return true;
        }));
    }
}