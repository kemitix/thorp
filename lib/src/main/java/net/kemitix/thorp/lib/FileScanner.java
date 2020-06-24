package net.kemitix.thorp.lib;

import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.filesystem.FileSystem;
import net.kemitix.thorp.filesystem.PathCache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface FileScanner {
    static void scanSources(
            Configuration configuration,
            Channel.Sink<LocalFile> fileSink
    ) {
        configuration.sources.paths()
                .forEach(path ->
                        scanSource(configuration, fileSink, path));
    }

    static void scanSource(
            Configuration configuration,
            Channel.Sink<LocalFile> fileSink,
            Path sourcePath
    ) {
        scanPath(configuration, fileSink, sourcePath);
    }

    static void scanPath(
            Configuration configuration,
            Channel.Sink<LocalFile> fileSink,
            Path path
    ) {
        // dirs
        FileSystem.listDirs(path).forEach(dir ->
                scanPath(configuration, fileSink, dir));
        // files
        List<File> files = FileSystem.listFiles(path);
        files.forEach(file -> handleFile(configuration, fileSink, file));
    }

    static void handleFile(
            Configuration configuration,
            Channel.Sink<LocalFile> fileSink,
            File file
    ) {
        boolean isIncluded = Filters.isIncluded(configuration, file);
        if (isIncluded) {
            File source = configuration.sources.forPath(file.toPath()).toFile();
            Hashes hashes = hashObject(file);
            RemoteKey remoteKey =
                    RemoteKey.from(source.toPath(), configuration.prefix, file);
            LocalFile localFile =
                    LocalFile.create(
                            file, source, hashes, remoteKey, file.length());
            fileSink.accept(localFile);
        }
    }

    static Hashes hashObject(File file) {
        try {
            return HashGenerator.hashObject(file.toPath());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing object: " + file, e);
        }
    }

    static PathCache findCache(Path sourcePath) {
        try {
            return FileSystem.findCache(sourcePath);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error finding source cache for source: " + sourcePath, e);
        }
    }
}
