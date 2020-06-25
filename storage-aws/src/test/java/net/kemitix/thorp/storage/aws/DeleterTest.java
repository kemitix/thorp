package net.kemitix.thorp.storage.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.RemoteKey;
import net.kemitix.thorp.domain.StorageEvent;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class DeleterTest
        implements WithAssertions {

    private final Bucket bucket = Bucket.named("aBucket");
    private final RemoteKey remoteKey = RemoteKey.create("aRemoteKey");
    private final DeleteObjectRequest request = S3Deleter.request(bucket, remoteKey);

    private final AmazonS3Client amazonS3Client;

    public DeleterTest(@Mock AmazonS3Client amazonS3Client) {
        this.amazonS3Client = amazonS3Client;
    }

    @Test
    @DisplayName("when delete then return delete storage event")
    public void whenDelete_thenDeleteEvent() {
        //when
        StorageEvent result = S3Deleter.deleter(amazonS3Client).apply(request);
        //then
        assertThat(result).isEqualTo(StorageEvent.deleteEvent(remoteKey));
    }

    @Test
    @DisplayName("when error deleting then return an error storage event")
    public void whenDeleteErrors_thenErrorEvent() {
        //given
        doThrow(SdkClientException.class)
                .when(amazonS3Client)
                .deleteObject(request);
        //when
        StorageEvent result = S3Deleter.deleter(amazonS3Client).apply(request);
        //then
        assertThat(result).isInstanceOf(StorageEvent.ErrorEvent.class);
    }
}
