package net.kemitix.thorp.uishell;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.config.Configuration;
import net.kemitix.thorp.console.Console;
import net.kemitix.thorp.domain.LocalFile;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.StringUtil;
import net.kemitix.thorp.domain.Terminal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static net.kemitix.thorp.domain.SizeTranslation.sizeInEnglish;
import static net.kemitix.thorp.domain.Terminal.progressBar;

public class ProgressUI {

    static UploadState uploadState(long transferred, long fileLength) {
        return new UploadState(transferred, fileLength);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class UploadState {
        public final long transferred;
        public final long fileLength;
    }

    private static final AtomicReference<Map<RemoteKey, UploadState>> uploads =
            new AtomicReference<>(Collections.emptyMap());

    private static final int statusHeight = 2;

    public static void requestCycle(
            Configuration configuration,
            LocalFile localFile,
            long bytesTransferred,
            int index,
            long totalBytesSoFar
    ) {
        if (bytesTransferred < localFile.length) {
            stillUploading(
                    localFile.remoteKey,
                    localFile.length,
                    bytesTransferred
            );
        } else {
            finishedUploading(localFile.remoteKey);
        }
    }

    static void stillUploading(RemoteKey remoteKey,
                               long fileLength,
                               long bytesTransferred) {
        Map<RemoteKey, UploadState> current =
                uploads.updateAndGet(map -> {
                    HashMap<RemoteKey, UploadState> updated = new HashMap<>(map);
                    updated.put(
                            remoteKey,
                            uploadState(bytesTransferred, fileLength));
                    return updated;
                });
        String resetCursor = StringUtil.repeat(
                Terminal.cursorPrevLine(statusHeight), current.size());
        current.forEach((key, state) -> {
            String percent = String.format("%2d", (state.transferred * 100) / state.fileLength);
            String transferred = sizeInEnglish(state.transferred);
            String fileLength1 = sizeInEnglish(state.fileLength);
            String line1 = String.format("%sUploading%s: %s:%s",
                    Terminal.green, Terminal.reset,
                    key.key(),
                    Terminal.eraseLineForward);
            String line2body = String.format(
                    "%s%% %s or %s",
                    percent, transferred, fileLength1);
            String bar = progressBar(
                    state.transferred,
                    state.fileLength,
                    Terminal.width() - line2body.length());
            String line2 = String.join("",
                    Terminal.green, line2body, Terminal.reset,
                    bar, Terminal.eraseLineForward);
            Console.putStrLn(line1);
            Console.putStrLn(line2);
        });
        Console.putStr(resetCursor);
    }

    static void finishedUploading(RemoteKey remoteKey) {
        uploads.updateAndGet(map -> {
            Map<RemoteKey, UploadState> updated = new HashMap<>(map);
            updated.remove(remoteKey);
            return updated;
        });
    }
}
