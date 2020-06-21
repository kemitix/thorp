package net.kemitix.thorp.storage.aws;

import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

public interface S3Upload {
    UploadResult waitForUploadResult();
    static InProgress inProgress(Upload upload) {
        return new InProgress(upload);
    }
    static Errored errored(Throwable e) {
        return new Errored(e);
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class InProgress implements S3Upload {
        private final Upload upload;
        @Override
        public UploadResult waitForUploadResult() {
            try {
                return upload.waitForUploadResult();
            } catch (InterruptedException e) {
                throw S3Exception.uploadError(e);
            }
        }
    }
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class Errored implements S3Upload {
        private final Throwable error;
        @Override
        public UploadResult waitForUploadResult() {
            throw new RuntimeException(error);
        }
    }
}
