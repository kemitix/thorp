package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.model.DeleteObjectRequest;
import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.StorageEvent;

import java.util.function.Function;

public interface S3Deleter {
    static DeleteObjectRequest request(Bucket bucket, RemoteKey remoteKey) {
        return new DeleteObjectRequest(bucket.name(), remoteKey.key());
    }
    static Function<DeleteObjectRequest, StorageEvent> deleter(AmazonS3Client client) {
        return request -> {
            client.deleteObject(request);
            RemoteKey remoteKey = RemoteKey.create(request.getKey());
            return StorageEvent.deleteEvent(remoteKey);
        };
    }
}
