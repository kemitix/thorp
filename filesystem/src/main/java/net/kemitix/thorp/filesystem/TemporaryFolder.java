package net.kemitix.thorp.filesystem;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Consumer;

public interface TemporaryFolder {

    default void withDirectory(Consumer<Path> testCode) throws IOException {
        Path dir = Files.createTempDirectory("thorp-temp");
        try {
            testCode.accept(dir);
        } finally {
            remove(dir);
        }
    }
    default void remove(Path root) throws IOException {
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
    }
    default File createFile(Path directory, String name, List<String> contents) throws FileNotFoundException, UnsupportedEncodingException {
        boolean x = directory.toFile().mkdirs();
        File file = directory.resolve(name).toFile();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        contents.forEach(writer::println);
        writer.close();
        return file;
    }
}
