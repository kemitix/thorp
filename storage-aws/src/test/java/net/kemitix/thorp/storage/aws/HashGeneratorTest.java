package net.kemitix.thorp.storage.aws;

import net.kemitix.thorp.domain.HashGenerator;
import net.kemitix.thorp.domain.Hashes;
import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.filesystem.MD5HashGenerator;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

public class HashGeneratorTest
        implements WithAssertions {

    @Test
    @DisplayName("load implementations")
    public void loadImplementations() {
        List<HashGenerator> all = HashGenerator.all();
        assertThat(all).hasSize(2);
        assertThat(all).hasAtLeastOneElementOfType(MD5HashGenerator.class);
        assertThat(all).hasAtLeastOneElementOfType(S3ETagGenerator.class);
    }

    @Nested
    @DisplayName("hashObject(Path)")
    public class HashObject {
        @Test
        @DisplayName("root-file")
        public void rootFile() throws IOException, NoSuchAlgorithmException {
            //given
            Path path = getResource("upload/root-file");
            //when
            Hashes result = HashGenerator.hashObject(path);
            //then
            assertThat(result.get(HashType.MD5)).contains(MD5HashData.rootHash());
        }
        @Test
        @DisplayName("leaf-file")
        public void leafFile() throws IOException, NoSuchAlgorithmException {
            //given
            Path path = getResource("upload/subdir/leaf-file");
            //when
            Hashes result = HashGenerator.hashObject(path);
            //then
            assertThat(result.get(HashType.MD5)).contains(MD5HashData.leafHash());
        }

        private Path getResource(String s) {
            return Paths.get(getClass().getResource(s).getPath());
        }
    }
}