package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class Action {
    private final Bucket bucket;
    private final Long size;
    private final RemoteKey remoteKey;

    public abstract String asString();

    public static DoNothing doNothing(
            Bucket body,
            RemoteKey remoteKey,
            Long size
    ) {
        return new DoNothing(body, size, remoteKey);
    }
    public static ToUpload toUpload(
            Bucket body,
            LocalFile localFile,
            Long size
    ) {
        return new ToUpload(body, localFile, size);
    }
    public static ToCopy toCopy(
            Bucket body,
            RemoteKey sourceKey,
            MD5Hash hash,
            RemoteKey targetKey,
            Long size
    ) {
        return new ToCopy(body, sourceKey, hash, targetKey, size);
    }
    public static ToDelete toDelete(
            Bucket body,
            RemoteKey remoteKey,
            Long size
    ) {
        return new ToDelete(body, size, remoteKey);
    }
    public static class DoNothing extends Action {
        private DoNothing(
                Bucket body,
                Long size,
                RemoteKey remoteKey
        ) {
            super(body, size, remoteKey);
        }
        @Override
        public String asString() {
            return String.format("Do nothing: %s", getRemoteKey().key());
        }
    }
    @Getter
    public static class ToUpload extends Action {
        private final LocalFile localFile;
        private ToUpload(
                Bucket body,
                LocalFile localFile,
                Long size
        ) {
            super(body, size, localFile.remoteKey());
            this.localFile = localFile;
        }
        @Override
        public String asString() {
            return String.format("Upload: %s", localFile.remoteKey().key());
        }
    }
    @Getter
    public static class ToCopy extends Action {
        private final RemoteKey sourceKey;
        private final MD5Hash hash;
        private final RemoteKey targetKey;
        private ToCopy(
                Bucket body,
                RemoteKey sourceKey,
                MD5Hash hash,
                RemoteKey targetKey,
                Long size
        ) {
            super(body, size, targetKey);
            this.sourceKey = sourceKey;
            this.hash = hash;
            this.targetKey = targetKey;
        }
        @Override
        public String asString() {
            return String.format("Copy: %s => %s",
                    sourceKey.key(), targetKey.key());
        }
    }
    public static class ToDelete extends Action {
        private ToDelete(
                Bucket body,
                Long size,
                RemoteKey remoteKey
        ) {
            super(body, size, remoteKey);
        }
        @Override
        public String asString() {
            return String.format("Delete: %s", getRemoteKey().key());
        }
    }
}
