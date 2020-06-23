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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public interface FileScanner {
    static MessageChannel.MessageSupplier<LocalFile> scanSources(
            Configuration configuration
    ) {
        FileScannerFileSupplier fileSupplier =
                new FileScannerFileSupplier();
        configuration.sources.paths()
                .forEach(path ->
                        scanSource(configuration, fileSupplier, path));;
        return fileSupplier;
    }

    static void scanSource(
            Configuration configuration,
            FileScannerFileSupplier fileSupplier,
            Path sourcePath
    ) {
        scanPath(configuration, fileSupplier, sourcePath);
    }

    static void scanPath(
            Configuration configuration,
            FileScannerFileSupplier fileSupplier,
            Path path
    ) {
        // dirs
        FileSystem.listDirs(path).forEach(dir ->
                scanPath(configuration, fileSupplier, dir));
        // files
        List<File> files = FileSystem.listFiles(path);
        files.forEach(file -> handleFile(configuration, fileSupplier, file));
    }

    static void handleFile(
            Configuration configuration,
            FileScannerFileSupplier fileSupplier,
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
            fileSupplier.offer(localFile);
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

    class FileScannerFileSupplier implements MessageChannel.MessageSupplier<LocalFile> {
        private final BlockingQueue<LocalFile> queue = new LinkedTransferQueue<>();
        private final AtomicBoolean completed = new AtomicBoolean(false);
        void offer(LocalFile localFile) {
            queue.add(localFile);
        }
        void setCompleted() {
            completed.set(true);
        }
        @Override
        public LocalFile take() throws InterruptedException {
            return queue.take();
        }

        @Override
        public boolean isComplete() {
            return completed.get();
        }
    }
}
