package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import lombok.With;
import net.kemitix.thorp.domain.HashGenerator;
import net.kemitix.thorp.domain.HashType;
import net.kemitix.thorp.domain.Hashes;
import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.filesystem.MD5HashGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static net.kemitix.thorp.filesystem.MD5HashGenerator.md5FileChunk;

@With
public class S3ETagGenerator implements HashGenerator {

    private final long multipartUploadThreshold;

    public S3ETagGenerator() {
        multipartUploadThreshold =
            new TransferManagerConfiguration().getMultipartUploadThreshold();
    }

    public S3ETagGenerator(long multipartUploadThreshold) {
        this.multipartUploadThreshold = multipartUploadThreshold;
    }

    @Deprecated // Use hashFile
    public String eTag(Path path) throws IOException, NoSuchAlgorithmException {
        return hashFile(path);
    }
    @Override
    public String hashFile(Path path) throws IOException, NoSuchAlgorithmException {
        long partSize = calculatePartSize();
        long parts = numParts(path.toFile().length(), partSize);
        String eTagHex = eTagHex(path, partSize, parts);
        return String.format("%s-%d", eTagHex, parts);
    }

    @Override
    public Hashes hash(Path path) throws IOException, NoSuchAlgorithmException {
        HashType key = hashType();
        MD5Hash value = MD5Hash.create(hashFile(path));
        return Hashes.create(key, value);
    }

    @Override
    public MD5Hash hashChunk(Path path, Long index, long partSize) throws IOException, NoSuchAlgorithmException {
        return HashGenerator.generatorFor("MD5").hashChunk(path, index, partSize);
    }

    public List<Long> offsets(long totalFileSizeBytes, long optimalPartSize) {
        return LongStream
                .rangeClosed(0, totalFileSizeBytes / optimalPartSize)
                .mapToObj(part -> part * optimalPartSize)
                .collect(Collectors.toList());
    }

    private long calculatePartSize() {
        return multipartUploadThreshold;
    }

    private long numParts(long length, long partSize) {
        long fullParts = Math.floorDiv(length, partSize);
        int incompleteParts = Math.floorMod(length, partSize) > 0
                ? 1
                : 0;
        return fullParts + incompleteParts;
    }

    private String eTagHex(Path path, long partSize, long parts) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (long i = 0; i < parts ; i++ ){
            bos.write(md5FileChunk(path, i * partSize, partSize).digest());
        }
        return MD5HashGenerator.hex(bos.toByteArray());
    }
    @Override
    public HashType hashType() {
        return net.kemitix.thorp.storage.aws.HashType.ETag;
    }
    @Override
    public String label() {
        return "ETag";
    }
}
