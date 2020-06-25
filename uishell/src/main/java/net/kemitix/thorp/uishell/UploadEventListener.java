package net.kemitix.thorp.uishell;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.domain.Channel;
import net.kemitix.thorp.domain.LocalFile;
import net.kemitix.thorp.uishell.UploadProgressEvent.RequestEvent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class UploadEventListener {

    Consumer<UploadProgressEvent> listener(Settings settings) {
        AtomicLong bytesTransferred = new AtomicLong(0L);
        return event -> {
            if (event instanceof RequestEvent) {
                RequestEvent requestEvent = (RequestEvent) event;
                settings.uiSink.accept(
                        UIEvent.requestCycle(
                                settings.localFile,
                                bytesTransferred.addAndGet(requestEvent.transferred),
                                settings.index,
                                settings.totalBytesSoFar
                        )
                );
            }
        };
    }

    public static Settings settings(
            Channel.Sink<UIEvent> uiSink,
            LocalFile localFile,
            int index,
            long totalBytesToFar,
            boolean batchMode
    ) {
        return new Settings(uiSink, localFile, index, totalBytesToFar);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Settings {
        public final Channel.Sink<UIEvent> uiSink;
        public final LocalFile localFile;
        public final int index;
        public final long totalBytesSoFar;
    }
}
