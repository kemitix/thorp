package net.kemitix.thorp.domain.channel;

public interface Listener<T> {
    void accept(T item);
}
