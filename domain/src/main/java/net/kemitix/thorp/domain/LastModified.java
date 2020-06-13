package net.kemitix.thorp.domain;

import net.kemitix.mon.TypeAlias;

import java.time.Instant;

public class LastModified extends TypeAlias<Instant> {
    private LastModified(Instant value) {
        super(value);
    }
    public static LastModified at(Instant instant) {
        return new LastModified(instant);
    }
    public Instant at() {
        return getValue();
    }
}
