package com.beeny.village;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
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

    // Helper method to safely get RegistryEntry.Reference
    private Optional<RegistryEntry.Reference<SoundEvent>> getSoundEntry(SoundEvent event) {
        return Registries.SOUND_EVENT.getEntry(event);
    }

    private void initializeSoundMaps() {
        // Egyptian
        List<RegistryEntry.Reference<SoundEvent>> egyptianAmbient = new ArrayList<>();
        getSoundEntry(SoundEvents.AMBIENT_CAVE).ifPresent(egyptianAmbient::add);
        getSoundEntry(SoundEvents.BLOCK_SAND_BREAK).ifPresent(egyptianAmbient::add);
        culturalAmbientSounds.put("egyptian", egyptianAmbient);

        List<RegistryEntry.Reference<SoundEvent>> egyptianMusic = new ArrayList<>();
        getSoundEntry(SoundEvents.MUSIC_DISC_CAT).ifPresent(egyptianMusic::add);
        culturalMusicSounds.put("egyptian", egyptianMusic);

        // Roman
        List<RegistryEntry.Reference<SoundEvent>> romanAmbient = new ArrayList<>();
        getSoundEntry(SoundEvents.BLOCK_ANVIL_USE).ifPresent(romanAmbient::add);
        getSoundEntry(SoundEvents.ENTITY_VILLAGER_TRADE).ifPresent(romanAmbient::add);
        culturalAmbientSounds.put("roman", romanAmbient);

        List<RegistryEntry.Reference<SoundEvent>> romanMusic = new ArrayList<>();
        getSoundEntry(SoundEvents.MUSIC_DISC_BLOCKS).ifPresent(romanMusic::add);
        culturalMusicSounds.put("roman", romanMusic);

        // Victorian
        List<RegistryEntry.Reference<SoundEvent>> victorianAmbient = new ArrayList<>();
        getSoundEntry(SoundEvents.BLOCK_BELL_USE).ifPresent(victorianAmbient::add);
        getSoundEntry(SoundEvents.BLOCK_CHAIN_BREAK).ifPresent(victorianAmbient::add);
        culturalAmbientSounds.put("victorian", victorianAmbient);

        List<RegistryEntry.Reference<SoundEvent>> victorianMusic = new ArrayList<>();
        getSoundEntry(SoundEvents.MUSIC_DISC_WAIT).ifPresent(victorianMusic::add);
        culturalMusicSounds.put("victorian", victorianMusic);

        // NYC
        List<RegistryEntry.Reference<SoundEvent>> nycAmbient = new ArrayList<>();
        getSoundEntry(SoundEvents.ENTITY_MINECART_RIDING).ifPresent(nycAmbient::add);
        getSoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_BELL).ifPresent(nycAmbient::add);
        culturalAmbientSounds.put("nyc", nycAmbient);

        List<RegistryEntry.Reference<SoundEvent>> nycMusic = new ArrayList<>();
        getSoundEntry(SoundEvents.MUSIC_DISC_13).ifPresent(nycMusic::add);
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

        List<RegistryEntry.Reference<SoundEvent>> ambientSounds = culturalAmbientSounds.get(culture.toLowerCase());
        if (ambientSounds == null || ambientSounds.isEmpty()) {
            // Optionally log a warning if sounds are missing for a culture
            // LOGGER.warn("No ambient sounds defined for culture: {}", culture);
            return;
        }

        world.getPlayers().forEach(player -> {
            if (isPlayerInRange(player, pos, 32)) {
                RegistryEntry.Reference<SoundEvent> soundEntry = ambientSounds.get(random.nextInt(ambientSounds.size()));
                float volume = calculateVolume(player.getBlockPos(), pos);
                // Play sound using the RegistryEntry reference
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    soundEntry, SoundCategory.AMBIENT, volume, 1.0f, random.nextLong()); // Added seed
            }
        });

        lastSoundTime.put(pos, world.getTime());
    }

    public void playEventSounds(ServerWorld world, BlockPos pos, String culture, String eventType) {
        Optional<RegistryEntry.Reference<SoundEvent>> eventSoundOpt = switch(culture.toLowerCase()) {
            case "egyptian" -> eventType.contains("festival") ?
                getSoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_FLUTE) : getSoundEntry(SoundEvents.AMBIENT_CAVE);
            case "roman" -> eventType.contains("market") ?
                getSoundEntry(SoundEvents.ENTITY_VILLAGER_YES) : getSoundEntry(SoundEvents.BLOCK_ANVIL_USE);
            case "victorian" -> eventType.contains("social") ?
                getSoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_CHIME) : getSoundEntry(SoundEvents.BLOCK_BELL_USE);
            case "nyc" -> eventType.contains("parade") ?
                getSoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_BASS) : getSoundEntry(SoundEvents.ENTITY_MINECART_RIDING);
            default -> getSoundEntry(SoundEvents.AMBIENT_CAVE); // Default sound
        };

        eventSoundOpt.ifPresent(eventSound ->
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                eventSound, SoundCategory.RECORDS, 1.0f, 1.0f, random.nextLong()) // Added seed
        );
    }

    public void playVillagerInteractionSounds(ServerWorld world, VillagerEntity villager1,
            VillagerEntity villager2, String culture) {
        BlockPos pos = villager1.getBlockPos();

        Optional<RegistryEntry.Reference<SoundEvent>> interactionSoundOpt = switch(culture.toLowerCase()) {
            case "egyptian" -> getSoundEntry(SoundEvents.ENTITY_VILLAGER_AMBIENT);
            case "roman" -> getSoundEntry(SoundEvents.ENTITY_VILLAGER_CELEBRATE);
            case "victorian" -> getSoundEntry(SoundEvents.ENTITY_VILLAGER_YES);
            case "nyc" -> getSoundEntry(SoundEvents.ENTITY_VILLAGER_TRADE);
            default -> getSoundEntry(SoundEvents.ENTITY_VILLAGER_AMBIENT); // Default sound
        };

        interactionSoundOpt.ifPresent(interactionSound ->
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                interactionSound, SoundCategory.NEUTRAL, 0.5f, 1.0f, random.nextLong()) // Added seed
        );
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

        if (random.nextFloat() < 0.01f) { // Consider making this probability configurable
            world.getPlayers().forEach(player -> {
                if (isPlayerInRange(player, pos, 48)) {
                    RegistryEntry.Reference<SoundEvent> musicEntry = musicSounds.get(random.nextInt(musicSounds.size()));
                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        musicEntry, SoundCategory.RECORDS, 0.3f, 1.0f, random.nextLong()); // Added seed
                }
            });
        }
    } // Added missing closing brace for playCulturalMusic method

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
    // Removed extra closing brace here

    public void clearSoundHistory(BlockPos pos) {
        lastSoundTime.remove(pos);
    }

    public void stopAllSounds(ServerWorld world, BlockPos pos) {
        // Stopping sounds directly isn't straightforward.
        // Playing a silent sound might not work as intended.
        // A better approach might involve client-side handling or specific stop packets if needed.
        // For now, this method might not function as expected.
        // Consider removing or redesigning if stopping sounds is critical.
        getSoundEntry(SoundEvents.BLOCK_NOTE_BLOCK_BASS).ifPresent(sound -> {
            world.getPlayers().forEach(player -> {
                if (isPlayerInRange(player, pos, 48)) {
                    // Playing a sound with 0 volume doesn't guarantee stopping others.
                    world.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                        sound, SoundCategory.MASTER, 0.0f, 1.0f, random.nextLong()); // Added seed
                }
            });
        });
        LOGGER.warn("stopAllSounds method may not effectively stop ongoing sounds.");
    }
}
