package net.kemitix.thorp.filesystem;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Resource {
    private final Object cls;
    public final String file;
    public static Resource select(Object cls, String file) {
        return new Resource(cls, file);
    }
    public Path toPath() {
        return Paths.get(cls.getClass().getResource(file).getPath());
    }
    public File toFile() {
        return toPath().toFile();
    }
    public String getCanonicalPath() throws IOException {
        return toFile().getCanonicalPath();
    }
    public long length() {
        return toFile().length();
    }
}
