package net.kemitix.thorp.lib;

import lombok.val;
import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.filesystem.FileSystem;
import net.kemitix.thorp.uishell.UIEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface LocalFileSystem {
    static List<StorageEvent> scanCopyUpload(
            Configuration configuration,
            Channel.Sink<UIEvent> uiSink,
            RemoteObjects remoteObjects,
            Archive archive
    ) throws InterruptedException {
        AtomicInteger actionCounter = new AtomicInteger();
        AtomicLong bytesCounter = new AtomicLong();
        Map<MD5Hash, RemoteKey> uploads = new HashMap<>();
        Deque<StorageEvent> events = new LinkedList<>();

        Channel.<LocalFile>create("file-scanner")
                .addListener(
                        listener(configuration, uiSink, remoteObjects, archive,
                                uploads, actionCounter, bytesCounter, events))
                .run(sink -> FileScanner.scanSources(configuration, sink))
                .start()
                .waitForShutdown();

        return new ArrayList<>(events);
    }

    static Channel.Listener<LocalFile> listener(
            Configuration configuration,
            Channel.Sink<UIEvent> uiSink,
            RemoteObjects remoteObjects,
            Archive archive,
            Map<MD5Hash, RemoteKey> uploads,
            AtomicInteger actionCounter,
            AtomicLong bytesCounter,
            Deque<StorageEvent> events
    ) {
        val chooseAction = chooseAction(configuration, remoteObjects, uploads, uiSink);
        val uiActionChosen = uiActionChosen(uiSink);
        val uiActionFinished = uiActionFinished(uiSink, actionCounter, bytesCounter);
        return localFile -> {
            uiSink.accept(UIEvent.fileFound(localFile));
            Action action = chooseAction.apply(localFile);
            actionCounter.incrementAndGet();
            bytesCounter.addAndGet(action.size);
            uiActionChosen.accept(action);
            SequencedAction sequencedAction =
                    new SequencedAction(action, actionCounter.get());
            StorageEvent event = archive.update(
                    configuration, uiSink, sequencedAction, bytesCounter.get());
            events.addFirst(event);
            uiActionFinished.accept(action, event);
        };
    }

    static BiConsumer<Action, StorageEvent> uiActionFinished(
            Channel.Sink<UIEvent> uiSink,
            AtomicInteger actionCounter,
            AtomicLong bytesCounter
    ) {
        return (action, event) -> uiSink.accept(
                UIEvent.actionFinished(action,
                        actionCounter.get(), bytesCounter.get(), event));
    }

    static Consumer<Action> uiActionChosen(Channel.Sink<UIEvent> uiSink) {
        return action -> uiSink.accept(UIEvent.actionChosen(action));
    }

    static Function<LocalFile, Action> chooseAction(
            Configuration configuration,
            RemoteObjects remoteObjects,
            Map<MD5Hash, RemoteKey> uploads,
            Channel.Sink<UIEvent> uiSink
    ) {
        return localFile -> {
            boolean remoteExists = remoteObjects.remoteKeyExists(localFile.remoteKey);
            boolean remoteMatches = remoteObjects.remoteMatchesLocalFile(localFile);
            Optional<Tuple<RemoteKey, MD5Hash>> remoteForHash =
                    remoteObjects.remoteHasHash(localFile.hashes);
            Bucket bucket = configuration.bucket;
            if (remoteExists && remoteMatches) {
                return Action.doNothing(bucket, localFile.remoteKey, localFile.length);
            }
            if (remoteForHash.isPresent()) {
                RemoteKey sourceKey = remoteForHash.get().a;
                MD5Hash hash = remoteForHash.get().b;
                return Action
                        .toCopy(bucket, sourceKey, hash,
                                localFile.remoteKey,
                                localFile.length);
            } else if (localFile.hashes.values().stream()
                    .anyMatch(uploads::containsKey)) {
                return doCopyWithPreviousUpload(localFile, bucket, uploads, uiSink);
            }
            return Action.toUpload(bucket, localFile, localFile.length);
        };
    }

    static Action doCopyWithPreviousUpload(
            LocalFile localFile,
            Bucket bucket,
            Map<MD5Hash, RemoteKey> uploads,
            Channel.Sink<UIEvent> uiSink
    ) {
        return localFile.hashes
                .values()
                .stream()
                .filter(uploads::containsKey)
                .findFirst()
                .map(hash -> {
                    uiSink.accept(UIEvent.awaitingAnotherUpload(localFile.remoteKey, hash));
                    Action action = Action.toCopy(bucket, uploads.get(hash), hash, localFile.remoteKey, localFile.length);
                    //FIXME - there is no waiting going on here!!
                    uiSink.accept(UIEvent.anotherUploadWaitComplete(action));
                    return action;
                }).orElseGet(() ->
                        Action.toUpload(bucket, localFile, localFile.length));
    }

    static List<StorageEvent> scanDelete(
            Configuration configuration,
            Channel.Sink<UIEvent> uiSink,
            RemoteObjects remoteData,
            Archive archive
    ) throws InterruptedException {
        AtomicInteger actionCounter = new AtomicInteger();
        AtomicLong bytesCounter = new AtomicLong();
        Deque<StorageEvent> events = new LinkedList<>();
        Channel.<RemoteKey>create("delete-scan")
                .addListener(deleteListener(
                        configuration, uiSink, archive,
                        actionCounter, bytesCounter, events))
                .run(sink -> remoteData.byKey.keys().forEach(sink::accept))
                .start()
                .waitForShutdown();
        return new ArrayList<>(events);
    }

    static Channel.Listener<RemoteKey> deleteListener(
            Configuration configuration,
            Channel.Sink<UIEvent> uiSink,
            Archive archive,
            AtomicInteger actionCounter,
            AtomicLong bytesCounter,
            Deque<StorageEvent> events
    ) {
        return remoteKey -> {
            uiSink.accept(UIEvent.keyFound(remoteKey));
            val sources = configuration.sources;
            val prefix = configuration.prefix;
            val exists = FileSystem.hasLocalFile(sources, prefix, remoteKey);
            if (!exists) {
                actionCounter.incrementAndGet();
                val bucket = configuration.bucket;
                val action = Action.toDelete(bucket, remoteKey, 0L);
                uiActionChosen(uiSink).accept(action);
                bytesCounter.addAndGet(action.size);
                val sequencedAction =
                        new SequencedAction(action, actionCounter.get());
                val event = archive.update(configuration, uiSink,
                        sequencedAction, 0L);
                events.addFirst(event);
                uiActionFinished(uiSink, actionCounter, bytesCounter)
                        .accept(action, event);
            }
        };
    }
}