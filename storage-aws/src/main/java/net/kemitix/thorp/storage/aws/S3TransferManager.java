package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;

import java.util.function.Function;

public interface S3TransferManager {
    void shutdownNow(boolean now);
    Function<PutObjectRequest, S3Upload> uploader();
    static S3TransferManager create(TransferManager transferManager) {
        return new S3TransferManager() {
            @Override
            public void shutdownNow(boolean now) {
                transferManager.shutdownNow(now);
            }
            @Override
            public Function<PutObjectRequest, S3Upload> uploader() {
                return request ->
                        S3Upload.inProgress(
                                transferManager.upload(request));
            }
        };
    }
}
