package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import net.kemitix.thorp.domain.*;

import java.util.function.Function;

public interface S3Uploader {
    static PutObjectRequest request(
            LocalFile localFile,
            Bucket bucket
    ) {
        return new PutObjectRequest(
                bucket.name(),
                localFile.remoteKey.key(),
                localFile.file
        ).withMetadata(metadata(localFile));
    }

    static ObjectMetadata metadata(LocalFile localFile) {
        ObjectMetadata metadata = new ObjectMetadata();
        localFile.md5base64().ifPresent(metadata::setContentMD5);
        return metadata;
    }

    static Function<PutObjectRequest, StorageEvent> uploader(
            S3TransferManager transferManager
    ) {
        return request -> {
            UploadResult uploadResult =
                    transferManager.uploader()
                            .apply(request)
                            .waitForUploadResult();
            return StorageEvent.uploadEvent(
                    RemoteKey.create(uploadResult.getKey()),
                    MD5Hash.create(uploadResult.getETag()));
        };
    }
}
