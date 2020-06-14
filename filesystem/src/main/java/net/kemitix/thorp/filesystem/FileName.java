package net.kemitix.thorp.filesystem;

import net.kemitix.mon.TypeAlias;

public class FileName extends TypeAlias<String> {
    private FileName(String value) {
        super(value);
    }
    public static FileName create(String filename) {
        return new FileName(filename);
    }
}
