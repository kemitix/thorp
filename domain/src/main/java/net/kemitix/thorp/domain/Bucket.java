package net.kemitix.thorp.domain;

import net.kemitix.mon.TypeAlias;

public class Bucket extends TypeAlias<String> {
    private Bucket(String value) {
        super(value);
    }
    public String name() {
        return getValue();
    }
    public static Bucket named(String name) {
        return new Bucket(name);
    }
}
