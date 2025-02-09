package com.villagesreborn.beeny

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import com.villagesreborn.beeny.client.render.entity.VillagerRenderer
import com.villagesreborn.beeny.entities.Villager
import com.villagesreborn.beeny.entities.HorseRidingTrader

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.toast.SystemToast
import net.minecraft.text.Text

object VillagesrebornClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRendererRegistry.register(Villager::class.java, ::VillagerRenderer)
        EntityRendererRegistry.register(HorseRidingTrader::class.java, ::VillagerRenderer)
        
        // Register performance notification handler
        EventDispatcher.registerHandler<PerformanceEvent> { event ->
            MinecraftClient.getInstance().inGameHud.setOverlayMessage(
                LiteralText("System Rating: ${event.displayName}\n${event.advisory}"),
                false
            )
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player != null && !performanceNotificationShown) {
                val rating = HardwareChecker.getPerformanceRating()
                client.toastManager.add(
                    SystemToast(
                        SystemToast.Type.PERIODIC_NOTIFICATION,
                        Text.of("System Performance: ${rating.displayName}"),
                        Text.of(rating.advisory)
                    )
                )
                performanceNotificationShown = true
            }
        }
    }
    
    companion object {
        private var performanceNotificationShown = false
    }
}
