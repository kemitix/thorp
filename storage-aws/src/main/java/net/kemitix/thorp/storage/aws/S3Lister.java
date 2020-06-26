package net.kemitix.thorp.storage.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.RemoteObjects;

import java.util.*;
import java.util.function.Function;

public interface S3Lister {
    static ListObjectsV2Request request(
            Bucket bucket,
            RemoteKey prefix
    ) {
        return new ListObjectsV2Request()
                .withBucketName(bucket.name())
                .withPrefix(prefix.key());
    }
    static Function<ListObjectsV2Request, RemoteObjects> lister(AmazonS3Client client) {
        return initialRequest -> {
            List<S3ObjectSummary> summaries = fetch(client, initialRequest);
            return RemoteObjects.create(
                    byHash(summaries),
                    byKey(summaries)
            );
        };
    }

    static Map<RemoteKey, MD5Hash> byKey(List<S3ObjectSummary> summaries) {
        Map<RemoteKey, MD5Hash> hashMap = new HashMap<>();
        summaries.forEach(
                summary ->
                        hashMap.put(
                                RemoteKey.create(summary.getKey()),
                                MD5Hash.create(summary.getETag())));
        return hashMap;
    }

    static Map<MD5Hash, RemoteKey> byHash(List<S3ObjectSummary> summaries) {
        Map<MD5Hash, RemoteKey> hashMap = new HashMap<>();
        summaries.forEach(
                summary ->
                        hashMap.put(
                                MD5Hash.create(summary.getETag()),
                                RemoteKey.create(summary.getKey())));
        return hashMap;
    }

    static Batch fetchBatch(AmazonS3Client client, ListObjectsV2Request request) {
        ListObjectsV2Result result = client.listObjects(request);
        return Batch.create(result.getObjectSummaries(), moreToken(result));
    }

    static List<S3ObjectSummary> fetchMore(
            AmazonS3Client client,
            ListObjectsV2Request request,
            Optional<String> token
    ) {
        return token
                .map(t -> fetch(client, request.withContinuationToken(t)))
                .orElseGet(Collections::emptyList);
    }

    static List<S3ObjectSummary> fetch(
            AmazonS3Client client,
            ListObjectsV2Request request
    ) {
        try {
            Batch batch = fetchBatch(client, request);
            List<S3ObjectSummary> more = fetchMore(client, request, batch.more);
            batch.summaries.addAll(more);
            return batch.summaries;
        } catch (SdkClientException e) {
            return Collections.emptyList();
        }
    };

    static Optional<String> moreToken(ListObjectsV2Result result) {
        if (result.isTruncated()) {
            return Optional.of(result.getNextContinuationToken());
        }
        return Optional.empty();
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Batch {
        final List<S3ObjectSummary> summaries;
        final Optional<String> more;
        static Batch create(List<S3ObjectSummary> summaries, Optional<String> more) {
            return new Batch(summaries, more);
        }
    }

}
