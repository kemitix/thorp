package net.kemitix.thorp.filesystem;

import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.Sources;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

public class FileSystemTest
        implements WithAssertions, TemporaryFolder {

    @Test
    @DisplayName("file exists")
    public void fileExists() throws IOException {
        withDirectory(dir -> {
            String filename = "filename";
            createFile(dir, filename, Collections.emptyList());
            RemoteKey remoteKey = RemoteKey.create(filename);
            Sources sources = Sources.create(Collections.singletonList(dir));
            RemoteKey prefix = RemoteKey.create("");
            boolean result = FileSystem.hasLocalFile(sources, prefix, remoteKey);
            assertThat(result).isTrue();
        });
    }
    @Test
    @DisplayName("file does not exist")
    public void fileNotExist() throws IOException {
        withDirectory(dir -> {
            String filename = "filename";
            RemoteKey remoteKey = RemoteKey.create(filename);
            Sources sources = Sources.create(Collections.singletonList(dir));
            RemoteKey prefix = RemoteKey.create("");
            boolean result = FileSystem.hasLocalFile(sources, prefix, remoteKey);
            assertThat(result).isFalse();
        });
    }
}
