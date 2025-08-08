package com.beeny.commands;

import com.beeny.Villagersreborn;

import com.beeny.util.TTSBridge;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;

public class TestTTSCommand implements Command<ServerCommandSource> {
    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        String text = StringArgumentType.getString(context, "text");
        player.sendMessage(Text.literal("Generating TTS for: " + text), false);

        try {
            // Generate WAV file
            String wavPath = TTSBridge.generateTTS(text);
            player.sendMessage(Text.literal("Generated WAV: " + wavPath), false);

            // Convert to OGG
            String oggPath = convertToOgg(wavPath);
            player.sendMessage(Text.literal("Converted to OGG: " + oggPath), false);

            // Play sound using SoundEvent (Identifier.of for newer Yarn mappings)
            Identifier id = Identifier.of("villagersreborn", "tts_output");
            SoundEvent soundEvent = SoundEvent.of(id);
            // PlayerEntity#playSound(SoundEvent, float, float) is the correct overload
            player.playSound(soundEvent, 1.0f, 1.0f);
            player.sendMessage(Text.literal("Playing TTS audio"), false);

            return 1;
        } catch (IOException | InterruptedException e) {
            player.sendMessage(Text.literal("TTS failed: " + e.getMessage()), false);
            com.beeny.Villagersreborn.LOGGER.error("TTS failed", e);
            return -1;
        }
    }

    private String convertToOgg(String wavPath) {
        // Placeholder for WAV to OGG conversion
        // In a real implementation, we'd use a library or command-line tool
        return wavPath.replace(".wav", ".ogg");
    }
}