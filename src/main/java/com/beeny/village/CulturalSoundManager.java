package com.beeny.village;

import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Optional; // Add Optional import

public class CulturalSoundManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final CulturalSoundManager INSTANCE = new CulturalSoundManager();

    private final Map<String, List<SoundEvent>> culturalAmbientSounds;
    private final Map<String, List<SoundEvent>> culturalMusicSounds;
    private final Random random;
    private final Map<BlockPos, Long> lastSoundTime;
    private static final long SOUND_COOLDOWN = 12000;

    private CulturalSoundManager() {
        this.culturalAmbientSounds = new HashMap<>();
        this.culturalMusicSounds = new HashMap<>();
        this.lastSoundTime = new HashMap<>();
        this.random = Random.create();
        initializeSoundMaps();
    }

    public static CulturalSoundManager getInstance() {
        return INSTANCE;
    }

    private void initializeSoundMaps() {
        // Add SoundEvents directly - they're actually SoundEvent objects
        List<SoundEvent> egyptianAmbient = new ArrayList<>();
        egyptianAmbient.add(SoundEvents.AMBIENT_CAVE.value());
        egyptianAmbient.add(SoundEvents.BLOCK_SAND_BREAK);
        culturalAmbientSounds.put("egyptian", egyptianAmbient);

        List<SoundEvent> egyptianMusic = new ArrayList<>();
        egyptianMusic.add(SoundEvents.MUSIC_DISC_CAT.value());
        culturalMusicSounds.put("egyptian", egyptianMusic);

        List<SoundEvent> romanAmbient = new ArrayList<>();
        romanAmbient.add(SoundEvents.BLOCK_ANVIL_USE);
        romanAmbient.add(SoundEvents.ENTITY_VILLAGER_TRADE);
        culturalAmbientSounds.put("roman", romanAmbient);

        List<SoundEvent> romanMusic = new ArrayList<>();
        romanMusic.add(SoundEvents.MUSIC_DISC_BLOCKS.value());
        culturalMusicSounds.put("roman", romanMusic);

        List<SoundEvent> victorianAmbient = new ArrayList<>();
        victorianAmbient.add(SoundEvents.BLOCK_BELL_USE);
        victorianAmbient.add(SoundEvents.BLOCK_CHAIN_BREAK);
        culturalAmbientSounds.put("victorian", victorianAmbient);

        List<SoundEvent> victorianMusic = new ArrayList<>();
        victorianMusic.add(SoundEvents.MUSIC_DISC_WAIT.value());
        culturalMusicSounds.put("victorian", victorianMusic);

        List<SoundEvent> nycAmbient = new ArrayList<>();
        nycAmbient.add(SoundEvents.ENTITY_MINECART_RIDING);
        nycAmbient.add(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value());
        culturalAmbientSounds.put("nyc", nycAmbient);

        List<SoundEvent> nycMusic = new ArrayList<>();
        nycMusic.add(SoundEvents.MUSIC_DISC_13.value());
        culturalMusicSounds.put("nyc", nycMusic);
    }

    public void playAmbientSounds(ServerWorld world, BlockPos pos, String culture) {
        if (!canPlaySound(pos)) {
            return;
        }

        List<SoundEvent> ambientSounds = culturalAmbientSounds.get(culture.toLowerCase());
        if (ambientSounds == null || ambientSounds.isEmpty()) {
            return;
        }

        world.getPlayers().forEach(player -> {
            if (isPlayerInRange(player, pos, 32)) {
                SoundEvent soundEvent = ambientSounds.get(random.nextInt(ambientSounds.size()));
                float volume = calculateVolume(player.getBlockPos(), pos);
                
                // Play sound with the sound event directly
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    soundEvent, SoundCategory.AMBIENT, volume, 1.0f, random.nextLong());
            }
        });

        lastSoundTime.put(pos, world.getTime());
    }

    public void playEventSounds(ServerWorld world, BlockPos pos, String culture, String eventType) {
        // Select appropriate sound based on culture and event type
        SoundEvent selectedSound = switch(culture.toLowerCase()) {
            case "egyptian" -> eventType.contains("festival") ?
                SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value() : SoundEvents.AMBIENT_CAVE.value();
            case "roman" -> eventType.contains("market") ?
                SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.BLOCK_ANVIL_USE;
            case "victorian" -> eventType.contains("social") ?
                SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value() : SoundEvents.BLOCK_BELL_USE;
            case "nyc" -> eventType.contains("parade") ?
                SoundEvents.BLOCK_NOTE_BLOCK_BASS.value() : SoundEvents.ENTITY_MINECART_RIDING;
            default -> SoundEvents.AMBIENT_CAVE.value(); // Default sound
        };

        // Play sound directly using SoundEvent
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
            selectedSound, SoundCategory.RECORDS, 1.0f, 1.0f, random.nextLong());
    }

    public void playVillagerInteractionSounds(ServerWorld world, VillagerEntity villager1,
            VillagerEntity villager2, String culture) {
        BlockPos pos = villager1.getBlockPos();

        // Select sound based on culture
        SoundEvent selectedSound = switch(culture.toLowerCase()) {
            case "egyptian" -> SoundEvents.ENTITY_VILLAGER_AMBIENT;
            case "roman" -> SoundEvents.ENTITY_VILLAGER_CELEBRATE;
            case "victorian" -> SoundEvents.ENTITY_VILLAGER_YES;
            case "nyc" -> SoundEvents.ENTITY_VILLAGER_TRADE;
            default -> SoundEvents.ENTITY_VILLAGER_AMBIENT; // Default sound
        };

        // Play sound directly using SoundEvent
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
            selectedSound, SoundCategory.NEUTRAL, 0.5f, 1.0f, random.nextLong());
    }

    private boolean canPlaySound(BlockPos pos) {
        long lastTime = lastSoundTime.getOrDefault(pos, 0L);
        return !lastSoundTime.containsKey(pos) ||
            System.currentTimeMillis() - lastTime >= SOUND_COOLDOWN;
    }

    private boolean isPlayerInRange(PlayerEntity player, BlockPos pos, int range) {
        return player.getBlockPos().isWithinDistance(pos, range);
    }

    private float calculateVolume(BlockPos playerPos, BlockPos soundPos) {
        double distance = playerPos.getSquaredDistance(soundPos);
        float maxDistance = 32.0f * 32.0f;
        return Math.max(0.0f, 1.0f - (float)(distance / maxDistance));
    }

    public void playCulturalMusic(ServerWorld world, BlockPos pos, String culture) {
        List<SoundEvent> musicSounds = culturalMusicSounds.get(culture.toLowerCase());
        if (musicSounds == null || musicSounds.isEmpty()) {
            return;
        }

        if (random.nextFloat() < 0.01f) { // Consider making this probability configurable
            world.getPlayers().forEach(player -> {
                if (isPlayerInRange(player, pos, 48)) {
                    SoundEvent musicEvent = musicSounds.get(random.nextInt(musicSounds.size()));
                    
                    // Play sound directly using SoundEvent
                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        musicEvent, SoundCategory.RECORDS, 0.3f, 1.0f, random.nextLong());
                }
            });
        }
    }

    public int getRegisteredCultureCount() {
        // Combine keys from both maps and count distinct cultures
        Set<String> cultures = new HashSet<>(culturalAmbientSounds.keySet());
        cultures.addAll(culturalMusicSounds.keySet());
        return cultures.size();
    }

    public long getTotalSoundCount() {
        long ambientCount = culturalAmbientSounds.values().stream().mapToLong(List::size).sum();
        long musicCount = culturalMusicSounds.values().stream().mapToLong(List::size).sum();
        return ambientCount + musicCount;
    }

    public void clearSoundHistory(BlockPos pos) {
        lastSoundTime.remove(pos);
    }

    public void stopAllSounds(ServerWorld world, BlockPos pos) {
        // Use a silent sound
        SoundEvent silentSound = SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();
        world.getPlayers().forEach(player -> {
            if (isPlayerInRange(player, pos, 48)) {
                // Play with 0 volume to effectively silence other sounds
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    silentSound, SoundCategory.MASTER, 0.0f, 1.0f, random.nextLong());
            }
        });
        LOGGER.warn("stopAllSounds method may not effectively stop ongoing sounds.");
    }
}
