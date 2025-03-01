package com.beeny.village;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CulturalTextureManager {
    private static final Map<String, Identifier> CULTURE_TEXTURES = new HashMap<>();
    
    static {
        // Egyptian villagers - sandy tunics with scarab jewelry
        CULTURE_TEXTURES.put("egyptian", Identifier.of("villagesreborn", "textures/entity/villager/egyptian"));
        
        // Roman villagers - togas with laurel crowns
        CULTURE_TEXTURES.put("roman", Identifier.of("villagesreborn", "textures/entity/villager/roman"));
        
        // Victorian villagers - top hats and vests
        CULTURE_TEXTURES.put("victorian", Identifier.of("villagesreborn", "textures/entity/villager/victorian"));
        
        // NYC villagers - hoodies and modern clothing
        CULTURE_TEXTURES.put("nyc", Identifier.of("villagesreborn", "textures/entity/villager/nyc"));
    }

    public static Identifier getTextureForCulture(String culture) {
        return CULTURE_TEXTURES.getOrDefault(culture, 
            Identifier.of("minecraft", "textures/entity/villager/villager"));
    }

    public static Map<String, String> getTextureDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("egyptian", "Sandy tunics with scarab jewelry, using sandstone-colored textures");
        descriptions.put("roman", "White togas with gold-accented laurel crowns");
        descriptions.put("victorian", "Dark wool vests and top hats with button details");
        descriptions.put("nyc", "Modern hoodies and sneakers with bright dye colors");
        return descriptions;
    }
}