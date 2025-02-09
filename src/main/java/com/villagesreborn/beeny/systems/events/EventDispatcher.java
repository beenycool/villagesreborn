package com.villagesreborn.beeny.systems.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventDispatcher {
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
