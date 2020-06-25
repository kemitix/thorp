package net.kemitix.thorp.storage.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import net.kemitix.thorp.domain.HashType;
import net.kemitix.thorp.domain.*;
import net.kemitix.thorp.filesystem.Resource;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class UploaderTest
        implements WithAssertions {

    private final File aSource = Resource.select(this, "").toFile();
    private final File aFile = Resource.select(this, "small-file").toFile();
    private final MD5Hash aHash = MD5Hash.create("aHash");
    private final Hashes hashes = Hashes.create(HashType.MD5, aHash);
    private final RemoteKey remoteKey = RemoteKey.create("aRemoteKey");
    private final LocalFile localFile =
            LocalFile.create(aFile, aSource, hashes, remoteKey, aFile.length());
    private final Bucket bucket = Bucket.named("aBucket");
    private final PutObjectRequest request =
            S3Uploader.request(localFile, bucket);

    private final TransferManager transferManager;

    private final S3TransferManager s3TransferManager;

    private final UploadResult uploadResult;
    private final Upload upload;

    public UploaderTest(@Mock TransferManager transferManager,
                        @Mock Upload upload) {
        this.transferManager = transferManager;
        this.upload = upload;
        s3TransferManager = S3TransferManager.create(transferManager);
        uploadResult = new UploadResult();
        uploadResult.setKey(remoteKey.key());
        uploadResult.setETag(aHash.hash());
    }

    @Test
    @DisplayName("when upload then return upload event")
    public void whenUpload_thenUploadEvent() throws InterruptedException {
        //given
        given(transferManager.upload(request))
                .willReturn(upload);
        given(upload.waitForUploadResult()).willReturn(uploadResult);
        //when
        StorageEvent result = S3Uploader.uploader(s3TransferManager).apply(request);
        //then
        assertThat(result).isInstanceOf(StorageEvent.UploadEvent.class);
    }

    @Test
    @DisplayName("when error uploading then return an error storage event")
    public void whenUploadErrors_thenErrorEvent() {
        //given
        doThrow(SdkClientException.class)
                .when(transferManager)
                .upload(request);
        //when
        StorageEvent result = S3Uploader.uploader(s3TransferManager).apply(request);
        //then
        assertThat(result).isInstanceOf(StorageEvent.ErrorEvent.class);
    }

}
