package pl.aitwar.auriga.utils.eventbus;

import com.google.inject.Singleton;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Singleton
public class EventBus {
    private EnumMap<Event, Set<Consumer<Object>>> handlers;

    public void listen(final Event ev, final Consumer<Object> eh) {
        if (handlers == null) {
            handlers = new EnumMap<>(Event.class);
        }

        if (!handlers.containsKey(ev)) {
            handlers.put(ev, new HashSet<>());
        }

        handlers.get(ev).add(eh);
    }

    public void publish(final Event ev, final Object payload) {
        if (handlers == null || !handlers.containsKey(ev)) {
            return;
        }

        handlers.get(ev).forEach(handler -> handler.accept(payload));
    }

    public void clear() {
        if (handlers == null) {
            return;
        }

        handlers.clear();
    }

    public void forget(final Event ev, final Consumer<Object> eh) {
        if (handlers == null || !handlers.containsKey(ev)) {
            throw new IllegalArgumentException("Given event is not initialized");
        }

        handlers.get(ev).remove(eh);
    }
}
