package net.kemitix.thorp.storage.aws;

import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.domain.Tuple;
import net.kemitix.thorp.filesystem.Resource;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class ETagGeneratorTest
        implements WithAssertions {

    public static final String BIG_FILE_ETAG = "f14327c90ad105244c446c498bfe9a7d-2";
    private final Resource bigFile = Resource.select(this, "big-file");
    private final long chunkSize = 1200000;
    private final S3ETagGenerator generator = new S3ETagGenerator();

    @Test
    @DisplayName("creates offsets")
    public void createsOffsets() {
        List<Long> offsets = generator.offsets(bigFile.length(), chunkSize);
        assertThat(offsets).containsExactly(
                0L, chunkSize, chunkSize * 2, chunkSize * 3, chunkSize  * 4);
    }

    @Test
    @DisplayName("generate valid hashes")
    public void generatesValidHashes() throws IOException, NoSuchAlgorithmException {
        List<Tuple<Integer, String>> md5Hashes = Arrays.asList(
                Tuple.create(0, "68b7d37e6578297621e06f01800204f1"),
                Tuple.create(1, "973475b14a7bda6ad8864a7f9913a947"),
                Tuple.create(2, "b9adcfc5b103fe2dd5924a5e5e6817f0"),
                Tuple.create(3, "5bd6e10a99fef100fe7bf5eaa0a42384"),
                Tuple.create(4, "8a0c1d0778ac8fcf4ca2010eba4711eb"));
        for (Tuple<Integer, String> t : md5Hashes) {
            long offset = t.a * chunkSize;
            MD5Hash md5Hash =
                    generator.hashChunk(bigFile.toPath(), offset, chunkSize);
            assertThat(md5Hash.getValue()).isEqualTo(t.b);
        }
    }

    @Test
    @DisplayName("create eTag for whole file")
    public void createTagForWholeFile() throws IOException, NoSuchAlgorithmException {
        String result = generator.withMultipartUploadThreshold(5 * 1024 * 1024)
                .hashFile(bigFile.toPath());
        assertThat(result).isEqualTo(BIG_FILE_ETAG);
    }
}
