package com.beeny.village;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CulturalSoundManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final CulturalSoundManager INSTANCE = new CulturalSoundManager();

    private final Map<String, List<RegistryEntry.Reference<SoundEvent>>> culturalAmbientSounds;
    private final Map<String, List<RegistryEntry.Reference<SoundEvent>>> culturalMusicSounds;
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

    @SuppressWarnings("unchecked")
    private void initializeSoundMaps() {
        List<RegistryEntry.Reference<SoundEvent>> egyptianAmbient = new ArrayList<>();
        egyptianAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.AMBIENT_CAVE);
        egyptianAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_SAND_BREAK);
        culturalAmbientSounds.put("egyptian", egyptianAmbient);

        List<RegistryEntry.Reference<SoundEvent>> egyptianMusic = new ArrayList<>();
        egyptianMusic.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.MUSIC_DISC_CAT);
        culturalMusicSounds.put("egyptian", egyptianMusic);

        List<RegistryEntry.Reference<SoundEvent>> romanAmbient = new ArrayList<>();
        romanAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_ANVIL_USE);
        romanAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_VILLAGER_TRADE);
        culturalAmbientSounds.put("roman", romanAmbient);

        List<RegistryEntry.Reference<SoundEvent>> romanMusic = new ArrayList<>();
        romanMusic.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.MUSIC_DISC_BLOCKS);
        culturalMusicSounds.put("roman", romanMusic);

        List<RegistryEntry.Reference<SoundEvent>> victorianAmbient = new ArrayList<>();
        victorianAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_BELL_USE);
        victorianAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_CHAIN_BREAK);
        culturalAmbientSounds.put("victorian", victorianAmbient);

        List<RegistryEntry.Reference<SoundEvent>> victorianMusic = new ArrayList<>();
        victorianMusic.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.MUSIC_DISC_WAIT);
        culturalMusicSounds.put("victorian", victorianMusic);

        List<RegistryEntry.Reference<SoundEvent>> nycAmbient = new ArrayList<>();
        nycAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_MINECART_RIDING);
        nycAmbient.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_NOTE_BLOCK_BELL);
        culturalAmbientSounds.put("nyc", nycAmbient);

        List<RegistryEntry.Reference<SoundEvent>> nycMusic = new ArrayList<>();
        nycMusic.add((RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.MUSIC_DISC_13);
        culturalMusicSounds.put("nyc", nycMusic);
    }

    public void playAmbientSounds(ServerWorld world, BlockPos pos, String culture) {
        if (!canPlaySound(pos)) {
            return;
        }

        List<RegistryEntry.Reference<SoundEvent>> ambientSounds = culturalAmbientSounds.get(culture.toLowerCase());
        if (ambientSounds == null || ambientSounds.isEmpty()) {
            return;
        }

        world.getPlayers().forEach(player -> {
            if (isPlayerInRange(player, pos, 32)) {
                RegistryEntry.Reference<SoundEvent> sound = ambientSounds.get(random.nextInt(ambientSounds.size()));
                float volume = calculateVolume(player.getBlockPos(), pos);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
                    sound.value(), SoundCategory.AMBIENT, volume, 1.0f);
            }
        });

        lastSoundTime.put(pos, world.getTime());
    }

    @SuppressWarnings("unchecked")
    public void playEventSounds(ServerWorld world, BlockPos pos, String culture, String eventType) {
        RegistryEntry.Reference<SoundEvent> eventSound = switch(culture.toLowerCase()) {
            case "egyptian" -> {
                if (eventType.contains("festival")) {
                    yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_NOTE_BLOCK_FLUTE;
                }
                yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.AMBIENT_CAVE;
            }
            case "roman" -> {
                if (eventType.contains("market")) {
                    yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_VILLAGER_YES;
                }
                yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_ANVIL_USE;
            }
            case "victorian" -> {
                if (eventType.contains("social")) {
                    yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_NOTE_BLOCK_CHIME;
                }
                yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_BELL_USE;
            }
            case "nyc" -> {
                if (eventType.contains("parade")) {
                    yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_NOTE_BLOCK_BASS;
                }
                yield (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_MINECART_RIDING;
            }
            default -> (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.AMBIENT_CAVE;
        };

        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
            eventSound.value(), SoundCategory.RECORDS, 1.0f, 1.0f);
    }

    @SuppressWarnings("unchecked")
    public void playVillagerInteractionSounds(ServerWorld world, VillagerEntity villager1, 
            VillagerEntity villager2, String culture) {
        BlockPos pos = villager1.getBlockPos();
        
        RegistryEntry.Reference<SoundEvent> interactionSound = switch(culture.toLowerCase()) {
            case "egyptian" -> (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_VILLAGER_AMBIENT;
            case "roman" -> (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_VILLAGER_CELEBRATE;
            case "victorian" -> (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_VILLAGER_YES;
            case "nyc" -> (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_VILLAGER_TRADE;
            default -> (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.ENTITY_VILLAGER_AMBIENT;
        };

        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
            interactionSound.value(), SoundCategory.NEUTRAL, 0.5f, 1.0f);
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
        List<RegistryEntry.Reference<SoundEvent>> musicSounds = culturalMusicSounds.get(culture.toLowerCase());
        if (musicSounds == null || musicSounds.isEmpty()) {
            return;
        }

        if (random.nextFloat() < 0.01f) {
            world.getPlayers().forEach(player -> {
                if (isPlayerInRange(player, pos, 48)) {
                    RegistryEntry.Reference<SoundEvent> music = musicSounds.get(random.nextInt(musicSounds.size()));
                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
                        music.value(), SoundCategory.RECORDS, 0.3f, 1.0f);
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
                RegistryEntry.Reference<SoundEvent> sound = 
                    (RegistryEntry.Reference<SoundEvent>) (Object) SoundEvents.BLOCK_NOTE_BLOCK_BASS;
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), 
                    sound.value(), SoundCategory.MASTER, 0.0f, 1.0f);
            }
        });
    }
}