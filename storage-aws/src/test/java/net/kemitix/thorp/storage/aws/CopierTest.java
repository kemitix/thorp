package net.kemitix.thorp.storage.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.MD5Hash;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.StorageEvent;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class CopierTest
        implements WithAssertions {

    private final Bucket bucket = Bucket.named("aBucket");
    private final RemoteKey sourceKey = RemoteKey.create("sourceKey");
    private final MD5Hash hash = MD5Hash.create("aHash");
    private final RemoteKey targetKey = RemoteKey.create("targetKey");
    private final CopyObjectRequest request = S3Copier.request(bucket, sourceKey, hash, targetKey);

    private final AmazonS3Client amazonS3Client;

    public CopierTest(@Mock AmazonS3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Test
    @DisplayName("copy produces copy event")
    public void copy_thenCopyEvent() {
        //given
        StorageEvent event = StorageEvent.copyEvent(sourceKey, targetKey);
        given(amazonS3Client.copyObject(request))
                .willReturn(Optional.of(new CopyObjectResult()));
        //when
        StorageEvent result = S3Copier.copier(amazonS3Client).apply(request);
        //then
        assertThat(result).isEqualTo(event);
    }

    @Test
    @DisplayName("when error copying then return an error storage event")
    public void whenCopyErrors_thenErrorEvent() {
        //given
        doThrow(SdkClientException.class)
                .when(amazonS3Client)
                .copyObject(request);
        //when
        StorageEvent result = S3Copier.copier(amazonS3Client).apply(request);
        //then
        assertThat(result).isInstanceOf(StorageEvent.ErrorEvent.class);
    }

}
