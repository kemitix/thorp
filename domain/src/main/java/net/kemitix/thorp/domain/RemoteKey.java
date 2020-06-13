package net.kemitix.thorp.domain;

import net.kemitix.mon.TypeAlias;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoteKey extends TypeAlias<String> {
    private RemoteKey(String value) {
        super(value);
    }
    public static RemoteKey create(String key) {
        return new RemoteKey(key);
    }
    public String key() {
        return getValue();
    }
    public Optional<File> asFile(Path source, RemoteKey prefix) {
        if (key().length() == 0 || !key().startsWith(prefix.key())) {
            return Optional.empty();
        }
        return Optional.of(
                source.resolve(relativeTo(prefix))
                        .toFile());
    }
    public Path relativeTo(RemoteKey prefix) {
        if (prefix.key().equals("")) {
            return Paths.get(key());
        }
        return Paths.get(prefix.key()).relativize(Paths.get(key()));
    }
    public RemoteKey resolve(String path) {
        return RemoteKey.create(
                Stream.of(key(), path)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining("/")));
    }
    public static RemoteKey fromSourcePath(Path source, Path path) {
        return RemoteKey.create(
                source.relativize(path).toString());
    }
    public static RemoteKey from(Path source, RemoteKey prefix, File file) {
        return prefix.resolve(source.relativize(file.toPath()).toString());
    }
}
