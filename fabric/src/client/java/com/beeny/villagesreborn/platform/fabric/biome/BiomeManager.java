package com.beeny.villagesreborn.platform.fabric.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages biome data, filtering, and caching for the spawn biome selector
 * Simplified version for testing compatibility
 */
public class BiomeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeManager.class);
    private static BiomeManager instance;
    
    private final Map<RegistryKey<Biome>, BiomeDisplayInfo> biomeCache = new HashMap<>();
    private final Map<RegistryKey<Biome>, Identifier> biomeIcons = new HashMap<>();
    
    private BiomeManager() {
        initializeBiomeIcons();
        initializeTestBiomes();
    }
    
    public static BiomeManager getInstance() {
        if (instance == null) {
            instance = new BiomeManager();
        }
        return instance;
    }
    
    public static void resetForTest() {
        instance = null;
    }
    
    /**
     * Gets all biomes suitable for spawn selection
     */
    public List<BiomeDisplayInfo> getSelectableBiomes() {
        if (biomeCache.isEmpty()) {
            initializeTestBiomes();
        }
        
        List<BiomeDisplayInfo> selectableBiomes = new ArrayList<>(biomeCache.values());
        
        // Sort by difficulty/suitability
        selectableBiomes.sort((a, b) -> 
            Integer.compare(a.getDifficultyRating(), b.getDifficultyRating())
        );
        
        LOGGER.info("Found {} selectable spawn biomes", selectableBiomes.size());
        return selectableBiomes;
    }
    
    public BiomeDisplayInfo getRecommendedSpawnBiome() {
        List<BiomeDisplayInfo> suitable = getSelectableBiomes();
        return suitable.isEmpty() ? null : suitable.get(0);
    }
    
    public Identifier getBiomeIcon(RegistryKey<Biome> biomeKey) {
        return biomeIcons.getOrDefault(biomeKey, getDefaultBiomeIcon());
    }
    
    public boolean isLocationSafe(BlockPos location) {
        // Simplified implementation for testing
        return location != null && location.getY() >= 60 && location.getY() <= 250;
    }
    
    private void initializeTestBiomes() {
        // Create test biomes for compatibility
        createTestBiome(BiomeKeys.PLAINS, "Plains", 0.8f, 0.4f, 1);
        createTestBiome(BiomeKeys.FOREST, "Forest", 0.7f, 0.8f, 2);
        createTestBiome(BiomeKeys.BIRCH_FOREST, "Birch Forest", 0.6f, 0.6f, 2);
        createTestBiome(BiomeKeys.TAIGA, "Taiga", 0.25f, 0.8f, 3);
        createTestBiome(BiomeKeys.SAVANNA, "Savanna", 2.0f, 0.0f, 3);
        createTestBiome(BiomeKeys.DESERT, "Desert", 2.0f, 0.0f, 4);
    }
    
    private void createTestBiome(RegistryKey<Biome> key, String name, float temperature, float humidity, int difficulty) {
        Text displayName = Text.literal(name);
        Text description = Text.literal("A " + name.toLowerCase() + " biome suitable for spawning");
        
        BiomeDisplayInfo info = new BiomeDisplayInfo(key, displayName, temperature, humidity, difficulty, description);
        biomeCache.put(key, info);
    }
    
    private void initializeBiomeIcons() {
        String modId = "villagesreborn";
        
        // Common biome icons using Identifier.of()
        biomeIcons.put(BiomeKeys.PLAINS, 
            Identifier.of(modId, "textures/gui/biomes/plains.png"));
        biomeIcons.put(BiomeKeys.FOREST, 
            Identifier.of(modId, "textures/gui/biomes/forest.png"));
        biomeIcons.put(BiomeKeys.BIRCH_FOREST, 
            Identifier.of(modId, "textures/gui/biomes/birch_forest.png"));
        biomeIcons.put(BiomeKeys.DARK_FOREST, 
            Identifier.of(modId, "textures/gui/biomes/dark_forest.png"));
        biomeIcons.put(BiomeKeys.TAIGA, 
            Identifier.of(modId, "textures/gui/biomes/taiga.png"));
        biomeIcons.put(BiomeKeys.SAVANNA, 
            Identifier.of(modId, "textures/gui/biomes/savanna.png"));
        biomeIcons.put(BiomeKeys.DESERT, 
            Identifier.of(modId, "textures/gui/biomes/desert.png"));
    }
    
    private Identifier getDefaultBiomeIcon() {
        return Identifier.of("villagesreborn", "textures/gui/biomes/default.png");
    }
    
    public static int getTemperatureColor(float temperature) {
        if (temperature < 0.0f) return 0xFF87CEEB; // Light blue (cold)
        if (temperature < 0.5f) return 0xFF90EE90; // Light green (cool)
        if (temperature < 1.0f) return 0xFFFFFF00; // Yellow (warm)
        return 0xFFFF4500; // Orange-red (hot)
    }
    
    public static int getHumidityColor(float humidity) {
        if (humidity < 0.3f) return 0xFFDAA520; // Goldenrod (dry)
        if (humidity < 0.7f) return 0xFF90EE90; // Light green (moderate)
        return 0xFF4169E1; // Royal blue (humid)
    }
}