package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.val;
import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.domain.RemoteKey;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ObjectsByHashTest
        implements WithAssertions {
    private S3ObjectSummary s3object(MD5Hash md5Hash,
                                     RemoteKey remoteKey) {
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setETag(md5Hash.hash());
        summary.setKey(remoteKey.key());
        return summary;
    }

    @Test
    @DisplayName("grouping s3 objects together by their hash value")
    public void groupS3ObjectsByHashValue() {
        //given
        MD5Hash hash = MD5Hash.create("hash");
        RemoteKey key1 = RemoteKey.create("key-1");
        RemoteKey key2 = RemoteKey.create("key-2");
        S3ObjectSummary o1 = s3object(hash, key1);
        S3ObjectSummary o2 = s3object(hash, key2);
        List<S3ObjectSummary> os = Arrays.asList(o1, o2);
        Map<MD5Hash, RemoteKey> expected = Collections.singletonMap(
                hash, key2);
        //when
        Map<MD5Hash, RemoteKey> result = S3Lister.byHash(os);
        //then
        assertThat(result).isEqualTo(expected);
    }

}
