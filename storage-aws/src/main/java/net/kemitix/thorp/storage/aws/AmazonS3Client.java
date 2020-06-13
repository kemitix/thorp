package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;

import java.util.Optional;

public interface AmazonS3Client {
    void shutdown();
    void deleteObject(DeleteObjectRequest request);
    Optional<CopyObjectResult> copyObject(CopyObjectRequest request);
    ListObjectsV2Result listObjects(ListObjectsV2Request request);
    PutObjectResult uploadObject(PutObjectRequest request);

    static AmazonS3Client create(AmazonS3 amazonS3) {
        return new AmazonS3Client() {
            @Override
            public void shutdown() {
                amazonS3.shutdown();
            }
            @Override
            public void deleteObject(DeleteObjectRequest request) {
                amazonS3.deleteObject(request);
            }
            @Override
            public Optional<CopyObjectResult> copyObject(CopyObjectRequest request) {
                return Optional.of(amazonS3.copyObject(request));
            }
            @Override
            public ListObjectsV2Result listObjects(ListObjectsV2Request request) {
                return amazonS3.listObjectsV2(request);
            }
            @Override
            public PutObjectResult uploadObject(PutObjectRequest request) {
                return amazonS3.putObject(request);
            }
        };
    }
}
