package net.kemitix.thorp.storage.aws;

public class S3Exception extends RuntimeException {
    public S3Exception(String message) {
        super(message);
    }
    public S3Exception(String message, Throwable error) {
        super(message, error);
    }
    public static S3Exception hashError() {
        return new HashError();
    }
    public static S3Exception copyError(Throwable error) {
        return new CopyError(error);
    }
    public static class HashError extends S3Exception {
        private HashError() {
            super("The hash of the object to be overwritten did not match the the expected value");
        }
    }
    public static class CopyError extends S3Exception {
        private CopyError(Throwable error) {
            super("The hash of the object to be overwritten did not match the the expected value", error);
        }
    }
}
