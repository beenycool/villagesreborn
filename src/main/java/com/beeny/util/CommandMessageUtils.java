package com.beeny.util;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CommandMessageUtils {
    
    public enum MessageType {
        SUCCESS(Formatting.GREEN, false),
        ERROR(Formatting.RED, true), 
        INFO(Formatting.YELLOW, false),
        WARNING(Formatting.GOLD, false);
        
        private final Formatting formatting;
        private final boolean isError;
        
        MessageType(Formatting formatting, boolean isError) {
            this.formatting = formatting;
            this.isError = isError;
        }
        
        public Formatting getFormatting() { return formatting; }
        public boolean isError() { return isError; }
    }
    
    public static void sendMessage(ServerCommandSource source, String message, MessageType type) {
        Text formattedMessage = Text.literal(message).formatted(type.getFormatting());
        
        if (type.isError()) {
            source.sendError(formattedMessage);
        } else {
            source.sendFeedback(() -> formattedMessage, false);
        }
    }
    
    public static void sendSuccess(ServerCommandSource source, String message) {
        sendMessage(source, message, MessageType.SUCCESS);
    }
    
    public static void sendError(ServerCommandSource source, String message) {
        sendMessage(source, message, MessageType.ERROR);
    }
    
    public static void sendInfo(ServerCommandSource source, String message) {
        sendMessage(source, message, MessageType.INFO);
    }
    
    public static void sendWarning(ServerCommandSource source, String message) {
        sendMessage(source, message, MessageType.WARNING);
    }
    
    public static void sendFormattedMessage(ServerCommandSource source, String format, Object... args) {
        sendInfo(source, String.format(format, args));
    }
    
    public static void sendSuccessWithFormat(ServerCommandSource source, String format, Object... args) {
        sendSuccess(source, String.format(format, args));
    }
    
    public static void sendErrorWithFormat(ServerCommandSource source, String format, Object... args) {
        sendError(source, String.format(format, args));
    }
}