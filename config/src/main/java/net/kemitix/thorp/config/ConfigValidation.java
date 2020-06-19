package net.kemitix.thorp.config;

import java.io.File;

@FunctionalInterface
public interface ConfigValidation {
    String errorMessage();

    static ConfigValidation sourceIsNotADirectory(File file) {
        return () -> "Source must be a directory: " + file;
    }

    static ConfigValidation sourceIsNotReadable(File file) {
        return () -> "Source must be readable: " + file;
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
