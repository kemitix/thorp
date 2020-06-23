package net.kemitix.thorp.uishell;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

public interface UploadProgressEvent {

    static UploadProgressEvent transferEvent(String name) {
        return new TransferEvent(name);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class TransferEvent implements UploadProgressEvent {
        public final String name;
    }

    static UploadProgressEvent requestEvent(String name,
                                            long bytes,
                                            long transferred) {
        return new RequestEvent(name, bytes, transferred);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class RequestEvent implements UploadProgressEvent {
        public final String name;
        public final long bytes;
        public final long transferred;
    }

    static UploadProgressEvent bytesTransferEvent(String name) {
        return new TransferEvent(name);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class ByteTransferEvent implements UploadProgressEvent {
        public final String name;
    }
}
