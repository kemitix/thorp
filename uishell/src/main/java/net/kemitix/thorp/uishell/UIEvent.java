package net.kemitix.thorp.uishell;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.domain.*;

public interface UIEvent {
    static UIEvent showValidConfig() {
        return new ShowValidConfig();
    }
    class ShowValidConfig implements UIEvent { }

    static UIEvent remoteDataFetched(int size) {
        return new RemoteDataFetched(size);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class RemoteDataFetched implements UIEvent {
        public final int size;
    }

    static UIEvent showSummary(Counters counters) {
        return new ShowSummary(counters);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class ShowSummary implements UIEvent {
        public final Counters counters;
    }

    static UIEvent fileFound(LocalFile localFile) {
        return new FileFound(localFile);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class FileFound implements UIEvent {
        public final LocalFile localFile;
    }

    static UIEvent actionChosen(Action action) {
        return new ActionChosen(action);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class ActionChosen implements UIEvent {
        public final Action action;
    }

    /**
     * The content of the file ({{hash}}) that will be placed
     * at {{remoteKey}} is already being uploaded to another
     * location. Once that upload has completed, its RemoteKey
     * will become available and a Copy action can be made.
     * @param remoteKey where this upload will copy the other to
     * @param hash the hash of the file being uploaded
     */
    static UIEvent awaitingAnotherUpload(RemoteKey remoteKey, MD5Hash hash) {
        return new AwaitingAnotherUpload(remoteKey, hash);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class AwaitingAnotherUpload implements UIEvent {
        public final RemoteKey remoteKey;
        public final MD5Hash hash;
    }

    static UIEvent anotherUploadWaitComplete(Action action) {
        return new AnotherUploadWaitComplete(action);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class AnotherUploadWaitComplete implements UIEvent {
        public final Action action;
    }

    static UIEvent actionFinished(Action action,
                                  int actionCounter,
                                  long bytesCounter,
                                  StorageEvent event) {
        return new ActionFinished(action, actionCounter, bytesCounter, event);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class ActionFinished implements UIEvent {
        public final Action action;
        public final int actionCounter;
        public final long bytesCounter;
        public final StorageEvent event;
    }

    static UIEvent keyFound(RemoteKey remoteKey) {
        return new KeyFound(remoteKey);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class KeyFound implements UIEvent {
        public final RemoteKey remoteKey;
    }

    static UIEvent requestCycle(LocalFile localFile,
                                long bytesTransfered,
                                int index,
                                long totalBytesSoFar) {
        return new RequestCycle(localFile, bytesTransfered, index, totalBytesSoFar);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class RequestCycle implements UIEvent {
        public final LocalFile localFile;
        public final long bytesTransferred;
        public final int index;
        public final long totalBytesSoFar;
    }
}
