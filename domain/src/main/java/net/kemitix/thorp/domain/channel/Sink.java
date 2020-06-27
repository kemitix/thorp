package net.kemitix.thorp.domain.channel;

public interface Sink<T> extends Listener<T> {
    void shutdown();
}
