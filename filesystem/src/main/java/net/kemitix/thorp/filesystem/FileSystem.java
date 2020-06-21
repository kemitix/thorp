package net.kemitix.thorp.filesystem;

import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.Sources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface FileSystem {
    static boolean hasLocalFile(Sources sources, RemoteKey prefix, RemoteKey remoteKey) {
        return sources.paths()
                .stream()
                .anyMatch(sourcePath ->
                        remoteKey.asFile(sourcePath, prefix)
                                .map(File::exists)
                                .orElse(false));
    }

    @Deprecated // use File.exists
    static boolean exists(File file) {
        return file.exists();
    }
    default List<String> lines(File file) throws IOException {
        return Files.readAllLines(file.toPath());
    }
    static void moveFile(Path source, Path target) throws IOException {
        if (source.toFile().exists()) {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        }
    }
    static PathCache findCache(Path directory) throws IOException {
        Path cachePath = directory.resolve(PathCache.fileName);
        List<String> cacheLines = fileLines(cachePath.toFile());
        return PathCache.fromLines(cacheLines);
    }
    static List<String> fileLines(File file) throws IOException {
        return Files.lines(file.toPath()).collect(Collectors.toList());
    }
    static List<Path> listDirs(Path path) {
        File dir = path.toFile();
        if (dir.isDirectory())
            return Arrays.stream(dir.listFiles())
                    .filter(File::isDirectory)
                    .map(File::toPath)
                    .collect(Collectors.toList());
        return Collections.emptyList();
    }
    static List<File> listFiles(Path path) {
        File dir = path.toFile();
        if (dir.isDirectory()) {
            return Arrays.stream(dir.listFiles())
                    .filter(File::isFile)
                    .filter(file -> !file.getName().equals(PathCache.fileName))
                    .filter(file -> !file.getName().equals(PathCache.tempFileName))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    static Instant lastModified(File file) {
        return Instant.ofEpochMilli(file.lastModified());
    }
    static void appendLines(List<String> lines, File file) throws IOException {
        try (Writer writer = new FileWriter(file, true)) {
            for (String line : lines) {
                writer.append(line + System.lineSeparator());
            }
        }
    }
}
