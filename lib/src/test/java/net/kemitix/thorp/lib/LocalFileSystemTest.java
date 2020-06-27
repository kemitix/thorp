package net.kemitix.thorp.lib;

import net.kemitix.thorp.config.*;
import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.domain.channel.Channel;
import net.kemitix.thorp.domain.channel.Listener;
import net.kemitix.thorp.domain.channel.Sink;
import net.kemitix.thorp.filesystem.Resource;
import net.kemitix.thorp.uishell.UIEvent;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class LocalFileSystemTest
        implements WithAssertions {

    private final Resource source = Resource.select(this, "upload");
    private final Path sourcePath = source.toPath();
    private final ConfigOption sourceOption = ConfigOption.source(sourcePath);
    private final Bucket bucket = Bucket.named("bucket");
    private final ConfigOption bucketOption = ConfigOption.bucket(bucket.name());
    private final ConfigOptions configOptions = ConfigOptions.create(
            Arrays.asList(
                    sourceOption,
                    bucketOption,
                    ConfigOption.ignoreGlobalOptions(),
                    ConfigOption.ignoreUserOptions()
            ));

    private final AtomicReference<List<UIEvent>> uiEvents = new AtomicReference<>(Collections.emptyList());
    private final AtomicReference<List<SequencedAction>> actions = new AtomicReference<>(Collections.emptyList());

    private final Archive archive =
             (configuration, uiSink, sequencedAction, totalBytesSoFar) -> {
                 actions.updateAndGet(l -> {
                     List<SequencedAction> list = new ArrayList<>();
                     list.add(sequencedAction);
                     list.addAll(l);
                     return list;
                 });
                 return StorageEvent.doNothingEvent(
                         sequencedAction.action().remoteKey);
             };

    @Nested
    @DisplayName("scanCopyUpload")
    public class ScanCopyUploadTests {
        @Nested
        @DisplayName("where remote is empty")
        public class WhereRemoteEmptyTests {
            RemoteObjects remoteObjects = RemoteObjects.empty;
            Configuration configuration = ConfigurationBuilder.buildConfig(configOptions);
            List<UIEvent> uiEventList = new ArrayList<>();
            Channel<UIEvent> uiSink = Channel.<UIEvent>create("ui-test")
                    .addListener(uiEventList::add)
                    .start();
            List<StorageEvent> storageEvents;

            public WhereRemoteEmptyTests() throws IOException, ConfigValidationException {
            }

            @BeforeEach
            public void setUp() throws InterruptedException {
                storageEvents = LocalFileSystem.scanCopyUpload(
                        configuration, uiSink, remoteObjects, archive);
                uiSink.shutdownNow();
            }

            @Test
            @DisplayName("update archive with all files")
            public void uploadArchive() {
                assertThat(storageEvents).hasSize(2);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action))
                        .allSatisfy(action ->
                                assertThat(action)
                                        .isInstanceOf(Action.ToUpload.class));
                assertThat(actions.get().stream()
                        .map(SequencedAction::action)
                        .map(action -> action.remoteKey))
                        .containsExactlyInAnyOrder(
                                MD5HashData.Root.remoteKey,
                                MD5HashData.Leaf.remoteKey
                        );
            }

            @Test
            @DisplayName("update ui with all files")
            public void updateUI() {
                assertThat(uiEventSummary(uiEventList))
                        .hasSize(6)
                        .contains(
                                "file found : root-file",
                                "action chosen : root-file : ToUpload",
                                "action finished : root-file : ToUpload")
                        .contains(
                                "file found : subdir/leaf-file",
                                "action chosen : subdir/leaf-file : ToUpload",
                                "action finished : subdir/leaf-file : ToUpload");
            }
        }

        @Nested
        @DisplayName("where remote has all")
        public class WhereRemoteHasAllTests {
            Map<MD5Hash, RemoteKey> hashToKey = new HashMap<>();
            Map<RemoteKey, MD5Hash> keyToHash = new HashMap<>();
            RemoteObjects remoteObjects;
            Configuration configuration = ConfigurationBuilder.buildConfig(configOptions);
            List<UIEvent> uiEventList = new ArrayList<>();
            Channel<UIEvent> uiSink = Channel.<UIEvent>create("ui-test")
                    .addListener(uiEventList::add)
                    .start();
            List<StorageEvent> storageEvents;

            public WhereRemoteHasAllTests() throws IOException, ConfigValidationException {
                hashToKey.put(MD5HashData.Root.hash, MD5HashData.Root.remoteKey);
                hashToKey.put(MD5HashData.Leaf.hash, MD5HashData.Leaf.remoteKey);
                keyToHash.put(MD5HashData.Root.remoteKey, MD5HashData.Root.hash);
                keyToHash.put(MD5HashData.Leaf.remoteKey, MD5HashData.Leaf.hash);
                remoteObjects = RemoteObjects.create(
                        MapView.of(hashToKey).asMap(),
                        MapView.of(keyToHash).asMap());
            }

            @BeforeEach
            public void setUp() throws InterruptedException {
                storageEvents = LocalFileSystem.scanCopyUpload(
                        configuration, uiSink, remoteObjects, archive);
                uiSink.shutdownNow();
            }

            @Test
            @DisplayName("do nothing for all files")
            public void doNothing() {
                assertThat(storageEvents).hasSize(2);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action))
                        .allSatisfy(action ->
                                assertThat(action)
                                        .isInstanceOf(Action.DoNothing.class));
            }

            @Test
            @DisplayName("update ui with do nothing")
            public void updateUI() {
                assertThat(uiEventSummary(uiEventList))
                        .hasSize(6)
                        .contains(
                                "file found : root-file",
                                "action chosen : root-file : DoNothing",
                                "action finished : root-file : DoNothing")
                        .contains(
                                "file found : subdir/leaf-file",
                                "action chosen : subdir/leaf-file : DoNothing",
                                "action finished : subdir/leaf-file : DoNothing");
            }
        }

        @Nested
        @DisplayName("where remote has some")
        public class WhereRemoteHasSomeTests {
            Map<MD5Hash, RemoteKey> hashToKey = new HashMap<>();
            Map<RemoteKey, MD5Hash> keyToHash = new HashMap<>();
            RemoteObjects remoteObjects;
            Configuration configuration = ConfigurationBuilder.buildConfig(configOptions);
            List<UIEvent> uiEventList = new ArrayList<>();
            Channel<UIEvent> uiSink = Channel.<UIEvent>create("ui-test")
                    .addListener(uiEventList::add)
                    .start();
            List<StorageEvent> storageEvents;

            public WhereRemoteHasSomeTests() throws IOException, ConfigValidationException {
                hashToKey.put(MD5HashData.Root.hash, MD5HashData.Root.remoteKey);
                keyToHash.put(MD5HashData.Root.remoteKey, MD5HashData.Root.hash);
                remoteObjects = RemoteObjects.create(
                        MapView.of(hashToKey).asMap(),
                        MapView.of(keyToHash).asMap());
            }

            @BeforeEach
            public void setUp() throws InterruptedException {
                storageEvents = LocalFileSystem.scanCopyUpload(
                        configuration, uiSink, remoteObjects, archive);
                uiSink.shutdownNow();
            }

            @Test
            @DisplayName("do nothing for some, upload for others")
            public void doNothingAnUpload() {
                assertThat(storageEvents).hasSize(2);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action))
                        .filteredOn(action -> action instanceof Action.DoNothing)
                        .hasSize(1);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action))
                        .filteredOn(action -> action instanceof Action.ToUpload)
                        .hasSize(1);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action)
                        .map(action -> action.remoteKey))
                        .containsExactlyInAnyOrder(
                                MD5HashData.Root.remoteKey,
                                MD5HashData.Leaf.remoteKey
                        );
            }

            @Test
            @DisplayName("update ui with do nothing")
            public void updateUI() {
                assertThat(uiEventSummary(uiEventList))
                        .hasSize(6)
                        .contains(
                                "file found : root-file",
                                "action chosen : root-file : DoNothing",
                                "action finished : root-file : DoNothing")
                        .contains(
                                "file found : subdir/leaf-file",
                                "action chosen : subdir/leaf-file : ToUpload",
                                "action finished : subdir/leaf-file : ToUpload");
            }
        }

        @Nested
        @DisplayName("where file has been renamed")
        public class WhereFileRenamedTests {
            Map<MD5Hash, RemoteKey> hashToKey = new HashMap<>();
            Map<RemoteKey, MD5Hash> keyToHash = new HashMap<>();
            RemoteObjects remoteObjects;
            Configuration configuration = ConfigurationBuilder.buildConfig(configOptions);
            List<UIEvent> uiEventList = new ArrayList<>();
            Channel<UIEvent> uiSink = Channel.<UIEvent>create("ui-test")
                    .addListener(uiEventList::add)
                    .start();
            List<StorageEvent> storageEvents;

            public WhereFileRenamedTests() throws IOException, ConfigValidationException {
                RemoteKey otherKey = RemoteKey.create("/old-filename");
                hashToKey.put(MD5HashData.Root.hash, otherKey);
                hashToKey.put(MD5HashData.Leaf.hash, MD5HashData.Leaf.remoteKey);
                keyToHash.put(otherKey, MD5HashData.Root.hash);
                keyToHash.put(MD5HashData.Leaf.remoteKey, MD5HashData.Leaf.hash);
                remoteObjects = RemoteObjects.create(
                        MapView.of(hashToKey).asMap(),
                        MapView.of(keyToHash).asMap());
            }

            @BeforeEach
            public void setUp() throws InterruptedException {
                storageEvents = LocalFileSystem.scanCopyUpload(
                        configuration, uiSink, remoteObjects, archive);
                uiSink.shutdownNow();
            }

            @Test
            @DisplayName("copy")
            public void copy() {
                assertThat(storageEvents).hasSize(2);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action))
                        .filteredOn(action -> action instanceof Action.ToCopy)
                        .hasSize(1);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action))
                        .filteredOn(action -> action instanceof Action.DoNothing)
                        .hasSize(1);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action)
                        .map(action -> action.remoteKey))
                        .containsExactlyInAnyOrder(
                                MD5HashData.Root.remoteKey,
                                MD5HashData.Leaf.remoteKey
                        );
            }

            @Test
            @DisplayName("update ui")
            public void updateUI() {
                assertThat(uiEventSummary(uiEventList))
                        .hasSize(6)
                        .contains(
                                "file found : root-file",
                                "action chosen : root-file : ToCopy",
                                "action finished : root-file : ToCopy")
                        .contains(
                                "file found : subdir/leaf-file",
                                "action chosen : subdir/leaf-file : DoNothing",
                                "action finished : subdir/leaf-file : DoNothing");
            }
        }
    }

    @Nested
    @DisplayName("scanDelete")
    public class ScanDeleteTests {
        @Nested
        @DisplayName("where remote has no extra objects")
        public class RemoteHasNoExtrasTests {
            RemoteObjects remoteObjects = RemoteObjects.empty;
            Configuration configuration = ConfigurationBuilder.buildConfig(configOptions);
            List<UIEvent> uiEventList = new ArrayList<>();
            Channel<UIEvent> uiSink = Channel.<UIEvent>create("ui-test")
                    .addListener(uiEventList::add)
                    .start();
            List<StorageEvent> storageEvents;

            public RemoteHasNoExtrasTests() throws IOException, ConfigValidationException {
            }

            @BeforeEach
            public void setUp() throws InterruptedException {
                storageEvents = LocalFileSystem.scanDelete(
                        configuration, uiSink, remoteObjects, archive);
                uiSink.shutdownNow();
            }

            @Test
            @DisplayName("no archive actions")
            public void noArchiveUpdates() {
                assertThat(storageEvents).isEmpty();
            }

            @Test
            @DisplayName("update ui")
            public void updateUI() {
                assertThat(uiEventList).isEmpty();
            }
        }
        @Nested
        @DisplayName("where remote has extra objects")
        public class RemoteHasExtrasTests {
            Map<MD5Hash, RemoteKey> hashToKey = new HashMap<>();
            Map<RemoteKey, MD5Hash> keyToHash = new HashMap<>();
            RemoteObjects remoteObjects;
            Configuration configuration = ConfigurationBuilder.buildConfig(configOptions);
            List<UIEvent> uiEventList = new ArrayList<>();
            Channel<UIEvent> uiSink = Channel.<UIEvent>create("ui-test")
                    .addListener(uiEventList::add)
                    .start();
            List<StorageEvent> storageEvents;
            RemoteKey extraKey = RemoteKey.create("/extra");
            MD5Hash extraHash = MD5Hash.create("extra-hash");

            public RemoteHasExtrasTests() throws IOException, ConfigValidationException {
                hashToKey.put(extraHash, extraKey);
                keyToHash.put(extraKey, extraHash);
                remoteObjects = RemoteObjects.create(
                        MapView.of(hashToKey).asMap(),
                        MapView.of(keyToHash).asMap());
            }

            @BeforeEach
            public void setUp() throws InterruptedException {
                storageEvents = LocalFileSystem.scanDelete(
                        configuration, uiSink, remoteObjects, archive);
                uiSink.shutdownNow();
            }

            @Test
            @DisplayName("archive delete action")
            public void archiveDeleteUpdates() {
                assertThat(storageEvents).hasSize(1);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action))
                        .filteredOn(action -> action instanceof Action.ToDelete)
                        .hasSize(1);
                assertThat(actions.get().stream()
                        .map(SequencedAction::action)
                        .map(action -> action.remoteKey))
                        .containsExactly(extraKey);
            }

            @Test
            @DisplayName("update ui")
            public void updateUI() {
                assertThat(uiEventSummary(uiEventList))
                        .hasSize(3)
                        .contains(
                                "key found: /extra",
                                "action chosen : /extra : ToDelete",
                                "action finished : /extra : ToDelete");
            }
        }
    }

    private List<String> uiEventSummary(List<UIEvent> uiEvents) {
        Deque<String> summary = new ArrayDeque<>();
        uiEvents.stream()
                .map(uiEvent -> {
                    if (uiEvent instanceof UIEvent.FileFound) {
                        return String.format("file found : %s",
                                ((UIEvent.FileFound) uiEvent).localFile
                                        .remoteKey.key());
                    } else if (uiEvent instanceof UIEvent.ActionChosen) {
                        Action action = ((UIEvent.ActionChosen) uiEvent).action;
                        return String.format(
                                "action chosen : %s : %s",
                                action.remoteKey.key(),
                                action.getClass().getSimpleName());
                    } else if (uiEvent instanceof UIEvent.ActionFinished) {
                        Action action = ((UIEvent.ActionFinished) uiEvent).action;
                        return String.format(
                                "action finished : %s : %s",
                                action.remoteKey.key(),
                                action.getClass().getSimpleName());
                    } else if (uiEvent instanceof UIEvent.KeyFound) {
                        return String.format("key found: %s",
                                ((UIEvent.KeyFound) uiEvent).remoteKey
                                        .key());
                    }
                    return String.format("unknown : %s",
                            uiEvent.getClass().getSimpleName());
                })
                .forEach(summary::addLast);
        return new ArrayList<>(summary);
    }
}
