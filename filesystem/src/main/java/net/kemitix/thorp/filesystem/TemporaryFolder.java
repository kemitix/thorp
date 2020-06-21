package net.kemitix.thorp.filesystem;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public interface TemporaryFolder {

    default void withDirectory(Consumer<Path> testCode) {
        Path dir = createTempDirectory();
        try {
            testCode.accept(dir);
        } finally {
            remove(dir);
        }
    }

    default Path createTempDirectory() {
        try {
            return Files.createTempDirectory("thorp-temp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void remove(Path root) {
        try {
            Files.walkFileTree(
                    root,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    default File createFile(Path directory, String name, List<String> contents) {
        boolean x = directory.toFile().mkdirs();
        File file = directory.resolve(name).toFile();
        PrintWriter writer = null;
        try {
            writer = getWriter(file);
            contents.forEach(writer::println);
        } finally {
            if (Objects.nonNull(writer)) {
                writer.close();
            }
        }
        return file;
    }

    default PrintWriter getWriter(File file) {
        try {
            return new PrintWriter(file, "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
