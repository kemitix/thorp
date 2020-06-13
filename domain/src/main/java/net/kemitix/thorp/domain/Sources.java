package net.kemitix.thorp.domain;

import net.kemitix.mon.TypeAlias;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Sources extends TypeAlias<List<Path>> {
    private Sources(List<Path> value) { super(value); }
    public static final Sources emptySources = new Sources(Collections.emptyList());
    public static Sources create(List<Path> paths) {
        return new Sources(paths);
    }
    public List<Path> paths() {
        return new ArrayList<>(getValue());
    }
    public Path forPath(Path path) {
        return getValue().stream()
                .filter(path::startsWith)
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Path is not within any known source"));
    }
    public Sources append(Path path) {
        return append(Collections.singletonList(path));
    }
    public Sources append(List<Path> paths) {
        List<Path> collected = new ArrayList<>();
        collected.addAll(getValue());
        collected.addAll(paths);
        return Sources.create(collected);
    }
}
