package com.villagesreborn.beeny.client.render.entity;

import com.villagesreborn.beeny.entities.Villager;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VillagerRenderer extends VillagerEntityRenderer<Villager, VillagerEntityModel<Villager>> { // Assuming VillagerEntityModel exists or will be created
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Identifier DEFAULT_SKIN = new Identifier("villagesreborn", "textures/entity/villager/default.png"); // Fallback skin

    public VillagerRenderer(EntityRendererFactory.Context context) {
        super(context, new VillagerEntityModel<>(context.getPart(EntityModelLayers.VILLAGER)), 0.5f); // Assuming VillagerEntityModel and EntityModelLayers.VILLAGER exist
    }

    @Override
    public Identifier getTexture(Villager villagerEntity) {
        String customSkinId = villagerEntity.getCustomSkinId();
        VillagerRole role = villagerEntity.getVillagerRole();
        LOGGER.debug("Getting texture for villager {}, customSkinId: {}, role: {}", villagerEntity.getUuid(), customSkinId, role);

        if (customSkinId != null && !customSkinId.isEmpty()) {
            try {
                Identifier customSkin = new Identifier("villagesreborn", "textures/entity/villager/" + customSkinId + ".png");
                LOGGER.debug("Using custom skin {} for villager {}", customSkin, villagerEntity.getUuid());
                return customSkin;
            } catch (Exception e) {
                LOGGER.error("Error loading custom skin {} for villager {}, falling back to role-based skin", customSkinId, villagerEntity.getUuid(), e);
                // Fallback to role-based skin if custom skin loading fails
            }
        }

        // Role-based skin selection
        String skinPath;
        switch (role) {
            case TRADER:
                skinPath = "trader";
                break;
            case BUILDER:
                skinPath = "builder";
                break;
            case DEFENDER:
                skinPath = "defender";
                break;
            case PATROLLER:
                skinPath = "patroller";
                break;
            default:
                skinPath = "default"; // Default skin if role is not recognized
                break;
        }

        Identifier roleSkin = new Identifier("villagesreborn", "textures/entity/villager/" + skinPath + ".png");
        LOGGER.debug("Using role-based skin {} for villager {}, role: {}", roleSkin, villagerEntity.getUuid(), role);
        return roleSkin;
    }
}
