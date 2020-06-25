package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import net.kemitix.thorp.domain.*;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ListerTest
        implements WithAssertions {

    private final Bucket bucket = Bucket.named("aBucket");
    private final RemoteKey prefix = RemoteKey.create("aRemoteKey");
    private final AmazonS3Client amazonS3Client;

    public ListerTest(@Mock AmazonS3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Test
    @DisplayName("single fetch")
    public void singleFetch() {
        //given
        Date nowDate = new Date();
        String key = "key";
        String etag = "etag";
        Map<MD5Hash, RemoteKey> expectedHashMap =
                Collections.singletonMap(
                        MD5Hash.create(etag),
                        RemoteKey.create(key)
                );
        Map<RemoteKey, MD5Hash> expectedKeyMap =
                Collections.singletonMap(
                        RemoteKey.create(key),
                        MD5Hash.create(etag)
                );
        given(amazonS3Client.listObjects(any()))
                .willReturn(objectResults(nowDate, key, etag, false));
        //when
        RemoteObjects result = invoke(amazonS3Client, bucket, prefix);
        //then
        assertThat(result.byHash.asMap()).isEqualTo(expectedHashMap);
        assertThat(result.byKey.asMap()).isEqualTo(expectedKeyMap);
    }

    @Test
    @DisplayName("two fetches")
    public void twoFetches() {
        //given
        Date nowDate = new Date();
        String key1 = "key1";
        String etag1 = "etag1";
        String key2 = "key2";
        String etag2 = "etag2";
        Map<MD5Hash, RemoteKey> expectedHashMap = new HashMap<>();
        expectedHashMap.put(
                MD5Hash.create(etag1),
                RemoteKey.create(key1));
        expectedHashMap.put(
                MD5Hash.create(etag2),
                RemoteKey.create(key2));
        Map<RemoteKey, MD5Hash> expectedKeyMap = new HashMap<>();
        expectedKeyMap.put(
                RemoteKey.create(key1),
                MD5Hash.create(etag1));
        expectedKeyMap.put(
                RemoteKey.create(key2),
                MD5Hash.create(etag2));
        given(amazonS3Client.listObjects(any()))
                .willReturn(objectResults(nowDate, key1, etag1, true))
                .willReturn(objectResults(nowDate, key2, etag2, false));
        //when
        RemoteObjects result = invoke(amazonS3Client, bucket, prefix);
        //then
        assertThat(result.byHash.asMap()).isEqualTo(expectedHashMap);
        assertThat(result.byKey.asMap()).isEqualTo(expectedKeyMap);
    }

    private ListObjectsV2Result objectResults(
            Date nowDate,
            String key,
            String etag,
            boolean truncated
    ) {
        ListObjectsV2Result result = new ListObjectsV2Result();
        result.getObjectSummaries().add(objectSummary(key, etag, nowDate));
        result.setNextContinuationToken("next token");
        result.setTruncated(truncated);
        return result;
    }

    private S3ObjectSummary objectSummary(
            String key,
            String etag,
            Date lastModified
    ) {
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setKey(key);
        summary.setETag(etag);
        summary.setLastModified(lastModified);
        return summary;
    }

    private RemoteObjects invoke(
            AmazonS3Client amazonS3Client,
            Bucket bucket,
            RemoteKey prefix
    ) {
        return S3Lister.lister(amazonS3Client)
                .apply(S3Lister.request(bucket, prefix));
    }
}
