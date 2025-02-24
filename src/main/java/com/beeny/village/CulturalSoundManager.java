package com.beeny.village;

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

public class CulturalSoundManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final CulturalSoundManager INSTANCE = new CulturalSoundManager();

    private final Map<String, List<SoundEvent>> culturalAmbientSounds;
    private final Map<String, List<SoundEvent>> culturalMusicSounds;
    private final Random random;
    private final Map<BlockPos, Long> lastSoundTime;
    private static final long SOUND_COOLDOWN = 12000; // 10 minutes in ticks

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
        // Egyptian sounds
        List<SoundEvent> egyptianAmbient = new ArrayList<>();
        egyptianAmbient.add(SoundEvents.AMBIENT_CAVE.get()); // Represents wind in desert
        egyptianAmbient.add(SoundEvents.BLOCK_SAND_BREAK.get()); // Sand movement sounds
        culturalAmbientSounds.put("egyptian", egyptianAmbient);

        List<SoundEvent> egyptianMusic = new ArrayList<>();
        egyptianMusic.add(SoundEvents.MUSIC_DISC_CAT.get()); // Placeholder for actual Egyptian music
        culturalMusicSounds.put("egyptian", egyptianMusic);

        // Roman sounds
        List<SoundEvent> romanAmbient = new ArrayList<>();
        romanAmbient.add(SoundEvents.BLOCK_ANVIL_USE.get()); // Forge/crafting sounds
        romanAmbient.add(SoundEvents.ENTITY_VILLAGER_TRADE.get()); // Market sounds
        culturalAmbientSounds.put("roman", romanAmbient);

        List<SoundEvent> romanMusic = new ArrayList<>();
        romanMusic.add(SoundEvents.MUSIC_DISC_BLOCKS.get()); // Placeholder for Roman music
        culturalMusicSounds.put("roman", romanMusic);

        // Victorian sounds
        List<SoundEvent> victorianAmbient = new ArrayList<>();
        victorianAmbient.add(SoundEvents.BLOCK_BELL_USE.get()); // Church bells
        victorianAmbient.add(SoundEvents.BLOCK_CHAIN_BREAK.get()); // Industrial sounds
        culturalAmbientSounds.put("victorian", victorianAmbient);

        List<SoundEvent> victorianMusic = new ArrayList<>();
        victorianMusic.add(SoundEvents.MUSIC_DISC_WAIT.get()); // Placeholder for Victorian era music
        culturalMusicSounds.put("victorian", victorianMusic);

        // NYC sounds
        List<SoundEvent> nycAmbient = new ArrayList<>();
        nycAmbient.add(SoundEvents.ENTITY_MINECART_RIDING.get()); // Traffic sounds
        nycAmbient.add(SoundEvents.BLOCK_NOTE_BLOCK_BELL.get()); // City ambience
        culturalAmbientSounds.put("nyc", nycAmbient);

        List<SoundEvent> nycMusic = new ArrayList<>();
        nycMusic.add(SoundEvents.MUSIC_DISC_13.get()); // Placeholder for city music
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

        // Play ambient sounds for nearby players
        world.getPlayers().forEach(player -> {
            if (isPlayerInRange(player, pos, 32)) {
                SoundEvent sound = ambientSounds.get(random.nextInt(ambientSounds.size()));
                float volume = calculateVolume(player.getBlockPos(), pos);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
                    sound, SoundCategory.AMBIENT, volume, 1.0f);
            }
        });

        lastSoundTime.put(pos, world.getTime());
    }

    public void playEventSounds(ServerWorld world, BlockPos pos, String culture, String eventType) {
        SoundEvent eventSound = switch(culture.toLowerCase()) {
            case "egyptian" -> {
                if (eventType.contains("festival")) {
                    yield SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.get();
                }
                yield SoundEvents.AMBIENT_CAVE.get();
            }
            case "roman" -> {
                if (eventType.contains("market")) {
                    yield SoundEvents.ENTITY_VILLAGER_YES.get();
                }
                yield SoundEvents.BLOCK_ANVIL_USE.get();
            }
            case "victorian" -> {
                if (eventType.contains("social")) {
                    yield SoundEvents.BLOCK_NOTE_BLOCK_CHIME.get();
                }
                yield SoundEvents.BLOCK_BELL_USE.get();
            }
            case "nyc" -> {
                if (eventType.contains("parade")) {
                    yield SoundEvents.BLOCK_NOTE_BLOCK_BASS.get();
                }
                yield SoundEvents.ENTITY_MINECART_RIDING.get();
            }
            default -> SoundEvents.AMBIENT_CAVE.get();
        };

        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
            eventSound, SoundCategory.RECORDS, 1.0f, 1.0f);
    }

    public void playVillagerInteractionSounds(ServerWorld world, VillagerEntity villager1, 
            VillagerEntity villager2, String culture) {
        BlockPos pos = villager1.getBlockPos();
        
        SoundEvent interactionSound = switch(culture.toLowerCase()) {
            case "egyptian" -> SoundEvents.ENTITY_VILLAGER_AMBIENT;
            case "roman" -> SoundEvents.ENTITY_VILLAGER_CELEBRATE;
            case "victorian" -> SoundEvents.ENTITY_VILLAGER_YES;
            case "nyc" -> SoundEvents.ENTITY_VILLAGER_TRADE;
            default -> SoundEvents.ENTITY_VILLAGER_AMBIENT;
        };

        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
            interactionSound, SoundCategory.NEUTRAL, 0.5f, 1.0f);
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
        float maxDistance = 32.0f * 32.0f; // Max hearing distance squared
        return Math.max(0.0f, 1.0f - (float)(distance / maxDistance));
    }

    public void playCulturalMusic(ServerWorld world, BlockPos pos, String culture) {
        List<SoundEvent> musicSounds = culturalMusicSounds.get(culture.toLowerCase());
        if (musicSounds == null || musicSounds.isEmpty()) {
            return;
        }

        // Only play music occasionally and for nearby players
        if (random.nextFloat() < 0.01f) { // 1% chance per check
            world.getPlayers().forEach(player -> {
                if (isPlayerInRange(player, pos, 48)) {
                    SoundEvent music = musicSounds.get(random.nextInt(musicSounds.size()));
                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
                        music, SoundCategory.RECORDS, 0.3f, 1.0f);
                }
            });
        }
    }

    public void clearSoundHistory(BlockPos pos) {
        lastSoundTime.remove(pos);
    }

    public void stopAllSounds(ServerWorld world, BlockPos pos) {
        world.getPlayers().forEach(player -> {
            if (isPlayerInRange(player, pos, 48)) {
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
                    SoundEvents.BLOCK_NOTE_BLOCK_BASS, SoundCategory.MASTER, 0.0f, 1.0f);
            }
        });
    }
}