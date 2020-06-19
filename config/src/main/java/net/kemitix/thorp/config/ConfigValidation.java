package net.kemitix.thorp.config;

import java.io.File;

@FunctionalInterface
public interface ConfigValidation {
    String errorMessage();

    static ConfigValidation sourceIsNotADirectory() {
        return () -> "Source must be a directory";
    }

    static ConfigValidation sourceIsNotReadable() {
        return () -> "Source must be readable";
    }

    static ConfigValidation bucketNameIsMissing() {
        return () -> "Bucket name is missing";
    }

    static ConfigValidation errorReadingFile(File file, String message) {
        return () -> String.format(
                "Error reading file '%s': %s",
                file, message);
    }
}
