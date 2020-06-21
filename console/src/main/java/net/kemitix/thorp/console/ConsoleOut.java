package net.kemitix.thorp.console;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.domain.*;
import scala.io.AnsiColor;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ConsoleOut {
    String en();
    default String eraseToEndOfScreen() {
        return Terminal.eraseToEndOfScreen;
    }
    default String reset() {
        return "\u001B[0m";
    }
    default String red() {
        return "\u001B[31m";
    }
    default String green() {
        return "\u001B[32m";
    }
    interface WithBatchMode extends ConsoleOut, Function<Boolean, String> {
        String enBatch();
        default String selectLine(boolean batchMode) {
            return batchMode
                    ? enBatch()
                    : en();
        }
        @Override
        default String apply(Boolean batchMode) {
            return selectLine(batchMode);
        }
    }

    static ConsoleOut validConfig(
            Bucket bucket,
            RemoteKey prefix,
            Sources sources
    ) {
        return new ValidConfig(bucket, prefix, sources);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class ValidConfig implements ConsoleOut {
        private final Bucket bucket;
        private final RemoteKey prefix;
        private final Sources sources;

        @Override
        public String en() {
            return String.join(", ", Arrays.asList(
                    "Bucket: " + bucket.name(),
                    "Prefix: " + prefix.key(),
                    "Source: " + sources.paths().stream()
                            .map(Path::toString)
                            .collect(Collectors.joining(", "))));
        }
    }

    static ConsoleOut.WithBatchMode uploadComplete(RemoteKey remoteKey) {
        return new UploadComplete(remoteKey);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class UploadComplete implements ConsoleOut.WithBatchMode {
        private final RemoteKey remoteKey;
        @Override
        public String en() {
            return String.format("%sUploaded:%s %s%s",
                    green(), reset(),
                    remoteKey.key(),
                    eraseToEndOfScreen());
        }
        @Override
        public String enBatch() {
            return String.format("Uploaded: %s", remoteKey.key());
        }
    }

    static ConsoleOut.WithBatchMode copyComplete(RemoteKey sourceKey, RemoteKey targetKey) {
        return new CopyComplete(sourceKey, targetKey);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class CopyComplete implements ConsoleOut.WithBatchMode {
        private final RemoteKey sourceKey;
        private final RemoteKey targetKey;
        @Override
        public String en() {
            return String.format("%sCopied:%s %s => %s%s",
                    green(), reset(),
                    sourceKey.key(),
                    targetKey.key(),
                    eraseToEndOfScreen());
        }
        @Override
        public String enBatch() {
            return String.format("Copied: %s => %s",
                    sourceKey.key(), targetKey.key());
        }
    }
    static ConsoleOut.WithBatchMode deleteComplete(RemoteKey remoteKey) {
        return new DeleteComplete(remoteKey);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class DeleteComplete implements WithBatchMode {
        private final RemoteKey remoteKey;
        @Override
        public String en() {
            return String.format("Deleted: %s", remoteKey);
        }
        @Override
        public String enBatch() {
            return String.format("%sDeleted%s: %s%s",
                    green(), reset(),
                    remoteKey,
                    eraseToEndOfScreen());
        }
    }
    static ConsoleOut.WithBatchMode errorQueueEventOccurred(
            StorageEvent.ActionSummary actionSummary,
            Throwable error
    ) {
        return new ErrorQueueEventOccurred(actionSummary, error);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class ErrorQueueEventOccurred implements WithBatchMode {
        private final StorageEvent.ActionSummary actionSummary;
        private final Throwable error;
        @Override
        public String en() {
            return String.format("%s failed: %s: %s",
                    actionSummary.name(),
                    actionSummary.keys(),
                    error.getMessage());
        }
        @Override
        public String enBatch() {
            return String.format("%sERROR%s: %s %s: %s%s",
                    red(), reset(),
                    actionSummary.name(),
                    actionSummary.keys(),
                    error.getMessage(),
                    eraseToEndOfScreen());
        }
    }

}
