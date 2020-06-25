package net.kemitix.thorp.domain;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

public class StorageEvent {
    public static DoNothingEvent doNothingEvent(RemoteKey remoteKey) {
        return new DoNothingEvent(remoteKey);
    }
    public static StorageEvent copyEvent(RemoteKey sourceKey, RemoteKey targetKey) {
        return new CopyEvent(sourceKey, targetKey);
    }
    public static UploadEvent uploadEvent(RemoteKey remoteKey, MD5Hash md5Hash) {
        return new UploadEvent(remoteKey, md5Hash);
    }
    public static DeleteEvent deleteEvent(RemoteKey remoteKey) {
        return new DeleteEvent(remoteKey);
    }
    public static ErrorEvent errorEvent(ActionSummary action, RemoteKey remoteKey, Throwable e) {
        return new ErrorEvent(action, remoteKey, e);
    }
    public static ShutdownEvent shutdownEvent() {
        return new ShutdownEvent();
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DoNothingEvent extends StorageEvent {
        public final RemoteKey remoteKey;
    }
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CopyEvent extends StorageEvent {
        public final RemoteKey sourceKey;
        public final RemoteKey targetKey;
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UploadEvent extends StorageEvent {
        public final RemoteKey remoteKey;
        public final MD5Hash md5Hash;
    }
    @EqualsAndHashCode(callSuper = false)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DeleteEvent extends StorageEvent {
        public final RemoteKey remoteKey;
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ErrorEvent extends StorageEvent {
        public final ActionSummary action;
        public final RemoteKey remoteKey;
        public final Throwable e;
    }
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ShutdownEvent extends StorageEvent {}
    public interface ActionSummary {
        String name();
        String keys();
        static Copy copy(String keys) {
            return new Copy(keys);
        }
        static Upload upload(String keys) {
            return new Upload(keys);
        }
        static Delete delete(String keys) {
            return new Delete(keys);
        }
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        class Copy implements ActionSummary {
            public final String keys;
            @Override
            public String name() {
                return "Copy";
            }
            @Override
            public String keys() {
                return keys;
            }
        }
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        class Upload implements ActionSummary {
            public final String keys;
            @Override
            public String name() {
                return "Upload";
            }
            @Override
            public String keys() {
                return keys;
            }
        }
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        class Delete implements ActionSummary {
            public final String keys;
            @Override
            public String name() {
                return "Delete";
            }
            @Override
            public String keys() {
                return keys;
            }
        }
    }
}
