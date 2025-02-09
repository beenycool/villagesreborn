package com.villagesreborn.beeny

import com.villagesreborn.beeny.ai.HardwareChecker
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import com.villagesreborn.beeny.systems.RoleManager
import com.villagesreborn.beeny.systems.QuestManager // Import QuestManager
import com.villagesreborn.beeny.items.ReturnToBedPotionItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object Villagesreborn : ModInitializer {
    val RETURN_TO_BED_POTION: Item = ReturnToBedPotionItem(Item.Settings().maxCount(1))
    private val logger = LoggerFactory.getLogger("villagesreborn")
    val playerData = VillagesRebornPlayerData()
    val roleManager = RoleManager() // Initialize RoleManager
    val questManager = QuestManager() // Initialize QuestManager

	override fun onInitialize() {
		// Run hardware check first
		val isLowEnd = HardwareChecker.isLowEndSystem()
		logger.info("System check - Low End: $isLowEnd")
		
		// Initialize core systems with hardware-aware configuration
		logger.info("Initializing Villages Reborn AI systems")
		TaskSystem().apply {
			logger.info("Dynamic task system initialized")
		}
		GovernanceSystem(roleManager).apply { // Pass RoleManager to GovernanceSystem
			setTaxRate(0.1f)
			logger.info("Village governance system initialized")
		}
        // Initialize RoleManager and QuestManager
        logger.info("Role management system initialized")
        logger.info("Quest management system initialized")
        QuestManager().apply {
            logger.info("Quest system initialized with example quests")
        }
        
        // Register custom items
        Registry.register(Registry.ITEM, Identifier("villagesreborn", "return_to_bed_potion"), RETURN_TO_BED_POTION)
        logger.info("Custom items registered")

        // Register custom entities
        val HORSE_RIDING_TRADER = Registry.register(
            Registry.ENTITY_TYPE,
            Identifier("villagesreborn", "horse_riding_trader"),
            EntityType.Builder.create { entityType, world -> HorseRidingTrader(entityType, world) }.category(EntityType.Category.MISC).build()
        )
        logger.info("Custom entities registered")

        ModBiomeModifiers.registerBiomeModifiers()
        logger.info("Biome modifiers registered")
    }
}
