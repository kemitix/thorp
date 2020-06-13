package net.kemitix.thorp.domain;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class RemoteKeyTest
        implements WithAssertions {

    private RemoteKey emptyKey = RemoteKey.create("");

    @Nested
    @DisplayName("Create a RemoteKey")
    public class CreateRemoteKey {
        @Nested
        @DisplayName("resolve()")
        public class ResolvePath {
            @Test
            @DisplayName("key is empty")
            public void keyIsEmpty() {
                //given
                RemoteKey expected = RemoteKey.create("path");
                RemoteKey key = emptyKey;
                //when
                RemoteKey result = key.resolve("path");
                //then
                assertThat(result).isEqualTo(expected);
            }
            @Test
            @DisplayName("path is empty")
            public void pathIsEmpty() {
                //given
                RemoteKey expected = RemoteKey.create("key");
                RemoteKey key = RemoteKey.create("key");
                String path = "";
                //when
                RemoteKey result = key.resolve(path);
                //then
                assertThat(result).isEqualTo(expected);
            }
            @Test
            @DisplayName("key and path are empty")
            public void keyAndPathEmpty() {
                //given
                RemoteKey expected = RemoteKeyTest.this.emptyKey;
                String path = "";
                RemoteKey key = emptyKey;
                //when
                RemoteKey result = key.resolve(path);
                //then
                assertThat(result).isEqualTo(expected);
            }
        }
        @Nested
        @DisplayName("asFile()")
        public class AsFile {
            @Test
            @DisplayName("key and prefix are non-empty")
            public void keyAndPrefixNonEmpty() {
                //given
                Optional<File> expected = Optional.of(new File("source/key"));
                RemoteKey key = RemoteKey.create("prefix/key");
                Path source = Paths.get("source");
                RemoteKey prefix = RemoteKey.create("prefix");
                //when
                Optional<File> result = key.asFile(source, prefix);
                //then
                assertThat(result).isEqualTo(expected);
            }
            @Test
            @DisplayName("prefix is empty")
            public void prefixEmpty() {
                //given
                Optional<File> expected = Optional.of(new File("source/key"));
                RemoteKey key = RemoteKey.create("key");
                Path source = Paths.get("source");
                RemoteKey prefix = emptyKey;
                //when
                Optional<File> result = key.asFile(source, prefix);
                //then
                assertThat(result).isEqualTo(expected);
            }

            @Test
            @DisplayName("key is empty")
            public void keyEmpty() {
                //given
                Optional<File> expected = Optional.empty();
                RemoteKey key = emptyKey;
                Path source = Paths.get("source");
                RemoteKey prefix = RemoteKey.create("source/key");
                //when
                Optional<File> result = key.asFile(source, prefix);
                //then
                assertThat(result).isEqualTo(expected);
            }

            @Test
            @DisplayName("key and prefix are empty")
            public void keyAndPrefixEmpty() {
                //given
                Optional<File> expected = Optional.empty();
                RemoteKey key = emptyKey;
                Path source = Paths.get("source");
                RemoteKey prefix = emptyKey;
                //when
                Optional<File> result = key.asFile(source, prefix);
                //then
                assertThat(result).isEqualTo(expected);
            }
        }
        @Nested
        @DisplayName("fromSourcePath()")
        public class FromSourcePath {
            @Test
            @DisplayName("path is in source")
            public void pathInSource() {
                //given
                RemoteKey expected = RemoteKey.create("child");
                Path source = Paths.get("/source");
                Path path = source.resolve("/source/child");
                //when
                RemoteKey result = RemoteKey.fromSourcePath(source, path);
                //then
                assertThat(result).isEqualTo(expected);
            }
        }
        @Nested
        @DisplayName("from(source, prefix, file)")
        public class FromSourcePrefixFile {
            @Test
            @DisplayName("file in source")
            public void fileInSource() {
                //given
                RemoteKey expected = RemoteKey.create("prefix/dir/filename");
                Path source = Paths.get("/source");
                RemoteKey prefix = RemoteKey.create("prefix");
                File file = new File("/source/dir/filename");
                //when
                RemoteKey result = RemoteKey.from(source, prefix, file);
                //then
                assertThat(result).isEqualTo(expected);
            }
        }
    }
    @Nested
    @DisplayName("asFile()")
    public class AsFile {
        @Test
        @DisplayName("remoteKey is empty")
        public void remoteKeyEmpty() {
            //given
            Optional<File> expected = Optional.empty();
            Path source = Paths.get("/source");
            RemoteKey prefix = RemoteKey.create("prefix");
            RemoteKey remoteKey = emptyKey;
            //when
            Optional<File> result = remoteKey.asFile(source, prefix);
            //then
            assertThat(result).isEqualTo(expected);
        }
        @Nested
        @DisplayName("remoteKey is not empty")
        public class RemoteKeyNotEmpty {
            @Test
            @DisplayName("remoteKey is within prefix")
            public void remoteKeyWithinPrefix() {
                //given
                Optional<File> expected = Optional.of(new File("/source/key"));
                Path source = Paths.get("/source");
                RemoteKey prefix = RemoteKey.create("prefix");
                RemoteKey remoteKey = RemoteKey.create("prefix/key");
                //when
                Optional<File> result = remoteKey.asFile(source, prefix);
                //then
                assertThat(result).isEqualTo(expected);
            }

            @Test
            @DisplayName("remoteKey is outwith prefix")
            public void remoteKeyIsOutwithPrefix() {
                //given
                Optional<File> expected = Optional.empty();
                Path source = Paths.get("/source");
                RemoteKey prefix = RemoteKey.create("prefix");
                RemoteKey remoteKey = RemoteKey.create("elsewhere/key");
                //when
                Optional<File> result = remoteKey.asFile(source, prefix);
                //then
                assertThat(result).isEqualTo(expected);
            }
        }
    }

}
