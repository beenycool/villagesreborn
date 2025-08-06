package com.beeny.mixin;

import com.beeny.Villagersreborn;
import com.beeny.config.AISystemConfig;
import com.beeny.data.VillagerData;
import com.beeny.system.ServerVillagerManager;
import com.beeny.system.VillagerEmotionalBehavior;
import com.beeny.system.VillagerMemoryEnhancer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Focused mixin for AI system integration with villagers.
 * Handles AI lifecycle management and updates.
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerAIIntegrationMixin extends LivingEntity {
    
    @Unique
    private int aiUpdateCounter = 0;
    
    protected VillagerAIIntegrationMixin() {
        super(null, null);
    }
    
    /**
     * Safely execute an operation with the AI manager, with fallback
     */
    @Unique
    private void withAIManager(Consumer<com.beeny.ai.AIWorldManagerRefactored> action, Runnable fallback) {
        try {
            com.beeny.ai.AIWorldManagerRefactored aiManager = com.beeny.ai.AIWorldManagerRefactored.getInstance();
            if (aiManager != null) {
                action.accept(aiManager);
                return;
            }
        } catch (Exception e) {
            // Fall through to fallback
        }
        if (fallback != null) {
            fallback.run();
        }
    }
    
    @Inject(method = "<init>*", at = @At("TAIL"))
    private void initializeAI(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        // Only initialize AI on server side and if villager has data
        if (!villager.getWorld().isClient && 
            villager.getWorld() instanceof ServerWorld &&
            villager.hasAttached(Villagersreborn.VILLAGER_DATA)) {
            
            // Initialize AI systems for this villager
            withAIManager(aiManager -> aiManager.initializeVillagerAI(villager), null);
        }
    }
    
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void updateAI(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data == null || villager.getWorld().isClient) return;
        
        aiUpdateCounter++;
        
        // Run AI subsystem updates through the centralized manager
        if (aiUpdateCounter % AISystemConfig.AI_UPDATE_FREQUENCY == 0) {
            withAIManager(aiManager -> aiManager.updateVillagerAI(villager), null);
        }
        
        // Perform hobby activities
        if (aiUpdateCounter % AISystemConfig.HOBBY_UPDATE_FREQUENCY == 0) {
            withAIManager(
                aiManager -> aiManager.getHobbySystem().performHobbyActivity(villager, data),
                () -> {
                    // Fallback to direct call if AI manager not available
                    com.beeny.system.VillagerHobbySystem tempHobbySystem = new com.beeny.system.VillagerHobbySystem();
                    tempHobbySystem.performHobbyActivity(villager, data);
                }
            );
        }
        
        // Update emotional state
        if (aiUpdateCounter % AISystemConfig.EMOTIONAL_UPDATE_FREQUENCY == 0) {
            VillagerEmotionalBehavior.updateEmotionalState(villager, data);
        }
        
        // Apply personality behaviors
        if (aiUpdateCounter % AISystemConfig.PERSONALITY_UPDATE_FREQUENCY == 0) {
            withAIManager(
                aiManager -> aiManager.getPersonalityBehavior().applyPersonalityEffects(villager, data),
                () -> {
                    // Fallback to direct call if AI manager not available
                    com.beeny.system.VillagerPersonalityBehavior tempPersonalityBehavior = new com.beeny.system.VillagerPersonalityBehavior();
                    tempPersonalityBehavior.applyPersonalityEffects(villager, data);
                }
            );
        }
        
        // Update memories and clean old ones
        if (aiUpdateCounter % AISystemConfig.MEMORY_UPDATE_FREQUENCY == 0) {
            VillagerMemoryEnhancer.updateMemoryBasedOnMood(data);
            VillagerMemoryEnhancer.clearOldMemories(data);
        }
    }
    
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void cleanupAI(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        
        if (villager.getWorld().isClient) return;
        
        // Cleanup AI systems for this villager
        withAIManager(aiManager -> aiManager.cleanupVillagerAI(villager.getUuidAsString()), null);
    }
}