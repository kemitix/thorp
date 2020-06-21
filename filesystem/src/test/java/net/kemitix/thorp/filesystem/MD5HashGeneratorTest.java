package net.kemitix.thorp.filesystem;

import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.domain.MD5HashData;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class MD5HashGeneratorTest
        implements WithAssertions {
    @Nested
    @DisplayName("md5File")
    public class Md5File {
        @Test
        @DisplayName("read a file smaller than buffer")
        public void readSmallFile() throws IOException, NoSuchAlgorithmException {
            Path path = Resource.select(this, "upload/root-file").toPath();
            MD5Hash result = MD5HashGenerator.md5File(path);
            assertThat(result).isEqualTo(MD5HashData.Root.hash);
        }
        @Test
        @DisplayName("read a file larger than buffer")
        public void readLargeFile() throws IOException, NoSuchAlgorithmException {
            Path path = Resource.select(this, "big-file").toPath();
            MD5Hash result = MD5HashGenerator.md5File(path);
            assertThat(result).isEqualTo(MD5HashData.BigFile.hash);
        }
    }
    @Nested
    @DisplayName("md5FileChunk")
    public class Md5FileChunk {
        @Test
        @DisplayName("read first chunk of file")
        public void chunk1() throws IOException, NoSuchAlgorithmException {
            Path path = Resource.select(this, "big-file").toPath();
            MD5Hash result = MD5HashGenerator.md5FileChunk(path,
                    MD5HashData.BigFile.Part1.offset,
                    MD5HashData.BigFile.Part1.size);
            assertThat(result).isEqualTo(MD5HashData.BigFile.Part1.hash);
        }
        @Test
        @DisplayName("read second chunk of file")
        public void chunk2() throws IOException, NoSuchAlgorithmException {
            Path path = Resource.select(this, "big-file").toPath();
            MD5Hash result = MD5HashGenerator.md5FileChunk(path,
                    MD5HashData.BigFile.Part2.offset,
                    MD5HashData.BigFile.Part2.size);
            assertThat(result).isEqualTo(MD5HashData.BigFile.Part2.hash);
        }
    }
}
