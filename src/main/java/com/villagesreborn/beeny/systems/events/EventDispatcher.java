package com.villagesreborn.beeny.systems.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventDispatcher {
    public static void dispatchPerformanceEvent(String ratingName, String advisory, float performanceFactor) {
        // Implementation would use Minecraft's notification system
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(
            Component.literal("System Rating: " + ratingName + " - " + advisory),
            false
        );
        
        // Send network packet to sync with server-side systems
        ClientPlayNetworking.send(
            new Identifier("villagesreborn", "performance_rating"),
            new PerformanceRatingPayload(performanceFactor).toPacketByteBuf()
        );
    }
    private static final Map<Class<?>, List<Consumer<?>>> handlers = new HashMap<>();

    public static <T> void registerHandler(Class<T> eventType, Consumer<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    public static <T> void publish(T event) {
        List<Consumer<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (Consumer<?> handler : eventHandlers) {
                ((Consumer<T>) handler).accept(event);
            }
        }
    }
}
