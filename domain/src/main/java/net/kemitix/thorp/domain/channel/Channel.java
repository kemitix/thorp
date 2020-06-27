package net.kemitix.thorp.domain.channel;

import java.util.Collection;
import java.util.function.Consumer;

public interface Channel<T> extends Sink<T> {

    static <T> Channel<T> create(String name) {
        return new ChannelImpl<>(name, false);
    }
    static <T> Channel<T> createWithTracing(String name) {
        return new ChannelImpl<>(name, true);
    }

    Channel<T> start();
    Channel<T> add(T item);
    Channel<T> addAll(Collection<T> items);
    Channel<T> addListener(Listener<T> listener);
    Channel<T> removeListener(Listener<T> listener);
    Channel<T> run(Consumer<Sink<T>> program);

    void shutdown();
    void shutdownNow() throws InterruptedException;
    void waitForShutdown() throws InterruptedException;

}
