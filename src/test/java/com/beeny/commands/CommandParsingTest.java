package com.beeny.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommandParsingTest {
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandSourceStack source;
    private ServerPlayer player;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher<>();
        new VillagerCommands().register(dispatcher);
        
        source = Mockito.mock(CommandSourceStack.class);
        player = Mockito.mock(ServerPlayer.class);
        when(source.getPlayer()).thenReturn(player);
        when(source.hasPermission(2)).thenReturn(true);
    }

    @Test
    void testValidCommandParsing() throws CommandSyntaxException {
        // Test valid command with all arguments
        CommandContext<CommandSourceStack> context = dispatcher.parse(
            "villager modify villager_name --personality friendly --profession farmer",
            source
        );
        assertNotNull(context);
    }

    @Test
    void testInvalidArgumentType() {
        // Test invalid argument type for numeric parameter
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class, () -> 
            dispatcher.parse("villager setage villager_name invalid_number", source)
        );
        assertTrue(exception.getMessage().contains("Invalid integer"));
    }

    @Test
    void testMissingRequiredArgument() {
        // Test missing required argument
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class, () -> 
            dispatcher.parse("villager modify", source)
        );
        assertTrue(exception.getMessage().contains("Expected whitespace"));
    }

    @Test
    void testSecurityWarningForPrivilegedCommand() {
        // Test command requiring operator permissions
        when(source.hasPermission(2)).thenReturn(false);
        
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class, () -> 
            dispatcher.parse("villager reloadai", source)
        );
        assertTrue(exception.getMessage().contains("requires operator permissions"));
    }

    @Test
    void testArgumentRangeValidation() {
        // Test numeric argument out of valid range
        CommandSyntaxException exception = assertThrows(CommandSyntaxException.class, () -> 
            dispatcher.parse("villager setage villager_name 150", source)
        );
        assertTrue(exception.getMessage().contains("Age must be between"));
    }

    @Test
    void testEntitySelectorParsing() throws CommandSyntaxException {
        // Test valid entity selector
        CommandContext<CommandSourceStack> context = dispatcher.parse(
            "villager modify @e[type=villager] --profession librarian",
            source
        );
        assertNotNull(context.getArgument("target", EntityArgument.class));
    }

    @Test
    void testMessageArgumentParsing() throws CommandSyntaxException {
        // Test message argument with spaces
        CommandContext<CommandSourceStack> context = dispatcher.parse(
            "villager say \"Hello world! This is a test\"",
            source
        );
        assertNotNull(context.getArgument("message", MessageArgument.class));
    }
}