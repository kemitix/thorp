package net.kemitix.thorp.config;

import net.kemitix.thorp.domain.Bucket;
import net.kemitix.thorp.domain.Sources;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface ConfigValidator {

    static Configuration validateConfig(Configuration config) throws ConfigValidationException {
        validateSources(config.sources);
        validateBucket(config.bucket);
        return config;
    }

    static void validateBucket(Bucket bucket) throws ConfigValidationException {
        if (bucket.name().isEmpty()) {
            throw new ConfigValidationException(
                    Collections.singletonList(
                            ConfigValidation.bucketNameIsMissing()));
        }
    }

    static void validateSources(Sources sources) throws ConfigValidationException {
        List<ConfigValidation> errors = new ArrayList<>();
        sources.paths().forEach(path ->
                errors.addAll(validateAsSource(path.toFile())));
        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
    }

    static Collection<? extends ConfigValidation> validateAsSource(File file) {
        if (!file.isDirectory())
            return Collections.singletonList(
                    ConfigValidation.sourceIsNotADirectory(file));
        if (!file.canRead())
            return Collections.singletonList(
                    ConfigValidation.sourceIsNotReadable(file));
        return Collections.emptyList();
    }
}
