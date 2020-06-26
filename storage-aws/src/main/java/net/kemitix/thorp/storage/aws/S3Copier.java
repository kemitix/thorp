package net.kemitix.thorp.storage.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.StorageEvent;

import java.util.function.Function;

public interface S3Copier {
    static CopyObjectRequest request(
            Bucket bucket,
            RemoteKey sourceKey,
            MD5Hash hash,
            RemoteKey  targetKey
    ) {
        return new CopyObjectRequest(
                bucket.name(), sourceKey.key(),
                bucket.name(), targetKey.key()
        ).withMatchingETagConstraint(hash.hash());
    }
    static Function<CopyObjectRequest, StorageEvent> copier(AmazonS3Client client) {
        return request -> {
            RemoteKey sourceKey = RemoteKey.create(request.getSourceKey());
            RemoteKey targetKey = RemoteKey.create(request.getDestinationKey());
            try {
                return client.copyObject(request)
                        .map(success -> StorageEvent.copyEvent(sourceKey, targetKey))
                        .orElseGet(() -> StorageEvent.errorEvent(
                                actionSummary(sourceKey, targetKey),
                                targetKey, S3Exception.hashError()));
            } catch (SdkClientException e) {
                return StorageEvent.errorEvent(
                        actionSummary(sourceKey, targetKey), targetKey, e);
            }
        };
    }

    static StorageEvent.ActionSummary.Copy actionSummary(RemoteKey sourceKey, RemoteKey targetKey) {
        return StorageEvent.ActionSummary.copy(
                String.format("%s => %s", sourceKey.key(), targetKey.key()));
    }
}
