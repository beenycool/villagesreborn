package com.villagesreborn.beeny

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import com.villagesreborn.beeny.client.render.entity.VillagerRenderer
import com.villagesreborn.beeny.entities.Villager
import com.villagesreborn.beeny.entities.HorseRidingTrader

object VillagesrebornClient : ClientModInitializer {
    override fun onInitializeClient() {
        EntityRendererRegistry.register(Villager::class.java, ::VillagerRenderer)
        EntityRendererRegistry.register(HorseRidingTrader::class.java, ::VillagerRenderer) // Reusing VillagerRenderer for HorseRidingTrader for now
    }
}
